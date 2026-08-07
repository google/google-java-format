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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Comparators.max;
import static com.google.googlejavaformat.java.javadoc.JavadocWriter.AutoIndent.AUTO_INDENT;
import static com.google.googlejavaformat.java.javadoc.JavadocWriter.AutoIndent.NO_AUTO_INDENT;
import static com.google.googlejavaformat.java.javadoc.JavadocWriter.RequestedWhitespace.BLANK_LINE;
import static com.google.googlejavaformat.java.javadoc.JavadocWriter.RequestedWhitespace.NEWLINE;
import static com.google.googlejavaformat.java.javadoc.JavadocWriter.RequestedWhitespace.NONE;
import static com.google.googlejavaformat.java.javadoc.JavadocWriter.RequestedWhitespace.WHITESPACE;

import com.google.googlejavaformat.java.javadoc.Token.BlockQuoteCloseTag;
import com.google.googlejavaformat.java.javadoc.Token.BlockQuoteOpenTag;
import com.google.googlejavaformat.java.javadoc.Token.BrTag;
import com.google.googlejavaformat.java.javadoc.Token.CodeCloseTag;
import com.google.googlejavaformat.java.javadoc.Token.CodeOpenTag;
import com.google.googlejavaformat.java.javadoc.Token.FooterJavadocTagStart;
import com.google.googlejavaformat.java.javadoc.Token.HeaderCloseTag;
import com.google.googlejavaformat.java.javadoc.Token.HeaderOpenTag;
import com.google.googlejavaformat.java.javadoc.Token.HtmlComment;
import com.google.googlejavaformat.java.javadoc.Token.ListCloseTag;
import com.google.googlejavaformat.java.javadoc.Token.ListItemOpenTag;
import com.google.googlejavaformat.java.javadoc.Token.ListOpenTag;
import com.google.googlejavaformat.java.javadoc.Token.Literal;
import com.google.googlejavaformat.java.javadoc.Token.MarkdownBlockQuoteOpen;
import com.google.googlejavaformat.java.javadoc.Token.MarkdownFencedCodeBlock;
import com.google.googlejavaformat.java.javadoc.Token.MarkdownTable;
import com.google.googlejavaformat.java.javadoc.Token.MoeBeginStripComment;
import com.google.googlejavaformat.java.javadoc.Token.MoeEndStripComment;
import com.google.googlejavaformat.java.javadoc.Token.PreCloseTag;
import com.google.googlejavaformat.java.javadoc.Token.PreOpenTag;
import com.google.googlejavaformat.java.javadoc.Token.SnippetBegin;
import com.google.googlejavaformat.java.javadoc.Token.SnippetEnd;
import com.google.googlejavaformat.java.javadoc.Token.StartOfLineToken;
import com.google.googlejavaformat.java.javadoc.Token.TableCloseTag;
import com.google.googlejavaformat.java.javadoc.Token.TableOpenTag;
import com.google.googlejavaformat.java.javadoc.Token.Whitespace;
import java.util.List;

/**
 * Stateful object that accepts "requests" and "writes," producing formatted Javadoc.
 *
 * <p>Our Javadoc formatter doesn't ever generate a parse tree, only a stream of tokens, so the
 * writer must compute and store the answer to questions like "How many levels of nested HTML list
 * are we inside?"
 */
final class JavadocWriter {

  private static final Literal BACKSLASH_LITERAL = new Literal("\\");

  private final int blockIndent;
  private final boolean classicJavadoc;
  private final StringBuilder output = new StringBuilder();

  /**
   * Whether we are inside an {@code <li>} element, excluding the case in which the {@code <li>}
   * contains a {@code <ul>} or {@code <ol>} that we are also inside -- unless of course we're
   * inside an {@code <li>} element in that inner list :)
   */
  private boolean continuingListItemOfInnermostList;

