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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import org.junit.Test;

/// Tests for [Edit].
public class EditTest {

  @Test
  public void apply_emptyReplacements() {
    Edit edit = Edit.apply("abcdef", ImmutableList.of());
    assertThat(edit.contents()).isEqualTo("abcdef");
    assertThat(edit.spans()).isEmpty();
    for (int i = 0; i <= 6; i++) {
      assertThat(edit.map(i)).isEqualTo(i);
    }
  }

  @Test
  public void apply_overlappingOrUnsorted_throws() {
    ImmutableList<Replacement> overlapping =
        ImmutableList.of(Replacement.create(1, 4, "x"), Replacement.create(3, 5, "y"));
    assertThrows(IllegalArgumentException.class, () -> Edit.apply("abcdef", overlapping));
    ImmutableList<Replacement> unsorted =
        ImmutableList.of(Replacement.create(3, 5, "y"), Replacement.create(1, 2, "x"));
    assertThrows(IllegalArgumentException.class, () -> Edit.apply("abcdef", unsorted));
  }

  @Test
  public void map_shorterReplacement() {
    // Replace "2345" ([2, 6)) with "ab" ([2, 4)) in "01234567" -> "01ab67"
    Edit edit = Edit.apply("01234567", ImmutableList.of(Replacement.create(2, 6, "ab")));
    assertThat(edit.contents()).isEqualTo("01ab67");
    // Unmodified prefix "01"
    assertThat(edit.map(0)).isEqualTo(0);
    assertThat(edit.map(1)).isEqualTo(1);
    // Inside replaced range [2, 6): clamped to [2, 4]
    assertThat(edit.map(2)).isEqualTo(2);
    assertThat(edit.map(3)).isEqualTo(3);
    assertThat(edit.map(4)).isEqualTo(4);
    assertThat(edit.map(5)).isEqualTo(4);
    // Unmodified suffix "67" (shifted by -2)
    assertThat(edit.map(6)).isEqualTo(4);
    assertThat(edit.map(7)).isEqualTo(5);
    assertThat(edit.map(8)).isEqualTo(6);
  }

  @Test
  public void map_longerReplacement() {
    // Replace "2" ([2, 3)) with "abcd" ([2, 6)) in "01234" -> "01abcd34"
    Edit edit = Edit.apply("01234", ImmutableList.of(Replacement.create(2, 3, "abcd")));
    assertThat(edit.contents()).isEqualTo("01abcd34");
    assertThat(edit.map(0)).isEqualTo(0);
    assertThat(edit.map(1)).isEqualTo(1);
    assertThat(edit.map(2)).isEqualTo(2);
    // End of replaced range (start of '3') maps to the end of "abcd" (offset 6)
    assertThat(edit.map(3)).isEqualTo(6);
    assertThat(edit.map(4)).isEqualTo(7);
    assertThat(edit.map(5)).isEqualTo(8);
  }

  @Test
  public void map_replacementAtStart() {
    // Replace "012" ([0, 3)) with "a" ([0, 1)) in "012345" -> "a345"
    Edit edit = Edit.apply("012345", ImmutableList.of(Replacement.create(0, 3, "a")));
    assertThat(edit.contents()).isEqualTo("a345");
    assertThat(edit.spans()).containsExactly(new Edit.Span(0, 3, 0, 1));
    assertThat(edit.map(0)).isEqualTo(0);
    assertThat(edit.map(1)).isEqualTo(1);
    assertThat(edit.map(2)).isEqualTo(1);
    assertThat(edit.map(3)).isEqualTo(1);
    assertThat(edit.map(4)).isEqualTo(2);
    assertThat(edit.map(6)).isEqualTo(4);
  }

  @Test
  public void map_replacementAtEnd() {
    // Replace "45" ([4, 6)) with "abcd" ([4, 8)) in "012345" -> "0123abcd"
    Edit edit = Edit.apply("012345", ImmutableList.of(Replacement.create(4, 6, "abcd")));
    assertThat(edit.contents()).isEqualTo("0123abcd");
    assertThat(edit.spans()).containsExactly(new Edit.Span(4, 6, 4, 8));
    assertThat(edit.map(3)).isEqualTo(3);
    assertThat(edit.map(4)).isEqualTo(4);
    assertThat(edit.map(5)).isEqualTo(5);
    assertThat(edit.map(6)).isEqualTo(8);
  }

