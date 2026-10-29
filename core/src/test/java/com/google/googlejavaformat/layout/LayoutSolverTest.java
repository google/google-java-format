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

package com.google.googlejavaformat.layout;

import static com.google.common.truth.Truth.assertThat;
import static com.google.googlejavaformat.layout.Docs.fill;
import static com.google.googlejavaformat.layout.Docs.group;
import static com.google.googlejavaformat.layout.Docs.hardline;
import static com.google.googlejavaformat.layout.Docs.line;
import static com.google.googlejavaformat.layout.Docs.softline;
import static com.google.googlejavaformat.layout.Docs.space;
import static com.google.googlejavaformat.layout.Docs.taggedLine;
import static com.google.googlejavaformat.layout.Docs.text;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.googlejavaformat.Output.BreakTag;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/// Unit tests for the Layout IR and [LayoutSolver].
@RunWith(JUnit4.class)
public class LayoutSolverTest {

  private static Doc call() {
    return group(2, text("foo("), softline(), text("a,"), line(), text("b)"));
  }

  @Test
  public void singleText() {
    assertThat(LayoutSolver.format(text("hello"), 80)).isEqualTo("hello");
  }

  @Test
  public void groupFitsFlat() {
    assertThat(LayoutSolver.format(call(), 20)).isEqualTo("foo(a, b)");
  }

  @Test
  public void groupBreaksWithIndent() {
    assertThat(LayoutSolver.format(call(), 5)).isEqualTo("foo(\n  a,\n  b)");
  }

  @Test
  public void unifiedBreaksAllBreakTogether() {
    // "b c" would fit on the second line, but unified breaks in a broken group all break.
    Doc doc = group(text("a"), line(), text("b"), line(), text("c"));
    assertThat(LayoutSolver.format(doc, 3)).isEqualTo("a\nb\nc");
  }

  @Test
  public void fillBreaksMeasureWholeSegment() {
    Doc doc =
        group(
            text("one,"),
            fill(" "),
            text("two,"),
            fill(" "),
            text("three,"),
            fill(" "),
            text("four"));
    assertThat(LayoutSolver.format(doc, 11)).isEqualTo("one, two,\nthree, four");
    assertThat(LayoutSolver.format(doc, 10)).isEqualTo("one, two,\nthree,\nfour");
  }

  @Test
  public void fillBreakAfterSegmentThatDoesNotFit() {
    // After breaking before the inner group, the group still doesn't fit on its own line, so the
    // following fill break is taken too even though "y" would fit after "b)".
    Doc doc =
        group(
            text("x"),
            fill(" "),
            group(4, text("ffff("), softline(), text("aaaa,"), line(), text("b)")),
            fill(" "),
            text("y"));
    assertThat(LayoutSolver.format(doc, 10)).isEqualTo("x\nffff(\n    aaaa,\n    b)\ny");
  }

  @Test
  public void nestedGroupStaysFlatInsideBrokenGroup() {
    Doc doc = group(text("xxxx"), line(), call());
    assertThat(LayoutSolver.format(doc, 10)).isEqualTo("xxxx\nfoo(a, b)");
  }

  @Test
  public void hardlineForcesEnclosingBreaks() {
    Doc doc = group(text("a"), line(), text("b"), hardline(), text("c"));
    assertThat(LayoutSolver.format(doc, 100)).isEqualTo("a\nb\nc");
  }

  @Test
  public void maxWidthTooLarge() {
    // Widths saturate at Doc.MAX_LINE_WIDTH, so a larger maxWidth would lay out forced breaks flat.
    Doc doc = group(text("a"), hardline(), text("b"));
    assertThat(LayoutSolver.format(doc, Doc.MAX_LINE_WIDTH - 1)).isEqualTo("a\nb");
    assertThrows(
        IllegalArgumentException.class, () -> LayoutSolver.format(doc, Doc.MAX_LINE_WIDTH));
  }

  @Test
  public void breakTagRecordsFlat() {
    BreakTag tag = new BreakTag();
    Doc doc = group(text("a"), taggedLine(" ", tag), text("b"));
    assertThat(LayoutSolver.format(doc, 10)).isEqualTo("a b");
    // Breaks inside a group that fits are not visited.
    assertThat(tag.wasBreakTaken()).isFalse();
  }

  @Test
  public void breakTagRecordsBroken() {
    BreakTag tag = new BreakTag();
    Doc doc = group(text("a"), taggedLine(" ", tag), text("b"));
    assertThat(LayoutSolver.format(doc, 2)).isEqualTo("a\nb");
    assertThat(tag.wasBreakTaken()).isTrue();
  }