  private boolean continuingFooterTag;
  private final NestingStack<Indent> indentStack = new NestingStack<>();
  private final NestingStack.Int postWriteModifiedContinuingListStack = new NestingStack.Int();
  private int remainingOnLine;
  private boolean atStartOfLine;
  private RequestedWhitespace requestedWhitespace = NONE;
  private Token requestedMoeBeginStripComment;
  private String indentForMoeEndStripComment = "";
  private boolean wroteAnythingSignificant;

  JavadocWriter(int blockIndent, boolean classicJavadoc) {
    this.blockIndent = blockIndent;
    this.classicJavadoc = classicJavadoc;
  }

  /**
   * Requests whitespace between the previously written token and the next written token. The
   * request may be honored, or it may be overridden by a request for "more significant" whitespace,
   * like a newline.
   */
  void requestWhitespace() {
    requestWhitespace(WHITESPACE);
  }

  private void requestWhitespace(RequestedWhitespace requestedWhitespace) {
    this.requestedWhitespace = max(requestedWhitespace, this.requestedWhitespace);
  }

  /**
   * Requests whitespace or a blank line depending on the whitespace token.
   *
   * <p>In Markdown Javadoc, if the whitespace token contains multiple newlines, it represents a
   * blank line in the input (e.g., between a paragraph and a list, or between loose list items). We
   * want to preserve these blank lines, so we request a blank line. Otherwise, or in classic
   * Javadoc (where blank lines are handled via inferred {@code <p>} tags), we just request standard
   * whitespace.
   */
  void requestWhitespaceOrBlankLine(Whitespace token) {
    if (!classicJavadoc && JavadocLexer.hasMultipleNewlines(token.value())) {
      requestBlankLine();
    } else {
      requestWhitespace();
    }
  }

  void requestMoeBeginStripComment(MoeBeginStripComment token) {
    // We queue this up so that we can put it after any requested whitespace.
    requestedMoeBeginStripComment = checkNotNull(token);
  }

  void writeBeginJavadoc() {
    if (classicJavadoc) {
      /*
       * JavaCommentsHelper will make sure this is indented right. But it seems sensible enough
       * that, if our input starts with ∕✱✱, so too does our output.
       */
      output.append("/**");
      writeNewline();
    } else {
      output.append("/// ");
      remainingOnLine = JavadocFormatter.MAX_LINE_LENGTH - blockIndent - 4;
    }
  }

  void writeEndJavadoc() {
    if (classicJavadoc) {
      output.append("\n");
      appendSpaces(blockIndent + 1);
      output.append("*/");
    }
  }

  void writeFooterJavadocTagStart(FooterJavadocTagStart token) {
    // Close any unclosed lists (e.g., <li> without <ul>).
    // TODO(cpovirk): Actually generate </ul>, etc.?
    /*
     * TODO(cpovirk): Also generate </pre> and </table> if appropriate. This is necessary for
     * idempotency in broken Javadoc. (We don't necessarily need that, but full idempotency may be a
     * nice goal, especially if it helps us use a fuzzer to test.) Unfortunately, the writer doesn't
     * currently know which of those tags are open.
     */
    continuingListItemOfInnermostList = false;
    indentStack.reset();
    /*
     * There's probably no need for this, since its only effect is to disable blank lines in some
     * cases -- and we're doing that already in the footer.
     */
    postWriteModifiedContinuingListStack.reset();

    if (!continuingFooterTag) {
      // First footer tag after a body tag.
      requestBlankLine();
    } else {
      // Subsequent footer tag.
      continuingFooterTag = false;
      requestNewline();
    }
    writeToken(token);
    continuingFooterTag = true;
  }

  void writeSnippetBegin(SnippetBegin token) {
    requestBlankLine();
    writeToken(token);
    /*
     * We don't request a newline here because we should have at least a colon following on this
     * line, and we may have attributes after that.
     *
     * (If we find it convenient, we could instead consume the entire rest of the line as part of
     * the same token as `{@snippet` itself. But we already would never split the rest of the line
     * across lines (because we preserve whitespace), so that might not accomplish anything. Plus,
     * we'd probably want to be careful not to swallow an expectedly early closing `}`.)
     */
  }

