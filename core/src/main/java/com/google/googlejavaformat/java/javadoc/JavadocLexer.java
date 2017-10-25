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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verify;
import static com.google.common.collect.Iterators.peekingIterator;
import static com.google.googlejavaformat.java.javadoc.Token.Type.BEGIN_JAVADOC;
import static com.google.googlejavaformat.java.javadoc.Token.Type.BLOCKQUOTE_CLOSE_TAG;
import static com.google.googlejavaformat.java.javadoc.Token.Type.BLOCKQUOTE_OPEN_TAG;
import static com.google.googlejavaformat.java.javadoc.Token.Type.BR_TAG;
import static com.google.googlejavaformat.java.javadoc.Token.Type.CODE_CLOSE_TAG;
import static com.google.googlejavaformat.java.javadoc.Token.Type.CODE_OPEN_TAG;
import static com.google.googlejavaformat.java.javadoc.Token.Type.END_JAVADOC;
import static com.google.googlejavaformat.java.javadoc.Token.Type.FOOTER_JAVADOC_TAG_START;
import static com.google.googlejavaformat.java.javadoc.Token.Type.FORCED_NEWLINE;
import static com.google.googlejavaformat.java.javadoc.Token.Type.HEADER_CLOSE_TAG;
import static com.google.googlejavaformat.java.javadoc.Token.Type.HEADER_OPEN_TAG;
import static com.google.googlejavaformat.java.javadoc.Token.Type.HTML_COMMENT;
import static com.google.googlejavaformat.java.javadoc.Token.Type.LIST_CLOSE_TAG;
import static com.google.googlejavaformat.java.javadoc.Token.Type.LIST_ITEM_CLOSE_TAG;
import static com.google.googlejavaformat.java.javadoc.Token.Type.LIST_ITEM_OPEN_TAG;
import static com.google.googlejavaformat.java.javadoc.Token.Type.LIST_OPEN_TAG;
import static com.google.googlejavaformat.java.javadoc.Token.Type.LITERAL;
import static com.google.googlejavaformat.java.javadoc.Token.Type.MOE_BEGIN_STRIP_COMMENT;
import static com.google.googlejavaformat.java.javadoc.Token.Type.MOE_END_STRIP_COMMENT;
import static com.google.googlejavaformat.java.javadoc.Token.Type.OPTIONAL_LINE_BREAK;
import static com.google.googlejavaformat.java.javadoc.Token.Type.PARAGRAPH_CLOSE_TAG;
import static com.google.googlejavaformat.java.javadoc.Token.Type.PARAGRAPH_OPEN_TAG;
import static com.google.googlejavaformat.java.javadoc.Token.Type.PRE_CLOSE_TAG;
import static com.google.googlejavaformat.java.javadoc.Token.Type.PRE_OPEN_TAG;
import static com.google.googlejavaformat.java.javadoc.Token.Type.TABLE_CLOSE_TAG;
import static com.google.googlejavaformat.java.javadoc.Token.Type.TABLE_OPEN_TAG;
import static com.google.googlejavaformat.java.javadoc.Token.Type.WHITESPACE;
import static java.lang.String.format;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.PeekingIterator;
import com.google.googlejavaformat.java.javadoc.Token.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Pattern;

/** Lexer for the Javadoc formatter. */
final class JavadocLexer {
  /** Takes a Javadoc comment, including ∕✱✱ and ✱∕, and returns tokens, including ∕✱✱ and ✱∕. */
  static ImmutableList<Token> lex(String input) throws LexException {
    /*
     * TODO(cpovirk): In theory, we should interpret Unicode escapes (yet output them in their
     * original form). This would mean mean everything from an encoded ∕✱✱ to an encoded <pre> tag,
     * so we'll probably never bother.
     */
    input = stripJavadocBeginAndEnd(input);
    input = normalizeLineEndings(input);
    return new JavadocLexer(new CharStream(input)).generateTokens();
  }

  /** The lexer crashes on windows line endings, so for now just normalize to `\n`. */
  // TODO(cushon): use the platform line separator for output
  private static String normalizeLineEndings(String input) {
    return NON_UNIX_LINE_ENDING.matcher(input).replaceAll("\n");
  }

