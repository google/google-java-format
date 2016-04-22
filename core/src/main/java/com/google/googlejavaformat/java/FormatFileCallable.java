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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.google.googlejavaformat.java.JavaFormatterOptions.SortImports;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Encapsulates information about a file to be formatted, including which parts of the file to
 * format.
 */
public class FormatFileCallable implements Callable<String> {
  private final String fileName;
  private final String input;
  private final ImmutableRangeSet<Integer> lineRanges;
  private final ImmutableList<Integer> offsets;
  private final ImmutableList<Integer> lengths;
  private final JavaFormatterOptions options;

  public FormatFileCallable(
      String fileName,
      RangeSet<Integer> lineRanges,
      List<Integer> offsets,
      List<Integer> lengths,
      String input,
      JavaFormatterOptions options) {
    this.fileName = fileName;
    this.input = input;
    this.lineRanges = ImmutableRangeSet.copyOf(lineRanges);
    this.offsets = ImmutableList.copyOf(offsets);
    this.lengths = ImmutableList.copyOf(lengths);
    this.options = options;
  }

  @Override
  public String call() throws FormatterException {

    // TODO(cushon): figure out how to integrate import ordering into Formatter
    String inputString = input;
    if (options.sortImports() != SortImports.NO) {
      inputString = ImportOrderer.reorderImports(fileName, inputString);
      if (options.sortImports() == SortImports.ONLY) {
        return inputString;
      }
    }

    return new Formatter(fileName, options)
        .formatSource(inputString, characterRanges(inputString).asRanges());
  }

  static final CharMatcher NEWLINE = CharMatcher.is('\n');

  private RangeSet<Integer> characterRanges(String input) {
    final RangeSet<Integer> characterRanges = TreeRangeSet.create();

    if (lineRanges.isEmpty() && offsets.isEmpty()) {
      characterRanges.add(Range.closedOpen(0, input.length()));
      return characterRanges;
    }

    characterRanges.addAll(lineRangesToCharRanges(input, lineRanges));

    for (int i = 0; i < offsets.size(); i++) {
      characterRanges.add(Range.closedOpen(offsets.get(i), offsets.get(i) + lengths.get(i)));
    }

    return characterRanges;
  }

  @VisibleForTesting
  static RangeSet<Integer> lineRangesToCharRanges(String input, RangeSet<Integer> lineRanges) {
    List<Integer> lines = new ArrayList<>();
    lines.add(0);
    int idx = NEWLINE.indexIn(input);
    while (idx >= 0) {
      lines.add(idx + 1);
      idx = NEWLINE.indexIn(input, idx + 1);
    }
    lines.add(input.length() + 1);

    final RangeSet<Integer> characterRanges = TreeRangeSet.create();
    for (Range<Integer> lineRange :
        lineRanges.subRangeSet(Range.closedOpen(0, lines.size() - 1)).asRanges()) {
      int lineStart = lines.get(lineRange.lowerEndpoint());
      // Exclude the trailing newline. This isn't strictly necessary, but handling blank lines
      // as empty ranges is convenient.
      int lineEnd = lines.get(lineRange.upperEndpoint()) - 1;
      Range<Integer> range = Range.closedOpen(lineStart, lineEnd);
      characterRanges.add(range);
    }
    return characterRanges;
  }
}
