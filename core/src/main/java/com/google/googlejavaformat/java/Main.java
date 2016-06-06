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
import com.google.common.collect.ObjectArrays;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.google.common.io.ByteStreams;
import com.google.googlejavaformat.java.JavaFormatterOptions.JavadocFormatter;
import com.google.googlejavaformat.java.JavaFormatterOptions.SortImports;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * The main class for the Java formatter CLI.
 */
public final class Main {
  private static final int MAX_THREADS = 20;
  private static final Splitter COMMA_SPLITTER = Splitter.on(',');
  private static final Splitter COLON_SPLITTER = Splitter.on(':');
  private static final String STDIN_FILENAME = "<stdin>";

  @Parameters(separators = "=")
  private static final class FormatterParameters {
    @Parameter(
      names = {"-i", "-r", "-replace", "--replace"},
      description = "Send formatted output back to files, not stdout."
    )
    boolean iFlag = false;

    @Parameter(
      names = {"--lines", "-lines", "--line", "-line"},
      description = "Line range(s) to format, like 5:10 (1-based; default is all)."
    )
    final List<String> linesFlags = new ArrayList<>();

    @Parameter(
      names = {"--offset", "-offset"},
      description = "Character offset to format (0-based; default is all)."
    )
    private final List<Integer> offsetFlags = new ArrayList<>();

    @Parameter(
      names = {"--length", "-length"},
      description = "Character length to format."
    )
    private final List<Integer> lengthFlags = new ArrayList<>();

    @Parameter(
      names = {"--aosp", "-aosp", "-a"},
      description = "Use AOSP style instead of Google Style (4-space indentation)."
    )
    boolean aospFlag = false;

    @Parameter(
      names = {"--version", "-version", "-v"},
      description = "Print the version."
    )
    boolean versionFlag = false;

    @Parameter(
      names = {"--help", "-help", "-h"},
      description = "Print an extended usage statement."
    )
    boolean helpFlag = false;

    @Parameter(
      names = {"--sort-imports", "-sort-imports"},
      description =
          "Sort import statements. "
              + "--sort-imports=only to sort imports but do no other formatting. "
              + "--sort-imports=also to sort imports and do other formatting.",
      hidden = true
    )
    String sortImportsFlag = "";

    // TODO(eaftan): clang-format formats stdin -> stdout when no options are passed.  We should
    // match that behavior.
    @Parameter(names = "-", description = "Format stdin -> stdout.")
    boolean stdinStdoutFlag = false;

    @Parameter(description = "file(s)")
    final List<String> fileNamesFlag = new ArrayList<>();
  }

  private static final String[] VERSION = {
    "google-java-format: Version " + GoogleJavaFormatVersion.VERSION
  };

  private static final String[] USAGE =
      ObjectArrays.concat(
          VERSION,
          new String[] {
            "https://github.com/google/google-java-format",
          },
          String.class);

  private static final String[] ADDITIONAL_USAGE = {
    "If -i is given with -, the result is sent to stdout.",
    "The --lines, --offset, and --length flags may be given more than once.",
    "The --offset and --length flags must be given an equal number of times.",
    "If --lines, --offset, or --length are given, only one file (or -) may be given."
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
      err.print(e.usage());
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
    ArgInfo argInfo = ArgInfo.processArgs(args);

    if (argInfo.parameters.versionFlag) {
      version();
      return 0;
    }
    if (argInfo.parameters.helpFlag) {
      argInfo.throwUsage();
    }
    SortImports sortImports;
    switch (argInfo.parameters.sortImportsFlag) {
      case "":
        sortImports = SortImports.NO;
        break;
      case "only":
        sortImports = SortImports.ONLY;
        break;
      case "also":
        sortImports = SortImports.ALSO;
        break;
      default:
        errWriter.println("Invalid value for --sort-imports. Should be \"only\" or \"also\".");
        return 1;
    }
    if (sortImports != SortImports.NO && argInfo.isSelection()) {
      errWriter.println("--sort-imports can currently only apply to the whole file");
      return 1;
    }

    JavaFormatterOptions options =
        new JavaFormatterOptions(
            JavadocFormatter.NONE,
            argInfo.parameters.aospFlag
                ? JavaFormatterOptions.Style.AOSP
                : JavaFormatterOptions.Style.GOOGLE,
            sortImports);

    if (argInfo.parameters.stdinStdoutFlag) {
      return formatStdin(argInfo, options);
    } else {
      return formatFiles(argInfo, options);
    }
  }

