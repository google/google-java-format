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

import static com.google.common.collect.Iterables.getLast;
import static com.google.googlejavaformat.CommentsHelper.reformatParameterComment;
import static java.lang.Math.max;

import com.google.common.base.MoreObjects;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Iterators;
import com.google.common.collect.Range;
import com.google.googlejavaformat.Output.BreakTag;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/// [`JavaInputAstVisitor`][com.google.googlejavaformat.java.JavaInputAstVisitor] outputs a sequence
/// of [Op]s using [OpsBuilder]. This linear sequence is then transformed by [DocBuilder] into a
/// tree-structured `Doc`. The top-level `Doc` is a [Level], which contains a sequence of `Doc`s,
/// including other [Level]s. Leaf `Doc`s are [Token]s, representing language-level tokens; [Tok]s,
/// which may also represent non-token [Input.Tok]s, including comments and other white-space;
/// [Space]s, representing single spaces; and [Break]s, which represent optional line-breaks.
public abstract class Doc {
  /// Each [Break] in a [Level] is either [FillMode#UNIFIED] or [FillMode#INDEPENDENT].
  public enum FillMode {
    /// If a [Level] will not fit on one line, all of its `UNIFIED` [Break]s will be broken.
    UNIFIED,

    /// If a [Level] will not fit on one line, its `INDEPENDENT` [Break]s will be broken
    /// independently of each other, to fill in the [Level].
    INDEPENDENT,

    /// A `FORCED` [Break] will always be broken, and a [Level] it appears in will not fit on one
    /// line.
    FORCED
  }

  /// The maximum supported line width.
  ///
  /// This can be used as a sentinel/threshold for `Doc`s that break unconditionally.
  ///
  /// The value was selected to be obviously too large for any practical line, but small enough to
  /// prevent accidental overflow.
  public static final int MAX_LINE_WIDTH = 1000;

  /// State for writing.
  public record State(int lastIndent, int indent, int column, boolean mustBreak) {
    public State(int indent0, int column0) {
      this(indent0, indent0, column0, false);
    }

    State withColumn(int column) {
      return new State(lastIndent, indent, column, mustBreak);
    }

    State withMustBreak(boolean mustBreak) {
      return new State(lastIndent, indent, column, mustBreak);
    }
  }

  private static final Range<Integer> EMPTY_RANGE = Range.closedOpen(-1, -1);
  private static final DiscreteDomain<Integer> INTEGERS = DiscreteDomain.integers();

  private final Supplier<Integer> width = Suppliers.memoize(this::computeWidth);

  // Memoized flat; not defined (and never computed) if contains forced breaks.
  private final Supplier<String> flat = Suppliers.memoize(this::computeFlat);

  // Memoized Range.
  private final Supplier<Range<Integer>> range = Suppliers.memoize(this::computeRange);

  /// Return the width of a `Doc`.
  ///
  /// @return the width
  final int getWidth() {
    return width.get();
  }

  /// Return a `Doc`'s flat-string value; not defined (and never called) if the `Doc` contains
  /// forced breaks.
  ///
  /// @return the flat-string value
  final String getFlat() {
    return flat.get();
  }

  /// Return the [Range] of a `Doc`.
  ///
  /// @return the `Doc`'s [Range]
  final Range<Integer> range() {
    return range.get();
  }

  /// Compute the `Doc`'s width.
  ///
  /// @return the width
  abstract int computeWidth();

  /// Compute the `Doc`'s flat value. Not defined (and never called) if contains forced breaks.
  ///
  /// @return the flat value
  abstract String computeFlat();

  /// Compute the `Doc`'s [Range] of [Input.Token]s.
  ///
  /// @return the [Range]
  abstract Range<Integer> computeRange();

  /// Make breaking decisions for a `Doc`.
  ///
  /// @param maxWidth the maximum line width
  /// @param state the current output state
  /// @return the new output state
  public abstract State computeBreaks(CommentsHelper commentsHelper, int maxWidth, State state);

