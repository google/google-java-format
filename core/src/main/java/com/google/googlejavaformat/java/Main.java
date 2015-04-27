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

import com.google.common.base.Splitter;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeMultimap;
import com.google.common.collect.TreeRangeSet;
import com.google.common.io.CharStreams;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

import java.io.File;
import java.io.FileInputStream;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.CheckReturnValue;

/**
 * The main class for the Java formatter CLI.
 */
public final class Main {
  private static final int MAX_THREADS = 20;
  private static final Splitter COMMA_SPLITTER = Splitter.on(',');
  private static final Splitter COLON_SPLITTER = Splitter.on(':');

  @Parameters(separators = "=")
  private static final class FormatterParameters {
    @Parameter(
        names = {"-i", "-r", "-replace", "--replace"},
        description = "Send formatted output back to files, not stdout.")
    boolean iFlag = false;

    @Parameter(
        names = {"--lines", "-lines", "--line", "-line"},
        description = "Line range(s) to format, like 5:10 (1-based; default is all).")
    final List<String> linesFlags = new ArrayList<>();

    @Parameter(
        names = {"--offset", "-offset"},
        description = "Character offset to format (0-based; default is all).")
    private final List<Integer> offsetFlags = new ArrayList<>();

    @Parameter(names = {"--length", "-length"}, description = "Character length to format.")
    private final List<Integer> lengthFlags = new ArrayList<>();

    @Parameter(
        names = {"--indent", "-indent", "-t"},
        description = "The atomic unit of indentation (default is 2).")
    Integer indentFlag = 2;

    @Parameter(
        names = {"--addComments", "-addComments"},
        description = "Insert diagnostics as comments in output.")
    boolean addCommentsFlag = false;

    @Parameter(names = {"--version", "-version", "-v"}, description = "Print the version.")
    boolean versionFlag = false;

    @Parameter(
        names = {"--help", "-help", "-h"}, description = "Print an extended usage statement.")
    boolean helpFlag = false;

    @Parameter(names = "-", description = "Format stdin -> stdout.")
    boolean stdinStdoutFlag = false;

    @Parameter(description = "file(s)")
    final List<String> fileNamesFlag = new ArrayList<>();
  }

  private static final String[] VERSION =
      {"google-java-format: Version " + GoogleJavaFormatVersion.VERSION};

  private static final String[] ADDITIONAL_USAGE = {
      "The --indent value must be a positive multiple of 2.",
      "If -i is given with -, the result is sent to stdout.",
      "The --lines, --offset, and --length flags may be given more than once.",
      "The --offset and --length flags must be given an equal number of times.",
      "If --lines, --offset, or --length are given, only one file (or -) may be given."
  };

  private final PrintWriter outWriter;
  private final PrintWriter errWriter;

  public Main(PrintWriter outWriter, PrintWriter errWriter) {
    this.outWriter = outWriter;
    this.errWriter = errWriter;
  }

  /**
   * The main method for the formatter, with some number of file names to format. We process them in
   * parallel, but we must be careful; if multiple file names refer to the same file (which is hard
   * to determine), we must serialize their updates.
   * @param args the command-line arguments
   */
  public static void main(String... args) {
    Main formatter = new Main(new PrintWriter(System.out, true), new PrintWriter(System.err, true));
    try {
      int result = formatter.format(args);
      System.exit(result);
    } catch (UsageException e) {
      System.err.print(e.usage());
    }
  }

