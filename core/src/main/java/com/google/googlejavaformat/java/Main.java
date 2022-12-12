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

import com.google.common.io.ByteStreams;
import com.google.googlejavaformat.FormatterDiagnostic;
import com.google.googlejavaformat.java.JavaFormatterOptions.Style;

import java.io.*;
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

import static java.lang.Math.min;
import static java.nio.charset.StandardCharsets.UTF_8;

/** The main class for the Java formatter CLI. */
public final class Main {
  private static final int MAX_THREADS = 20;
  private static final String STDIN_FILENAME = "<stdin>";

  static String versionString() {
    return "google-java-format: Version " + GoogleJavaFormatVersion.version();
  }

  private final PrintWriter outWriter;
  private final PrintWriter errWriter;
  private final InputStream inStream;

  public Main(final PrintWriter outWriter, final PrintWriter errWriter, final InputStream inStream) {
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
  public static void main(final String[] args) {
    final PrintWriter out = new PrintWriter(new OutputStreamWriter(System.out, UTF_8));
    final PrintWriter err = new PrintWriter(new OutputStreamWriter(System.err, UTF_8));
    final int result = main(out, err, args);
    System.exit(result);
  }

  /**
   * Package-private main entry point used this CLI program and the java.util.spi.ToolProvider
   * implementation in the same package as this Main class.
   */
  static int main(final PrintWriter out, final PrintWriter err, final String... args) {
    try {
      final Main formatter = new Main(out, err, System.in);
      return formatter.format(args);
    } catch (final UsageException e) {
      err.print(e.getMessage());
      return 0;
    } finally {
      err.flush();
      out.flush();
    }
  }

  /**
   * The main entry point for the formatter, with some number of file names to format. We process
   * them in parallel, but we must be careful; if multiple file names refer to the same file (which
   * is hard to determine), we must serialize their update.
   *
   * @param args the command-line arguments
   */
  public int format(final String... args) throws UsageException {
    final CommandLineOptions parameters = processArgs(args);
    if (parameters.version()) {
      errWriter.println(versionString());
      return 0;
    }
    if (parameters.help()) {
      throw new UsageException();
    }

    final JavaFormatterOptions options =
        JavaFormatterOptions.builder()
            .style(parameters.aosp() ? Style.AOSP : Style.GOOGLE)
            .formatJavadoc(parameters.formatJavadoc())
                .maxLineWidth(parameters.width())
            .build();

    if (parameters.stdin()) {
      return formatStdin(parameters, options);
    } else {
      return formatFiles(parameters, options);
    }
  }

  private int formatFiles(final CommandLineOptions parameters, final JavaFormatterOptions options) {
    final int numThreads = min(MAX_THREADS, parameters.files().size());
    final ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

    final Map<Path, String> inputs = new LinkedHashMap<>();
    final Map<Path, Future<String>> results = new LinkedHashMap<>();
    boolean allOk = true;

    for (final String fileName : parameters.files()) {
      if (!fileName.endsWith(".java")) {
        errWriter.println("Skipping non-Java file: " + fileName);
        continue;
      }
      final Path path = Paths.get(fileName);
      final String input;
      try {
        input = new String(Files.readAllBytes(path), UTF_8);
        inputs.put(path, input);
        results.put(
            path, executorService.submit(new FormatFileCallable(parameters, input, options)));
      } catch (final IOException e) {
        errWriter.println(fileName + ": could not read file: " + e.getMessage());
        allOk = false;
      }
    }

    for (final Map.Entry<Path, Future<String>> result : results.entrySet()) {
      final Path path = result.getKey();
      final String formatted;
      try {
        formatted = result.getValue().get();
      } catch (final InterruptedException e) {
        errWriter.println(e.getMessage());
        allOk = false;
        continue;
      } catch (final ExecutionException e) {
        if (e.getCause() instanceof FormatterException) {
          for (final FormatterDiagnostic diagnostic : ((FormatterException) e.getCause()).diagnostics()) {
            errWriter.println(path + ":" + diagnostic);
          }
        } else {
          errWriter.println(path + ": error: " + e.getCause().getMessage());
          e.getCause().printStackTrace(errWriter);
        }
        allOk = false;
        continue;
      }
      final boolean changed = !formatted.equals(inputs.get(path));
      if (changed && parameters.setExitIfChanged()) {
        allOk = false;
      }
      if (parameters.inPlace()) {
        if (!changed) {
          continue; // preserve original file
        }
        try {
          Files.write(path, formatted.getBytes(UTF_8));
        } catch (final IOException e) {
          errWriter.println(path + ": could not write file: " + e.getMessage());
          allOk = false;
          continue;
        }
      } else if (parameters.dryRun()) {
        if (changed) {
          outWriter.println(path);
        }
      } else {
        outWriter.write(formatted);
      }
    }
    return allOk ? 0 : 1;
  }

  private int formatStdin(final CommandLineOptions parameters, final JavaFormatterOptions options) {
    final String input;
    try {
      input = new String(ByteStreams.toByteArray(inStream), UTF_8);
    } catch (final IOException e) {
      throw new IOError(e);
    }
    final String stdinFilename = parameters.assumeFilename().orElse(STDIN_FILENAME);
    boolean ok = true;
    try {
      final String output = new FormatFileCallable(parameters, input, options).call();
      final boolean changed = !input.equals(output);
      if (changed && parameters.setExitIfChanged()) {
        ok = false;
      }
      if (parameters.dryRun()) {
        if (changed) {
          outWriter.println(stdinFilename);
        }
      } else {
        outWriter.write(output);
      }
    } catch (final FormatterException e) {
      for (final FormatterDiagnostic diagnostic : e.diagnostics()) {
        errWriter.println(stdinFilename + ":" + diagnostic);
      }
      ok = false;
      // TODO(cpovirk): Catch other types of exception (as we do in the formatFiles case).
    }
    return ok ? 0 : 1;
  }

  /** Parses and validates command-line flags. */
  public static CommandLineOptions processArgs(final String... args) throws UsageException {
    final CommandLineOptions parameters;
    try {
      parameters = CommandLineOptionsParser.parse(Arrays.asList(args));
    } catch (final IllegalArgumentException e) {
      throw new UsageException(e.getMessage());
    } catch (final Throwable t) {
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
      throw new UsageException("-offsets and -lengths flags must be provided in matching pairs");
    }
    if (filesToFormat <= 0 && !parameters.version() && !parameters.help()) {
      throw new UsageException("no files were provided");
    }
    if (parameters.stdin() && !parameters.files().isEmpty()) {
      throw new UsageException("cannot format from standard input and files simultaneously");
    }
    if (parameters.assumeFilename().isPresent() && !parameters.stdin()) {
      throw new UsageException(
          "--assume-filename is only supported when formatting standard input");
    }
    if (parameters.dryRun() && parameters.inPlace()) {
      throw new UsageException("cannot use --dry-run and --in-place at the same time");
    }
    return parameters;
  }
}
