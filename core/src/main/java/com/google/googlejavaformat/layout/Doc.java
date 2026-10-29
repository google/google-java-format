/*
 * Copyright 2026 Google Inc.
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

package com.google.googlejavaformat.layout;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.googlejavaformat.CommentsHelper;
import com.google.googlejavaformat.Doc.Token.RealOrImaginary;
import com.google.googlejavaformat.Indent;
import com.google.googlejavaformat.Input;
import com.google.googlejavaformat.Newlines;
import com.google.googlejavaformat.Output.BreakTag;
import java.util.List;
import java.util.Optional;

/// An immutable document describing the layout choices available for a piece of source.
///
/// The vocabulary follows Wadler's [A prettier
/// printer](https://homepages.inf.ed.ac.uk/wadler/papers/prettier/prettier.pdf) (2003): a [Text] is
/// `text`, a [Group] is `group` combined with `nest` (the amount of nesting is the group's
/// `plusIndent`), and a [BreakMode#UNIFIED] [Break] is `line`. As with `group x = flatten x <|> x`
/// (Wadler §2), a group is laid out either entirely flat or with all of its unified breaks taken.
/// Unlike Wadler's `line`, a break's flat text is arbitrary (often `""` or `" "`).
///
/// A document is a tree of [Group]s. The leaves are [Text], [Tok], [Space] and [Break]. The layout
/// rules are implemented by [LayoutSolver]:
///
/// - A group whose flat width fits in the remaining line is laid out flat, i.e. every break in it
///   (including nested ones) takes its flat text. A group that contains a [BreakMode#FORCED] break
///   never fits (its [#width] is [#MAX_LINE_WIDTH]), so this case never applies to it.
/// - Otherwise the group's direct breaks split it into segments, and each break is decided by its
///   [BreakMode]: [BreakMode#UNIFIED] and [BreakMode#FORCED] breaks are always taken, and
///   [BreakMode#FILL] breaks are taken only if the following segment does not fit flat on the
///   current line, or if the preceding segment did not fit.
///
/// Fitting a group only considers its own width. This is the main difference from Wadler's `fits`,
/// which measures the whole first line of the remaining document, including any text after the
/// group. Trailing text that must stay on the same line as a group (e.g. a `;`) is instead placed
/// inside the group by [LayoutBuilder]. These are the rules of the existing
/// [com.google.googlejavaformat.Doc] engine, and are kept for output compatibility.
///
/// A [Choice] offers a preferred alternative layout for a group, used when the group doesn't fit
/// flat but the preferred layout's first line does.
public sealed interface Doc
    permits Doc.Text, Doc.Lines, Doc.Tok, Doc.Space, Doc.Break, Doc.Group, Doc.Choice {

  /// Width used for documents that cannot be laid out on a single line.
  int MAX_LINE_WIDTH = 1000;

  /// Range for documents that do not correspond to input tokens.
  Range<Integer> EMPTY_RANGE = Range.closedOpen(-1, -1);

  /// The width of this document laid out flat, or [#MAX_LINE_WIDTH] if it can't be.
  int width();

  /// Appends the text of this document laid out flat to `sb`. Only defined if `width() <
  /// MAX_LINE_WIDTH`.
  void appendFlat(StringBuilder sb);

  /// The text of this document laid out flat. Only defined if `width() < MAX_LINE_WIDTH`.
  default String flat() {
    StringBuilder sb = new StringBuilder();
    appendFlat(sb);
    return sb.toString();
  }

  /// The half-open range of input token indices covered by this document, or [#EMPTY_RANGE] if it
  /// covers no tokens. [LayoutSolver] reports it with the text it emits so that the output can be
  /// mapped back to the input for partial formatting.
  Range<Integer> range();

  /// How a [Break] is decided once its enclosing group does not fit. A group that contains a
  /// [#FORCED] break is never considered to fit.
  ///
  /// The modes are not prioritized against each other. The priority between breaks comes from the
  /// group hierarchy: an enclosing group is decided before its nested groups, and a nested group
  /// that still fits on the remaining line stays flat. A group's direct breaks almost always share
  /// one mode (apart from [#FORCED] breaks around comments), so the mode effectively describes how
  /// the whole group breaks.
  enum BreakMode {
    /// Taken whenever the enclosing group does not fit, so all of the unified breaks in a group are
    /// taken together: the group is laid out either entirely flat or with every unified break
    /// taken. This is Wadler's `line` inside a `group`, and Oppen's "consistent" breaking.
    UNIFIED,
    /// Decided independently for each break: taken if the following segment doesn't fit on the
    /// current line, or if the preceding segment didn't fit. A group of fill breaks packs as many
    /// segments as fit onto each line, like Wadler's `fill` and Oppen's "inconsistent" breaking.
    FILL,
    /// Always taken. A group containing a forced break never fits.
    FORCED
  }

  /// An input token.
  record Text(String text, Range<Integer> range) implements Doc {
    public Text {
      requireNonNull(text, "text");
      requireNonNull(range, "range");
    }

    public Text(String text) {
      this(text, EMPTY_RANGE);
    }

    @Override
    public int width() {
      return Newlines.firstBreak(text) >= 0 ? MAX_LINE_WIDTH : text.length();
    }

    @Override
    public void appendFlat(StringBuilder sb) {
      sb.append(text);
    }
  }

  /// A multi-line input token whose non-empty lines after the first are indented to the column
  /// where the token starts, for example text blocks.
  record Lines(ImmutableList<String> lines, Range<Integer> range) implements Doc {
    @Override
    public int width() {
      return MAX_LINE_WIDTH;
    }

    @Override
    public void appendFlat(StringBuilder sb) {
      throw new UnsupportedOperationException();
    }
  }

  /// Input text that isn't a token (the equivalent of the `Doc.Tok` leaf in
  /// [com.google.googlejavaformat.Doc]). This is usually a comment. It can also be whitespace or
  /// newlines around an *imaginary* token ([RealOrImaginary#IMAGINARY]): a token the AST visitor
  /// never emitted, such as the end-of-input token, which [LayoutBuilder#drain] reproduces verbatim
  /// together with the whitespace and comments around it.
  ///
  /// When its enclosing group is broken a comment is re-formatted by a [CommentsHelper] for the
  /// column it starts at; otherwise it's written as [#flat].
  record Tok(Input.Tok tok, Range<Integer> range) implements Doc {
    public Tok {
      requireNonNull(tok, "tok");
      requireNonNull(range, "range");
    }

    public Tok(Input.Tok tok) {
      this(tok, Range.singleton(tok.getIndex()).canonical(DiscreteDomain.integers()));
    }

    /// Only the first line of a multi-line comment counts towards the width (more precisely, the
    /// offset of its second line). Any other text that contains a newline never fits.
    @Override
    public int width() {
      int idx = Newlines.firstBreak(tok.getOriginalText());
      if (idx == -1) {
        return flat().length();
      }
      return tok.isComment() ? idx : MAX_LINE_WIDTH;
    }

    @Override
    public void appendFlat(StringBuilder sb) {
      sb.append(flat());
    }

    @Override
    public String flat() {
      if (tok.isSlashSlashComment() && !tok.getOriginalText().startsWith("// ")) {
        return "// " + tok.getOriginalText().substring("//".length());
      }
      return CommentsHelper.reformatParameterComment(tok).orElse(tok.getOriginalText());
    }
  }

  /// A single space that is never broken.
  enum Space implements Doc {
    INSTANCE;

    @Override
    public int width() {
      return 1;
    }

    @Override
    public void appendFlat(StringBuilder sb) {
      sb.append(' ');
    }

    @Override
    public Range<Integer> range() {
      return EMPTY_RANGE;
    }
  }

  /// A possible line break. When not taken it is written as `flatText`; when taken the next line is
  /// indented by the enclosing group's indent plus `plusIndent`.
  record Break(BreakMode mode, String flatText, Indent plusIndent, Optional<BreakTag> tag)
      implements Doc {
    public Break {
      requireNonNull(mode, "mode");
      requireNonNull(flatText, "flatText");
      requireNonNull(plusIndent, "plusIndent");
      requireNonNull(tag, "tag");
    }

    @Override
    public int width() {
      return mode == BreakMode.FORCED ? MAX_LINE_WIDTH : flatText.length();
    }

    @Override
    public void appendFlat(StringBuilder sb) {
      sb.append(flatText);
    }

    @Override
    public Range<Integer> range() {
      return EMPTY_RANGE;
    }
  }

  /// A unit of layout: either laid out flat, or split at its direct [Break]s. When broken, the
  /// indent for its breaks is the enclosing indent plus `plusIndent`.
  record Group(Indent plusIndent, ImmutableList<Doc> docs, int width, Range<Integer> range)
      implements Doc {
    public Group {
      requireNonNull(plusIndent, "plusIndent");
      requireNonNull(docs, "docs");
      requireNonNull(range, "range");
    }

    public Group(Indent plusIndent, ImmutableList<Doc> docs) {
      this(plusIndent, docs, totalWidth(docs), computeRange(docs));
    }

    /// Returns the sum of the widths of `docs`, saturating at [#MAX_LINE_WIDTH].
    static int totalWidth(List<? extends Doc> docs) {
      int width = 0;
      for (Doc doc : docs) {
        width += doc.width();
        if (width >= MAX_LINE_WIDTH) {
          return MAX_LINE_WIDTH;
        }
      }
      return width;
    }

    /// Returns the smallest range enclosing the token ranges of all of `docs`, ignoring children
    /// that cover no tokens; or [#EMPTY_RANGE] if none of them do.
    private static Range<Integer> computeRange(ImmutableList<Doc> docs) {
      Range<Integer> result = EMPTY_RANGE;
      for (Doc doc : docs) {
        Range<Integer> r = doc.range();
        if (r.isEmpty()) {
          continue;
        }
        result = result.isEmpty() ? r : result.span(r).canonical(DiscreteDomain.integers());
      }
      return result;
    }

    @Override
    public void appendFlat(StringBuilder sb) {
      for (Doc doc : docs) {
        doc.appendFlat(sb);
      }
    }
  }

  /// A choice between two layouts of the same content.
  ///
  /// If the content fits flat it is laid out flat, as for a [Group]. Otherwise, if
  /// `fallbackIfFlatOnNextLine` is true, `fallback` is used if taking a single direct break in
  /// `fallback` allows the rest of it to fit flat on the next line (e.g. `f(\n x -> g(x))` rather
  /// than breaking inside the lambda). Otherwise, `preferred` is used if, when laid out, its first
  /// line fits and contains at least its first `prefixWidth` columns; `fallback` is used if not.
  ///
  /// Both alternatives must have the same flat text, so the choice never affects layouts in which
  /// it is flat, or the fit decisions of enclosing groups. They usually share subtrees.
  ///
  /// @param preferred the preferred layout
  /// @param prefixWidth the width of the part of `preferred` that must be on the current line, e.g.
  ///   `f(x ->` for a hugged lambda argument
  /// @param fallback the layout to use if `preferred` isn't chosen
  /// @param fallbackIfFlatOnNextLine whether to prefer `fallback` when a single direct break lets
  ///   its suffix fit flat on the next line
  record Choice(Group preferred, int prefixWidth, Group fallback, boolean fallbackIfFlatOnNextLine)
      implements Doc {
    public Choice {
      requireNonNull(preferred, "preferred");
      requireNonNull(fallback, "fallback");
    }

    @Override
    public int width() {
      return fallback.width();
    }

    @Override
    public void appendFlat(StringBuilder sb) {
      fallback.appendFlat(sb);
    }

    @Override
    public Range<Integer> range() {
      return fallback.range();
    }
  }
}