  /**
   * The main entry point for the formatter, with some number of file names to format. We process
   * them in parallel, but we must be careful; if multiple file names refer to the same file (which
   * is hard to determine), we must serialize their update.
   * @param args the command-line arguments
   */
  public int format(String... args) throws UsageException {
    FormatterParameters parameters = new FormatterParameters();
    JCommander jCommander = new JCommander(parameters);
    jCommander.setProgramName("google-java-format");
    try {
      jCommander.parse(args);
    } catch (ParameterException ignored) {
      throwUsage(jCommander);
    }
    if (parameters.iFlag && parameters.fileNamesFlag.isEmpty()) {
      throwUsage(jCommander);
    }
    if (!(parameters.linesFlags.isEmpty() && parameters.offsetFlags.isEmpty()
            && parameters.lengthFlags.isEmpty()
        || parameters.fileNamesFlag.size() == 1)) {
      throwUsage(jCommander);
    }
    if (parameters.offsetFlags.size() != parameters.lengthFlags.size()) {
      throwUsage(jCommander);
    }
    if (parameters.fileNamesFlag.isEmpty() && !parameters.versionFlag && !parameters.helpFlag) {
      throwUsage(jCommander);
    }
    if (parameters.versionFlag) {
      version();
    }
    if (parameters.helpFlag) {
      throwUsage(jCommander);
    }
    final Multimap<String, String> filesByBasename = TreeMultimap.create();
    for (String fileName : parameters.fileNamesFlag) {
      if (fileName.endsWith(".java")) {
        filesByBasename.put(new File(fileName).getName(), fileName);
      } else {
        errWriter.println("Skipping non-Java file: " + fileName);
      }
    }
    if (parameters.stdinStdoutFlag) {
      filesByBasename.put("-", "-");
    }
    List<Future<Boolean>> results = new ArrayList<>();
    ExecutorService executorService = Executors.newFixedThreadPool(MAX_THREADS);
    final boolean iFlagFinal = parameters.iFlag;
    final boolean addCommentsFlagFinal = parameters.addCommentsFlag;
    final int indentMultiplierFlagFinal = parameters.indentFlag / 2;
    final List<String> linesFlagsFinal = parameters.linesFlags;
    final List<Integer> offsetFlagsFinal = parameters.offsetFlags;
    final List<Integer> lengthFlagsFinal = parameters.lengthFlags;
    final Object mutex = new Object();
    for (final String baseName : filesByBasename.keySet()) {
      results.add(
          executorService.submit(
              new Callable<Boolean>() {
                @Override
                public Boolean call() {
                  boolean theseOkay = true;
                  for (String fileName : filesByBasename.get(baseName)) {
                    try (InputStream in =
                             fileName.equals("-") ? System.in : new FileInputStream(fileName)) {
                      String stringFromStream =
                          CharStreams.toString(new InputStreamReader(in, StandardCharsets.UTF_8));
                      JavaInput javaInput = new JavaInput(stringFromStream);
                      JavaOutput javaOutput =
                          new JavaOutput(
                              javaInput, new JavaCommentsHelper(),
                              addCommentsFlagFinal);
                      RangeSet<Integer> lines = TreeRangeSet.create();
                      for (String line : linesFlagsFinal) {
                        lines.addAll(parseRangeSet(line));
                      }
                      for (int i = 0; i < offsetFlagsFinal.size(); i++) {
                        lines.add(
                            javaInput.characterRangeToLineRange(
                                offsetFlagsFinal.get(i), lengthFlagsFinal.get(i)));
                      }
                      if (lines.isEmpty()) {
                        lines.add(Range.<Integer>all());
                      }
                      List<String> errors = new ArrayList<>();
                      Formatter.format(
                          javaInput, javaOutput, Formatter.MAX_WIDTH, errors,
                          indentMultiplierFlagFinal);
                      theseOkay &= errors.isEmpty();
                      synchronized (mutex) {
                        for (String error : errors) {
                          errWriter.println(
                              (fileName.equals("-") ? "<stdin>" : fileName) + ": " + error);
                        }
                      }
                      if (!iFlagFinal || fileName.equals("-")) {
                        synchronized (mutex) {
                          javaOutput.writeMerged(outWriter, lines, Formatter.MAX_WIDTH, errors);
                          outWriter.flush();
                        }
                      } else {
                        String tempFileName = fileName + '#';
                        try (Writer writer =
                            new OutputStreamWriter(new FileOutputStream(tempFileName), UTF_8)) {
                          javaOutput.writeMerged(writer, lines, Formatter.MAX_WIDTH, errors);
                          outWriter.flush();
                        } catch (IOException e) {
                          synchronized (mutex) {
                            errWriter.append(tempFileName)
                                .append(": cannot write temp file: ")
                                .append(e.getMessage())
                                .append('\n')
                                .flush();
                          }
                          theseOkay = false;
                          continue;
                        }
                        if (!new File(tempFileName).renameTo(new File(fileName))) {
                          synchronized (mutex) {
                            errWriter.append(tempFileName)
                                .append(": cannot rename temp file")
                                .append('\n')
                                .flush();
                          }
                          theseOkay = false;
                        }
                      }
                    } catch (IOException e) {
                      synchronized (mutex) {
                        errWriter.append(fileName)
                            .append(": could not read file: ")
                            .append(e.getMessage())
                            .append('\n')
                            .flush();
                      }
                      theseOkay = false;
                    } catch (FormatterException e) {
                      synchronized (mutex) {
                        errWriter.append(fileName)
                            .append(": lexing error: ")
                            .append(e.getMessage())
                            .append('\n')
                            .flush();
                      }
                      theseOkay = false;
                    } catch (RuntimeException e) {
                      e.printStackTrace();
                      theseOkay = false;
                    }
                  }
                  return theseOkay;
                }
              }));
    }
    boolean allOkay = true;
    for (Future<Boolean> result : results) {
      try {
        allOkay &= result.get();
      } catch (InterruptedException | ExecutionException e) {
        synchronized (mutex) {
          errWriter.println(e.getMessage());
        }
        allOkay = false;
      }
    }
    return allOkay ? 0 : 1;
  }

