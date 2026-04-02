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
            .put(firstBullet, new Token(Token.Type.LIST_OPEN_TAG, ""))
            .put(firstBullet, new Token(Token.Type.LIST_ITEM_OPEN_TAG, ""))
            .put(secondBullet - 1, new Token(Token.Type.LIST_ITEM_CLOSE_TAG, ""))
            .put(secondBullet, new Token(Token.Type.LIST_ITEM_OPEN_TAG, ""))
            .put(end, new Token(Token.Type.LIST_ITEM_CLOSE_TAG, ""))
            .put(end, new Token(Token.Type.LIST_CLOSE_TAG, ""))
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
            .put(firstHeading, new Token(Token.Type.HEADER_OPEN_TAG, ""))
            .put(firstHeadingEnd, new Token(Token.Type.HEADER_CLOSE_TAG, ""))
            .put(firstParagraph, new Token(Token.Type.PARAGRAPH_OPEN_TAG, ""))
            .put(firstParagraphEnd, new Token(Token.Type.PARAGRAPH_CLOSE_TAG, ""))
            .put(secondHeading, new Token(Token.Type.HEADER_OPEN_TAG, ""))
            .put(secondHeadingEnd, new Token(Token.Type.HEADER_CLOSE_TAG, ""))
            .put(secondParagraph, new Token(Token.Type.PARAGRAPH_OPEN_TAG, ""))
            .put(secondParagraphEnd, new Token(Token.Type.PARAGRAPH_CLOSE_TAG, ""))
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
