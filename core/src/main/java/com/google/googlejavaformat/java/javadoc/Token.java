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

/// Javadoc token. Our idea of what constitutes a token is often larger or smaller than what you'd
/// naturally expect. The decision is usually pragmatic rather than theoretical. Most of the details
/// are in [JavadocLexer].
///
/// The general idea is that every token that requires special handling (extra line breaks,
/// indentation, forcing or forbidding whitespace) from [JavadocWriter] gets its own type. But I
/// haven't been super careful about it, so I'd imagine that we could merge or remove some of these
/// if we wanted. (For example, ParagraphCloseTag and ListItemCloseTag could be a common Ignorable
/// token type. But their corresponding Open types exist, so I've kept the Close types.)
///
/// Note, though, that tokens of the same type may still have been handled differently by
/// [JavadocLexer] when it created them. For example, Literal is used for both plain text and inline
/// tags, even though the two affect the lexer's state differently.
sealed interface Token {
  String value();

  default int length() {
    return value().length();
  }

  /// Tokens that are always pinned to the following token. For example, `<p>` in `<p>Foo bar`
  /// (never `<p> Foo bar` or `<p>\nFoo bar`); or `<li>` or `-␣` in `<li>Foo bar` or `-␣Foo bar`.
  ///
  /// This is not the only kind of "pinning" that we do: See also the joining of Literal tokens done
  /// by the lexer. The special pinning here is necessary because these tokens are not of type
  /// Literal (because they require other special handling).
  interface StartOfLineToken {}

  /// ∕✱✱
  record BeginJavadoc(String value) implements Token {}

  /// ✱∕
  record EndJavadoc(String value) implements Token {}

  /// The `@foo` that begins a block Javadoc tag like `@throws`.
  record FooterJavadocTagStart(String value) implements Token {}

  /// The opening `｛@snippet` of a code snippet.
  record SnippetBegin(String value) implements Token {}

  /// The closing `｝` of a code snippet.
  record SnippetEnd(String value) implements Token {}

  record ListOpenTag(String value) implements Token {}

  record ListCloseTag(String value) implements Token {}

  record ListItemOpenTag(String value) implements Token, StartOfLineToken {}

  record ListItemCloseTag(String value) implements Token {}

  record HeaderOpenTag(String value) implements Token, StartOfLineToken {}

  record HeaderCloseTag(String value) implements Token {}

  record ParagraphOpenTag(String value) implements Token, StartOfLineToken {}

  record ParagraphCloseTag(String value) implements Token {}

  record BlockQuoteOpenTag(String value) implements Token, StartOfLineToken {}

  record BlockQuoteCloseTag(String value) implements Token {}

  record MarkdownBlockQuoteOpen(String value) implements Token, StartOfLineToken {}

  record MarkdownBlockQuoteClose(String value) implements Token {}

  record PreOpenTag(String value) implements Token {}

  record PreCloseTag(String value) implements Token {}

  record CodeOpenTag(String value) implements Token {}

  record CodeCloseTag(String value) implements Token {}

  record TableOpenTag(String value) implements Token {}

  record TableCloseTag(String value) implements Token {}

  /// `<!-- MOE：begin_intracomment_strip -->`
  record MoeBeginStripComment(String value) implements Token {}

  /// `<!-- MOE：end_intracomment_strip -->`
  record MoeEndStripComment(String value) implements Token {}

  record HtmlComment(String value) implements Token {}

  record BrTag(String value) implements Token {}

  /// A Markdown code span start, like the start of `` `foo` `` or ```` ``bar`` ````.
  ///
  /// @param value the start backtick string, one or more backtick characters.
  record MarkdownCodeSpanStart(String value) implements Token {}

  /// A Markdown code span end, like the end of `` `foo` `` or ```` ``bar`` ````.
  ///
  /// @param value the end backtick string, one or more backtick characters (the same number as the
  ///   corresponding start backtick string).
  record MarkdownCodeSpanEnd(String value) implements Token {}

  /// A fenced code block, like:
  ///
  /// ````
  /// ```java
  /// code block
  /// with an info string ("java")
  /// ```
  /// ````
  ///
  /// @param value the full text of the code block as it appeared in the input, including the start
  ///   and end fences and the literal content.
  /// @param start the start fence, including the info string if any (```` ```java ```` in the
  ///   example).
  /// @param end the end fence.
  /// @param literal the text that the code block represents. This does not include the start and
  ///   end fences, nor any indentation that precedes these fences and every intervening line.
  record MarkdownFencedCodeBlock(String value, String start, String end, String literal)
      implements Token {}

  /// A Markdown table, like:
  ///
  /// ```
  /// | foo | bar |
  /// | --- | --- |
  /// | baz | qux |
  /// ```
  ///
  /// @param value the full text of the table as it appeared in the input, including the delimiters
  ///   and the literal content.
  record MarkdownTable(String value) implements Token {}

  /// Whitespace that is not in a `<pre>` or `<table>` section. Whitespace includes leading
  /// newlines, asterisks, and tabs and spaces. In the output, it is translated to newlines (with
  /// leading spaces and asterisks) or spaces.
  record Whitespace(String value) implements Token {}

  /// A newline in a `<pre>` or `<table>` section. We preserve user formatting in these sections,
  /// including newlines.
  record ForcedNewline(String value) implements Token {}

  /// A Markdown hard line break (`\` at the end of a line).
  record MarkdownHardLineBreak(String value) implements Token {}

  /// Token that permits but does not force a line break. The way that we accomplish this is
  /// somewhat indirect: As far as [JavadocWriter] is concerned, this token is meaningless. But its
  /// mere existence prevents [JavadocLexer] from joining two [Literal] tokens that would otherwise
  /// be adjacent. Since this token is not real whitespace, the writer may end up writing the
  /// literals together with no space between, just as if they'd been joined. However, if they don't
  /// fit together on the line, the writer will write the first one, start a new line, and write the
  /// second. Hence, the token acts as an optional line break.
  record OptionalLineBreak(String value) implements Token {}

  /// Token representing a Markdown block quote marker (`>`). This token causes the corresponding
  /// text to be consumed, but does not itself lead to any output.
  record BlockQuoteMarker(String value) implements Token {}

  /// Anything else: `foo`, `<b>`, `{@code foo}` etc. [JavadocLexer] sometimes creates adjacent
  /// literal tokens, which it then merges into a single, larger literal token before returning its
  /// output.
  ///
  /// This also includes whitespace in a `<pre>` or `<table>` section. We preserve user formatting
  /// in these sections, including arbitrary numbers of spaces. By treating such whitespace as a
  /// literal, we can merge it with adjacent literals, preventing us from autowrapping inside these
  /// sections -- and doing so naively, to boot. The wrapped line would have no indentation after "*
  /// " or, possibly worse, it might begin with an arbitrary amount of whitespace that didn't fit on
  /// the previous line. Of course, by doing this, we're potentially creating lines of more than 100
  /// characters. But it seems fair to call in the humans to resolve such problems.
  record Literal(String value) implements Token {}
}