  /// Write a `Doc` to an [Output], after breaking decisions have been made.
  public abstract void write(Output output);

  /// A `Level` inside a [Doc].
  static final class Level extends Doc {
    private final Indent plusIndent; // The extra indent following breaks.
    private final List<Doc> docs = new ArrayList<>(); // The elements of the level.

    private Level(Indent plusIndent) {
      this.plusIndent = plusIndent;
    }

    /// Factory method for `Level`s.
    ///
    /// @param plusIndent the extra indent inside the `Level`
    /// @return the new `Level`
    static Level make(Indent plusIndent) {
      return new Level(plusIndent);
    }

    /// Add a [Doc] to the `Level`.
    ///
    /// @param doc the [Doc] to add
    void add(Doc doc) {
      docs.add(doc);
    }

    @Override
    int computeWidth() {
      return getWidth(docs);
    }

    @Override
    String computeFlat() {
      StringBuilder builder = new StringBuilder();
      for (Doc doc : docs) {
        builder.append(doc.getFlat());
      }
      return builder.toString();
    }

    @Override
    Range<Integer> computeRange() {
      Range<Integer> docRange = EMPTY_RANGE;
      for (Doc doc : docs) {
        docRange = union(docRange, doc.range());
      }
      return docRange;
    }

    // State that needs to be preserved between calculating breaks and
    // writing output.
    // TODO(cushon): represent phases as separate immutable data.

    /// True if the entire [Level] fits on one line.
    boolean oneLine = false;

    /// Groups of [Doc]s that are children of the current [Level], separated by [Break]s.
    List<List<Doc>> splits = new ArrayList<>();

    /// [Break]s between [Doc]s in the current [Level].
    List<Break> breaks = new ArrayList<>();

    @Override
    public State computeBreaks(CommentsHelper commentsHelper, int maxWidth, State state) {
      int thisWidth = getWidth();
      if (state.column + thisWidth <= maxWidth) {
        oneLine = true;
        return state.withColumn(state.column + thisWidth);
      }
      State broken =
          computeBroken(
              commentsHelper, maxWidth, new State(state.indent + plusIndent.eval(), state.column));
      return state.withColumn(broken.column);
    }

    private static void splitByBreaks(List<Doc> docs, List<List<Doc>> splits, List<Break> breaks) {
      splits.clear();
      breaks.clear();
      splits.add(new ArrayList<>());
      for (Doc doc : docs) {
        if (doc instanceof Break b) {
          breaks.add(b);
          splits.add(new ArrayList<>());
        } else {
          getLast(splits).add(doc);
        }
      }
    }

    /// Compute breaks for a [Level] that spans multiple lines.
    private State computeBroken(CommentsHelper commentsHelper, int maxWidth, State state) {
      splitByBreaks(docs, splits, breaks);

      state =
          computeBreakAndSplit(
              commentsHelper, maxWidth, state, /* optBreakDoc= */ Optional.empty(), splits.get(0));

      // Handle following breaks and split.
      for (int i = 0; i < breaks.size(); i++) {
        state =
            computeBreakAndSplit(
                commentsHelper, maxWidth, state, Optional.of(breaks.get(i)), splits.get(i + 1));
      }
      return state;
    }

    /// Lay out a Break-separated group of Docs in the current Level.
    private static State computeBreakAndSplit(
        CommentsHelper commentsHelper,
        int maxWidth,
        State state,
        Optional<Break> optBreakDoc,
        List<Doc> split) {
      int breakWidth = optBreakDoc.isPresent() ? optBreakDoc.get().getWidth() : 0;
      int splitWidth = getWidth(split);
      boolean shouldBreak =
          (optBreakDoc.isPresent() && optBreakDoc.get().fillMode == FillMode.UNIFIED)
              || state.mustBreak
              || state.column + breakWidth + splitWidth > maxWidth;

      if (optBreakDoc.isPresent()) {
        state = optBreakDoc.get().computeBreaks(state, state.lastIndent, shouldBreak);
      }
      boolean enoughRoom = state.column + splitWidth <= maxWidth;
      state = computeSplit(commentsHelper, maxWidth, split, state.withMustBreak(false));
      if (!enoughRoom) {
        state = state.withMustBreak(true); // Break after, too.
      }
      return state;
    }