  private int formatFiles(ArgInfo argInfo, JavaFormatterOptions options) {
    int numThreads = Math.min(MAX_THREADS, argInfo.parameters.fileNamesFlag.size());
    ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

    Map<Path, String> inputs = new LinkedHashMap<>();
    Map<Path, Future<String>> results = new LinkedHashMap<>();
    for (String fileName : argInfo.parameters.fileNamesFlag) {
      if (!fileName.endsWith(".java")) {
        errWriter.println("Skipping non-Java file: " + fileName);
        continue;
      }
      Path path = Paths.get(fileName);
      String input;
      try {
        input = new String(Files.readAllBytes(path), UTF_8);
      } catch (IOException e) {
        errWriter.write(fileName + ": could not read file: " + e.getMessage());
        return 1;
      }
      inputs.put(path, input);
      results.put(
          path,
          executorService.submit(
              new FormatFileCallable(
                  parseRangeSet(argInfo.parameters.linesFlags),
                  argInfo.parameters.offsetFlags,
                  argInfo.parameters.lengthFlags,
                  input,
                  options)));
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
          errWriter.println(result.getKey() + ":" + e.getCause().getMessage());
        } else {
          errWriter.println(result.getKey() + ": error: " + e.getCause().getMessage());
        }
        allOk = false;
        continue;
      }
      if (argInfo.parameters.iFlag) {
        if (formatted.equals(inputs.get(result.getKey()))) {
          continue; // preserve original file
        }
        try {
          Files.write(result.getKey(), formatted.getBytes(UTF_8));
        } catch (IOException e) {
          errWriter.write(result.getKey() + ": could not write file: " + e.getMessage());
          allOk = false;
          continue;
        }
      } else {
        outWriter.write(formatted);
      }
    }
    return allOk ? 0 : 1;
  }

  private int formatStdin(ArgInfo argInfo, JavaFormatterOptions options) {
    String input;
    try {
      input = new String(ByteStreams.toByteArray(inStream), UTF_8);
    } catch (IOException e) {
      throw new IOError(e);
    }
    try {
      String output =
          new FormatFileCallable(
                  parseRangeSet(argInfo.parameters.linesFlags),
                  argInfo.parameters.offsetFlags,
                  argInfo.parameters.lengthFlags,
                  input,
                  options)
              .call();
      outWriter.write(output);
      return 0;
    } catch (FormatterException e) {
      errWriter.println(STDIN_FILENAME + ":" + e.getMessage());
      return 1;
    }
  }

  static class ArgInfo {
    public final FormatterParameters parameters;
    private final JCommander jCommander;

    public static ArgInfo processArgs(String... args) throws UsageException {
      FormatterParameters parameters = new FormatterParameters();
      JCommander jCommander = new JCommander(parameters);
      ArgInfo argInfo = new ArgInfo(parameters, jCommander);

      jCommander.setProgramName("google-java-format");
      try {
        jCommander.parse(args);
      } catch (ParameterException ignored) {
        argInfo.throwUsage();
      }

      int filesToFormat = parameters.fileNamesFlag.size();
      if (parameters.stdinStdoutFlag) {
        filesToFormat++;
      }

      if (parameters.iFlag && parameters.fileNamesFlag.isEmpty()) {
        argInfo.throwUsage();
      }
      if (argInfo.isSelection() && filesToFormat != 1) {
        argInfo.throwUsage();
      }
      if (parameters.offsetFlags.size() != parameters.lengthFlags.size()) {
        argInfo.throwUsage();
      }
      if (filesToFormat <= 0 && !parameters.versionFlag && !parameters.helpFlag) {
        argInfo.throwUsage();
      }

      return argInfo;
    }

    boolean isSelection() {
      return !parameters.linesFlags.isEmpty()
          || !parameters.offsetFlags.isEmpty()
          || !parameters.lengthFlags.isEmpty();
    }

    private ArgInfo(FormatterParameters parameters, JCommander jCommander) {
      this.parameters = parameters;
      this.jCommander = jCommander;
    }

    public void throwUsage() throws UsageException {
      StringBuilder builder = new StringBuilder();
      addLines(builder, USAGE);
      jCommander.usage(builder);
      addLines(builder, ADDITIONAL_USAGE);
      throw new UsageException(builder.toString());
    }

    private static void addLines(StringBuilder builder, String[] lines) {
      for (String line : lines) {
        builder.append(line);
        builder.append(System.lineSeparator());
      }
    }
  }

  private void version() {
    for (String line : VERSION) {
      errWriter.println(line);
    }
  }

  /**
   * Parse multiple --lines flags, like {"1:12,14,20:36", "40:45,50"}. Multiple ranges can be given
   * with multiple --lines flags or separated by commas. A single line can be set by a single
   * number. Line numbers are {@code 1}-based, but are converted to the {@code 0}-based numbering
   * used internally by google-java-format.
   *
   * @param linesFlags a list of command-line flags
   * @return the {@link RangeSet} of line numbers, converted to {@code 0}-based
   */
  private static RangeSet<Integer> parseRangeSet(List<String> linesFlags) {
    RangeSet<Integer> result = TreeRangeSet.create();
    for (String linesFlag : linesFlags) {
      for (String range : COMMA_SPLITTER.split(linesFlag)) {
        result.add(parseRange(range));
      }
    }
    return result;
  }

  /**
   * Parse a range, as in "1:12" or "42". Line numbers provided are {@code 1}-based, but are
   * converted here to {@code 0}-based.
   *
   * @param arg the command-line argument
   * @return the {@link RangeSet} of line numbers, converted to {@code 0}-based
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
}
