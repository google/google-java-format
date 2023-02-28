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

import com.google.auto.value.AutoValue;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Encapsulates information about a file to be formatted, including which parts of the file to
 * format.
 */
class FormatFileCallable implements Callable<FormatFileCallable.Result> {

  @AutoValue
  abstract static class Result {
    abstract @Nullable Path path();

    abstract String input();

    abstract @Nullable String output();

    boolean changed() {
      return !input().equals(output());
    }

    abstract @Nullable FormatterException exception();

    static Result create(
        @Nullable Path path,
        String input,
        @Nullable String output,
        @Nullable FormatterException exception) {
      return new AutoValue_FormatFileCallable_Result(path, input, output, exception);
    }
  }

  private final Path path;
  private final String input;
  private final CommandLineOptions parameters;
  private final JavaFormatterOptions options;

  public FormatFileCallable(
      CommandLineOptions parameters, Path path, String input, JavaFormatterOptions options) {
    this.path = path;
    this.input = input;
    this.parameters = parameters;
    this.options = options;
  }

  @Override
  public Result call() {
    try {
      if (parameters.fixImportsOnly()) {
        return Result.create(path, input, fixImports(input), /* exception= */ null);
      }

      Formatter formatter = new Formatter(options);
      String formatted = formatter.formatSource(input, characterRanges(input).asRanges());
      formatted = fixImports(formatted);
      if (parameters.reflowLongStrings()) {
        formatted = StringWrapper.wrap(Formatter.MAX_LINE_LENGTH, formatted, formatter);
      }
      return Result.create(path, input, formatted, /* exception= */ null);
    } catch (FormatterException e) {
      return Result.create(path, input, /* output= */ null, e);
    }
  }

  private String fixImports(String input) throws FormatterException {
    if (parameters.removeUnusedImports()) {
      input = RemoveUnusedImports.removeUnusedImports(input);
    }
    if (parameters.sortImports()) {
      input = ImportOrderer.reorderImports(input, options.style());
    }
    return input;
  }

  private RangeSet<Integer> characterRanges(String input) {
    final RangeSet<Integer> characterRanges = TreeRangeSet.create();

    if (parameters.lines().isEmpty() && parameters.offsets().isEmpty()) {
      characterRanges.add(Range.closedOpen(0, input.length()));
      return characterRanges;
    }

    characterRanges.addAll(Formatter.lineRangesToCharRanges(input, parameters.lines()));

    for (int i = 0; i < parameters.offsets().size(); i++) {
      Integer length = parameters.lengths().get(i);
      if (length == 0) {
        // 0 stands for "format the line under the cursor"
        length = 1;
      }
      characterRanges.add(
          Range.closedOpen(parameters.offsets().get(i), parameters.offsets().get(i) + length));
    }

    return characterRanges;
  }
}