    private static State computeSplit(
        CommentsHelper commentsHelper, int maxWidth, List<Doc> docs, State state) {
      for (Doc doc : docs) {
        state = doc.computeBreaks(commentsHelper, maxWidth, state);
      }
      return state;
    }

    @Override
    public void write(Output output) {
      if (oneLine) {
        output.append(getFlat(), range()); // This is defined because width is finite.
      } else {
        writeFilled(output);
      }
    }

    private void writeFilled(Output output) {
      // Handle first split.
      for (Doc doc : splits.get(0)) {
        doc.write(output);
      }
      // Handle following breaks and split.
      for (int i = 0; i < breaks.size(); i++) {
        breaks.get(i).write(output);
        for (Doc doc : splits.get(i + 1)) {
          doc.write(output);
        }
      }
    }

    /// Get the width of a sequence of [Doc]s.
    ///
    /// @param docs the [Doc]s
    /// @return the width
    static int getWidth(List<Doc> docs) {
      int width = 0;
      for (Doc doc : docs) {
        width += doc.getWidth();

        if (width >= MAX_LINE_WIDTH) {
          return MAX_LINE_WIDTH; // Paranoid overflow protection
        }
      }
      return width;
    }

    private static Range<Integer> union(Range<Integer> x, Range<Integer> y) {
      return x.isEmpty() ? y : y.isEmpty() ? x : x.span(y).canonical(INTEGERS);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("plusIndent", plusIndent)
          .add("docs", docs)
          .toString();
    }
  }

  /// A leaf [Doc] for a token.
  public static final class Token extends Doc implements Op {
    /// Is a Token a real token, or imaginary (e.g., a token generated incorrectly, or an EOF)?
    public enum RealOrImaginary {
      REAL,
      IMAGINARY;

      public boolean isReal() {
        return this == REAL;
      }
    }

    private final Input.Token token;
    private final RealOrImaginary realOrImaginary;
    private final Indent plusIndentCommentsBefore;
    private final Optional<Indent> breakAndIndentTrailingComment;

    private Input.Tok tok() {
      return token.getTok();
    }

    private Token(
        Input.Token token,
        RealOrImaginary realOrImaginary,
        Indent plusIndentCommentsBefore,
        Optional<Indent> breakAndIndentTrailingComment) {
      this.token = token;
      this.realOrImaginary = realOrImaginary;
      this.plusIndentCommentsBefore = plusIndentCommentsBefore;
      this.breakAndIndentTrailingComment = breakAndIndentTrailingComment;
    }

    /// How much extra to indent comments before the `Token`.
    ///
    /// @return the extra indent
    public Indent getPlusIndentCommentsBefore() {
      return plusIndentCommentsBefore;
    }

    /// Force a line break and indent trailing javadoc or block comments.
    public Optional<Indent> breakAndIndentTrailingComment() {
      return breakAndIndentTrailingComment;
    }

    /// Make a `Token`.
    ///
    /// @param token the [Input.Token] to wrap
    /// @param realOrImaginary did this [Input.Token] appear in the input, or was it generated
    ///   incorrectly?
    /// @param plusIndentCommentsBefore extra `plusIndent` for comments just before this token
    /// @return the new `Token`
    static Op make(
        Input.Token token,
        Doc.Token.RealOrImaginary realOrImaginary,
        Indent plusIndentCommentsBefore,
        Optional<Indent> breakAndIndentTrailingComment) {
      return new Token(
          token, realOrImaginary, plusIndentCommentsBefore, breakAndIndentTrailingComment);
    }

    /// Return the wrapped [Input.Token].
    ///
    /// @return the [Input.Token]
    public Input.Token getToken() {
      return token;
    }

