/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.googlejavaformat.java;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Preconditions;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.google.common.io.CharStreams;
import com.google.googlejavaformat.FormatterDiagnostic;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.Nullable;

/**
 * A {@link Callable} that formats a file.
 */
// TODO(eaftan): Consider returning the output instead of writing it in the callable.  This way
// we could serialize the output to make sure it is presented in the correct order (b/21335725),
// and we could avoid passing a lock around.
class FormatFileCallable implements Callable<Boolean> {
  private final FileToFormat fileToFormat;
  private final Object outputLock;
  private final int indentMultiplier;
  private final boolean inPlace;
  private final SortImports sortImports;
  private final PrintWriter outWriter;
  private final PrintWriter errWriter;

  FormatFileCallable(
      FileToFormat fileToFormat,
      Object outputLock,
      int indentMultiplier,
      boolean inPlace,
      SortImports sortImports,
      PrintWriter outWriter,
      PrintWriter errWriter) {
    Preconditions.checkArgument(
        !(inPlace && fileToFormat instanceof FileToFormatStdin),
        "Cannot format stdin in place");

    this.fileToFormat = Preconditions.checkNotNull(fileToFormat);
    this.outputLock = Preconditions.checkNotNull(outputLock);
    this.indentMultiplier = indentMultiplier;
    this.inPlace = inPlace;
    this.sortImports = sortImports;
    this.outWriter = Preconditions.checkNotNull(outWriter);
    this.errWriter = Preconditions.checkNotNull(errWriter);
  }

  enum SortImports {
    NO,
    ONLY,
    ALSO
  }

  /**
   * Formats a file and returns whether the operation succeeded.
   */
  @Override
  public Boolean call() {
    String inputString = readInput();
    if (inputString == null) {
      return false;
    }

    if (sortImports != SortImports.NO) {
      String reordered = reorderImports(inputString);
      if (reordered == null) {
        return false;
      }

      if (sortImports == SortImports.ONLY) {
        if (reordered.equals(inputString)) {
          return true;
        }
        return writeString(reordered);
      }

      inputString = reordered;
    }

    JavaInput javaInput;
    final RangeSet<Integer> tokens;
    try {
      javaInput = new JavaInput(fileToFormat.fileName(), inputString);
      tokens = TreeRangeSet.create();
      for (Range<Integer> lineRange : fileToFormat.lineRanges().asRanges()) {
        tokens.add(javaInput.lineRangeToTokenRange(lineRange));
      }
      for (int i = 0; i < fileToFormat.offsets().size(); i++) {
        tokens.add(
            javaInput.characterRangeToTokenRange(
                fileToFormat.offsets().get(i), fileToFormat.lengths().get(i)));
      }
    } catch (FormatterException e) {
      synchronized (outputLock) {
        errWriter
            .append(fileToFormat.fileName())
            .append(": error: ")
            .append(e.getMessage())
            .append('\n')
            .flush();
      }
      return false;
    }

    if (tokens.isEmpty()) {
      if (fileToFormat.lineRanges().asRanges().isEmpty() && fileToFormat.offsets().isEmpty()) {
        tokens.add(Range.<Integer>all());
      }
    }

    final JavaOutput javaOutput = new JavaOutput(javaInput, new JavaCommentsHelper());
    List<FormatterDiagnostic> errors = new ArrayList<>();
    Formatter.format(javaInput, javaOutput, Formatter.MAX_WIDTH, errors, indentMultiplier);
    if (!errors.isEmpty()) {
      synchronized (outputLock) {
        for (FormatterDiagnostic error : errors) {
          errWriter.println(error.toString());
        }
      }
      return false;
    }

    Write writeTokens =
        new Write() {
          @Override
          public void write(Writer writer) throws IOException {
            javaOutput.writeMerged(writer, tokens);
          }
        };
    return writeOutput(writeTokens);
  }

  @Nullable
  private String readInput() {
    try (InputStream in = fileToFormat.inputStream()) {
      return CharStreams.toString(new InputStreamReader(in, StandardCharsets.UTF_8));
      // The filename in the JavaInput is only used to create diagnostics, so it is safe to
      // pass in a synthetic filename like "<stdin>".
    } catch (IOException e) {
      synchronized (outputLock) {
        errWriter
            .append(fileToFormat.fileName())
            .append(": could not read file: ")
            .append(e.getMessage())
            .append('\n')
            .flush();
      }
      return null;
    }
  }

  @Nullable
  private String reorderImports(String inputString) {
    try {
      return ImportOrderer.reorderImports(fileToFormat.fileName(), inputString);
    } catch (FormatterException e) {
      synchronized (outputLock) {
        errWriter
            .append(fileToFormat.fileName())
            .append(": error sorting imports: ")
            .append(e.getMessage())
            .append('\n')
            .flush();
      }
      return null;
    }
  }

  interface Write {
    void write(Writer writer) throws IOException;
  }

  private boolean writeString(final String s) {
    return writeOutput(
        new Write() {
          @Override
          public void write(Writer writer) throws IOException {
            writer.write(s);
          }
        });
  }

  private boolean writeOutput(Write write) {
    if (!inPlace) {
      synchronized (outputLock) {
        try {
          write.write(outWriter);
        } catch (IOException e) {
          errWriter.append("cannot write output: " + e.getMessage()).flush();
        }
        outWriter.flush();
        return true;
      }
    } else {
      String tempFileName = fileToFormat.fileName() + '#';
      try (Writer writer =
          new OutputStreamWriter(
              new BufferedOutputStream(new FileOutputStream(tempFileName)),
              UTF_8)) {
        write.write(writer);
        outWriter.flush();
      } catch (IOException e) {
        synchronized (outputLock) {
          errWriter
              .append(tempFileName)
              .append(": cannot write temp file: ")
              .append(e.getMessage())
              .append('\n')
              .flush();
        }
        return false;
      }
      if (!new File(tempFileName).renameTo(new File(fileToFormat.fileName()))) {
        synchronized (outputLock) {
          errWriter.append(tempFileName).append(": cannot rename temp file").append('\n').flush();
        }
        return false;
      }
      return true;
    }
  }
}
