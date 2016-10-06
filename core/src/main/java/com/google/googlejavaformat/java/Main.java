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

import com.google.common.io.ByteStreams;
import com.google.googlejavaformat.FormatterDiagnostic;
import com.google.googlejavaformat.java.JavaFormatterOptions.Style;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** The main class for the Java formatter CLI. */
public final class Main {
  private static final int MAX_THREADS = 20;
  private static final String STDIN_FILENAME = "<stdin>";

  static final String[] VERSION = {
    "google-java-format: Version " + GoogleJavaFormatVersion.VERSION
  };

  private final PrintWriter outWriter;
  private final PrintWriter errWriter;
  private final InputStream inStream;

  public Main(PrintWriter outWriter, PrintWriter errWriter, InputStream inStream) {
    this.outWriter = outWriter;
    this.errWriter = errWriter;
    this.inStream = inStream;
  }

  /**
   * The main method for the formatter, with some number of file names to format. We process them in
   * parallel, but we must be careful; if multiple file names refer to the same file (which is hard
   * to determine), we must serialize their updates.
   *
   * @param args the command-line arguments
   */
  public static void main(String[] args) {
    int result;
    PrintWriter out = new PrintWriter(new OutputStreamWriter(System.out, UTF_8));
    PrintWriter err = new PrintWriter(new OutputStreamWriter(System.err, UTF_8));
    try {
      Main formatter = new Main(out, err, System.in);
      result = formatter.format(args);
    } catch (UsageException e) {
      err.print(e.getMessage());
      result = 0;
    } finally {
      err.flush();
      out.flush();
    }
    System.exit(result);
  }

  /**
   * The main entry point for the formatter, with some number of file names to format. We process
   * them in parallel, but we must be careful; if multiple file names refer to the same file (which
   * is hard to determine), we must serialize their update.
   *
   * @param args the command-line arguments
   */
  public int format(String... args) throws UsageException {
    CommandLineOptions parameters = processArgs(args);
    if (parameters.version()) {
      for (String line : VERSION) {
        errWriter.println(line);
      }
      return 0;
    }
    if (parameters.help()) {
      throw new UsageException();
    }

    JavaFormatterOptions options =
        JavaFormatterOptions.builder().style(parameters.aosp() ? Style.AOSP : Style.GOOGLE).build();

    if (parameters.stdin()) {
      return formatStdin(parameters, options);
    } else {
      return formatFiles(parameters, options);
    }
  }

  private int formatFiles(CommandLineOptions parameters, JavaFormatterOptions options) {
    int numThreads = Math.min(MAX_THREADS, parameters.files().size());
    ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

    Map<Path, String> inputs = new LinkedHashMap<>();
    Map<Path, Future<String>> results = new LinkedHashMap<>();
    for (String fileName : parameters.files()) {
      if (!fileName.endsWith(".java")) {
        errWriter.println("Skipping non-Java file: " + fileName);
        continue;
      }
      Path path = Paths.get(fileName);
      String input;
      try {
        input = new String(Files.readAllBytes(path), UTF_8);
      } catch (IOException e) {
        errWriter.println(fileName + ": could not read file: " + e.getMessage());
        return 1;
      }
      inputs.put(path, input);
      results.put(path, executorService.submit(new FormatFileCallable(parameters, input, options)));
    }

    boolean allOk = true;
    for (Map.Entry<Path, Future<String>> result : results.entrySet()) {
      String formatted;
      try {
        formatted = result.getValue().get();
      } catch (InterruptedException e) {
        errWriter.println(e.getMessage());
        allOk = false;
        continue;
      } catch (ExecutionException e) {
        if (e.getCause() instanceof FormatterException) {
          for (FormatterDiagnostic diagnostic : ((FormatterException) e.getCause()).diagnostics()) {
            errWriter.println(result.getKey() + ":" + diagnostic.toString());
          }
        } else {
          errWriter.println(result.getKey() + ": error: " + e.getCause().getMessage());
          e.getCause().printStackTrace(errWriter);
        }
        allOk = false;
        continue;
      }
      if (parameters.inPlace()) {
        if (formatted.equals(inputs.get(result.getKey()))) {
          continue; // preserve original file
        }
        try {
          Files.write(result.getKey(), formatted.getBytes(UTF_8));
        } catch (IOException e) {
          errWriter.println(result.getKey() + ": could not write file: " + e.getMessage());
          allOk = false;
          continue;
        }
      } else {
        outWriter.write(formatted);
      }
    }
    return allOk ? 0 : 1;
  }

  private int formatStdin(CommandLineOptions parameters, JavaFormatterOptions options) {
    String input;
    try {
      input = new String(ByteStreams.toByteArray(inStream), UTF_8);
    } catch (IOException e) {
      throw new IOError(e);
    }
    try {
      String output = new FormatFileCallable(parameters, input, options).call();
      outWriter.write(output);
      return 0;
    } catch (FormatterException e) {
      for (FormatterDiagnostic diagnostic : e.diagnostics()) {
        errWriter.println(STDIN_FILENAME + ":" + diagnostic.toString());
      }
      return 1;
      // TODO(cpovirk): Catch other types of exception (as we do in the formatFiles case).
    }
  }

  /** Parses and validates command-line flags. */
  public static CommandLineOptions processArgs(String... args) throws UsageException {
    CommandLineOptions parameters;
    try {
      parameters = CommandLineOptionsParser.parse(Arrays.asList(args));
    } catch (IllegalArgumentException e) {
      throw new UsageException(e.getMessage());
    } catch (Throwable t) {
      t.printStackTrace();
      throw new UsageException(t.getMessage());
    }
    int filesToFormat = parameters.files().size();
    if (parameters.stdin()) {
      filesToFormat++;
    }

    if (parameters.inPlace() && parameters.files().isEmpty()) {
      throw new UsageException("in-place formatting was requested but no files were provided");
    }
    if (parameters.isSelection() && filesToFormat != 1) {
      throw new UsageException("partial formatting is only support for a single file");
    }
    if (parameters.offsets().size() != parameters.lengths().size()) {
      throw new UsageException(
          String.format("-offsets and -lengths flags must be provided in matching pairs"));
    }
    if (filesToFormat <= 0 && !parameters.version() && !parameters.help()) {
      throw new UsageException("no files were provided");
    }
    return parameters;
  }
}