  @Test
  public void spacesDoNotBreak() {
    Doc doc = group(text("a"), space(), text("b"), space(), text("c"));
    assertThat(LayoutSolver.format(doc, 3)).isEqualTo("a b c");
  }

  /// `f(a, x -> body)`, with a preferred layout that hugs the lambda.
  private static Doc hugChoice(String body) {
    Doc.Group lambdaBody = (Doc.Group) group(4, line(), text(body));
    Doc.Group fallback =
        (Doc.Group)
            group(
                4,
                text("f("),
                softline(),
                group(text("a,"), line(), text("x ->"), lambdaBody),
                text(")"));
    Doc.Group preferred =
        (Doc.Group) group(text("f("), text("a,"), text(" "), text("x ->"), lambdaBody, text(")"));
    return new Doc.Choice(
        preferred, "f(a, ".length(), fallback, /* fallbackIfFlatOnNextLine= */ true);
  }

  @Test
  public void choiceFlat() {
    assertThat(LayoutSolver.format(hugChoice("y"), 20)).isEqualTo("f(a, x -> y)");
    // Also exercise a Choice that fits flat inside an enclosing broken Group, where Solver.choice
    // is reached directly instead of the root Group being flattened up front.
    Doc inBrokenGroup = group(text("prefix"), line(), hugChoice("y"));
    assertThat(LayoutSolver.format(inBrokenGroup, 15)).isEqualTo("prefix\nf(a, x -> y)");
  }

  @Test
  public void choicePreferred() {
    // The body is indented relative to the line, not the fallback's +4 argument indent.
    assertThat(LayoutSolver.format(hugChoice("longBody"), 13))
        .isEqualTo("f(a, x ->\n    longBody)");
  }

  @Test
  public void choiceFallbackWhenFirstLineDoesNotFit() {
    assertThat(LayoutSolver.format(hugChoice("longBody"), 8))
        .isEqualTo("f(\n    a,\n    x ->\n        longBody)");
  }

  @Test
  public void choiceFallbackWhenRestFitsOnNextLine() {
    Doc.Group lambdaBody = (Doc.Group) group(4, line(), text("body"));
    Doc.Group fallback =
        (Doc.Group)
            group(
                4, text("someLongMethod("), softline(), group(text("x ->"), lambdaBody), text(")"));
    Doc.Group preferred =
        (Doc.Group) group(text("someLongMethod("), text("x ->"), lambdaBody, text(")"));
    Doc choice =
        new Doc.Choice(
            preferred,
            "someLongMethod(x ->".length(),
            fallback,
            /* fallbackIfFlatOnNextLine= */ true);
    assertThat(LayoutSolver.format(choice, 20)).isEqualTo("someLongMethod(\n    x -> body)");
  }

  @Test
  public void choicePreferredWhenFallbackIfFlatOnNextLineDisabled() {
    Doc.Group lambdaBody = (Doc.Group) group(group(4, line(), text("body")), line(), text("}"));
    Doc.Group fallback =
        (Doc.Group) group(4, text("val g ="), line(), group(text("{"), lambdaBody));
    Doc.Group preferred = (Doc.Group) group(text("val g = {"), lambdaBody);
    assertThat(
            LayoutSolver.format(
                new Doc.Choice(preferred, "val g = {".length(), fallback, true), 12))
        .isEqualTo("val g =\n    { body }");
    assertThat(
            LayoutSolver.format(
                new Doc.Choice(preferred, "val g = {".length(), fallback, false), 12))
        .isEqualTo("val g = {\n    body\n}");
  }

  @Test
  public void choicePreferredWhenFallbackCannotFitOnSingleNextLine() {
    Doc.Group lambdaBody = (Doc.Group) group(4, line(), text("body"));
    Doc.Group preferred = (Doc.Group) group(text("f("), text("x ->"), lambdaBody, text(")"));

    // Multiple direct breaks in fallback: fallbackFitsOnNextLine returns false, so preferred hugs.
    Doc.Group multiBreakFallback =
        (Doc.Group)
            group(
                4, text("f("), softline(), text("x ->"), softline(), group(lambdaBody), text(")"));
    assertThat(
            LayoutSolver.format(
                new Doc.Choice(
                    preferred,
                    "f(x ->".length(),
                    multiBreakFallback,
                    /* fallbackIfFlatOnNextLine= */ true),
                8))
        .isEqualTo("f(x ->\n    body)");

    // No direct breaks in fallback: fallbackFitsOnNextLine returns false, so preferred hugs.
    Doc.Group noBreakFallback =
        (Doc.Group) group(4, text("f("), group(text("x ->"), lambdaBody), text(")"));
    assertThat(
            LayoutSolver.format(
                new Doc.Choice(
                    preferred,
                    "f(x ->".length(),
                    noBreakFallback,
                    /* fallbackIfFlatOnNextLine= */ true),
                8))
        .isEqualTo("f(x ->\n    body)");

    // Prefix before the single break in fallback is wider than maxWidth: fallbackFitsOnNextLine
    // returns false, and the choice falls back to breaking the group.
    Doc.Group widePrefixFallback =
        (Doc.Group)
            group(
                4, text("someLongMethod("), softline(), group(text("x ->"), lambdaBody), text(")"));
    Doc.Group widePrefixPreferred =
        (Doc.Group) group(text("someLongMethod("), text("x ->"), lambdaBody, text(")"));
    assertThat(
            LayoutSolver.format(
                new Doc.Choice(
                    widePrefixPreferred,
                    "someLongMethod(x ->".length(),
                    widePrefixFallback,
                    /* fallbackIfFlatOnNextLine= */ true),
                10))
        .isEqualTo("someLongMethod(\n    x ->\n        body)");
  }