  private static final Pattern NON_UNIX_LINE_ENDING = Pattern.compile("\r\n?");

  private static String stripJavadocBeginAndEnd(String input) {
    /*
     * We do this ahead of time so that the main part of the lexer need not say things like
     * "(?![*]/)" to avoid accidentally swallowing ✱∕ when consuming a newline.
     */
    checkArgument(input.startsWith("/**"), "Missing /**: %s", input);
    checkArgument(input.endsWith("*/") && input.length() > 4, "Missing */: %s", input);
    return input.substring("/**".length(), input.length() - "*/".length());
  }

  private final CharStream input;
  private final NestingCounter braceDepth = new NestingCounter();
  private final NestingCounter preDepth = new NestingCounter();
  private final NestingCounter codeDepth = new NestingCounter();
  private final NestingCounter tableDepth = new NestingCounter();
  private boolean somethingSinceNewline;

  private JavadocLexer(CharStream input) {
    this.input = checkNotNull(input);
  }

  private ImmutableList<Token> generateTokens() throws LexException {
    ImmutableList.Builder<Token> tokens = ImmutableList.builder();

    Token token = new Token(BEGIN_JAVADOC, "/**");
    tokens.add(token);

    while (!input.isExhausted()) {
      token = readToken();
      tokens.add(token);
    }

    checkMatchingTags();

    token = new Token(END_JAVADOC, "*/");
    tokens.add(token);

    ImmutableList<Token> result = tokens.build();
    result = joinAdjacentLiteralsAndAdjacentWhitespace(result);
    result = inferParagraphTags(result);
    result = optionalizeSpacesAfterLinks(result);
    result = deindentPreCodeBlocks(result);
    return result;
  }

  private Token readToken() throws LexException {
    Type type = consumeToken();
    String value = input.readAndResetRecorded();
    return new Token(type, value);
  }

