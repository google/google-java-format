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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;

/** Checked exception class for formatter command-line usage errors. */
final class UsageException extends Exception {

  private static final Joiner NEWLINE_JOINER = Joiner.on(System.lineSeparator());

  private static final String[] DOCS_LINK = {
    "https://github.com/google/google-java-format",
  };

  private static final String[] USAGE = {
    "",
    "Usage: google-java-format [options] file(s)",
    "",
    "Options:",
    "  -i, -r, -replace, --replace",
    "    Send formatted output back to files, not stdout.",
    "  -",
    "    Format stdin -> stdout",
    "  --assume-filename, -assume-filename",
    "    File name to use for diagnostics when formatting standard input (default is <stdin>).",
    "  --aosp, -aosp, -a",
    "    Use AOSP style instead of Google Style (4-space indentation).",
    "  --fix-imports-only",
    "    Fix import order and remove any unused imports, but do no other formatting.",
    "  --skip-sorting-imports",
    "    Do not fix the import order. Unused imports will still be removed.",
    "  --skip-removing-unused-imports",
    "    Do not remove unused imports. Imports will still be sorted.",
    "  --dry-run, -n",
    "    Prints the paths of the files whose contents would change if the formatter were run"
        + " normally.",
    "  --set-exit-if-changed",
    "    Return exit code 1 if there are any formatting changes.",
    "  --length, -length",
    "    Character length to format.",
    "  --lines, -lines, --line, -line",
    "    Line range(s) to format, like 5:10 (1-based; default is all).",
    "  --offset, -offset",
    "    Character offset to format (0-based; default is all).",
    "  --help, -help, -h",
    "    Print this usage statement.",
    "  --version, -version, -v",
    "    Print the version.",
    "  @<filename>",
    "    Read options and filenames from file.",
    "",
  };

  private static final String[] ADDITIONAL_USAGE = {
    "If -i is given with -, the result is sent to stdout.",
    "The --lines, --offset, and --length flags may be given more than once.",
    "The --offset and --length flags must be given an equal number of times.",
    "If --lines, --offset, or --length are given, only one file (or -) may be given."
  };

  UsageException() {
    super(buildMessage(null));
  }

  UsageException(String message) {
    super(buildMessage(checkNotNull(message)));
  }

  private static String buildMessage(String message) {
    StringBuilder builder = new StringBuilder();
    if (message != null) {
      builder.append(message).append('\n');
    }
    appendLines(builder, USAGE);
    appendLines(builder, ADDITIONAL_USAGE);
    appendLines(builder, new String[] {""});
    appendLine(builder, Main.versionString());
    appendLines(builder, DOCS_LINK);
    return builder.toString();
  }

  private static void appendLine(StringBuilder builder, String line) {
    builder.append(line).append(System.lineSeparator());
  }

  private static void appendLines(StringBuilder builder, String[] lines) {
    NEWLINE_JOINER.appendTo(builder, lines).append(System.lineSeparator());
  }
}
