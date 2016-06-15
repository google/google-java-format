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
public final class UsageException extends Exception {

  private static final Joiner NEWLINE_JOINER = Joiner.on(System.lineSeparator());

  private static final String[] USAGE = {
    "https://github.com/google/google-java-format",
    "Usage: google-java-format [options] file(s)",
    "Options:",
    "    -",
    "Format stdin -> stdout.",
    "    Default: false",
    "    --aosp, -aosp, -a",
    "Use AOSP style instead of Google Style (4-space indentation).",
    "Default: false",
    "    --help, -help, -h",
    "Print an extended usage statement.",
    "    Default: false",
    "    --length, -length",
    "Character length to format.",
    "Default: []",
    "    --lines, -lines, --line, -line",
    "Line range(s) to format, like 5:10 (1-based; default is all).",
    "Default: []",
    "    --offset, -offset",
    "Character offset to format (0-based; default is all).",
    "Default: []",
    "    -i, -r, -replace, --replace",
    "Send formatted output back to files, not stdout.",
    "    Default: false",
    "    --version, -version, -v",
    "Print the version.",
    "    Default: false",
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
    appendLines(builder, Main.VERSION);
    if (message != null) {
      builder.append(message).append('\n');
    }
    appendLines(builder, USAGE);
    appendLines(builder, ADDITIONAL_USAGE);
    return builder.toString();
  }

  private static void appendLines(StringBuilder builder, String[] lines) {
    NEWLINE_JOINER.appendTo(builder, lines).append(System.lineSeparator());
  }
}