    /// Is the token good? That is, does it match an [Input.Token]?
    ///
    /// @return whether the `Token` is good
    public RealOrImaginary realOrImaginary() {
      return realOrImaginary;
    }

    @Override
    public void add(DocBuilder builder) {
      builder.add(this);
    }

    @Override
    int computeWidth() {
      int idx = Newlines.firstBreak(tok().getOriginalText());
      return (idx >= 0) ? MAX_LINE_WIDTH : tok().length();
    }

    @Override
    String computeFlat() {
      return token.getTok().getOriginalText();
    }

    @Override
    Range<Integer> computeRange() {
      return Range.singleton(token.getTok().getIndex()).canonical(INTEGERS);
    }

    @Override
    public State computeBreaks(CommentsHelper commentsHelper, int maxWidth, State state) {
      return state.withColumn(state.column + computeWidth());
    }

    @Override
    public void write(Output output) {
      String text = token.getTok().getOriginalText();
      output.append(text, range());
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("token", token)
          .add("realOrImaginary", realOrImaginary)
          .add("plusIndentCommentsBefore", plusIndentCommentsBefore)
          .toString();
    }
  }

  /// A Leaf node in a [Doc] for a non-breaking space.
  public static final class Space extends Doc implements Op {
    private static final Space SPACE = new Space();

    private Space() {}

    /// Factor method for `Space`.
    ///
    /// @return the new `Space`
    static Space make() {
      return SPACE;
    }

    @Override
    public void add(DocBuilder builder) {
      builder.add(this);
    }

    @Override
    int computeWidth() {
      return 1;
    }

    @Override
    String computeFlat() {
      return " ";
    }

    @Override
    Range<Integer> computeRange() {
      return EMPTY_RANGE;
    }

    @Override
    public State computeBreaks(CommentsHelper commentsHelper, int maxWidth, State state) {
      return state.withColumn(state.column + 1);
    }