  void writeSnippetEnd(SnippetEnd token) {
    /*
     * We don't request a newline here because we have preserved all newlines that existed in the
     * input. TODO: b/323389829 - Improve upon that. Specifically:
     *
     * - If there is not yet a newline, we should add one.
     *
     * - If there are multiple newlines, we should probably collapse them.
     *
     * - If the closing brace isn't indented as we'd want (as in the link below, in which the whole
     *   @apiNote isn't indented), we should indent it.
     *
     * https://github.com/openjdk/jdk/blob/1ebf2cf639300728ffc024784f5dc1704317b0b3/src/java.base/share/classes/java/util/Collections.java#L5993-L6006
     */
    writeToken(token);
    requestBlankLine();
  }

  void writeListOpen(ListOpenTag token) {
    if (classicJavadoc) {
      requestBlankLine();
    }

    writeToken(token);
    continuingListItemOfInnermostList = false;
    int indent = token.value().isEmpty() ? 0 : 2; // No indent for Markdown since no explicit open
    indentStack.push(new ListIndent(indent));
    postWriteModifiedContinuingListStack.push();

    requestNewline();
  }

  void writeListClose(ListCloseTag token) {
    if (classicJavadoc) {
      requestNewline();
    }

    indentStack.popUntil(ListIndent.class);
    writeToken(token);
    postWriteModifiedContinuingListStack.popIfNotEmpty();

    if (classicJavadoc) {
      requestBlankLine();
    }
  }

  void writeListItemOpen(ListItemOpenTag token) {
    requestNewline();

    if (continuingListItemOfInnermostList) {
      // TODO(cpovirk): consider whether we can handle this only with operations on the indent stack
      continuingListItemOfInnermostList = false;
      indentStack.popUntil(ListItemIndent.class);
    }
    writeToken(token);
    continuingListItemOfInnermostList = true;
    int indent = token.value().length();
    indentStack.push(new ListItemIndent(indent));
  }

  void writeHeaderOpen(HeaderOpenTag token) {
    requestBlankLine();

    writeToken(token);
  }

  void writeHeaderClose(HeaderCloseTag token) {
    writeToken(token);

    requestBlankLine();
  }

  void writeParagraphOpen(Token token) {
    if (!wroteAnythingSignificant) {
      /*
       * The user included an initial <p> tag. Ignore it, and don't request a blank line before the
       * next token.
       */
      return;
    }

    requestBlankLine();

    writeToken(token);
  }

  void writeBlockQuoteOpen(BlockQuoteOpenTag token) {
    requestBlankLine();
    writeToken(token);
    requestNewline();
  }

  void writeBlockQuoteClose(BlockQuoteCloseTag token) {
    requestNewline();
    writeToken(token);
    requestBlankLine();
  }

  void writeMarkdownBlockQuoteOpen(MarkdownBlockQuoteOpen token) {
    if (!atStartOfLine) {
      requestNewline();
    }
    writeToken(new MarkdownBlockQuoteOpen("> "));
    indentStack.push(new BlockQuoteIndent());
  }

  void writeMarkdownBlockQuoteClose() {
    indentStack.popUntil(BlockQuoteIndent.class);
  }

  void writePreOpen(PreOpenTag token) {
    requestBlankLine();

    writeToken(token);
  }

  void writePreClose(PreCloseTag token) {
    writeToken(token);

    requestBlankLine();
  }

  void writeCodeOpen(CodeOpenTag token) {
    writeToken(token);
  }

  void writeCodeClose(CodeCloseTag token) {
    writeToken(token);
  }

  void writeTableOpen(TableOpenTag token) {
    requestBlankLine();

    writeToken(token);
  }

  void writeTableClose(TableCloseTag token) {
    writeToken(token);

    requestBlankLine();
  }

