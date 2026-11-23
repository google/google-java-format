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
package com.google.googlejavaformat.java.javadoc;

import static com.google.common.collect.ImmutableListMultimap.flatteningToImmutableListMultimap;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableListMultimap;
import com.google.googlejavaformat.java.javadoc.Token.HeaderCloseTag;
import com.google.googlejavaformat.java.javadoc.Token.HeaderOpenTag;
import com.google.googlejavaformat.java.javadoc.Token.ListCloseTag;
import com.google.googlejavaformat.java.javadoc.Token.ListItemCloseTag;
import com.google.googlejavaformat.java.javadoc.Token.ListItemOpenTag;
import com.google.googlejavaformat.java.javadoc.Token.ListOpenTag;
import com.google.googlejavaformat.java.javadoc.Token.MarkdownCodeSpanEnd;
import com.google.googlejavaformat.java.javadoc.Token.MarkdownCodeSpanStart;
import com.google.googlejavaformat.java.javadoc.Token.MarkdownFencedCodeBlock;
import com.google.googlejavaformat.java.javadoc.Token.ParagraphCloseTag;
import com.google.googlejavaformat.java.javadoc.Token.ParagraphOpenTag;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class MarkdownPositionsTest {
  @Test
  public void empty() {
    String text = "";
    var positions = MarkdownPositions.parse(text);
    assertThat(positionToToken(positions, text)).isEmpty();
  }

  @Test
  public void list() {
    String text =
"""
- foo
- bar
""";
    var positions = MarkdownPositions.parse(text);
    ImmutableListMultimap<Integer, Token> map = positionToToken(positions, text);
    int firstBullet = text.indexOf('-');
    int secondBullet = text.lastIndexOf('-');
    int end = text.length() - 1;
    ImmutableListMultimap<Integer, Token> expected =
        ImmutableListMultimap.<Integer, Token>builder()
            .put(firstBullet, new ListOpenTag(""))
            .put(firstBullet, new ListItemOpenTag("- "))
            .put(secondBullet - 1, new ListItemCloseTag(""))
            .put(secondBullet, new ListItemOpenTag("- "))
            .put(end, new ListItemCloseTag(""))
            .put(end, new ListCloseTag(""))
            .build();
    assertThat(map).isEqualTo(expected);
  }

  @Test
  public void listWithParenthesis() {
    String text =
"""
1) foo
2) bar
""";
    var positions = MarkdownPositions.parse(text);
    ImmutableListMultimap<Integer, Token> map = positionToToken(positions, text);
    int firstItem = text.indexOf('1');
    int secondItem = text.indexOf('2');
    int end = text.length() - 1;
    ImmutableListMultimap<Integer, Token> expected =
        ImmutableListMultimap.<Integer, Token>builder()
            .put(firstItem, new ListOpenTag(""))
            .put(firstItem, new ListItemOpenTag("1) "))
            .put(secondItem - 1, new ListItemCloseTag(""))
            .put(secondItem, new ListItemOpenTag("2) "))
            .put(end, new ListItemCloseTag(""))
            .put(end, new ListCloseTag(""))
            .build();
    assertThat(map).isEqualTo(expected);
  }

  @Test
  public void listWithPlus() {
    String text =
"""
+ foo
+ bar
""";
    var positions = MarkdownPositions.parse(text);
    ImmutableListMultimap<Integer, Token> map = positionToToken(positions, text);
    int firstItem = text.indexOf('+');
    int secondItem = text.lastIndexOf('+');
    int end = text.length() - 1;
    ImmutableListMultimap<Integer, Token> expected =
        ImmutableListMultimap.<Integer, Token>builder()
            .put(firstItem, new ListOpenTag(""))
            .put(firstItem, new ListItemOpenTag("+ "))
            .put(secondItem - 1, new ListItemCloseTag(""))
            .put(secondItem, new ListItemOpenTag("+ "))
            .put(end, new ListItemCloseTag(""))
            .put(end, new ListCloseTag(""))
            .build();
    assertThat(map).isEqualTo(expected);
  }

  @Test
  public void heading() {
    String text =
"""
# Foo

blah blah blah

## Bar

tiddly pom
""";
    var positions = MarkdownPositions.parse(text);
    ImmutableListMultimap<Integer, Token> map = positionToToken(positions, text);
    int firstHeading = text.indexOf('#');
    int firstHeadingEnd = text.indexOf('\n', firstHeading);
    int firstParagraph = text.indexOf("blah");
    int firstParagraphEnd = text.indexOf('\n', firstParagraph);
    int secondHeading = text.indexOf('#', firstHeading + 1);
    int secondHeadingEnd = text.indexOf('\n', secondHeading);
    int secondParagraph = text.indexOf("tiddly");
    int secondParagraphEnd = text.indexOf('\n', secondParagraph);
    ImmutableListMultimap<Integer, Token> expected =
        ImmutableListMultimap.<Integer, Token>builder()
            .put(firstHeading, new HeaderOpenTag(""))
            .put(firstHeadingEnd, new HeaderCloseTag(""))
            .put(firstParagraph, new ParagraphOpenTag(""))
            .put(firstParagraphEnd, new ParagraphCloseTag(""))
            .put(secondHeading, new HeaderOpenTag(""))
            .put(secondHeadingEnd, new HeaderCloseTag(""))
            .put(secondParagraph, new ParagraphOpenTag(""))
            .put(secondParagraphEnd, new ParagraphCloseTag(""))
            .build();
    assertThat(map).isEqualTo(expected);
  }

  @Test
  public void codeBlock() {
    String text =
"""
- ```
  foo
  bar
  ```

~~~java
code
with tildes
~~~

  ````
  indented code
  with more than three backticks
  ````
""";
    var positions = MarkdownPositions.parse(text);
    ImmutableListMultimap<Integer, Token> map = positionToToken(positions, text);
    int bullet = text.indexOf('-');
    int firstCodeStart = text.indexOf("```");
    int firstCodeEnd = text.indexOf("```", firstCodeStart + 3) + 3;
    int secondCodeStart = text.indexOf("~~~", firstCodeEnd);
    int secondCodeEnd = text.indexOf("~~~", secondCodeStart + 3) + 3;
    int thirdCodeStart = text.indexOf("````", secondCodeEnd);
    int thirdCodeEnd = text.indexOf("````", thirdCodeStart + 4) + 4;
    ImmutableListMultimap<Integer, Token> expected =
        ImmutableListMultimap.<Integer, Token>builder()
            .put(bullet, new ListOpenTag(""))
            .put(bullet, new ListItemOpenTag("- "))
            .put(
                firstCodeStart,
                new MarkdownFencedCodeBlock(
                    text.substring(firstCodeStart, firstCodeEnd), "```", "```", "foo\nbar\n"))
            .put(firstCodeEnd, new ListItemCloseTag(""))
            .put(firstCodeEnd, new ListCloseTag(""))
            .put(
                secondCodeStart,
                new MarkdownFencedCodeBlock(
                    text.substring(secondCodeStart, secondCodeEnd),
                    "~~~java",
                    "~~~",
                    "code\nwith tildes\n"))
            .put(
                thirdCodeStart,
                new MarkdownFencedCodeBlock(
                    text.substring(thirdCodeStart, thirdCodeEnd),
                    "````",
                    "````",
                    "indented code\nwith more than three backticks\n"))
            .build();
    assertThat(map).isEqualTo(expected);
  }

  @Test
  public void codeLinkMultilineInList() {
    String text = "- See [a][Foo#bar(\n      int)].";
    var positions = MarkdownPositions.parse(text);
    ImmutableListMultimap<Integer, Token> map = positionToToken(positions, text);
    // TODO: emit a Literal token for multi-line JEP 467 reference link targets in lists
    ImmutableListMultimap<Integer, Token> expected =
        ImmutableListMultimap.<Integer, Token>builder()
            .put(0, new ListOpenTag(""))
            .put(0, new ListItemOpenTag("- "))
            .put(text.length(), new ListItemCloseTag(""))
            .put(text.length(), new ListCloseTag(""))
            .build();
    assertThat(map).isEqualTo(expected);
  }

  @Test
  public void paragraphAfterHtmlComment() {
    String text =
        """
        Paragraph 1.
        <!-- comment -->
        Paragraph 2.
        """;
    var positions = MarkdownPositions.parse(text);
    ImmutableListMultimap<Integer, Token> map = positionToToken(positions, text);
    int firstParagraphEnd = text.indexOf('\n');
    int secondParagraph = text.indexOf("Paragraph 2.");
    int secondParagraphEnd = text.length() - 1;
    // TODO: formatting should not insert blank lines around an HTML comment block when none
    // existed in the input (e.g. MOE directives).
    ImmutableListMultimap<Integer, Token> expected =
        ImmutableListMultimap.<Integer, Token>builder()
            .put(0, new ParagraphOpenTag(""))
            .put(firstParagraphEnd, new ParagraphCloseTag(""))
            .put(secondParagraph, new ParagraphOpenTag(""))
            .put(secondParagraphEnd, new ParagraphCloseTag(""))
            .build();
    assertThat(map).isEqualTo(expected);
  }

  @Test
  public void codeLink() {
    String text = "See [Foo#bar(int, int)] and [`bar`][Foo#bar(int, int)].";
    var positions = MarkdownPositions.parse(text);
    ImmutableListMultimap<Integer, Token> map = positionToToken(positions, text);
    int codeSpanStart = text.indexOf("`bar`");
    int codeSpanEnd = codeSpanStart + "`bar`".length() - 1;
    // TODO: emit Literal tokens for JEP 467 reference link targets so they are not wrapped (a line
    // break causes javac's MarkdownTransformer to misclassify shortcut links as @linkplain and fail
    // to compute reference source spans).
    ImmutableListMultimap<Integer, Token> expected =
        ImmutableListMultimap.<Integer, Token>builder()
            .put(0, new ParagraphOpenTag(""))
            .put(codeSpanStart, new MarkdownCodeSpanStart("`"))
            .put(codeSpanEnd, new MarkdownCodeSpanEnd("`"))
            .put(text.length(), new ParagraphCloseTag(""))
            .build();
    assertThat(map).isEqualTo(expected);
  }

  @Test
  public void codeLinkVariantsAndNonMatches() {
    String text =
        "See [ImmutableMap<String, Integer>], [Scanner#scan(Tree, VisitorState)][],"
            + " [Arrays#equals(byte\\[\\], byte\\[\\])], [Foo.bar(a, b)], and [note(with `code`)].";
    var positions = MarkdownPositions.parse(text);
    ImmutableListMultimap<Integer, Token> map = positionToToken(positions, text);
    int codeSpanStart = text.indexOf("`code`");
    int codeSpanEnd = codeSpanStart + "`code`".length() - 1;
    // TODO: emit Literal tokens for valid JEP 467 reference link variants (parameterized types,
    // collapsed links, escaped arrays), but not for dotted non-`#` calls or prose containing code.
    ImmutableListMultimap<Integer, Token> expected =
        ImmutableListMultimap.<Integer, Token>builder()
            .put(0, new ParagraphOpenTag(""))
            .put(codeSpanStart, new MarkdownCodeSpanStart("`"))
            .put(codeSpanEnd, new MarkdownCodeSpanEnd("`"))
            .put(text.length(), new ParagraphCloseTag(""))
            .build();
    assertThat(map).isEqualTo(expected);
  }

  @Test
  public void paragraphAfterHtmlCommentWithBlankLine() {
    String text =
        """
        Paragraph 1.

        <!-- comment -->

        Paragraph 2.
        """;
    var positions = MarkdownPositions.parse(text);
    ImmutableListMultimap<Integer, Token> map = positionToToken(positions, text);
    int firstParagraphEnd = text.indexOf('\n');
    int secondParagraph = text.indexOf("Paragraph 2.");
    int secondParagraphEnd = text.length() - 1;
    ImmutableListMultimap<Integer, Token> expected =
        ImmutableListMultimap.<Integer, Token>builder()
            .put(0, new ParagraphOpenTag(""))
            .put(firstParagraphEnd, new ParagraphCloseTag(""))
            .put(secondParagraph, new ParagraphOpenTag(""))
            .put(secondParagraphEnd, new ParagraphCloseTag(""))
            .build();
    assertThat(map).isEqualTo(expected);
  }

  private static ImmutableListMultimap<Integer, Token> positionToToken(
      MarkdownPositions positions, String input) {
    return IntStream.rangeClosed(0, input.length())
        .mapToObj(i -> Map.entry(i, positions.tokensAt(i)))
        .collect(flatteningToImmutableListMultimap(Map.Entry::getKey, e -> e.getValue().stream()));
  }
}