  private Type consumeToken() throws LexException {
    boolean preserveExistingFormatting = preserveExistingFormatting();

    if (input.tryConsumeRegex(NEWLINE_PATTERN)) {
      somethingSinceNewline = false;
      return preserveExistingFormatting ? FORCED_NEWLINE : WHITESPACE;
    } else if (input.tryConsume(" ") || input.tryConsume("\t")) {
      // TODO(cpovirk): How about weird whitespace chars? Ideally we'd distinguish breaking vs. not.
      // Returning LITERAL here prevent us from breaking a <pre> line. For more info, see LITERAL.
      return preserveExistingFormatting ? LITERAL : WHITESPACE;
    }

    /*
     * TODO(cpovirk): Maybe try to detect things like "{@code\n@GwtCompatible}" that aren't intended
     * as tags. But in the most likely case, in which that happens inside <pre>{@code, we have no
     * great options for fixing it.
     * https://github.com/google/google-java-format/issues/7#issuecomment-197383926
     */
    if (!somethingSinceNewline && input.tryConsumeRegex(FOOTER_TAG_PATTERN)) {
      checkMatchingTags();
      somethingSinceNewline = true;
      return FOOTER_JAVADOC_TAG_START;
    }
    somethingSinceNewline = true;

    if (input.tryConsumeRegex(INLINE_TAG_OPEN_PATTERN)) {
      braceDepth.increment();
      return LITERAL;
    } else if (input.tryConsume("{")) {
      braceDepth.incrementIfPositive();
      return LITERAL;
    } else if (input.tryConsume("}")) {
      braceDepth.decrementIfPositive();
      return LITERAL;
    }

    // Inside an inline tag, don't do any HTML interpretation.
    if (braceDepth.isPositive()) {
      verify(input.tryConsumeRegex(LITERAL_PATTERN));
      return LITERAL;
    }

    if (input.tryConsumeRegex(PRE_OPEN_PATTERN)) {
      preDepth.increment();
      return preserveExistingFormatting ? LITERAL : PRE_OPEN_TAG;
    } else if (input.tryConsumeRegex(PRE_CLOSE_PATTERN)) {
      preDepth.decrementIfPositive();
      return preserveExistingFormatting() ? LITERAL : PRE_CLOSE_TAG;
    }

    if (input.tryConsumeRegex(CODE_OPEN_PATTERN)) {
      codeDepth.increment();
      return preserveExistingFormatting ? LITERAL : CODE_OPEN_TAG;
    } else if (input.tryConsumeRegex(CODE_CLOSE_PATTERN)) {
      codeDepth.decrementIfPositive();
      return preserveExistingFormatting() ? LITERAL : CODE_CLOSE_TAG;
    }

    if (input.tryConsumeRegex(TABLE_OPEN_PATTERN)) {
      tableDepth.increment();
      return preserveExistingFormatting ? LITERAL : TABLE_OPEN_TAG;
    } else if (input.tryConsumeRegex(TABLE_CLOSE_PATTERN)) {
      tableDepth.decrementIfPositive();
      return preserveExistingFormatting() ? LITERAL : TABLE_CLOSE_TAG;
    }

    if (preserveExistingFormatting) {
      verify(input.tryConsumeRegex(LITERAL_PATTERN));
      return LITERAL;
    }

    if (input.tryConsumeRegex(PARAGRAPH_OPEN_PATTERN)) {
      return PARAGRAPH_OPEN_TAG;
    } else if (input.tryConsumeRegex(PARAGRAPH_CLOSE_PATTERN)) {
      return PARAGRAPH_CLOSE_TAG;
    } else if (input.tryConsumeRegex(LIST_OPEN_PATTERN)) {
      return LIST_OPEN_TAG;
    } else if (input.tryConsumeRegex(LIST_CLOSE_PATTERN)) {
      return LIST_CLOSE_TAG;
    } else if (input.tryConsumeRegex(LIST_ITEM_OPEN_PATTERN)) {
      return LIST_ITEM_OPEN_TAG;
    } else if (input.tryConsumeRegex(LIST_ITEM_CLOSE_PATTERN)) {
      return LIST_ITEM_CLOSE_TAG;
    } else if (input.tryConsumeRegex(BLOCKQUOTE_OPEN_PATTERN)) {
      return BLOCKQUOTE_OPEN_TAG;
    } else if (input.tryConsumeRegex(BLOCKQUOTE_CLOSE_PATTERN)) {
      return BLOCKQUOTE_CLOSE_TAG;
    } else if (input.tryConsumeRegex(HEADER_OPEN_PATTERN)) {
      return HEADER_OPEN_TAG;
    } else if (input.tryConsumeRegex(HEADER_CLOSE_PATTERN)) {
      return HEADER_CLOSE_TAG;
    } else if (input.tryConsumeRegex(BR_PATTERN)) {
      return BR_TAG;
    } else if (input.tryConsumeRegex(MOE_BEGIN_STRIP_COMMENT_PATTERN)) {
      return MOE_BEGIN_STRIP_COMMENT;
    } else if (input.tryConsumeRegex(MOE_END_STRIP_COMMENT_PATTERN)) {
      return MOE_END_STRIP_COMMENT;
    } else if (input.tryConsumeRegex(HTML_COMMENT_PATTERN)) {
      return HTML_COMMENT;
    } else if (input.tryConsumeRegex(LITERAL_PATTERN)) {
      return LITERAL;
    }
    throw new AssertionError();
  }

  private boolean preserveExistingFormatting() {
    return preDepth.isPositive() || tableDepth.isPositive() || codeDepth.isPositive();
  }

  private void checkMatchingTags() throws LexException {
    if (braceDepth.isPositive()
        || preDepth.isPositive()
        || tableDepth.isPositive()
        || codeDepth.isPositive()) {
      throw new LexException();
    }
  }

