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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ObjectArrays;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.google.googlejavaformat.java.JavaFormatterOptions.JavadocFormatter;
import com.google.googlejavaformat.java.JavaFormatterOptions.SortImports;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        names = {"--aosp", "-aosp", "-a"},
        description = "Use AOSP style instead of Google Style (4-space indentation).")
    boolean aospFlag = false;

    @Parameter(names = {"--version", "-version", "-v"}, description = "Print the version.")
    boolean versionFlag = false;

    @Parameter(
        names = {"--help", "-help", "-h"}, description = "Print an extended usage statement.")
    boolean helpFlag = false;

    @Parameter(
        names = {"--sort-imports", "-sort-imports"},
        description = "Sort import statements. "
            + "--sort-imports=only to sort imports but do no other formatting. "
            + "--sort-imports=also to sort imports and do other formatting.",
        hidden = true)
    String sortImportsFlag = "";

    // TODO(eaftan): clang-format formats stdin -> stdout when no options are passed.  We should
    // match that behavior.
    @Parameter(names = "-", description = "Format stdin -> stdout.")
    boolean stdinStdoutFlag = false;

    @Parameter(description = "file(s)")
    final List<String> fileNamesFlag = new ArrayList<>();
  }

  private static final String[] VERSION =
      {"google-java-format: Version " + GoogleJavaFormatVersion.VERSION};

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
  public static void main(String... args) {
    Main formatter =
        new Main(
            new PrintWriter(new OutputStreamWriter(System.out, UTF_8), true),
            new PrintWriter(new OutputStreamWriter(System.err, UTF_8), true),
            System.in);
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
   *
   * @param args the command-line arguments
   */
  public int format(String... args) throws UsageException {
    ArgInfo argInfo = ArgInfo.processArgs(args);

    if (argInfo.parameters.versionFlag) {
      version();
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

    ConstructFilesToFormatResult constructFilesToFormatResult = constructFilesToFormat(argInfo);
    boolean allOkay = constructFilesToFormatResult.allOkay;
    ImmutableList<FileToFormat> filesToFormat = constructFilesToFormatResult.filesToFormat;
    if (filesToFormat.isEmpty()) {
      return allOkay ? 0 : 1;
    }

    List<Future<Boolean>> results = new ArrayList<>();
    int numThreads = Math.min(MAX_THREADS, filesToFormat.size());
    ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
    JavaFormatterOptions options =
        new JavaFormatterOptions(
            JavadocFormatter.NONE,
            argInfo.parameters.aospFlag
                ? JavaFormatterOptions.Style.AOSP
                : JavaFormatterOptions.Style.GOOGLE,
            sortImports);
    Object outputLock = new Object();
    for (FileToFormat fileToFormat : filesToFormat) {
      results.add(
          executorService.submit(
              new FormatFileCallable(
                  fileToFormat,
                  outputLock,
                  options,
                  argInfo.parameters.iFlag,
                  outWriter,
                  errWriter)));
    }
    for (Future<Boolean> result : results) {
      try {
        allOkay &= result.get();
      } catch (InterruptedException e) {
        synchronized (outputLock) {
          errWriter.println(e);
        }
        allOkay = false;
      } catch (ExecutionException e) {
        synchronized (outputLock) {
          errWriter.println(e.getCause());
        }
        allOkay = false;
      }
    }
    return allOkay ? 0 : 1;
  }

  // Package-private for testing
  ConstructFilesToFormatResult constructFilesToFormat(ArgInfo argInfo) {
    boolean allOkay = true;
    Set<Path> seenRealPaths = new HashSet<>();
    ImmutableList.Builder<FileToFormat> filesToFormat = ImmutableList.builder();
    for (String fileName : argInfo.parameters.fileNamesFlag) {
      if (fileName.endsWith(".java")) {
        try {
          Path originalPath = Paths.get(fileName);
          boolean added = seenRealPaths.add(originalPath.toRealPath());
          if (added) {
            filesToFormat.add(
                new FileToFormatPath(
                    originalPath,
                    parseRangeSet(argInfo.parameters.linesFlags),
                    argInfo.parameters.offsetFlags,
                    argInfo.parameters.lengthFlags));
          }
        } catch (IOException e) {
          errWriter
              .append(fileName)
              .append(": could not read file: ")
              .append(e.getMessage())
              .append('\n')
              .flush();
          allOkay = false;
        }
      } else {
        errWriter.println("Skipping non-Java file: " + fileName);
      }
    }

    if (argInfo.parameters.stdinStdoutFlag) {
      filesToFormat.add(
          new FileToFormatStdin(
              parseRangeSet(argInfo.parameters.linesFlags),
              argInfo.parameters.offsetFlags,
              argInfo.parameters.lengthFlags,
              inStream));
    }

    return new ConstructFilesToFormatResult(allOkay, filesToFormat.build());
  }

  // Package-private for testing
  static class ConstructFilesToFormatResult {
    final boolean allOkay;
    final ImmutableList<FileToFormat> filesToFormat;

    ConstructFilesToFormatResult(boolean allOkay, ImmutableList<FileToFormat> filesToFormat) {
      this.allOkay = allOkay;
      this.filesToFormat = filesToFormat;
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
      return !parameters.linesFlags.isEmpty() || !parameters.offsetFlags.isEmpty()
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