  @Test
  public void choicePreferredIndentIsRelativeToLine() {
    // The enclosing group's +8 indent doesn't apply, since its break wasn't taken.
    Doc doc = group(8, text("g("), hugChoice("longBody"));
    assertThat(LayoutSolver.format(doc, 14)).isEqualTo("g(f(a, x ->\n    longBody)");
  }

  @Test
  public void choiceTrialDoesNotLeakBreakTags() {
    BreakTag tag = new BreakTag();
    Doc.Group preferred = (Doc.Group) group(text("aaaa"), taggedLine("", tag), text("bbbbbbbbbb"));
    Doc.Group fallback = (Doc.Group) group(text("aaaa"), line(""), text("bbbbbbbbbb"));
    // The preferred layout's first line ends before its prefix, so it's rejected. The tag recorded
    // while trying it is reset.
    assertThat(
            LayoutSolver.format(
                new Doc.Choice(preferred, 5, fallback, /* fallbackIfFlatOnNextLine= */ true), 8))
        .isEqualTo("aaaa\nbbbbbbbbbb");
    assertThat(tag.wasBreakTaken()).isFalse();
  }

  @Test
  public void nestedChoicesAreLinear() {
    // Each choice prefers to keep its contents on the current line, so every trial reaches the
    // innermost choice. Re-laying out each accepted preferred layout would take 2^depth steps.
    int depth = 100;
    Doc doc = group(text("{"), hardline(), text("}"));
    for (int i = 0; i < depth; i++) {
      Doc.Group preferred = (Doc.Group) group(text("a"), doc);
      Doc.Group fallback = (Doc.Group) group(4, text("a"), softline(), doc);
      doc = new Doc.Choice(preferred, 1, fallback, /* fallbackIfFlatOnNextLine= */ true);
    }
    assertThat(LayoutSolver.format(doc, 999)).isEqualTo("a".repeat(depth) + "{\n}");
  }

  @Test
  public void multilineTextBreaksEnclosingGroupAndEndsTrialLine() {
    Doc doc = group(4, text("val s ="), line(), text("\"\"\"\nhello\n\"\"\""));
    assertThat(LayoutSolver.format(doc, 80)).isEqualTo("val s =\n    \"\"\"\nhello\n\"\"\"");

    Doc.Group preferred = (Doc.Group) group(text("f("), text("\"\"\"\nhello\n\"\"\""), text(")"));
    Doc.Group fallback =
        (Doc.Group) group(4, text("f("), softline(), text("\"\"\"\nhello\n\"\"\""), text(")"));
    // First line of preferred is `f("""\n` (6 chars); fits at maxWidth 10, overflows at maxWidth 5.
    assertThat(LayoutSolver.format(new Doc.Choice(preferred, 2, fallback, false), 10))
        .isEqualTo("f(\"\"\"\nhello\n\"\"\")");
    assertThat(LayoutSolver.format(new Doc.Choice(preferred, 2, fallback, false), 5))
        .isEqualTo("f(\n    \"\"\"\nhello\n\"\"\")");
  }

  @Test
  public void linesIndentedToLineIndent() {
    Doc doc =
        group(
            4,
            text("String s ="),
            line(),
            lines("\"\"\"", "hello", "", "world", "\"\"\""),
            text(";"));
    assertThat(LayoutSolver.format(doc, 80))
        .isEqualTo(
            """
            String s =
                \"""
                hello

                world
                \""";\
            """);
  }

  private static Doc lines(String... lines) {
    return new Doc.Lines(ImmutableList.copyOf(lines), Doc.EMPTY_RANGE);
  }
}