  /**
   * Join together adjacent literal tokens, and join together adjacent whitespace tokens.
   *
   * <p>For literal tokens, this means something like {@code ["<b>", "foo", "</b>"] =>
   * ["<b>foo</b>"]}. See {@link #LITERAL_PATTERN} for discussion of why those tokens are separate
   * to begin with.
   *
   * <p>Whitespace tokens are treated analogously. We don't really "want" to join whitespace tokens,
   * but in the course of joining literals, we incidentally join whitespace, too. We do take
   * advantage of the joining later on: It simplifies {@link #inferParagraphTags}.
   *
   * <p>Note that we do <i>not</i> merge a literal token and a whitespace token together.
   */
  private static ImmutableList<Token> joinAdjacentLiteralsAndAdjacentWhitespace(List<Token> input) {
    /*
     * Note: Our final token is always END_JAVADOC. This saves us some trouble:
     *
     * - Our inner while() doesn't need a hasNext() check.
     *
     * - We don't need to check for leftover accumulated literals after we exit the loop.
     */
    ImmutableList.Builder<Token> output = ImmutableList.builder();
    StringBuilder accumulated = new StringBuilder();

    for (PeekingIterator<Token> tokens = peekingIterator(input.iterator()); tokens.hasNext(); ) {
      if (tokens.peek().getType() == LITERAL) {
        accumulated.append(tokens.peek().getValue());
        tokens.next();
        continue;
      }

      /*
       * IF we have accumulated some literals to join together (say, "foo<b>bar</b>"), and IF we'll
       * next see whitespace followed by a "@" literal, we need to join that together with the
       * previous literals. That ensures that we won't insert a line break before the "@," turning
       * it into a tag.
       */

      if (accumulated.length() == 0) {
        output.add(tokens.peek());
        tokens.next();
        continue;
      }

      StringBuilder seenWhitespace = new StringBuilder();
      while (tokens.peek().getType() == WHITESPACE) {
        seenWhitespace.append(tokens.next().getValue());
      }

      if (tokens.peek().getType() == LITERAL && tokens.peek().getValue().startsWith("@")) {
        // OK, we're in the case described above.
        accumulated.append(" ");
        accumulated.append(tokens.peek().getValue());
        tokens.next();
        continue;
      }

      output.add(new Token(LITERAL, accumulated.toString()));
      accumulated.setLength(0);

      if (seenWhitespace.length() > 0) {
        output.add(new Token(WHITESPACE, seenWhitespace.toString()));
      }

      // We have another token coming, possibly of type OTHER. Leave it for the next iteration.
    }

    /*
     * TODO(cpovirk): Another case where we could try to join tokens is if a line ends with
     * /[^ -]-/, as in "non-\nblocking."
     */
    return output.build();
  }

  /**
   * Where the input has two consecutive line breaks between literals, insert a {@code <p>} tag
   * between the literals.
   *
   * <p>This method must be called after {@link #joinAdjacentLiteralsAndAdjacentWhitespace}, as it
   * assumes that adjacent whitespace tokens have already been joined.
   */
  private static ImmutableList<Token> inferParagraphTags(List<Token> input) {
    ImmutableList.Builder<Token> output = ImmutableList.builder();

    for (PeekingIterator<Token> tokens = peekingIterator(input.iterator()); tokens.hasNext(); ) {
      if (tokens.peek().getType() == LITERAL) {
        output.add(tokens.next());

        if (tokens.peek().getType() == WHITESPACE
            && hasMultipleNewlines(tokens.peek().getValue())) {
          output.add(tokens.next());

          if (tokens.peek().getType() == LITERAL) {
            output.add(new Token(PARAGRAPH_OPEN_TAG, "<p>"));
          }
        }
      } else {
        // TODO(cpovirk): Or just `continue` from the <p> case and move this out of the `else`?
        output.add(tokens.next());
      }
    }

    return output.build();

    /*
     * Note: We do not want to insert <p> tags inside <pre>. Fortunately, the formatter gets that
     * right without special effort on our part. The reason: Line breaks inside a <pre> section are
     * of type FORCED_NEWLINE rather than WHITESPACE.
     */
  }