  @Test
  public void apply_insertionAndDeletionAtBoundaries() {
    // Insert "x" at the start and delete the trailing "5" in "012345" -> "x01234"
    Edit edit =
        Edit.apply(
            "012345",
            ImmutableList.of(Replacement.create(0, 0, "x"), Replacement.create(5, 6, "")));
    assertThat(edit.contents()).isEqualTo("x01234");
    assertThat(edit.spans())
        .containsExactly(new Edit.Span(0, 0, 0, 1), new Edit.Span(5, 6, 6, 6))
        .inOrder();
    assertThat(edit.map(0)).isEqualTo(1);
    assertThat(edit.map(5)).isEqualTo(6);
    assertThat(edit.map(6)).isEqualTo(6);
  }

  @Test
  public void apply_unchangedReplacementHasNoSpan() {
    // Replacing text with itself, including the whole input, doesn't record a span.
    Edit edit = Edit.apply("012345", ImmutableList.of(Replacement.create(0, 6, "012345")));
    assertThat(edit.contents()).isEqualTo("012345");
    assertThat(edit.spans()).isEmpty();
  }

  @Test
  public void map_deletionAndMultipleSpans() {
    // In "0123456789":
    // - delete [1, 3) ("12") -> [1, 1)
    // - replace [5, 6) ("5") with "XYZ" -> [3, 6)
    // - replace [6, 9) ("678") with "W" -> [6, 7)
    // Result: "0" + "" + "34" + "XYZ" + "W" + "9" = "034XYZW9"
    Edit edit =
        Edit.apply(
            "0123456789",
            ImmutableList.of(
                Replacement.create(1, 3, ""),
                Replacement.create(5, 6, "XYZ"),
                Replacement.create(6, 9, "W")));
    assertThat(edit.contents()).isEqualTo("034XYZW9");
    assertThat(edit.map(0)).isEqualTo(0);
    // Deleted [1, 3) collapses to offset 1
    assertThat(edit.map(1)).isEqualTo(1);
    assertThat(edit.map(2)).isEqualTo(1);
    assertThat(edit.map(3)).isEqualTo(1);
    // Between first and second replacement ("34" at [3, 5) -> [1, 3))
    assertThat(edit.map(4)).isEqualTo(2);
    // Start of second replacement ("5" at [5, 6) -> "XYZ" at [3, 6))
    assertThat(edit.map(5)).isEqualTo(3);
    // Boundary between adjacent second and third replacements ([6, 9) -> "W" at [6, 7))
    assertThat(edit.map(6)).isEqualTo(6);
    assertThat(edit.map(7)).isEqualTo(7);
    assertThat(edit.map(8)).isEqualTo(7);
    assertThat(edit.map(9)).isEqualTo(7);
    // Trailing "9" at [9, 10) -> [7, 8)
    assertThat(edit.map(10)).isEqualTo(8);
  }

  @Test
  public void map_coarseReplacementTrimsCommonPrefixAndSuffix() {
    // Replace the whole string "prefix    suffix" with "prefix suffix"; offsets inside "suffix"
    // should be shifted by -3 rather than clamped to the end of the replacement.
    String input = "prefix    suffix";
    String output = "prefix suffix";
    Edit edit = Edit.apply(input, ImmutableList.of(Replacement.create(0, input.length(), output)));
    assertThat(edit.contents()).isEqualTo(output);
    int inSuffixStart = input.indexOf("suffix");
    int outSuffixStart = output.indexOf("suffix");
    assertThat(edit.map(inSuffixStart)).isEqualTo(outSuffixStart);
    assertThat(edit.map(input.length())).isEqualTo(output.length());
  }

  @Test
  public void mapRanges() {
    // In "0123456789":
    // - delete [2, 5) ("234")
    // - replace [7, 8) ("7") with "ABCD"
    // Result: "01" + "56" + "ABCD" + "89" = "0156ABCD89"
    Edit edit =
        Edit.apply(
            "0123456789",
            ImmutableList.of(Replacement.create(2, 5, ""), Replacement.create(7, 8, "ABCD")));
    assertThat(edit.contents()).isEqualTo("0156ABCD89");
    assertThat(
            edit.mapRanges(
                    ImmutableList.of(
                        Range.closedOpen(0, 1), // "0" -> [0, 1)
                        Range.closedOpen(2, 5), // deleted "234" -> empty, dropped
                        Range.closedOpen(3, 4), // inside deleted "234" -> empty, dropped
                        Range.closedOpen(5, 7), // "56" -> [2, 4)
                        Range.closedOpen(7, 8), // "7" -> "ABCD" at [4, 8)
                        Range.closedOpen(9, 10))) // "9" -> [9, 10)
                .asRanges())
        .containsExactly(Range.closedOpen(0, 1), Range.closedOpen(2, 8), Range.closedOpen(9, 10))
        .inOrder();
  }
}
