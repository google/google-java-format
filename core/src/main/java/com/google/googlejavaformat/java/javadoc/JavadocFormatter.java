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

import static com.google.common.base.Preconditions.checkState;
import static com.google.googlejavaformat.java.javadoc.JavadocLexer.lex;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.joining;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.google.googlejavaformat.java.javadoc.JavadocLexer.LexException;
import com.google.googlejavaformat.java.javadoc.Token.BeginJavadoc;
import com.google.googlejavaformat.java.javadoc.Token.BlockquoteCloseTag;
import com.google.googlejavaformat.java.javadoc.Token.BlockquoteOpenTag;
import com.google.googlejavaformat.java.javadoc.Token.BrTag;
import com.google.googlejavaformat.java.javadoc.Token.CodeCloseTag;
import com.google.googlejavaformat.java.javadoc.Token.CodeOpenTag;
import com.google.googlejavaformat.java.javadoc.Token.EndJavadoc;
import com.google.googlejavaformat.java.javadoc.Token.FooterJavadocTagStart;
import com.google.googlejavaformat.java.javadoc.Token.ForcedNewline;
import com.google.googlejavaformat.java.javadoc.Token.HeaderCloseTag;
import com.google.googlejavaformat.java.javadoc.Token.HeaderOpenTag;
import com.google.googlejavaformat.java.javadoc.Token.HtmlComment;
import com.google.googlejavaformat.java.javadoc.Token.ListCloseTag;
import com.google.googlejavaformat.java.javadoc.Token.ListItemCloseTag;
import com.google.googlejavaformat.java.javadoc.Token.ListItemOpenTag;
import com.google.googlejavaformat.java.javadoc.Token.ListOpenTag;
import com.google.googlejavaformat.java.javadoc.Token.Literal;
import com.google.googlejavaformat.java.javadoc.Token.MoeBeginStripComment;
import com.google.googlejavaformat.java.javadoc.Token.MoeEndStripComment;
import com.google.googlejavaformat.java.javadoc.Token.OptionalLineBreak;
import com.google.googlejavaformat.java.javadoc.Token.ParagraphCloseTag;
import com.google.googlejavaformat.java.javadoc.Token.ParagraphOpenTag;
import com.google.googlejavaformat.java.javadoc.Token.PreCloseTag;
import com.google.googlejavaformat.java.javadoc.Token.PreOpenTag;
import com.google.googlejavaformat.java.javadoc.Token.SnippetBegin;
import com.google.googlejavaformat.java.javadoc.Token.SnippetEnd;
import com.google.googlejavaformat.java.javadoc.Token.TableCloseTag;
import com.google.googlejavaformat.java.javadoc.Token.TableOpenTag;
import com.google.googlejavaformat.java.javadoc.Token.Whitespace;
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

  static final int MAX_LINE_LENGTH = 100;

  /**
   * Formats the given Javadoc comment. A classic Javadoc comment must start with ∕✱✱ and end with
   * ✱∕, and the output will start and end with the same characters. A Markdown Javadoc comment
   * consists of lines each of which starts with ///, and the output will also consist of such
   * lines.
   */
  public static String formatJavadoc(String input, int blockIndent) {
    boolean classicJavadoc =
        switch (input) {
          case String s when s.startsWith("/**") -> true;
          case String s when s.startsWith("///") -> false;
          default ->
              throw new IllegalArgumentException("Input does not start with /** or ///: " + input);
        };
    if (!classicJavadoc) {
      input = "///" + markdownCommentText(input);
    }
    ImmutableList<Token> tokens;
    try {
      tokens = lex(input, classicJavadoc);
    } catch (LexException e) {
      return input;
    }
    String result = render(tokens, blockIndent, classicJavadoc);
    if (classicJavadoc) {
      result = makeSingleLineIfPossible(blockIndent, result);
    }
    return result;
  }

  private static String render(List<Token> input, int blockIndent, boolean classicJavadoc) {
    JavadocWriter output = new JavadocWriter(blockIndent, classicJavadoc);
    for (Token token : input) {
      switch (token) {
        case BeginJavadoc unused -> output.writeBeginJavadoc();
        case EndJavadoc unused -> {
          output.writeEndJavadoc();
          return output.toString();
        }
        case FooterJavadocTagStart t -> output.writeFooterJavadocTagStart(t);
        case SnippetBegin t -> output.writeSnippetBegin(t);
        case SnippetEnd t -> output.writeSnippetEnd(t);
        case ListOpenTag t -> output.writeListOpen(t);
        case ListCloseTag t -> output.writeListClose(t);
        case ListItemOpenTag t -> output.writeListItemOpen(t);
        case HeaderOpenTag t -> output.writeHeaderOpen(t);
        case HeaderCloseTag t -> output.writeHeaderClose(t);
        case ParagraphOpenTag t -> output.writeParagraphOpen(standardizePToken(t));
        case BlockquoteOpenTag t -> output.writeBlockquoteOpenOrClose(t);
        case BlockquoteCloseTag t -> output.writeBlockquoteOpenOrClose(t);
        case PreOpenTag t -> output.writePreOpen(t);
        case PreCloseTag t -> output.writePreClose(t);
        case CodeOpenTag t -> output.writeCodeOpen(t);
        case CodeCloseTag t -> output.writeCodeClose(t);
        case TableOpenTag t -> output.writeTableOpen(t);
        case TableCloseTag t -> output.writeTableClose(t);
        case MoeBeginStripComment t -> output.requestMoeBeginStripComment(t);
        case MoeEndStripComment t -> output.writeMoeEndStripComment(t);
        case HtmlComment t -> output.writeHtmlComment(t);
        case BrTag t -> output.writeBr(standardizeBrToken(t));
        case Whitespace unused -> output.requestWhitespace();
        case ForcedNewline unused -> output.writeLineBreakNoAutoIndent();
        case Literal t -> output.writeLiteral(t);
        case ParagraphCloseTag unused -> {}
        case ListItemCloseTag unused -> {}
        case OptionalLineBreak unused -> {}
      }
    }
    throw new AssertionError();
  }

  /*
   * TODO(cpovirk): Is this really the right location for the standardize* methods? Maybe the lexer
   * should include them as part of its own postprocessing? Or even the writer could make sense.
   */

  private static BrTag standardizeBrToken(BrTag token) {
    return standardize(token, STANDARD_BR_TOKEN);
  }

  private static ParagraphOpenTag standardizePToken(ParagraphOpenTag token) {
    return standardize(token, STANDARD_P_TOKEN);
  }

  private static <T extends Token> T standardize(T token, T standardToken) {
    return SIMPLE_TAG_PATTERN.matcher(token.value()).matches() ? standardToken : token;
  }

  private static final BrTag STANDARD_BR_TOKEN = new BrTag("<br>");
  private static final ParagraphOpenTag STANDARD_P_TOKEN = new ParagraphOpenTag("<p>");
  private static final Pattern SIMPLE_TAG_PATTERN = compile("^<\\w+\\s*/?\\s*>", CASE_INSENSITIVE);

  private static final Pattern ONE_CONTENT_LINE_PATTERN = compile(" */[*][*]\n *[*] (.*)\n *[*]/");

  /**
   * Returns the given string or a one-line version of it (e.g., "∕✱✱ Tests for foos. ✱∕") if it
   * fits on one line.
   */
  private static String makeSingleLineIfPossible(int blockIndent, String input) {
    Matcher matcher = ONE_CONTENT_LINE_PATTERN.matcher(input);
    if (matcher.matches()) {
      String line = matcher.group(1);
      if (line.isEmpty()) {
        return "/** */";
      } else if (oneLineJavadoc(line, blockIndent)) {
        return "/** " + line + " */";
      }
    }
    return input;
  }

  private static boolean oneLineJavadoc(String line, int blockIndent) {
    int oneLinerContentLength = MAX_LINE_LENGTH - "/**  */".length() - blockIndent;
    if (line.length() > oneLinerContentLength) {
      return false;
    }
    // If the javadoc contains only a tag, use multiple lines to encourage writing a summary
    // fragment, unless it's /** @hide */.
    if (line.startsWith("@") && !line.equals("@hide")) {
      return false;
    }
    return true;
  }

  private static final CharMatcher NOT_SPACE_OR_TAB = CharMatcher.noneOf(" \t");

  /**
   * Returns the given string with the leading /// and any common leading whitespace removed from
   * each line. The resultant string can then be fed to a standard Markdown parser.
   */
  private static String markdownCommentText(String input) {
    List<String> lines =
        input
            .lines()
            .peek(line -> checkState(line.contains("///"), "Line does not contain ///: %s", line))
            .map(line -> line.substring(line.indexOf("///") + 3))
            .toList();
    int leadingSpace =
        lines.stream()
            .filter(line -> NOT_SPACE_OR_TAB.matchesAnyOf(line))
            .mapToInt(NOT_SPACE_OR_TAB::indexIn)
            .min()
            .orElse(0);
    return lines.stream()
        .map(line -> line.length() < leadingSpace ? "" : line.substring(leadingSpace))
        .collect(joining("\n"));
  }

  private JavadocFormatter() {}
}
