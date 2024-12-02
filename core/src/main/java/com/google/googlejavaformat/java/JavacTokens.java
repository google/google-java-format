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
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.sun.tools.javac.parser.JavaTokenizer;
import com.sun.tools.javac.parser.Scanner;
import com.sun.tools.javac.parser.ScannerFactory;
import com.sun.tools.javac.parser.Tokens.Comment;
import com.sun.tools.javac.parser.Tokens.Comment.CommentStyle;
import com.sun.tools.javac.parser.Tokens.Token;
import com.sun.tools.javac.parser.Tokens.TokenKind;
import com.sun.tools.javac.util.Context;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** A wrapper around javac's lexer. */
final class JavacTokens {

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
    CommentSavingTokenizer tokenizer = new CommentSavingTokenizer(fac, buffer, buffer.length);
    Scanner scanner = new AccessibleScanner(fac, tokenizer);
    ImmutableList.Builder<RawTok> tokens = ImmutableList.builder();
    int end = source.length();
    int last = 0;
    do {
      scanner.nextToken();
      Token t = scanner.token();
      if (t.comments != null) {
        for (CommentWithTextAndPosition c : getComments(t, tokenizer.comments())) {
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

  private static ImmutableList<CommentWithTextAndPosition> getComments(
      Token token, Map<Comment, CommentWithTextAndPosition> comments) {
    if (token.comments == null) {
      return ImmutableList.of();
    }
    // javac stores the comments in reverse declaration order
    return token.comments.stream().map(comments::get).collect(toImmutableList()).reverse();
  }

  /** A {@link JavaTokenizer} that saves comments. */
  static class CommentSavingTokenizer extends JavaTokenizer {

    private final Map<Comment, CommentWithTextAndPosition> comments = new HashMap<>();

    CommentSavingTokenizer(ScannerFactory fac, char[] buffer, int length) {
      super(fac, buffer, length);
    }

    Map<Comment, CommentWithTextAndPosition> comments() {
      return comments;
    }

    @Override
    protected Comment processComment(int pos, int endPos, CommentStyle style) {
      char[] buf = getRawCharactersReflectively(pos, endPos);
      Comment comment = super.processComment(pos, endPos, style);
      CommentWithTextAndPosition commentWithTextAndPosition =
          new CommentWithTextAndPosition(pos, endPos, new String(buf));
      comments.put(comment, commentWithTextAndPosition);
      return comment;
    }

    private char[] getRawCharactersReflectively(int beginIndex, int endIndex) {
      Object instance;
      try {
        instance = JavaTokenizer.class.getDeclaredField("reader").get(this);
      } catch (ReflectiveOperationException e) {
        instance = this;
      }
      try {
        return (char[])
            instance
                .getClass()
                .getMethod("getRawCharacters", int.class, int.class)
                .invoke(instance, beginIndex, endIndex);
      } catch (ReflectiveOperationException e) {
        throw new LinkageError(e.getMessage(), e);
      }
    }
  }

  /** A {@link Comment} that saves its text and start position. */
  static class CommentWithTextAndPosition {

    private final int pos;
    private final int endPos;
    private final String text;

    public CommentWithTextAndPosition(int pos, int endPos, String text) {
      this.pos = pos;
      this.endPos = endPos;
      this.text = text;
    }

    /**
     * Returns the source position of the character at index {@code index} in the comment text.
     *
     * <p>The handling of javadoc comments in javac has more logic to skip over leading whitespace
     * and '*' characters when indexing into doc comments, but we don't need any of that.
     */
    public int getSourcePos(int index) {
      checkArgument(
          0 <= index && index < (endPos - pos),
          "Expected %s in the range [0, %s)",
          index,
          endPos - pos);
      return pos + index;
    }

    public String getText() {
      return text;
    }

    @Override
    public String toString() {
      return String.format("Comment: '%s'", getText());
    }
  }

  // Scanner(ScannerFactory, JavaTokenizer) is protected
  static class AccessibleScanner extends Scanner {
    protected AccessibleScanner(ScannerFactory fac, JavaTokenizer tokenizer) {
      super(fac, tokenizer);
    }
  }

  private JavacTokens() {}
}
