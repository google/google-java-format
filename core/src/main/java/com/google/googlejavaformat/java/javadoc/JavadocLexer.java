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
import static java.lang.String.format;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.PeekingIterator;
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

/** Lexer for the Javadoc formatter. */
final class JavadocLexer {
  /** Takes a Javadoc comment, including ∕✱✱ and ✱∕, and returns tokens, including ∕✱✱ and ✱∕. */
  static ImmutableList<Token> lex(String input, boolean classicJavadoc) throws LexException {
    MarkdownPositions markdownPositions;
    if (classicJavadoc) {
      /*
       * TODO(cpovirk): In theory, we should interpret Unicode escapes (yet output them in their
       * original form). This would mean mean everything from an encoded ∕✱✱ to an encoded <pre>
       * tag, so we'll probably never bother.
       */
      input = stripJavadocBeginAndEnd(input);
      markdownPositions = MarkdownPositions.EMPTY;
    } else {
      checkArgument(input.startsWith("///"));
      input = input.substring("///".length());
      markdownPositions = MarkdownPositions.parse(input);
    }
    input = normalizeLineEndings(input);
    return new JavadocLexer(new CharStream(input), markdownPositions, classicJavadoc)
        .generateTokens();
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
  private final boolean classicJavadoc;
  private final MarkdownPositions markdownPositions;
  private final NestingStack braceStack = new NestingStack();
  private final NestingStack preStack = new NestingStack();
  private final NestingStack codeStack = new NestingStack();
  private final NestingStack tableStack = new NestingStack();
  private boolean outerInlineTagIsSnippet;
  private boolean somethingSinceNewline;

  private JavadocLexer(
      CharStream input, MarkdownPositions markdownPositions, boolean classicJavadoc) {
    this.input = checkNotNull(input);
    this.markdownPositions = markdownPositions;
    this.classicJavadoc = classicJavadoc;
  }

  private ImmutableList<Token> generateTokens() throws LexException {
    ImmutableList.Builder<Token> tokens = ImmutableList.builder();

    Token token = new BeginJavadoc(classicJavadoc ? "/**" : "///");
    tokens.add(token);

    while (!input.isExhausted()) {
      for (Token markdownToken : markdownPositions.tokensAt(input.position())) {
        boolean consumed = input.tryConsume(markdownToken.value());
        verify(consumed, "Did not consume markdown token: %s", markdownToken);
        var unused = input.readAndResetRecorded();
        tokens.add(markdownToken);
      }
      token = readToken();
      tokens.add(token);
    }

    checkMatchingTags();

    token = new EndJavadoc(classicJavadoc ? "*/" : "");
    tokens.add(token);

    ImmutableList<Token> result = tokens.build();
    result = joinAdjacentLiteralsAndAdjacentWhitespace(result);
    if (classicJavadoc) {
      result = inferParagraphTags(result);
    }
    result = optionalizeSpacesAfterLinks(result);
    result = deindentPreCodeBlocks(result);
    return result;
  }

  private Token readToken() throws LexException {
    Function<String, Token> tokenFactory = consumeToken();
    String value = input.readAndResetRecorded();
    return tokenFactory.apply(value);
  }

  private Function<String, Token> consumeToken() throws LexException {
    boolean preserveExistingFormatting = preserveExistingFormatting();

    Pattern newlinePattern = classicJavadoc ? CLASSIC_NEWLINE_PATTERN : MARKDOWN_NEWLINE_PATTERN;
    if (input.tryConsumeRegex(newlinePattern)) {
      somethingSinceNewline = false;
      return preserveExistingFormatting ? ForcedNewline::new : Whitespace::new;
    } else if (input.tryConsume(" ") || input.tryConsume("\t")) {
      // TODO(cpovirk): How about weird whitespace chars? Ideally we'd distinguish breaking vs. not.
      // Returning Literal here prevents us from breaking a <pre> line. For more info, see Literal.
      return preserveExistingFormatting ? Literal::new : Whitespace::new;
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
      return FooterJavadocTagStart::new;
    }
    somethingSinceNewline = true;

    if (input.tryConsumeRegex(SNIPPET_TAG_OPEN_PATTERN)) {
      if (braceStack.isEmpty()) {
        braceStack.push();
        outerInlineTagIsSnippet = true;
        return SnippetBegin::new;
      }
      braceStack.push();
      return Literal::new;
    } else if (input.tryConsumeRegex(INLINE_TAG_OPEN_PATTERN)) {
      braceStack.push();
      return Literal::new;
    } else if (input.tryConsume("{")) {
      braceStack.incrementIfPositive();
      return Literal::new;
    } else if (input.tryConsume("}")) {
      if (outerInlineTagIsSnippet && braceStack.total() == 1) {
        braceStack.popIfNotEmpty();
        outerInlineTagIsSnippet = false;
        return SnippetEnd::new;
      }
      braceStack.popIfNotEmpty();
      return Literal::new;
    }

    // Inside an inline tag, don't do any HTML interpretation.
    if (!braceStack.isEmpty()) {
      verify(input.tryConsumeRegex(literalPattern()));
      return Literal::new;
    }

    if (input.tryConsumeRegex(PRE_OPEN_PATTERN)) {
      preStack.push();
      return preserveExistingFormatting ? Literal::new : PreOpenTag::new;
    } else if (input.tryConsumeRegex(PRE_CLOSE_PATTERN)) {
      preStack.popIfNotEmpty();
      return preserveExistingFormatting() ? Literal::new : PreCloseTag::new;
    }

    if (input.tryConsumeRegex(CODE_OPEN_PATTERN)) {
      codeStack.push();
      return preserveExistingFormatting ? Literal::new : CodeOpenTag::new;
    } else if (input.tryConsumeRegex(CODE_CLOSE_PATTERN)) {
      codeStack.popIfNotEmpty();
      return preserveExistingFormatting() ? Literal::new : CodeCloseTag::new;
    }

    if (input.tryConsumeRegex(TABLE_OPEN_PATTERN)) {
      tableStack.push();
      return preserveExistingFormatting ? Literal::new : TableOpenTag::new;
    } else if (input.tryConsumeRegex(TABLE_CLOSE_PATTERN)) {
      tableStack.popIfNotEmpty();
      return preserveExistingFormatting() ? Literal::new : TableCloseTag::new;
    }

    if (preserveExistingFormatting) {
      verify(input.tryConsumeRegex(literalPattern()));
      return Literal::new;
    }

    if (input.tryConsumeRegex(PARAGRAPH_OPEN_PATTERN)) {
      return ParagraphOpenTag::new;
    } else if (input.tryConsumeRegex(PARAGRAPH_CLOSE_PATTERN)) {
      return ParagraphCloseTag::new;
    } else if (input.tryConsumeRegex(LIST_OPEN_PATTERN)) {
      return ListOpenTag::new;
    } else if (input.tryConsumeRegex(LIST_CLOSE_PATTERN)) {
      return ListCloseTag::new;
    } else if (input.tryConsumeRegex(LIST_ITEM_OPEN_PATTERN)) {
      return ListItemOpenTag::new;
    } else if (input.tryConsumeRegex(LIST_ITEM_CLOSE_PATTERN)) {
      return ListItemCloseTag::new;
    } else if (input.tryConsumeRegex(BLOCKQUOTE_OPEN_PATTERN)) {
      return BlockquoteOpenTag::new;
    } else if (input.tryConsumeRegex(BLOCKQUOTE_CLOSE_PATTERN)) {
      return BlockquoteCloseTag::new;
    } else if (input.tryConsumeRegex(HEADER_OPEN_PATTERN)) {
      return HeaderOpenTag::new;
    } else if (input.tryConsumeRegex(HEADER_CLOSE_PATTERN)) {
      return HeaderCloseTag::new;
    } else if (input.tryConsumeRegex(BR_PATTERN)) {
      return BrTag::new;
    } else if (input.tryConsumeRegex(MOE_BEGIN_STRIP_COMMENT_PATTERN)) {
      return MoeBeginStripComment::new;
    } else if (input.tryConsumeRegex(MOE_END_STRIP_COMMENT_PATTERN)) {
      return MoeEndStripComment::new;
    } else if (input.tryConsumeRegex(HTML_COMMENT_PATTERN)) {
      return HtmlComment::new;
    } else if (input.tryConsumeRegex(literalPattern())) {
      return Literal::new;
    }
    throw new AssertionError();
  }

  private boolean preserveExistingFormatting() {
    return !preStack.isEmpty()
        || !tableStack.isEmpty()
        || !codeStack.isEmpty()
        || outerInlineTagIsSnippet;
  }

  private void checkMatchingTags() throws LexException {
    if (!braceStack.isEmpty()
        || !preStack.isEmpty()
        || !tableStack.isEmpty()
        || !codeStack.isEmpty()) {
      throw new LexException();
    }
  }

  /**
   * Join together adjacent literal tokens, and join together adjacent whitespace tokens.
   *
   * <p>For literal tokens, this means something like {@code ["<b>", "foo", "</b>"] =>
   * ["<b>foo</b>"]}. See {@link #literalPattern()} for discussion of why those tokens are separate
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
      if (tokens.peek() instanceof Literal) {
        accumulated.append(tokens.next().value());
        continue;
      }

      /*
       * IF we have accumulated some literals to join together (say, "foo<b>bar</b>"), and IF we'll
       * next see whitespace followed by a "@" literal, we need to join that together with the
       * previous literals. That ensures that we won't insert a line break before the "@," turning
       * it into a tag.
       */

      if (accumulated.length() == 0) {
        output.add(tokens.next());
        continue;
      }

      StringBuilder seenWhitespace = new StringBuilder();
      while (tokens.peek() instanceof Whitespace) {
        seenWhitespace.append(tokens.next().value());
      }

      if (tokens.peek() instanceof Literal literal && literal.value().startsWith("@")) {
        // OK, we're in the case described above.
        accumulated.append(" ");
        accumulated.append(tokens.next().value());
        continue;
      }

      output.add(new Literal(accumulated.toString()));
      accumulated.setLength(0);

      if (seenWhitespace.length() > 0) {
        output.add(new Whitespace(seenWhitespace.toString()));
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
      if (tokens.peek() instanceof Literal) {
        output.add(tokens.next());

        if (tokens.peek() instanceof Whitespace && hasMultipleNewlines(tokens.peek().value())) {
          output.add(tokens.next());

          if (tokens.peek() instanceof Literal) {
            output.add(new ParagraphOpenTag("<p>"));
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
      if (tokens.peek() instanceof Literal && tokens.peek().value().matches("href=[^>]*>")) {
        output.add(tokens.next());

        if (tokens.peek() instanceof Whitespace) {
          output.add(new OptionalLineBreak(tokens.next().value()));
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
    // TODO: b/323389829 - De-indent {@snippet ...} blocks, too.
    ImmutableList.Builder<Token> output = ImmutableList.builder();
    for (PeekingIterator<Token> tokens = peekingIterator(input.iterator()); tokens.hasNext(); ) {
      if (!(tokens.peek() instanceof PreOpenTag)) {
        output.add(tokens.next());
        continue;
      }

      output.add(tokens.next());
      List<Token> initialNewlines = new ArrayList<>();
      while (tokens.hasNext() && tokens.peek() instanceof ForcedNewline) {
        initialNewlines.add(tokens.next());
      }
      if (!(tokens.peek() instanceof Literal) || !tokens.peek().value().matches("[ \t]*[{]@code")) {
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
    output.add(new Literal(tokens.next().value().trim()));
    while (tokens.hasNext() && !(tokens.peek() instanceof PreCloseTag)) {
      Token token = tokens.next();
      saved.addLast(token);
    }
    while (!saved.isEmpty() && saved.peekFirst() instanceof ForcedNewline) {
      saved.removeFirst();
    }
    while (!saved.isEmpty() && saved.peekLast() instanceof ForcedNewline) {
      saved.removeLast();
    }
    if (saved.isEmpty()) {
      return;
    }

    // move the trailing `}` to its own line
    Token last = saved.peekLast();
    boolean trailingBrace = false;
    if (last instanceof Literal && last.value().endsWith("}")) {
      saved.removeLast();
      if (last.length() > 1) {
        saved.addLast(new Literal(last.value().substring(0, last.value().length() - 1)));
        saved.addLast(new ForcedNewline(null));
      }
      trailingBrace = true;
    }

    int trim = -1;
    for (Token token : saved) {
      if (token instanceof Literal) {
        int idx = CharMatcher.isNot(' ').indexIn(token.value());
        if (idx != -1 && (trim == -1 || idx < trim)) {
          trim = idx;
        }
      }
    }

    output.add(new ForcedNewline("\n"));
    for (Token token : saved) {
      if (token instanceof Literal) {
        output.add(
            new Literal(
                trim > 0 && token.length() > trim ? token.value().substring(trim) : token.value()));
      } else {
        output.add(token);
      }
    }

    if (trailingBrace) {
      output.add(new Literal("}"));
    } else {
      output.add(new ForcedNewline("\n"));
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
  private static final Pattern CLASSIC_NEWLINE_PATTERN = compile("[ \t]*\n[ \t]*[*]?[ \t]?");
  private static final Pattern MARKDOWN_NEWLINE_PATTERN = compile("[ \t]*\n[ \t]*");

  // We ensure elsewhere that we match this only at the beginning of a line.
  // Only match tags that start with a lowercase letter, to avoid false matches on unescaped
  // annotations inside code blocks.
  // Match "@param <T>" specially in case the <T> is a <P> or other HTML tag we treat specially.
  private static final Pattern FOOTER_TAG_PATTERN = compile("@(param\\s+<\\w+>|[a-z]\\w*)");
  private static final Pattern MOE_BEGIN_STRIP_COMMENT_PATTERN =
      compile("<!--\\s*M" + "OE:begin_intracomment_strip\\s*-->");
  private static final Pattern MOE_END_STRIP_COMMENT_PATTERN =
      compile("<!--\\s*M" + "OE:end_intracomment_strip\\s*-->");
  private static final Pattern HTML_COMMENT_PATTERN = compile("<!--.*?-->", DOTALL);
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
  private static final Pattern SNIPPET_TAG_OPEN_PATTERN = compile("[{]@snippet\\b");
  private static final Pattern INLINE_TAG_OPEN_PATTERN = compile("[{]@\\w*");

  /*
   * We exclude < so that we don't swallow following HTML tags. This lets us fix up "foo<p>" (~400
   * hits in Google-internal code).
   *
   * TODO(cpovirk): might not need to exclude @ or *.
   */
  private static final Pattern CLASSIC_LITERAL_PATTERN = compile(".[^ \t\n@<{}*]*", DOTALL);

  /*
   * Many characters have special meaning in Markdown. Rather than list them all, we'll just match
   * a sequence of alphabetic characters. Even digits can have special meaning, for numbered lists.
   */
  private static final Pattern MARKDOWN_LITERAL_PATTERN = compile(".\\p{IsAlphabetic}*", DOTALL);

  /**
   * The pattern used for "literals", things that do not have any special formatting meaning. This
   * doesn't have to be a maximal sequence of literal characters, since adjacent literals will be
   * joined together in a later step.
   */
  private Pattern literalPattern() {
    return classicJavadoc ? CLASSIC_LITERAL_PATTERN : MARKDOWN_LITERAL_PATTERN;
  }

  private static Pattern openTagPattern(String namePattern) {
    return compile(format("<(?:%s)\\b[^>]*>", namePattern), CASE_INSENSITIVE);
  }

  private static Pattern closeTagPattern(String namePattern) {
    return compile(format("</(?:%s)\\b[^>]*>", namePattern), CASE_INSENSITIVE);
  }

  static class LexException extends Exception {}
}
