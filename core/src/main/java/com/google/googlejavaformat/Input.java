/*
 * Copyright 2015 Google Inc.
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

package com.google.googlejavaformat;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableRangeMap;

/// An input to the formatter.
public abstract class Input extends InputOutput {
  /// A `Tok` ("tock") is a token, or a comment, or a newline, or a maximal string of blanks. A
  /// token `Tok` underlies a [Token], and each other `Tok` is attached to a single `Token`. Tokens
  /// and comments have indices; white space `Tok`s do not.
  public interface Tok {
    /// Return the `Tok`'s index.
    ///
    /// @return its index
    int getIndex();

    /// Return the `Tok`'s `0`-based position.
    ///
    /// @return its position
    int getPosition();

    /// Return the `Tok`'s `0`-based column number.
    ///
    /// @return its column number
    int getColumn();

    /// The `Tok`'s text.
    String getText();

    /// The `Tok`'s original text (before processing escapes).
    String getOriginalText();

    /// The length of the `Tok`'s original text.
    int length();

    /// Is the `Tok` a newline?
    boolean isNewline();

    /// Is the `Tok` a "//" comment?
    boolean isSlashSlashComment();

    /// Is the `Tok` a "/*" comment?
    boolean isSlashStarComment();

    /// Is the `Tok` a javadoc comment?
    boolean isJavadocComment();

    /// Is the `Tok` a comment?
    boolean isComment();
  }

  /// A `Token` is a language-level token.
  public interface Token {
    /// Get the token's [Tok].
    ///
    /// @return the token's [Tok]
    Tok getTok();

    /// Get the earlier [Tok]s assigned to this `Token`.
    ///
    /// @return the earlier [Tok]s assigned to this `Token`
    ImmutableList<? extends Tok> getToksBefore();

    /// Get the later [Tok]s assigned to this `Token`.
    ///
    /// @return the later [Tok]s assigned to this `Token`
    ImmutableList<? extends Tok> getToksAfter();
  }

  /// Get the input tokens.
  ///
  /// @return the input tokens
  public abstract ImmutableList<? extends Token> getTokens();

  /// A map from [start, end] position ranges to [Token]s.
  public abstract ImmutableRangeMap<Integer, ? extends Token> getPositionTokenMap();

  public abstract ImmutableMap<Integer, Integer> getPositionToColumnMap();

  public abstract String getText();

  /// Get the number of toks.
  ///
  /// @return the number of toks, excluding the EOF tok
  public abstract int getkN();

  /// Get the Token by index.
  ///
  /// @param k the Tok index
  public abstract Token getToken(int k);

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("super", super.toString()).toString();
  }

  /// Converts a character offset in the input to a line number.
  public abstract int getLineNumber(int inputPosition);

  /// Converts a character offset in the input to a 0-based column number.
  public abstract int getColumnNumber(int inputPosition);

  /// Construct a diagnostic. Populates the input filename, and converts character offsets to
  /// numbers.
  public FormatterDiagnostic createDiagnostic(int inputPosition, String message) {
    return new FormatterDiagnostic(
        getLineNumber(inputPosition), getColumnNumber(inputPosition), message);
  }
}
