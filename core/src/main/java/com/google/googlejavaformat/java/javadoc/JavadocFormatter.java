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

package com.google.googlejavaformat.java.javadoc;

import static com.google.googlejavaformat.java.javadoc.JavadocLexer.lex;
import static com.google.googlejavaformat.java.javadoc.Token.Type.BR_TAG;
import static com.google.googlejavaformat.java.javadoc.Token.Type.PARAGRAPH_OPEN_TAG;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;

import com.google.common.collect.ImmutableList;
import com.google.googlejavaformat.java.javadoc.JavadocLexer.LexException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Entry point for formatting Javadoc.
 *
 * <p>This stateless class reads tokens from the stateful lexer and translates them to "requests"
 * and "writes" to the stateful writer. It also munges tokens into "standardized" forms. Finally, it
 * performs postprocessing to convert the written Javadoc to a one-liner if possible or to leave a
 * single blank line if it's empty.
 */
public final class JavadocFormatter {
  /**
   * Formats the given Javadoc comment, which must start with ∕✱✱ and end with ✱∕. The output will
   * start and end with the same characters.
   */
  public static String formatJavadoc(String input, int blockIndent, JavadocOptions options) {
    ImmutableList<Token> tokens;
    try {
      tokens = lex(input);
    } catch (LexException e) {
      return input;
    }
    String result = render(tokens, blockIndent, options);
    return makeSingleLineIfPossible(blockIndent, result, options);
  }

  private static String render(List<Token> input, int blockIndent, JavadocOptions options) {
    JavadocWriter output = new JavadocWriter(blockIndent, options);
    for (Token token : input) {
      switch (token.getType()) {
        case BEGIN_JAVADOC:
          output.writeBeginJavadoc();
          break;
        case END_JAVADOC:
          output.writeEndJavadoc();
          return output.toString();
        case FOOTER_JAVADOC_TAG_START:
          output.writeFooterJavadocTagStart(token);
          break;
        case LIST_OPEN_TAG:
          output.writeListOpen(token);
          break;
        case LIST_CLOSE_TAG:
          output.writeListClose(token);
          break;
        case LIST_ITEM_OPEN_TAG:
          output.writeListItemOpen(token);
          break;
        case HEADER_OPEN_TAG:
          output.writeHeaderOpen(token);
          break;
        case HEADER_CLOSE_TAG:
          output.writeHeaderClose(token);
          break;
        case PARAGRAPH_OPEN_TAG:
          output.writeParagraphOpen(standardizePToken(token));
          break;
        case BLOCKQUOTE_OPEN_TAG:
        case BLOCKQUOTE_CLOSE_TAG:
          output.writeBlockquoteOpenOrClose(token);
          break;
        case PRE_OPEN_TAG:
          output.writePreOpen(token);
          break;
        case PRE_CLOSE_TAG:
          output.writePreClose(token);
          break;
        case CODE_OPEN_TAG:
          output.writeCodeOpen(token);
          break;
        case CODE_CLOSE_TAG:
          output.writeCodeClose(token);
          break;
        case TABLE_OPEN_TAG:
          output.writeTableOpen(token);
          break;
        case TABLE_CLOSE_TAG:
          output.writeTableClose(token);
          break;
        case MOE_BEGIN_STRIP_COMMENT:
          output.requestMoeBeginStripComment(token);
          break;
        case MOE_END_STRIP_COMMENT:
          output.writeMoeEndStripComment(token);
          break;
        case HTML_COMMENT:
          output.writeHtmlComment(token);
          break;
        case BR_TAG:
          output.writeBr(standardizeBrToken(token));
          break;
        case WHITESPACE:
          output.requestWhitespace();
          break;
        case FORCED_NEWLINE:
          output.writeLineBreakNoAutoIndent();
          break;
        case LITERAL:
          output.writeLiteral(token);
          break;
        case PARAGRAPH_CLOSE_TAG:
        case LIST_ITEM_CLOSE_TAG:
        case OPTIONAL_LINE_BREAK:
          break;
        default:
          throw new AssertionError(token.getType());
      }
    }
    throw new AssertionError();
  }

  /*
   * TODO(cpovirk): Is this really the right location for the standardize* methods? Maybe the lexer
   * should include them as part of its own postprocessing? Or even the writer could make sense.
   */

  private static Token standardizeBrToken(Token token) {
    return standardize(token, STANDARD_BR_TOKEN);
  }

  private static Token standardizePToken(Token token) {
    return standardize(token, STANDARD_P_TOKEN);
  }

  private static Token standardize(Token token, Token standardToken) {
    return SIMPLE_TAG_PATTERN.matcher(token.getValue()).matches() ? standardToken : token;
  }

  private static final Token STANDARD_BR_TOKEN = new Token(BR_TAG, "<br>");
  private static final Token STANDARD_P_TOKEN = new Token(PARAGRAPH_OPEN_TAG, "<p>");
  private static final Pattern SIMPLE_TAG_PATTERN = compile("^<\\w+\\s*/?\\s*>", CASE_INSENSITIVE);

  private static final Pattern ONE_CONTENT_LINE_PATTERN = compile(" */[*][*]\n *[*] (.*)\n *[*]/");

  /**
   * Returns the given string or a one-line version of it (e.g., "∕✱✱ Tests for foos. ✱∕") if it
   * fits on one line.
   */
  private static String makeSingleLineIfPossible(
      int blockIndent, String input, JavadocOptions options) {
    int oneLinerContentLength = options.maxLineLength() - "/**  */".length() - blockIndent;
    Matcher matcher = ONE_CONTENT_LINE_PATTERN.matcher(input);
    if (matcher.matches() && matcher.group(1).isEmpty()) {
      return "/** */";
    } else if (matcher.matches() && matcher.group(1).length() <= oneLinerContentLength) {
      return "/** " + matcher.group(1) + " */";
    }
    return input;
  }

  private JavadocFormatter() {}
}
