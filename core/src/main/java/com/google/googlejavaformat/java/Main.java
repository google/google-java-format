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

import static java.lang.Math.min;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Comparator.comparing;

import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.googlejavaformat.FormatterDiagnostic;
import com.google.googlejavaformat.java.JavaFormatterOptions.Style;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
  public static void main(String... args) {
    int result = main(System.in, System.out, System.err, args);
    System.exit(result);
  }

  /**
   * Package-private main entry point used by the {@link javax.tools.Tool Tool} implementation in
   * the same package as this Main class.
   */
  static int main(InputStream in, PrintStream out, PrintStream err, String... args) {
    PrintWriter outWriter = new PrintWriter(new OutputStreamWriter(out, UTF_8));
    PrintWriter errWriter = new PrintWriter(new OutputStreamWriter(err, UTF_8));
    return main(in, outWriter, errWriter, args);
  }

  /**
   * Package-private main entry point used by the {@link java.util.spi.ToolProvider ToolProvider}
   * implementation in the same package as this Main class.
   */
  static int main(InputStream in, PrintWriter out, PrintWriter err, String... args) {
    try {
      Main formatter = new Main(out, err, in);
      return formatter.format(args);
    } catch (UsageException e) {
      err.print(e.getMessage());
      // We return exit code 2 to differentiate usage issues from code formatting issues.
      return 2;
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
  public int format(String... args) throws UsageException {
    CommandLineOptions parameters = processArgs(args);
    if (parameters.version()) {
      errWriter.println(versionString());
      return 0;
    }
    if (parameters.help()) {
      throw new UsageException();
    }

    JavaFormatterOptions options =
        JavaFormatterOptions.builder()
            .style(parameters.aosp() ? Style.AOSP : Style.GOOGLE)
            .formatJavadoc(parameters.formatJavadoc())
            .build();

    if (parameters.stdin()) {
      return formatStdin(parameters, options);
    } else {
      return formatFiles(parameters, options);
    }
  }

  private int formatFiles(CommandLineOptions parameters, JavaFormatterOptions options) {
    int numThreads = min(MAX_THREADS, parameters.files().size());
    ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

    ExecutorCompletionService<FormatFileCallable.Result> cs =
        new ExecutorCompletionService<>(executorService);
    boolean allOk = true;

    int files = 0;
    for (String fileName : parameters.files()) {
      if (!fileName.endsWith(".java")) {
        errWriter.println("Skipping non-Java file: " + fileName);
        continue;
      }
      Path path = Paths.get(fileName);
      try {
        String input = new String(Files.readAllBytes(path), UTF_8);
        cs.submit(new FormatFileCallable(parameters, path, input, options));
        files++;
      } catch (IOException e) {
        errWriter.println(fileName + ": could not read file: " + e.getMessage());
        allOk = false;
      }
    }

    List<FormatFileCallable.Result> results = new ArrayList<>();
    while (files > 0) {
      try {
        files--;
        results.add(cs.take().get());
      } catch (InterruptedException e) {
        errWriter.println(e.getMessage());
        allOk = false;
        continue;
      } catch (ExecutionException e) {
        errWriter.println("error: " + e.getCause().getMessage());
        e.getCause().printStackTrace(errWriter);
        allOk = false;
        continue;
      }
    }
    Collections.sort(results, comparing(FormatFileCallable.Result::path));
    for (FormatFileCallable.Result result : results) {
      Path path = result.path();
      if (result.exception() != null) {
        for (FormatterDiagnostic diagnostic : result.exception().diagnostics()) {
          errWriter.println(path + ":" + diagnostic);
        }
        allOk = false;
        continue;
      }
      String formatted = result.output();
      boolean changed = result.changed();
      if (changed && parameters.setExitIfChanged()) {
        allOk = false;
      }
      if (parameters.inPlace()) {
        if (!changed) {
          continue; // preserve original file
        }
        try {
          Files.write(path, formatted.getBytes(UTF_8));
        } catch (IOException e) {
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
    if (!MoreExecutors.shutdownAndAwaitTermination(executorService, Duration.ofSeconds(5))) {
      errWriter.println("Failed to shut down ExecutorService");
      allOk = false;
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
    String stdinFilename = parameters.assumeFilename().orElse(STDIN_FILENAME);
    boolean ok = true;
    FormatFileCallable.Result result =
        new FormatFileCallable(parameters, null, input, options).call();
    if (result.exception() != null) {
      for (FormatterDiagnostic diagnostic : result.exception().diagnostics()) {
        errWriter.println(stdinFilename + ":" + diagnostic);
      }
      ok = false;
    } else {
      String output = result.output();
      boolean changed = result.changed();
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
    }
    return ok ? 0 : 1;
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
