/*
 * Copyright 2026 Google Inc.
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.commonPrefix;
import static com.google.common.base.Strings.commonSuffix;
import static java.lang.Math.min;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import java.nio.CharBuffer;
import java.util.Collection;
import java.util.List;

/// The result of applying sorted, non-overlapping replacements to some text, which can map offsets
/// in the old text to offsets in the new text.
record Edit(String contents, ImmutableList<Edit.Span> spans) {

  /// One replacement: `[inStart, inEnd)` in the old contents became `[outStart, outEnd)`.
  record Span(int inStart, int inEnd, int outStart, int outEnd) {}

  /// Applies sorted, non-overlapping replacements to `contents`.
  static Edit apply(String contents, List<Replacement> replacements) {
    StringBuilder sb = new StringBuilder(contents.length());
    ImmutableList.Builder<Span> spans = ImmutableList.builderWithExpectedSize(replacements.size());
    int cursor = 0;
    for (Replacement replacement : replacements) {
      Range<Integer> range = replacement.replaceRange();
      int inStart = range.lowerEndpoint();
      int inEnd = range.upperEndpoint();
      checkArgument(
          inStart >= cursor,
          "Replacements must be sorted and non-overlapping, but %s overlaps previous end %s",
          range,
          cursor);
      sb.append(contents, cursor, inStart);
      String replacementString = replacement.replacementString();
      int outStart = sb.length();
      sb.append(replacementString);
      int outEnd = sb.length();
      // Trim any unchanged prefix and suffix so coarse replacements (such as reformatting an entire
      // statement) don't treat unchanged tokens at the start or end of the replacement as part of
      // the modified span.
      int prefix =
          commonPrefix(CharBuffer.wrap(contents, inStart, inEnd), replacementString).length();
      int suffix =
          commonSuffix(
                  CharBuffer.wrap(contents, inStart + prefix, inEnd),
                  CharBuffer.wrap(replacementString, prefix, replacementString.length()))
              .length();
      if (inStart + prefix < inEnd - suffix || outStart + prefix < outEnd - suffix) {
        spans.add(new Span(inStart + prefix, inEnd - suffix, outStart + prefix, outEnd - suffix));
      }
      cursor = inEnd;
    }
    sb.append(contents, cursor, contents.length());
    return new Edit(sb.toString(), spans.build());
  }

  /// Maps an offset in the old contents to the corresponding offset in the new contents.
  ///
  /// Offsets in unmodified text are shifted by the cumulative length change of preceding
  /// replacements. Offsets inside a replaced range `[inStart, inEnd)` are shifted relative to
  /// `inStart` and clamped to `outEnd` if the replacement is shorter than the original range.
  int map(int offset) {
    int shift = 0;
    for (Span span : spans) {
      if (offset < span.inStart()) {
        return offset + shift;
      }
      if (offset < span.inEnd()) {
        return min(offset + shift, span.outEnd());
      }
      shift = span.outEnd() - span.inEnd();
    }
    return offset + shift;
  }

  /// Maps ranges in the old contents to the corresponding ranges in the new contents, dropping
  /// ranges that become empty.
  ImmutableRangeSet<Integer> mapRanges(Collection<Range<Integer>> ranges) {
    RangeSet<Integer> result = TreeRangeSet.create();
    for (Range<Integer> range : ranges) {
      int start = map(range.lowerEndpoint());
      int end = map(range.upperEndpoint());
      if (start < end) {
        result.add(Range.closedOpen(start, end));
      }
    }
    return ImmutableRangeSet.copyOf(result);
  }
}