    @Override
    public void write(Output output) {
      output.append(" ", range());
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).toString();
    }
  }

  /// A leaf node in a [Doc] for an optional break.
  public static final class Break extends Doc implements Op {
    private final FillMode fillMode;
    private final String flat;
    private final Indent plusIndent;
    private final Optional<BreakTag> optTag;

    private Break(FillMode fillMode, String flat, Indent plusIndent, Optional<BreakTag> optTag) {
      this.fillMode = fillMode;
      this.flat = flat;
      this.plusIndent = plusIndent;
      this.optTag = optTag;
    }

    /// Make a `Break`.
    ///
    /// @param fillMode the [FillMode]
    /// @param flat the text when not broken
    /// @param plusIndent extra indent if taken
    /// @return the new `Break`
    public static Break make(FillMode fillMode, String flat, Indent plusIndent) {
      return new Break(fillMode, flat, plusIndent, /* optTag= */ Optional.empty());
    }

    /// Make a `Break`.
    ///
    /// @param fillMode the [FillMode]
    /// @param flat the text when not broken
    /// @param plusIndent extra indent if taken
    /// @param optTag an optional tag for remembering whether the break was taken
    /// @return the new `Break`
    public static Break make(
        FillMode fillMode, String flat, Indent plusIndent, Optional<BreakTag> optTag) {
      return new Break(fillMode, flat, plusIndent, optTag);
    }

    /// Make a forced `Break`.
    ///
    /// @return the new forced `Break`
    public static Break makeForced() {
      return make(FillMode.FORCED, "", Indent.Const.ZERO);
    }

    /// Return the `Break`'s extra indent.
    ///
    /// @return the extra indent
    int getPlusIndent() {
      return plusIndent.eval();
    }

    /// Is the `Break` forced?
    ///
    /// @return whether the `Break` is forced
    boolean isForced() {
      return fillMode == FillMode.FORCED;
    }

    public FillMode fillMode() {
      return fillMode;
    }

    public String flat() {
      return flat;
    }

    public Indent plusIndent() {
      return plusIndent;
    }

    public Optional<BreakTag> optTag() {
      return optTag;
    }

    @Override
    public void add(DocBuilder builder) {
      builder.breakDoc(this);
    }

    @Override
    int computeWidth() {
      return isForced() ? MAX_LINE_WIDTH : flat.length();
    }

    @Override
    String computeFlat() {
      return flat;
    }

    @Override
    Range<Integer> computeRange() {
      return EMPTY_RANGE;
    }

    /// Was this break taken?
    boolean broken;

    /// New indent after this break.
    int newIndent;

    public State computeBreaks(State state, int lastIndent, boolean broken) {
      if (optTag.isPresent()) {
        optTag.get().recordBroken(broken);
      }

      if (broken) {
        this.broken = true;
        this.newIndent = max(lastIndent + plusIndent.eval(), 0);
        return state.withColumn(newIndent);
      } else {
        this.broken = false;
        this.newIndent = -1;
        return state.withColumn(state.column + flat.length());
      }
    }

    @Override
    public State computeBreaks(CommentsHelper commentsHelper, int maxWidth, State state) {
      // Updating the state for {@link Break}s requires deciding if the break
      // should be taken.
      // TODO(cushon): this hierarchy is wrong, create a separate interface
      // for unbreakable Docs?
      throw new UnsupportedOperationException("Did you mean computeBreaks(State, int, boolean)?");
    }

    @Override
    public void write(Output output) {
      if (broken) {
        output.append("\n", EMPTY_RANGE);
        output.indent(newIndent);
      } else {
        output.append(flat, range());
      }
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("fillMode", fillMode)
          .add("flat", flat)
          .add("plusIndent", plusIndent)
          .add("optTag", optTag)
          .toString();
    }
  }

  /// A leaf node in a [Doc] for a non-token.
  static final class Tok extends Doc implements Op {
    private final Input.Tok tok;

    private Tok(Input.Tok tok) {
      this.tok = tok;
    }

    /// Factory method for a `Tok`.
    ///
    /// @param tok the [Input.Tok] to wrap
    /// @return the new `Tok`
    static Tok make(Input.Tok tok) {
      return new Tok(tok);
    }

    @Override
    public void add(DocBuilder builder) {
      builder.add(this);
    }

    @Override
    int computeWidth() {
      int idx = Newlines.firstBreak(tok.getOriginalText());
      // only count the first line of multi-line block comments
      if (tok.isComment()) {
        if (idx > 0) {
          return idx;
        } else if (tok.isSlashSlashComment() && !tok.getOriginalText().startsWith("// ")) {
          // Account for line comments with missing spaces, see computeFlat.
          return tok.length() + 1;
        } else {
          return reformatParameterComment(tok).map(String::length).orElse(tok.length());
        }
      }
      return idx != -1 ? MAX_LINE_WIDTH : tok.length();
    }

    @Override
    String computeFlat() {
      // TODO(cushon): commentsHelper.rewrite doesn't get called for spans that fit in a single
      // line. That's fine for multi-line comment reflowing, but problematic for adding missing
      // spaces in line comments.
      if (tok.isSlashSlashComment() && !tok.getOriginalText().startsWith("// ")) {
        return "// " + tok.getOriginalText().substring("//".length());
      }
      return reformatParameterComment(tok).orElse(tok.getOriginalText());
    }

    @Override
    Range<Integer> computeRange() {
      return Range.singleton(tok.getIndex()).canonical(INTEGERS);
    }

    String text;

    @Override
    public State computeBreaks(CommentsHelper commentsHelper, int maxWidth, State state) {
      text = commentsHelper.rewrite(tok, maxWidth, state.column);
      int firstLineLength = text.length() - Iterators.getLast(Newlines.lineOffsetIterator(text));
      return state.withColumn(state.column + firstLineLength);
    }

    @Override
    public void write(Output output) {
      output.append(text, range());
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("tok", tok).toString();
    }
  }
}