  void writeMoeEndStripComment(MoeEndStripComment token) {
    writeLineBreakNoAutoIndent();
    output.append(indentForMoeEndStripComment);

    // Or maybe just "output.append(token.getValue())?" I'm kind of surprised this is so easy.
    writeToken(token);

    requestNewline();
  }

  void writeHtmlComment(HtmlComment token) {
    requestNewline();

    List<String> lines = token.value().lines().toList();
    writeToken(new HtmlComment(lines.get(0)));
    for (String line : lines.subList(1, lines.size())) {
      writeNewline(AutoIndent.NO_AUTO_INDENT);
      output.append(line);
      remainingOnLine -= line.length();
    }

    requestNewline();
  }

  void writeBr(BrTag token) {
    writeToken(token);

    requestNewline();
  }

  void writeLineBreakNoAutoIndent() {
    writeNewline(NO_AUTO_INDENT);
  }

  void writeMarkdownHardLineBreak() {
    writeLiteral(BACKSLASH_LITERAL);
    writeNewline();
  }

  void writeLiteral(Literal token) {
    writeToken(token);
  }

  void writeMarkdownFencedCodeBlock(MarkdownFencedCodeBlock token) {
    if (!atStartOfLine) {
      // A reminder that atStartOfLine is still true after `-␣` because it is a StartOfLineToken.
      requestBlankLine();
    }
    flushWhitespace();
    output.append(token.start());
    token
        .literal()
        .lines()
        .forEach(
            line -> {
              writeNewline();
              output.append(line);
            });
    writeNewline();
    output.append(token.end());
    wroteAnythingSignificant = true;
    requestBlankLine();
  }

  void writeMarkdownTable(MarkdownTable token) {
    if (!atStartOfLine) {
      requestBlankLine();
    }
    flushWhitespace();
    List<String> lines = token.value().lines().toList();
    output.append(lines.get(0));
    for (String line : lines.subList(1, lines.size())) {
      writeNewline(AutoIndent.NO_AUTO_INDENT);
      output.append(line);
    }
    wroteAnythingSignificant = true;
    requestBlankLine();
  }

  @Override
  public String toString() {
    return output.toString();
  }

  private void requestBlankLine() {
    requestWhitespace(BLANK_LINE);
  }

  private void requestNewline() {
    requestWhitespace(NEWLINE);
  }

  /**
   * The kind of whitespace that has been requested between the previous and next tokens. The order
   * of the values is significant: It goes from lowest priority to highest. For example, if the
   * previous token requests {@link #BLANK_LINE} after it but the next token requests only {@link
   * #NEWLINE} before it, we insert {@link #BLANK_LINE}.
   */
  enum RequestedWhitespace {
    NONE,
    WHITESPACE,
    NEWLINE,
    BLANK_LINE,
  }

  private void flushWhitespace() {
    if (requestedMoeBeginStripComment != null) {
      requestNewline();
    }

    if (!wroteAnythingSignificant) {
      requestedWhitespace = NONE;
    }

    if (classicJavadoc
        && requestedWhitespace == BLANK_LINE
        && (!postWriteModifiedContinuingListStack.isEmpty() || continuingFooterTag)) {
      /*
       * We don't write blank lines inside lists or footer tags, even in cases where we otherwise
       * would (e.g., before a <p> tag). Justification: We don't write blank lines _between_ list
       * items or footer tags, so it would be strange to write blank lines _within_ one. Of course,
       * an alternative approach would be to go ahead and write blank lines between items/tags,
       * either always or only in the case that an item contains a blank line.
       */
      requestedWhitespace = NEWLINE;
    }

    if (requestedWhitespace == BLANK_LINE) {
      writeBlankLine();
      requestedWhitespace = NONE;
    } else if (requestedWhitespace == NEWLINE) {
      writeNewline();
      requestedWhitespace = NONE;
    }
  }

