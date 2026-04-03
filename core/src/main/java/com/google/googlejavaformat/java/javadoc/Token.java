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

/**
 * Javadoc token. Our idea of what constitutes a token is often larger or smaller than what you'd
 * naturally expect. The decision is usually pragmatic rather than theoretical. Most of the details
 * are in {@link JavadocLexer}.
 *
 * <p>The general idea is that every token that requires special handling (extra line breaks,
 * indentation, forcing or forbidding whitespace) from {@link JavadocWriter} gets its own type. But
 * I haven't been super careful about it, so I'd imagine that we could merge or remove some of these
 * if we wanted. (For example, ParagraphCloseTag and ListItemCloseTag could be a common Ignorable
 * token type. But their corresponding Open types exist, so I've kept the Close types.)
 *
 * <p>Note, though, that tokens of the same type may still have been handled differently by {@link
 * JavadocLexer} when it created them. For example, Literal is used for both plain text and inline
 * tags, even though the two affect the lexer's state differently.
 */
sealed interface Token {
  String value();

  default int length() {
    return value().length();
  }

  /**
   * Tokens that are always pinned to the following token. For example, {@code <p>} in {@code <p>Foo
   * bar} (never {@code <p> Foo bar} or {@code <p>\nFoo bar}).
   *
   * <p>This is not the only kind of "pinning" that we do: See also the joining of Literal tokens
   * done by the lexer. The special pinning here is necessary because these tokens are not of type
   * Literal (because they require other special handling).
   */
  interface StartOfLineToken {}

  /** ∕✱✱ */
  record BeginJavadoc(String value) implements Token {}

  /** ✱∕ */
  record EndJavadoc(String value) implements Token {}

  /** The {@code @foo} that begins a block Javadoc tag like {@code @throws}. */
  record FooterJavadocTagStart(String value) implements Token {}

  /** The opening {@code ｛@snippet} of a code snippet. */
  record SnippetBegin(String value) implements Token {}

  /** The closing {@code ｝} of a code snippet. */
  record SnippetEnd(String value) implements Token {}

  record ListOpenTag(String value) implements Token {}

  record ListCloseTag(String value) implements Token {}

  record ListItemOpenTag(String value) implements Token, StartOfLineToken {}

  record ListItemCloseTag(String value) implements Token {}

  record HeaderOpenTag(String value) implements Token, StartOfLineToken {}

  record HeaderCloseTag(String value) implements Token {}

  record ParagraphOpenTag(String value) implements Token, StartOfLineToken {}

  record ParagraphCloseTag(String value) implements Token {}

  record BlockquoteOpenTag(String value) implements Token {}

  record BlockquoteCloseTag(String value) implements Token {}

  record PreOpenTag(String value) implements Token {}

  record PreCloseTag(String value) implements Token {}

  record CodeOpenTag(String value) implements Token {}

  record CodeCloseTag(String value) implements Token {}

  record TableOpenTag(String value) implements Token {}

  record TableCloseTag(String value) implements Token {}

  /** {@code <!-- MOE：begin_intracomment_strip -->} */
  record MoeBeginStripComment(String value) implements Token {}

  /** {@code <!-- MOE：end_intracomment_strip -->} */
  record MoeEndStripComment(String value) implements Token {}

  record HtmlComment(String value) implements Token {}

  record BrTag(String value) implements Token {}

  /**
   * Whitespace that is not in a {@code <pre>} or {@code <table>} section. Whitespace includes
   * leading newlines, asterisks, and tabs and spaces. In the output, it is translated to newlines
   * (with leading spaces and asterisks) or spaces.
   */
  record Whitespace(String value) implements Token {}

  /**
   * A newline in a {@code <pre>} or {@code <table>} section. We preserve user formatting in these
   * sections, including newlines.
   */
  record ForcedNewline(String value) implements Token {}

  /**
   * Token that permits but does not force a line break. The way that we accomplish this is somewhat
   * indirect: As far as {@link JavadocWriter} is concerned, this token is meaningless. But its mere
   * existence prevents {@link JavadocLexer} from joining two {@link Literal} tokens that would
   * otherwise be adjacent. Since this token is not real whitespace, the writer may end up writing
   * the literals together with no space between, just as if they'd been joined. However, if they
   * don't fit together on the line, the writer will write the first one, start a new line, and
   * write the second. Hence, the token acts as an optional line break.
   */
  record OptionalLineBreak(String value) implements Token {}

  /**
   * Anything else: {@code foo}, {@code <b>}, {@code {@code foo}} etc. {@link JavadocLexer}
   * sometimes creates adjacent literal tokens, which it then merges into a single, larger literal
   * token before returning its output.
   *
   * <p>This also includes whitespace in a {@code <pre>} or {@code <table>} section. We preserve
   * user formatting in these sections, including arbitrary numbers of spaces. By treating such
   * whitespace as a literal, we can merge it with adjacent literals, preventing us from
   * autowrapping inside these sections -- and doing so naively, to boot. The wrapped line would
   * have no indentation after "* " or, possibly worse, it might begin with an arbitrary amount of
   * whitespace that didn't fit on the previous line. Of course, by doing this, we're potentially
   * creating lines of more than 100 characters. But it seems fair to call in the humans to resolve
   * such problems.
   */
  record Literal(String value) implements Token {}
}