  /**
   * Replaces whitespace after a {@code href=...>} token with an "optional link break." This allows
   * us to output either {@code <a href=foo>foo</a>} or {@code <a href=foo>\nfoo</a>}, depending on
   * how much space we have left on the line.
   *
   * <p>This method must be called after {@link #joinAdjacentLiteralsAndAdjacentWhitespace}, as it
   * assumes that adjacent whitespace tokens have already been joined.
   */
  private static ImmutableList<Token> optionalizeSpacesAfterLinks(List<Token> input) {
    ImmutableList.Builder<Token> output = ImmutableList.builder();

    for (PeekingIterator<Token> tokens = peekingIterator(input.iterator()); tokens.hasNext(); ) {
      if (tokens.peek().getType() == LITERAL && tokens.peek().getValue().matches("^href=[^>]*>")) {
        output.add(tokens.next());

        if (tokens.peek().getType() == WHITESPACE) {
          output.add(new Token(OPTIONAL_LINE_BREAK, tokens.next().getValue()));
        }
      } else {
        output.add(tokens.next());
      }
    }

    return output.build();

    /*
     * Note: We do not want to insert <p> tags inside <pre>. Fortunately, the formatter gets that
     * right without special effort on our part. The reason: Line breaks inside a <pre> section are
     * of type FORCED_NEWLINE rather than WHITESPACE.
     */
  }

  /**
   * Adjust indentation inside `<pre>{@code` blocks.
   *
   * <p>Also trim leading and trailing blank lines, and move the trailing `}` to its own line.
   */
  private static ImmutableList<Token> deindentPreCodeBlocks(List<Token> input) {
    ImmutableList.Builder<Token> output = ImmutableList.builder();
    for (PeekingIterator<Token> tokens = peekingIterator(input.iterator()); tokens.hasNext(); ) {
      if (tokens.peek().getType() != PRE_OPEN_TAG) {
        output.add(tokens.next());
        continue;
      }

      output.add(tokens.next());
      List<Token> initialNewlines = new ArrayList<>();
      while (tokens.hasNext() && tokens.peek().getType() == FORCED_NEWLINE) {
        initialNewlines.add(tokens.next());
      }
      if (tokens.peek().getType() != LITERAL
          || !tokens.peek().getValue().matches("[ \t]*[{]@code")) {
        output.addAll(initialNewlines);
        output.add(tokens.next());
        continue;
      }

      deindentPreCodeBlock(output, tokens);
    }
    return output.build();
  }

  private static void deindentPreCodeBlock(
      ImmutableList.Builder<Token> output, PeekingIterator<Token> tokens) {
    Deque<Token> saved = new ArrayDeque<>();
    output.add(new Token(LITERAL, tokens.next().getValue().trim()));
    while (tokens.hasNext() && tokens.peek().getType() != PRE_CLOSE_TAG) {
      Token token = tokens.next();
      saved.addLast(token);
    }
    while (!saved.isEmpty() && saved.peekFirst().getType() == FORCED_NEWLINE) {
      saved.removeFirst();
    }
    while (!saved.isEmpty() && saved.peekLast().getType() == FORCED_NEWLINE) {
      saved.removeLast();
    }
    if (saved.isEmpty()) {
      return;
    }

    // move the trailing `}` to its own line
    Token last = saved.peekLast();
    boolean trailingBrace = false;
    if (last.getType() == LITERAL && last.getValue().endsWith("}")) {
      saved.removeLast();
      if (last.length() > 1) {
        saved.addLast(
            new Token(LITERAL, last.getValue().substring(0, last.getValue().length() - 1)));
        saved.addLast(new Token(FORCED_NEWLINE, null));
      }
      trailingBrace = true;
    }

    int trim = -1;
    for (Token token : saved) {
      if (token.getType() == LITERAL) {
        int idx = CharMatcher.isNot(' ').indexIn(token.getValue());
        if (idx != -1 && (trim == -1 || idx < trim)) {
          trim = idx;
        }
      }
    }

    output.add(new Token(FORCED_NEWLINE, "\n"));
    for (Token token : saved) {
      if (token.getType() == LITERAL) {
        output.add(
            new Token(
                LITERAL,
                trim > 0 && token.length() > trim
                    ? token.getValue().substring(trim)
                    : token.getValue()));
      } else {
        output.add(token);
      }
    }

    if (trailingBrace) {
      output.add(new Token(LITERAL, "}"));
    } else {
      output.add(new Token(FORCED_NEWLINE, "\n"));
    }
  }

  private static final CharMatcher NEWLINE = CharMatcher.is('\n');

  private static boolean hasMultipleNewlines(String s) {
    return NEWLINE.countIn(s) > 1;
  }

