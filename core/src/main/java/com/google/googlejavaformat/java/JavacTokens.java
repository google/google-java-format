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

/// A wrapper around javac's lexer.
final class JavacTokens {

  /// The lexer eats terminal comments, so feed it one we don't care about.
  // TODO(b/33103797): fix javac and remove the work-around
  private static final String EOF_COMMENT = "\n//EOF";

  /// An unprocessed input token, including whitespace and comments.
  ///
  /// @param kind the token kind, or `null` for whitespace and comments.
  /// @param pos the start position.
  /// @param endPos the end position.
  record RawTok(TokenKind kind, int pos, int endPos) {}

  /// Lex the input and return a list of [RawTok]s.
  static ImmutableList<RawTok> getTokens(
      String source, Context context, Set<TokenKind> stopTokens) {
    if (source == null) {
      return ImmutableList.of();
    }
    ScannerFactory fac = ScannerFactory.instance(context);
    int sourceLength = source.length();
    char[] buffer = new char[sourceLength + EOF_COMMENT.length()];
    source.getChars(0, sourceLength, buffer, 0);
    EOF_COMMENT.getChars(0, EOF_COMMENT.length(), buffer, sourceLength);
    CommentSavingTokenizer tokenizer = new CommentSavingTokenizer(fac, buffer, buffer.length);
    Scanner scanner = new AccessibleScanner(fac, tokenizer);
    ImmutableList.Builder<RawTok> tokens = ImmutableList.builder();
    int end = sourceLength;
    int last = 0;
    do {
      scanner.nextToken();
      Token t = scanner.token();
      if (t.comments != null) {
        // javac stores the comments in reverse declaration order
        for (Comment comment : t.comments.reverse()) {
          RawTok c = tokenizer.comments().get(comment);
          if (last < c.pos()) {
            tokens.add(new RawTok(null, last, c.pos()));
          }
          tokens.add(c);
          last = c.endPos();
        }
      }
      if (stopTokens.contains(t.kind)) {
        if (t.kind != TokenKind.EOF) {
          end = t.pos;
        }
        break;
      }
      if (last < t.pos) {
        tokens.add(new RawTok(null, last, t.pos));
      }
      tokens.add(new RawTok(t.kind, t.pos, t.endPos));
      last = t.endPos;
    } while (scanner.token().kind != TokenKind.EOF);
    if (last < end) {
      tokens.add(new RawTok(null, last, end));
    }
    return tokens.build();
  }

  /// A [JavaTokenizer] that saves comments' source ranges.
  private static class CommentSavingTokenizer extends JavaTokenizer {

    private final Map<Comment, RawTok> comments = new HashMap<>();

    CommentSavingTokenizer(ScannerFactory fac, char[] buffer, int length) {
      super(fac, buffer, length);
    }

    Map<Comment, RawTok> comments() {
      return comments;
    }

    @Override
    protected Comment processComment(int pos, int endPos, CommentStyle style) {
      Comment comment = super.processComment(pos, endPos, style);
      comments.put(comment, new RawTok(null, pos, endPos));
      return comment;
    }
  }

  // Scanner(ScannerFactory, JavaTokenizer) is protected
  private static class AccessibleScanner extends Scanner {
    AccessibleScanner(ScannerFactory fac, JavaTokenizer tokenizer) {
      super(fac, tokenizer);
    }
  }

  private JavacTokens() {}
}
