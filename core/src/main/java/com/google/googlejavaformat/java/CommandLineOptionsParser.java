/*
 * Copyright 2016 Google Inc.
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

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Range;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** A parser for {@link CommandLineOptions}. */
final class CommandLineOptionsParser {

  private static final Splitter COMMA_SPLITTER = Splitter.on(',');
  private static final Splitter COLON_SPLITTER = Splitter.on(':');
  private static final Splitter ARG_SPLITTER =
      Splitter.on(CharMatcher.breakingWhitespace()).omitEmptyStrings().trimResults();

  /** Parses {@link CommandLineOptions}. */
  static CommandLineOptions parse(Iterable<String> options) {
    CommandLineOptions.Builder optionsBuilder = CommandLineOptions.builder();
    List<String> expandedOptions = new ArrayList<>();
    expandParamsFiles(options, expandedOptions);
    Iterator<String> it = expandedOptions.iterator();
    while (it.hasNext()) {
      String option = it.next();
      if (!option.startsWith("-")) {
        optionsBuilder.filesBuilder().add(option).addAll(it);
        break;
      }
      String flag;
      String value;
      int idx = option.indexOf('=');
      if (idx >= 0) {
        flag = option.substring(0, idx);
        value = option.substring(idx + 1, option.length());
      } else {
        flag = option;
        value = null;
      }
      // NOTE: update usage information in UsageException when new flags are added
      switch (flag) {
        case "-i":
        case "-r":
        case "-replace":
        case "--replace":
          optionsBuilder.inPlace(true);
          break;
        case "--lines":
        case "-lines":
        case "--line":
        case "-line":
          parseRangeSet(optionsBuilder.linesBuilder(), getValue(flag, it, value));
          break;
        case "--offset":
        case "-offset":
          optionsBuilder.addOffset(parseInteger(it, flag, value));
          break;
        case "--length":
        case "-length":
          optionsBuilder.addLength(parseInteger(it, flag, value));
          break;
        case "--aosp":
        case "-aosp":
        case "-a":
          optionsBuilder.aosp(true);
          break;
        case "--version":
        case "-version":
        case "-v":
          optionsBuilder.version(true);
          break;
        case "--help":
        case "-help":
        case "-h":
          optionsBuilder.help(true);
          break;
        case "--fix-imports-only":
          optionsBuilder.fixImportsOnly(true);
          break;
        case "--skip-sorting-imports":
          optionsBuilder.sortImports(false);
          break;
        case "--skip-removing-unused-imports":
          optionsBuilder.removeUnusedImports(false);
          break;
        case "-":
          optionsBuilder.stdin(true);
          break;
        case "-n":
        case "--dry-run":
          optionsBuilder.dryRun(true);
          break;
        case "--set-exit-if-changed":
          optionsBuilder.setExitIfChanged(true);
          break;
        case "-assume-filename":
        case "--assume-filename":
          optionsBuilder.assumeFilename(getValue(flag, it, value));
          break;
        default:
          throw new IllegalArgumentException("unexpected flag: " + flag);
      }
    }
    return optionsBuilder.build();
  }

  private static Integer parseInteger(Iterator<String> it, String flag, String value) {
    try {
      return Integer.valueOf(getValue(flag, it, value));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          String.format("invalid integer value for %s: %s", flag, value), e);
    }
  }

  private static String getValue(String flag, Iterator<String> it, String value) {
    if (value != null) {
      return value;
    }
    if (!it.hasNext()) {
      throw new IllegalArgumentException("required value was not provided for: " + flag);
    }
    return it.next();
  }

  /**
   * Parse multiple --lines flags, like {"1:12,14,20:36", "40:45,50"}. Multiple ranges can be given
   * with multiple --lines flags or separated by commas. A single line can be set by a single
   * number. Line numbers are {@code 1}-based, but are converted to the {@code 0}-based numbering
   * used internally by google-java-format.
   */
  private static void parseRangeSet(ImmutableRangeSet.Builder<Integer> result, String ranges) {
    for (String range : COMMA_SPLITTER.split(ranges)) {
      result.add(parseRange(range));
    }
  }

  /**
   * Parse a range, as in "1:12" or "42". Line numbers provided are {@code 1}-based, but are
   * converted here to {@code 0}-based.
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

  /**
   * Pre-processes an argument list, expanding arguments of the form {@code @filename} by reading
   * the content of the file and appending whitespace-delimited options to {@code arguments}.
   */
  private static void expandParamsFiles(Iterable<String> args, List<String> expanded) {
    for (String arg : args) {
      if (arg.isEmpty()) {
        continue;
      }
      if (!arg.startsWith("@")) {
        expanded.add(arg);
      } else if (arg.startsWith("@@")) {
        expanded.add(arg.substring(1));
      } else {
        Path path = Paths.get(arg.substring(1));
        try {
          String sequence = new String(Files.readAllBytes(path), UTF_8);
          expandParamsFiles(ARG_SPLITTER.split(sequence), expanded);
        } catch (IOException e) {
          throw new UncheckedIOException(path + ": could not read file: " + e.getMessage(), e);
        }
      }
    }
  }
}