  /*
   * This also eats any trailing whitespace. We would be smart enough to ignore that, anyway --
   * except in the case of <pre>/<table>, inside which we otherwise leave whitespace intact.
   *
   * We'd remove the trailing whitespace later on (in JavaCommentsHelper.rewrite), but I feel safer
   * stripping it now: It otherwise might confuse our line-length count, which we use for wrapping.
   */
  private static final Pattern NEWLINE_PATTERN = compile("^[ \t]*\n[ \t]*[*]?[ \t]?");

  // We ensure elsewhere that we match this only at the beginning of a line.
  // Only match tags that start with a lowercase letter, to avoid false matches on unescaped
  // annotations inside code blocks.
  // Match "@param <T>" specially in case the <T> is a <P> or other HTML tag we treat specially.
  private static final Pattern FOOTER_TAG_PATTERN = compile("^@(param\\s+<\\w+>|[a-z]\\w*)");
  private static final Pattern MOE_BEGIN_STRIP_COMMENT_PATTERN =
      compile("^<!--\\s*MOE:begin_intracomment_strip\\s*-->");
  private static final Pattern MOE_END_STRIP_COMMENT_PATTERN =
      compile("^<!--\\s*MOE:end_intracomment_strip\\s*-->");
  private static final Pattern HTML_COMMENT_PATTERN = fullCommentPattern();
  private static final Pattern PRE_OPEN_PATTERN = openTagPattern("pre");
  private static final Pattern PRE_CLOSE_PATTERN = closeTagPattern("pre");
  private static final Pattern CODE_OPEN_PATTERN = openTagPattern("code");
  private static final Pattern CODE_CLOSE_PATTERN = closeTagPattern("code");
  private static final Pattern TABLE_OPEN_PATTERN = openTagPattern("table");
  private static final Pattern TABLE_CLOSE_PATTERN = closeTagPattern("table");
  private static final Pattern LIST_OPEN_PATTERN = openTagPattern("ul|ol|dl");
  private static final Pattern LIST_CLOSE_PATTERN = closeTagPattern("ul|ol|dl");
  private static final Pattern LIST_ITEM_OPEN_PATTERN = openTagPattern("li|dt|dd");
  private static final Pattern LIST_ITEM_CLOSE_PATTERN = closeTagPattern("li|dt|dd");
  private static final Pattern HEADER_OPEN_PATTERN = openTagPattern("h[1-6]");
  private static final Pattern HEADER_CLOSE_PATTERN = closeTagPattern("h[1-6]");
  private static final Pattern PARAGRAPH_OPEN_PATTERN = openTagPattern("p");
  private static final Pattern PARAGRAPH_CLOSE_PATTERN = closeTagPattern("p");
  private static final Pattern BLOCKQUOTE_OPEN_PATTERN = openTagPattern("blockquote");
  private static final Pattern BLOCKQUOTE_CLOSE_PATTERN = closeTagPattern("blockquote");
  private static final Pattern BR_PATTERN = openTagPattern("br");
  private static final Pattern INLINE_TAG_OPEN_PATTERN = compile("^[{]@\\w*");
  /*
   * We exclude < so that we don't swallow following HTML tags. This lets us fix up "foo<p>" (~400
   * hits in Google-internal code). We will join unnecessarily split "words" (like "foo<b>bar</b>")
   * in a later step. There's a similar story for braces. I'm not sure I actually need to exclude @
   * or *. TODO(cpovirk): Try removing them.
   *
   * Thanks to the "rejoin" step in joinAdjacentLiteralsAndAdjacentWhitespace(), we could get away
   * with matching only one character here. That would eliminate the need for the regex entirely.
   * That might be faster or slower than what we do now.
   */
  private static final Pattern LITERAL_PATTERN = compile("^.[^ \t\n@<{}*]*", DOTALL);

  private static Pattern fullCommentPattern() {
    return compile("^<!--.*?-->", DOTALL);
  }

  private static Pattern openTagPattern(String namePattern) {
    return compile(format("^<(?:%s)\\b[^>]*>", namePattern), CASE_INSENSITIVE);
  }

  private static Pattern closeTagPattern(String namePattern) {
    return compile(format("^</(?:%s)\\b[^>]*>", namePattern), CASE_INSENSITIVE);
  }

  static class LexException extends Exception {}
}