  /**
   * Parse --lines flag, like "1:12,14,20:36". Multiple ranges can be given with multiple --lines
   * flags, or separated by commas. A single line can be set by a single number. Line numbers are
   * {@code 1}-based, but are converted to the {@code 0}-based numbering used internally by
   * google-java-format.
   * @param arg the command-line flag
   * @return the {@link RangeSet} of line numbers, converted to {@link 0}-based
   */
  private static RangeSet<Integer> parseRangeSet(String arg) {
    RangeSet<Integer> result = TreeRangeSet.create();
    for (String range : COMMA_SPLITTER.split(arg)) {
      result.add(parseRange(range));
    }
    return result;
  }

  /**
   * Parse a range, as in "1:12" or "42". Line numbers provided are {@code 1}-based, but are
   * converted here to {@code 0}-based.
   * @param arg the command-line argument
   * @return the {@link RangeSet} of line numbers, converted to {@link 0}-based
   */
  private static Range<Integer> parseRange(String arg) {
    List<String> args = COLON_SPLITTER.splitToList(arg);
    switch (args.size()) {
      case 1:
        int line = Integer.parseInt(args.get(0)) - 1;
        return Range.closedOpen(line, line + 1);
      case 2:
        int line0 = Integer.parseInt(args.get(0)) - 1;
        int line1 = Integer.parseInt(args.get(1)) - 1;
        return Range.closedOpen(line0, line1 + 1);
      default:
        throw new IllegalArgumentException(arg);
    }
  }

  private void version() {
    for (String line : VERSION) {
      errWriter.println(line);
    }
  }

  /** Throws a {@link UsageException} containing a usage message. */
  private void throwUsage(JCommander jCommander) throws UsageException {
    throw new UsageException(usage(jCommander));
  }

  /** Returns a usage message. */
  @CheckReturnValue
  private String usage(JCommander jCommander) {
    StringBuilder builder = new StringBuilder();
    jCommander.usage(builder);
    for (String line : ADDITIONAL_USAGE) {
      builder.append(line).append(System.lineSeparator());
    }
    return builder.toString();
  }
}
