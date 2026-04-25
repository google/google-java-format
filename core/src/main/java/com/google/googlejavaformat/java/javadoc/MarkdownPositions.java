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

import static com.google.common.base.Verify.verify;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.googlejavaformat.java.javadoc.Token.HeaderCloseTag;
import com.google.googlejavaformat.java.javadoc.Token.HeaderOpenTag;
import com.google.googlejavaformat.java.javadoc.Token.ListCloseTag;
import com.google.googlejavaformat.java.javadoc.Token.ListItemCloseTag;
import com.google.googlejavaformat.java.javadoc.Token.ListItemOpenTag;
import com.google.googlejavaformat.java.javadoc.Token.ListOpenTag;
import com.google.googlejavaformat.java.javadoc.Token.MarkdownCodeSpanEnd;
import com.google.googlejavaformat.java.javadoc.Token.MarkdownCodeSpanStart;
import com.google.googlejavaformat.java.javadoc.Token.MarkdownFencedCodeBlock;
import com.google.googlejavaformat.java.javadoc.Token.MarkdownTable;
import com.google.googlejavaformat.java.javadoc.Token.ParagraphCloseTag;
import com.google.googlejavaformat.java.javadoc.Token.ParagraphOpenTag;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.BulletList;
import org.commonmark.node.Code;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Heading;
import org.commonmark.node.ListItem;
import org.commonmark.node.Node;
import org.commonmark.node.OrderedList;
import org.commonmark.node.Paragraph;
import org.commonmark.parser.IncludeSourceSpans;
import org.commonmark.parser.Parser;

/**
 * Determines the locations in a Markdown string where Markdown constructs occur. For example, if
 * position 10 in the string looks like {@code # Heading\n}, the {@code positionToToken} map will
 * contain an entry for 10 with a {@code HeaderOpenTag} token and an entry for (10 +
 * "#&nbsp;Heading".length()) with a {@code HeaderCloseTag} token. These locations can then be
 * inserted at the appropriate point in the stream of {@link Token} instances that the lexer
 * produces.
 *
 * <p>The text ({@Token#value()}) of these inserted tokens is mostly not added to the output, since
 * the original Markdown characters are treated as literals.
 */
final class MarkdownPositions {
  final ImmutableListMultimap<Integer, Token> positionToToken;

  private MarkdownPositions(ImmutableListMultimap<Integer, Token> positionToToken) {
    this.positionToToken = positionToToken;
  }

  static final MarkdownPositions EMPTY = new MarkdownPositions(ImmutableListMultimap.of());

  static MarkdownPositions parse(String input) {
    Node document = PARSER.parse(input);
    ListMultimap<Integer, Token> positionToToken = ArrayListMultimap.create();
    new TokenVisitor(input, positionToToken).visit(document);
    return new MarkdownPositions(ImmutableListMultimap.copyOf(positionToToken));
  }

  ImmutableList<Token> tokensAt(int position) {
    return positionToToken.get(position);
  }

  private static class TokenVisitor {
    private final String input;
    private final ListMultimap<Integer, Token> positionToToken;

    TokenVisitor(String input, ListMultimap<Integer, Token> positionToToken) {
      this.input = input;
      this.positionToToken = positionToToken;
    }

    void visit(Node node) {
      boolean alreadyVisitedChildren = false;
      switch (node) {
        case Heading heading -> addSpan(heading, HEADER_OPEN_TOKEN, HEADER_CLOSE_TOKEN);
        case Paragraph paragraph -> addSpan(paragraph, PARAGRAPH_OPEN_TOKEN, PARAGRAPH_CLOSE_TOKEN);
        case BulletList bulletList -> addSpan(bulletList, LIST_OPEN_TOKEN, LIST_CLOSE_TOKEN);
        case OrderedList orderedList -> addSpan(orderedList, LIST_OPEN_TOKEN, LIST_CLOSE_TOKEN);
        case ListItem listItem -> alreadyVisitedChildren = visitListItem(listItem);
        case FencedCodeBlock fencedCodeBlock -> visitFencedCodeBlock(fencedCodeBlock);
        case TableBlock tableBlock -> {
          visitTableBlock(tableBlock);
          alreadyVisitedChildren = true;
        }
        case Code code -> visitCodeSpan(code);
        // TODO: others
        default -> {}
      }
      if (!alreadyVisitedChildren) {
        visitNodeList(node.getFirstChild());
      }
    }

    // Returns true if this method visited the children of the given ListItem.
    private boolean visitListItem(ListItem listItem) {
      int startPosition = listItem.getSourceSpans().getFirst().getInputIndex();
      Matcher matcher =
          LIST_ITEM_START_PATTERN.matcher(input).region(startPosition, input.length());
      verify(matcher.lookingAt());
      ListItemOpenTag openToken = new ListItemOpenTag(matcher.group(1));
      addSpan(listItem, openToken, LIST_ITEM_CLOSE_TOKEN);
      return switch (listItem.getFirstChild()) {
        case Paragraph paragraph -> {
          // A ListItem typically contains a Paragraph, but we don't want to visit that Paragraph
          // because that would lead us to introduce a line break after the list introduction
          // (the `-` or whatever). So we visit the children and siblings of the Paragraph instead.
          visitNodeList(paragraph.getFirstChild());
          visitNodeList(paragraph.getNext());
          yield true;
        }
        default -> false;
      };
    }