  private void writeToken(Token token) {
    if (token.value().isEmpty()) {
      return;
    }

    flushWhitespace();
    boolean needWhitespace = (requestedWhitespace == WHITESPACE);

    /*
     * Write a newline if necessary to respect the line limit. (But if we're at the beginning of the
     * line, a newline won't help. Or it might help but only by separating "<p>veryverylongword,"
     * which goes against our style.)
     */
    if (!atStartOfLine && token.length() + (needWhitespace ? 1 : 0) > remainingOnLine) {
      writeNewline();
    }
    if (!atStartOfLine && needWhitespace) {
      output.append(" ");
      remainingOnLine--;
    }

    if (requestedMoeBeginStripComment != null) {
      output.append(requestedMoeBeginStripComment.value());
      requestedMoeBeginStripComment = null;
      indentForMoeEndStripComment = innerIndentString();
      wroteAnythingSignificant = true;
      requestNewline();
      writeToken(token);
      return;
    }

    output.append(token.value());

    if (!(token instanceof StartOfLineToken)) {
      atStartOfLine = false;
    }

    /*
     * TODO(cpovirk): We really want the number of "characters," not chars. Figure out what the
     * right way of measuring that is (grapheme count (with BreakIterator?)? sum of widths of all
     * graphemes? I don't think that our style guide is specific about this.). Moreover, I am
     * probably brushing other problems with surrogates, etc. under the table. Hopefully I mostly
     * get away with it by joining all non-space, non-tab characters together.
     *
     * Possibly the "width" question has no right answer:
     * http://denisbider.blogspot.com/2015/09/when-monospace-fonts-arent-unicode.html
     */
    remainingOnLine -= token.length();
    requestedWhitespace = NONE;
    wroteAnythingSignificant = true;
  }

  private void writeNewlineStart() {
    output.append("\n");
    appendSpaces(blockIndent + (classicJavadoc ? 1 : 0));
    output.append(classicJavadoc ? "*" : "///");
  }

  private void writeBlankLine() {
    writeNewlineStart();
    String indent = innerIndentString();
    if (!indent.isBlank()) {
      // The motivation here is that we may have a blank line that is inside a Markdown block quote.
      // Then we still want the `>` marker or markers at the start of the line, but we don't want
      // any trailing whitespace.
      output.append(" ");
      output.append(indent.stripTrailing());
    }
    writeNewline();
  }

  private void writeNewline() {
    writeNewline(AUTO_INDENT);
  }

  private void writeNewline(AutoIndent autoIndent) {
    writeNewlineStart();
    appendSpaces(1);
    remainingOnLine = JavadocFormatter.MAX_LINE_LENGTH - blockIndent - (classicJavadoc ? 3 : 4);
    if (autoIndent == AUTO_INDENT) {
      String indent = innerIndentString();
      output.append(indent);
      remainingOnLine -= indent.length();
    }
    atStartOfLine = true;
  }

  enum AutoIndent {
    AUTO_INDENT,
    NO_AUTO_INDENT
  }

  private String innerIndentString() {
    StringBuilder sb = new StringBuilder();
    if (continuingFooterTag) {
      sb.repeat(' ', classicJavadoc ? 4 : 2);
    }
    for (Indent indent : indentStack.bottomToTop()) {
      indent.render(sb);
    }
    return sb.toString();
  }

  private sealed interface Indent {
    void render(StringBuilder sb);
  }

  private record ListIndent(int width) implements Indent {
    @Override
    public void render(StringBuilder sb) {
      sb.repeat(' ', width);
    }
  }

  private record ListItemIndent(int width) implements Indent {
    @Override
    public void render(StringBuilder sb) {
      sb.repeat(' ', width);
    }
  }

  private record BlockQuoteIndent() implements Indent {
    @Override
    public void render(StringBuilder sb) {
      sb.append("> ");
    }
  }

  private void appendSpaces(int count) {
    output.repeat(' ', count);
  }
}
