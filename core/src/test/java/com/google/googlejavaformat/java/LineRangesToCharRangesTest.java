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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link Formatter#lineRangesToCharRanges} */
@RunWith(JUnit4.class)
public class LineRangesToCharRangesTest {

  @SafeVarargs
  final Set<Range<Integer>> getCharRanges(String input, Range<Integer>... ranges) {
    RangeSet<Integer> rangeSet = TreeRangeSet.create();
    for (Range<Integer> range : ranges) {
      rangeSet.add(range);
    }
    return Formatter.lineRangesToCharRanges(input, rangeSet).asRanges();
  }

  @Test
  public void emptyLineRanges() throws Exception {
    assertThat(getCharRanges("", Range.closedOpen(0, 1))).isEmpty();
  }

  @Test
  public void lineRanges() throws Exception {
    assertThat(getCharRanges("_\n_\n_\n", Range.closedOpen(0, 1)))
        .containsExactly(Range.closedOpen(0, 1));
    assertThat(getCharRanges("_\n_\n_\n", Range.closedOpen(1, 2)))
        .containsExactly(Range.closedOpen(2, 3));
    assertThat(getCharRanges("_\n_\n_\n", Range.closedOpen(2, 3)))
        .containsExactly(Range.closedOpen(4, 5));
    assertThat(getCharRanges("_\n_\n_\n", Range.closedOpen(3, 4))).isEmpty();
  }

  @Test
  public void blankLineRange() throws Exception {
    assertThat(getCharRanges("hello\n\nworld", Range.closedOpen(0, 1)))
        .containsExactly(Range.closedOpen(0, 5));
    assertThat(getCharRanges("hello\n\nworld", Range.closedOpen(1, 2))).isEmpty();
    assertThat(getCharRanges("hello\n\nworld", Range.closedOpen(2, 3)))
        .containsExactly(Range.closedOpen(7, 12));
  }
}
