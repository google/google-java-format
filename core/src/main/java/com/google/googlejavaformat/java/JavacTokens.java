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

package com.google.googlejavaformat.java;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.Set;
import org.openjdk.tools.javac.parser.JavaTokenizer;
import org.openjdk.tools.javac.parser.Scanner;
import org.openjdk.tools.javac.parser.ScannerFactory;
import org.openjdk.tools.javac.parser.Tokens.Comment;
import org.openjdk.tools.javac.parser.Tokens.Comment.CommentStyle;
import org.openjdk.tools.javac.parser.Tokens.Token;
import org.openjdk.tools.javac.parser.Tokens.TokenKind;
import org.openjdk.tools.javac.parser.UnicodeReader;
import org.openjdk.tools.javac.util.Context;

/** A wrapper around javac's lexer. */
class JavacTokens {

  /** The lexer eats terminal comments, so feed it one we don't care about. */
  // TODO(b/33103797): fix javac and remove the work-around
  private static final CharSequence EOF_COMMENT = "\n//EOF";

  /** An unprocessed input token, including whitespace and comments. */
  static class RawTok {
    private final String stringVal;
    private final TokenKind kind;
    private final int pos;
    private final int endPos;

    RawTok(String stringVal, TokenKind kind, int pos, int endPos) {
      this.stringVal = stringVal;
      this.kind = kind;
      this.pos = pos;
      this.endPos = endPos;
    }

    /** The token kind, or {@code null} for whitespace and comments. */
    public TokenKind kind() {
      return kind;
    }

    /** The start position. */
    public int pos() {
      return pos;
    }

    /** The end position. */
    public int endPos() {
      return endPos;
    }

    /** The escaped string value of a literal, or {@code null} for other tokens. */
    public String stringVal() {
      return stringVal;
    }
  }

  /** Lex the input and return a list of {@link RawTok}s. */
  public static ImmutableList<RawTok> getTokens(
      String source, Context context, Set<TokenKind> stopTokens) {
    if (source == null) {
      return ImmutableList.of();
    }
    ScannerFactory fac = ScannerFactory.instance(context);
    char[] buffer = (source + EOF_COMMENT).toCharArray();
    Scanner scanner =
        new AccessibleScanner(fac, new CommentSavingTokenizer(fac, buffer, buffer.length));
    ImmutableList.Builder<RawTok> tokens = ImmutableList.builder();
    int end = source.length();
    int last = 0;
    do {
      scanner.nextToken();
      Token t = scanner.token();
      if (t.comments != null) {
        for (Comment c : Lists.reverse(t.comments)) {
          if (last < c.getSourcePos(0)) {
            tokens.add(new RawTok(null, null, last, c.getSourcePos(0)));
          }
          tokens.add(
              new RawTok(null, null, c.getSourcePos(0), c.getSourcePos(0) + c.getText().length()));
          last = c.getSourcePos(0) + c.getText().length();
        }
      }
      if (stopTokens.contains(t.kind)) {
        if (t.kind != TokenKind.EOF) {
          end = t.pos;
        }
        break;
      }
      if (last < t.pos) {
        tokens.add(new RawTok(null, null, last, t.pos));
      }
      tokens.add(
          new RawTok(
              t.kind == TokenKind.STRINGLITERAL ? "\"" + t.stringVal() + "\"" : null,
              t.kind,
              t.pos,
              t.endPos));
      last = t.endPos;
    } while (scanner.token().kind != TokenKind.EOF);
    if (last < end) {
      tokens.add(new RawTok(null, null, last, end));
    }
    return tokens.build();
  }

  /** A {@link JavaTokenizer} that saves comments. */
  static class CommentSavingTokenizer extends JavaTokenizer {
    CommentSavingTokenizer(ScannerFactory fac, char[] buffer, int length) {
      super(fac, buffer, length);
    }

    @Override
    protected Comment processComment(int pos, int endPos, CommentStyle style) {
      char[] buf = reader.getRawCharacters(pos, endPos);
      return new CommentWithTextAndPosition(
          pos, endPos, new AccessibleReader(fac, buf, buf.length), style);
    }
  }

  /** A {@link Comment} that saves its text and start position. */
  static class CommentWithTextAndPosition implements Comment {

    private final int pos;
    private final int endPos;
    private final AccessibleReader reader;
    private final CommentStyle style;

    private String text = null;

    public CommentWithTextAndPosition(
        int pos, int endPos, AccessibleReader reader, CommentStyle style) {
      this.pos = pos;
      this.endPos = endPos;
      this.reader = reader;
      this.style = style;
    }

    /**
     * Returns the source position of the character at index {@code index} in the comment text.
     *
     * <p>The handling of javadoc comments in javac has more logic to skip over leading whitespace
     * and '*' characters when indexing into doc comments, but we don't need any of that.
     */
    @Override
    public int getSourcePos(int index) {
      checkArgument(
          0 <= index && index < (endPos - pos),
          "Expected %s in the range [0, %s)",
          index,
          endPos - pos);
      return pos + index;
    }

    @Override
    public CommentStyle getStyle() {
      return style;
    }

    @Override
    public String getText() {
      String text = this.text;
      if (text == null) {
        this.text = text = new String(reader.getRawCharacters());
      }
      return text;
    }

    /**
     * We don't care about {@code @deprecated} javadoc tags (see the DepAnn check).
     *
     * @return false
     */
    @Override
    public boolean isDeprecated() {
      return false;
    }

    @Override
    public String toString() {
      return String.format("Comment: '%s'", getText());
    }
  }

  // Scanner(ScannerFactory, JavaTokenizer) is package-private
  static class AccessibleScanner extends Scanner {
    protected AccessibleScanner(ScannerFactory fac, JavaTokenizer tokenizer) {
      super(fac, tokenizer);
    }
  }

  // UnicodeReader(ScannerFactory, char[], int) is package-private
  static class AccessibleReader extends UnicodeReader {
    protected AccessibleReader(ScannerFactory fac, char[] buffer, int length) {
      super(fac, buffer, length);
    }
  }
}
