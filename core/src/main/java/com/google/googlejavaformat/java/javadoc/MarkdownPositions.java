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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import org.commonmark.node.BulletList;
import org.commonmark.node.Heading;
import org.commonmark.node.ListItem;
import org.commonmark.node.Node;
import org.commonmark.node.Paragraph;
import org.commonmark.parser.IncludeSourceSpans;
import org.commonmark.parser.Parser;

/**
 * Determines the locations in a Markdown string where Markdown constructs occur. For example, if
 * position 10 in the string looks like {@code # Heading\n}, the {@code positionToToken} map will
 * contain an entry for 10 with a {@code HEADER_OPEN_TAG} token and an entry for (10 +
 * "#&nbsp;Heading".length()) with a {@code HEADER_CLOSE_TAG} token. These locations can then be
 * inserted at the appropriate point in the stream of {@link Token} instances that the lexer
 * produces.
 *
 * <p>The text ({@Token#value()}) of these inserted tokens is not added to the output, since the
 * original Markdown characters are treated as literals. The tokens serve only to identify
 * constructs, for example to manage indentation of lists.
 */
// TODO: it might make more sense to give some of the inserted tokens non-empty text
final class MarkdownPositions {
  final ImmutableListMultimap<Integer, Token> positionToToken;

  private MarkdownPositions(ImmutableListMultimap<Integer, Token> positionToToken) {
    this.positionToToken = positionToToken;
  }

  static final MarkdownPositions EMPTY = new MarkdownPositions(ImmutableListMultimap.of());

  static MarkdownPositions parse(String input) {
    Node document = PARSER.parse(input);
    ListMultimap<Integer, Token> positionToToken = ArrayListMultimap.create();
    new TokenVisitor(positionToToken).visit(document);
    return new MarkdownPositions(ImmutableListMultimap.copyOf(positionToToken));
  }

  ImmutableList<Token> tokensAt(int position) {
    return positionToToken.get(position);
  }

  private static class TokenVisitor {
    private final ListMultimap<Integer, Token> positionToToken;

    TokenVisitor(ListMultimap<Integer, Token> positionToToken) {
      this.positionToToken = positionToToken;
    }

    void visit(Node node) {
      boolean alreadyVisitedChildren = false;
      switch (node) {
        case Heading heading ->
            addSpan(positionToToken, heading, HEADER_OPEN_TOKEN, HEADER_CLOSE_TOKEN);
        case Paragraph paragraph ->
            addSpan(positionToToken, paragraph, PARAGRAPH_OPEN_TOKEN, PARAGRAPH_CLOSE_TOKEN);
        case BulletList bulletList ->
            addSpan(positionToToken, bulletList, LIST_OPEN_TOKEN, LIST_CLOSE_TOKEN);
        case ListItem listItem -> {
          addSpan(positionToToken, listItem, LIST_ITEM_OPEN_TOKEN, LIST_ITEM_CLOSE_TOKEN);
          if (listItem.getFirstChild() instanceof Paragraph paragraph) {
            // A ListItem typically contains a Paragraph, but we don't want to visit that Paragraph
            // because that would lead us to introduce a line break after the list introduction
            // (the `-` or whatever). So we visit the children and siblings of the Paragraph
            // instead.
            alreadyVisitedChildren = true;
            visitNodeList(paragraph.getFirstChild());
            visitNodeList(paragraph.getNext());
          }
        }
        // TODO: others
        default -> {}
      }
      if (!alreadyVisitedChildren) {
        visitNodeList(node.getFirstChild());
      }
    }

    /**
     * Visits the given node and the other nodes that are reachable from it via the {@link
     * Node#getNext()} references. Does nothing if {@code node} is null.
     */
    private void visitNodeList(Node node) {
      for (; node != null; node = node.getNext()) {
        visit(node);
      }
    }
  }

  /**
   * Adds tokens for the given node, {@code startToken} at the point where the node starts in the
   * input, and {@code endToken} at the point where it ends. The {@code startToken} goes after any
   * other tokens at that position and the {@code endToken} goes before any other tokens at that
   * position. That reflects the structure. For example, at the start of a bullet list, the visitor
   * will see {@link BulletList} then {@link ListItem}. We will translate this into {@link
   * Token.Type.LIST_OPEN_TAG} then {@link Token.Type.LIST_ITEM_OPEN_TAG} at the start position, and
   * {@link Token.Type.LIST_ITEM_CLOSE_TAG} then {@link Token.Type.LIST_CLOSE_TAG} (in that order)
   * at the end position.
   */
  private static void addSpan(
      ListMultimap<Integer, Token> positionToToken, Node node, Token startToken, Token endToken) {
    // We could write the first part more simply as a `put`, but we do it this way for symmetry.
    var first = node.getSourceSpans().getFirst();
    int startPosition = first.getInputIndex();
    positionToToken.get(startPosition).addLast(startToken);
    var last = node.getSourceSpans().getLast();
    int endPosition = last.getInputIndex() + last.getLength();
    positionToToken.get(endPosition).addFirst(endToken);
  }

  @Override
  public String toString() {
    return positionToToken.toString();
  }

  private static final Parser PARSER =
      Parser.builder().includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES).build();
  private static final Token HEADER_OPEN_TOKEN = new Token(Token.Type.HEADER_OPEN_TAG, "");
  private static final Token HEADER_CLOSE_TOKEN = new Token(Token.Type.HEADER_CLOSE_TAG, "");
  private static final Token PARAGRAPH_OPEN_TOKEN = new Token(Token.Type.PARAGRAPH_OPEN_TAG, "");
  private static final Token PARAGRAPH_CLOSE_TOKEN = new Token(Token.Type.PARAGRAPH_CLOSE_TAG, "");
  private static final Token LIST_OPEN_TOKEN = new Token(Token.Type.LIST_OPEN_TAG, "");
  private static final Token LIST_CLOSE_TOKEN = new Token(Token.Type.LIST_CLOSE_TAG, "");
  private static final Token LIST_ITEM_OPEN_TOKEN = new Token(Token.Type.LIST_ITEM_OPEN_TAG, "");
  private static final Token LIST_ITEM_CLOSE_TOKEN = new Token(Token.Type.LIST_ITEM_CLOSE_TAG, "");
}