    private void visitFencedCodeBlock(FencedCodeBlock fencedCodeBlock) {
      // Any indentation before the code block is part of FencedCodeBlock. This makes sense
      // because the lines inside the code block must also be indented by that amount. That
      // indentation gets subtracted from FencedCodeBlock.getLiteral(), which is the actual text
      // represented by the code block.
      int start = startPosition(fencedCodeBlock) + fencedCodeBlock.getFenceIndent();
      boolean precededByNonWhitespace = precededByNonWhitespace(start);
      int closingLength =
          Objects.requireNonNullElse(
              fencedCodeBlock.getClosingFenceLength(), fencedCodeBlock.getOpeningFenceLength());
      MarkdownFencedCodeBlock token =
          new MarkdownFencedCodeBlock(
              input.substring(start, endPosition(fencedCodeBlock)),
              fencedCodeBlock.getFenceCharacter().repeat(fencedCodeBlock.getOpeningFenceLength())
                  + fencedCodeBlock.getInfo(),
              fencedCodeBlock.getFenceCharacter().repeat(closingLength),
              fencedCodeBlock.getLiteral(),
              precededByNonWhitespace);
      positionToToken.get(start).addLast(token);
    }

    private void visitTableBlock(TableBlock tableBlock) {
      int start = startPosition(tableBlock);
      boolean precededByNonWhitespace = precededByNonWhitespace(start);
      int end = endPosition(tableBlock);
      positionToToken
          .get(start)
          .addLast(new MarkdownTable(input.substring(start, end), precededByNonWhitespace));
    }

    private void visitCodeSpan(Code code) {
      int start = startPosition(code);
      int end = endPosition(code);
      int count;
      for (count = 0; input.charAt(start + count) == '`'; count++) {
        verify(
            input.charAt(end - 1 - count) == '`',
            "Mismatched backticks: %s",
            input.substring(start, end));
      }
      verify(count > 0, "Code span does not start with backticks: %s", input.substring(start, end));
      String backticks = "`".repeat(count);
      positionToToken.get(start).addLast(new MarkdownCodeSpanStart(backticks));
      positionToToken.get(end - count).addFirst(new MarkdownCodeSpanEnd(backticks));
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

    private boolean precededByNonWhitespace(int position) {
      for (int i = position - 1; i >= 0 && input.charAt(i) != '\n'; i--) {
        if (!Character.isWhitespace(input.charAt(i))) {
          return true;
        }
      }
      return false;
    }

    /**
     * Adds tokens for the given node, {@code startToken} at the point where the node starts in the
     * input, and {@code endToken} at the point where it ends. The {@code startToken} goes after any
     * other tokens at that position and the {@code endToken} goes before any other tokens at that
     * position. That reflects the structure. For example, at the start of a bullet list, the
     * visitor we will translate this into {@link ListOpenTag} then {@link ListItemOpenTag} at the
     * start position, and {@link ListItemCloseTag} then {@link ListCloseTag} (in that order) at the
     * end position.
     */
    private void addSpan(Node node, Token startToken, Token endToken) {
      // We could write the first part more simply as a `put`, but we do it this way for symmetry.
      positionToToken.get(startPosition(node)).addLast(startToken);
      positionToToken.get(endPosition(node)).addFirst(endToken);
    }

    private int startPosition(Node node) {
      return node.getSourceSpans().getFirst().getInputIndex();
    }

    private int endPosition(Node node) {
      var last = node.getSourceSpans().getLast();
      return last.getInputIndex() + last.getLength();
    }
  }

  @Override
  public String toString() {
    return positionToToken.toString();
  }

  private static final Parser PARSER =
      Parser.builder()
          .includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES)
          .extensions(ImmutableList.of(TablesExtension.create()))
          .build();

  private static final HeaderOpenTag HEADER_OPEN_TOKEN = new HeaderOpenTag("");
  private static final HeaderCloseTag HEADER_CLOSE_TOKEN = new HeaderCloseTag("");
  private static final ParagraphOpenTag PARAGRAPH_OPEN_TOKEN = new ParagraphOpenTag("");
  private static final ParagraphCloseTag PARAGRAPH_CLOSE_TOKEN = new ParagraphCloseTag("");
  private static final ListOpenTag LIST_OPEN_TOKEN = new ListOpenTag("");
  private static final ListCloseTag LIST_CLOSE_TOKEN = new ListCloseTag("");
  private static final ListItemCloseTag LIST_ITEM_CLOSE_TOKEN = new ListItemCloseTag("");

  // The leading \s here works around what appears to be a CommonMark bug. We shouldn't ever see
  // space at the purported start of a list item?
  private static final Pattern LIST_ITEM_START_PATTERN =
      Pattern.compile("(?:\\s*)((-|\\*|[0-9]+\\.)\\s)");
}
