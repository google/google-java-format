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

import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Iterators;
import com.google.common.collect.Range;
import com.google.googlejavaformat.Output.BreakTag;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link com.google.googlejavaformat.java.JavaInputAstVisitor JavaInputAstVisitor} outputs a
 * sequence of {@link Op}s using {@link OpsBuilder}. This linear sequence is then transformed by
 * {@link DocBuilder} into a tree-structured {@code Doc}. The top-level {@code Doc} is a {@link
 * Level}, which contains a sequence of {@code Doc}s, including other {@link Level}s. Leaf {@code
 * Doc}s are {@link Token}s, representing language-level tokens; {@link Tok}s, which may also
 * represent non-token {@link Input.Tok}s, including comments and other white-space; {@link Space}s,
 * representing single spaces; and {@link Break}s, which represent optional line-breaks.
 */
public abstract class Doc {
  /**
   * Each {@link Break} in a {@link Level} is either {@link FillMode#UNIFIED} or {@link
   * FillMode#INDEPENDENT}.
   */
  public enum FillMode {
    /**
     * If a {@link Level} will not fit on one line, all of its {@code UNIFIED} {@link Break}s will
     * be broken.
     */
    UNIFIED,

    /**
     * If a {@link Level} will not fit on one line, its {@code INDEPENDENT} {@link Break}s will be
     * broken independently of each other, to fill in the {@link Level}.
     */
    INDEPENDENT,

    /**
     * A {@code FORCED} {@link Break} will always be broken, and a {@link Level} it appears in will
     * not fit on one line.
     */
    FORCED
  }

  /** State for writing. */
  public static final class State {
    final int lastIndent;
    final int indent;
    final int column;
    final boolean mustBreak;

    State(int lastIndent, int indent, int column, boolean mustBreak) {
      this.lastIndent = lastIndent;
      this.indent = indent;
      this.column = column;
      this.mustBreak = mustBreak;
    }

    public State(int indent0, int column0) {
      this(indent0, indent0, column0, false);
    }

    State withColumn(int column) {
      return new State(lastIndent, indent, column, mustBreak);
    }

    State withMustBreak(boolean mustBreak) {
      return new State(lastIndent, indent, column, mustBreak);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("lastIndent", lastIndent)
          .add("indent", indent)
          .add("column", column)
          .add("mustBreak", mustBreak)
          .toString();
    }
  }

  private static final Range<Integer> EMPTY_RANGE = Range.closedOpen(-1, -1);
  private static final DiscreteDomain<Integer> INTEGERS = DiscreteDomain.integers();

  // Memoized width; Float.POSITIVE_INFINITY if contains forced breaks.
  private boolean widthComputed = false;
  private float width = 0.0F;

  // Memoized flat; not defined (and never computed) if contains forced breaks.
  private boolean flatComputed = false;
  private String flat = "";

  // Memoized Range.
  private boolean rangeComputed = false;
  private Range<Integer> range = EMPTY_RANGE;

  /**
   * Return the width of a {@code Doc}, or {@code Float.POSITIVE_INFINITY} if it must be broken.
   *
   * @return the width
   */
  final float getWidth() {
    if (!widthComputed) {
      width = computeWidth();
      widthComputed = true;
    }
    return width;
  }

  /**
   * Return a {@code Doc}'s flat-string value; not defined (and never called) if the (@code Doc}
   * contains forced breaks.
   *
   * @return the flat-string value
   */
  final String getFlat() {
    if (!flatComputed) {
      flat = computeFlat();
      flatComputed = true;
    }
    return flat;
  }

  /**
   * Return the {@link Range} of a {@code Doc}.
   *
   * @return the {@code Doc}'s {@link Range}
   */
  final Range<Integer> range() {
    if (!rangeComputed) {
      range = computeRange();
      rangeComputed = true;
    }
    return range;
  }

  /**
   * Compute the {@code Doc}'s width.
   *
   * @return the width, or {@code Float.POSITIVE_INFINITY} if it must be broken
   */
  abstract float computeWidth();

  /**
   * Compute the {@code Doc}'s flat value. Not defined (and never called) if contains forced breaks.
   *
   * @return the flat value
   */
  abstract String computeFlat();

  /**
   * Compute the {@code Doc}'s {@link Range} of {@link Input.Token}s.
   *
   * @return the {@link Range}
   */
  abstract Range<Integer> computeRange();

  /**
   * Make breaking decisions for a {@code Doc}.
   *
   * @param maxWidth the maximum line width
   * @param state the current output state
   * @return the new output state
   */
  public abstract State computeBreaks(CommentsHelper commentsHelper, int maxWidth, State state);

  /** Write a {@code Doc} to an {@link Output}, after breaking decisions have been made. */
  public abstract void write(Output output);

  /** A {@code Level} inside a {@link Doc}. */
  static final class Level extends Doc {
    private final Indent plusIndent; // The extra indent following breaks.
    private final List<Doc> docs = new ArrayList<>(); // The elements of the level.

    private Level(Indent plusIndent) {
      this.plusIndent = plusIndent;
    }

    /**
     * Factory method for {@code Level}s.
     *
     * @param plusIndent the extra indent inside the {@code Level}
     * @return the new {@code Level}
     */
    static Level make(Indent plusIndent) {
      return new Level(plusIndent);
    }

    /**
     * Add a {@link Doc} to the {@code Level}.
     *
     * @param doc the {@link Doc} to add
     */
    void add(Doc doc) {
      docs.add(doc);
    }

    @Override
    float computeWidth() {
      float thisWidth = 0.0F;
      for (Doc doc : docs) {
        thisWidth += doc.getWidth();
      }
      return thisWidth;
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

    /** True if the entire {@link Level} fits on one line. */
    boolean oneLine = false;

    /**
     * Groups of {@link Doc}s that are children of the current {@link Level}, separated by {@link
     * Break}s.
     */
    List<List<Doc>> splits = new ArrayList<>();

    /** {@link Break}s between {@link Doc}s in the current {@link Level}. */
    List<Break> breaks = new ArrayList<>();

    @Override
    public State computeBreaks(CommentsHelper commentsHelper, int maxWidth, State state) {
      float thisWidth = getWidth();
      if (state.column + thisWidth <= maxWidth) {
        oneLine = true;
        return state.withColumn(state.column + (int) thisWidth);
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
        if (doc instanceof Break) {
          breaks.add((Break) doc);
          splits.add(new ArrayList<>());
        } else {
          getLast(splits).add(doc);
        }
      }
    }

    /** Compute breaks for a {@link Level} that spans multiple lines. */
    private State computeBroken(CommentsHelper commentsHelper, int maxWidth, State state) {
      splitByBreaks(docs, splits, breaks);

      state =
          computeBreakAndSplit(
              commentsHelper, maxWidth, state, /* optBreakDoc= */ Optional.absent(), splits.get(0));

      // Handle following breaks and split.
      for (int i = 0; i < breaks.size(); i++) {
        state =
            computeBreakAndSplit(
                commentsHelper, maxWidth, state, Optional.of(breaks.get(i)), splits.get(i + 1));
      }
      return state;
    }

    /** Lay out a Break-separated group of Docs in the current Level. */
    private static State computeBreakAndSplit(
        CommentsHelper commentsHelper,
        int maxWidth,
        State state,
        Optional<Break> optBreakDoc,
        List<Doc> split) {
      float breakWidth = optBreakDoc.isPresent() ? optBreakDoc.get().getWidth() : 0.0F;
      float splitWidth = getWidth(split);
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

    /**
     * Get the width of a sequence of {@link Doc}s.
     *
     * @param docs the {@link Doc}s
     * @return the width, or {@code Float.POSITIVE_INFINITY} if any {@link Doc} must be broken
     */
    static float getWidth(List<Doc> docs) {
      float width = 0.0F;
      for (Doc doc : docs) {
        width += doc.getWidth();
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

  /** A leaf {@link Doc} for a token. */
  public static final class Token extends Doc implements Op {
    /** Is a Token a real token, or imaginary (e.g., a token generated incorrectly, or an EOF)? */
    public enum RealOrImaginary {
      REAL,
      IMAGINARY;

      boolean isReal() {
        return this == REAL;
      }
    }

    private final Input.Token token;
    private final RealOrImaginary realOrImaginary;
    private final Indent plusIndentCommentsBefore;
    private final Optional<Indent> breakAndIndentTrailingComment;

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

    /**
     * How much extra to indent comments before the {@code Token}.
     *
     * @return the extra indent
     */
    Indent getPlusIndentCommentsBefore() {
      return plusIndentCommentsBefore;
    }

    /** Force a line break and indent trailing javadoc or block comments. */
    Optional<Indent> breakAndIndentTrailingComment() {
      return breakAndIndentTrailingComment;
    }

    /**
     * Make a {@code Token}.
     *
     * @param token the {@link Input.Token} to wrap
     * @param realOrImaginary did this {@link Input.Token} appear in the input, or was it generated
     *     incorrectly?
     * @param plusIndentCommentsBefore extra {@code plusIndent} for comments just before this token
     * @return the new {@code Token}
     */
    static Op make(
        Input.Token token,
        Doc.Token.RealOrImaginary realOrImaginary,
        Indent plusIndentCommentsBefore,
        Optional<Indent> breakAndIndentTrailingComment) {
      return new Token(
          token, realOrImaginary, plusIndentCommentsBefore, breakAndIndentTrailingComment);
    }

    /**
     * Return the wrapped {@link Input.Token}.
     *
     * @return the {@link Input.Token}
     */
    Input.Token getToken() {
      return token;
    }

    /**
     * Is the token good? That is, does it match an {@link Input.Token}?
     *
     * @return whether the @code Token} is good
     */
    RealOrImaginary realOrImaginary() {
      return realOrImaginary;
    }

    @Override
    public void add(DocBuilder builder) {
      builder.add(this);
    }

    @Override
    float computeWidth() {
      return token.getTok().length();
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
      String text = token.getTok().getOriginalText();
      return state.withColumn(state.column + text.length());
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

  /** A Leaf node in a {@link Doc} for a non-breaking space. */
  static final class Space extends Doc implements Op {
    private static final Space SPACE = new Space();

    private Space() {}

    /**
     * Factor method for {@code Space}.
     *
     * @return the new {@code Space}
     */
    static Space make() {
      return SPACE;
    }

    @Override
    public void add(DocBuilder builder) {
      builder.add(this);
    }

    @Override
    float computeWidth() {
      return 1.0F;
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

  /** A leaf node in a {@link Doc} for an optional break. */
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

    /**
     * Make a {@code Break}.
     *
     * @param fillMode the {@link FillMode}
     * @param flat the text when not broken
     * @param plusIndent extra indent if taken
     * @return the new {@code Break}
     */
    public static Break make(FillMode fillMode, String flat, Indent plusIndent) {
      return new Break(fillMode, flat, plusIndent, /* optTag= */ Optional.absent());
    }

    /**
     * Make a {@code Break}.
     *
     * @param fillMode the {@link FillMode}
     * @param flat the text when not broken
     * @param plusIndent extra indent if taken
     * @param optTag an optional tag for remembering whether the break was taken
     * @return the new {@code Break}
     */
    public static Break make(
        FillMode fillMode, String flat, Indent plusIndent, Optional<BreakTag> optTag) {
      return new Break(fillMode, flat, plusIndent, optTag);
    }

    /**
     * Make a forced {@code Break}.
     *
     * @return the new forced {@code Break}
     */
    public static Break makeForced() {
      return make(FillMode.FORCED, "", Indent.Const.ZERO);
    }

    /**
     * Return the {@code Break}'s extra indent.
     *
     * @return the extra indent
     */
    int getPlusIndent() {
      return plusIndent.eval();
    }

    /**
     * Is the {@code Break} forced?
     *
     * @return whether the {@code Break} is forced
     */
    boolean isForced() {
      return fillMode == FillMode.FORCED;
    }

    @Override
    public void add(DocBuilder builder) {
      builder.breakDoc(this);
    }

    @Override
    float computeWidth() {
      return isForced() ? Float.POSITIVE_INFINITY : (float) flat.length();
    }

    @Override
    String computeFlat() {
      return flat;
    }

    @Override
    Range<Integer> computeRange() {
      return EMPTY_RANGE;
    }

    /** Was this break taken? */
    boolean broken;

    /** New indent after this break. */
    int newIndent;

    public State computeBreaks(State state, int lastIndent, boolean broken) {
      if (optTag.isPresent()) {
        optTag.get().recordBroken(broken);
      }

      if (broken) {
        this.broken = true;
        this.newIndent = Math.max(lastIndent + plusIndent.eval(), 0);
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

  /** A leaf node in a {@link Doc} for a non-token. */
  static final class Tok extends Doc implements Op {
    private final Input.Tok tok;

    private Tok(Input.Tok tok) {
      this.tok = tok;
    }

    /**
     * Factory method for a {@code Tok}.
     *
     * @param tok the {@link Input.Tok} to wrap
     * @return the new {@code Tok}
     */
    static Tok make(Input.Tok tok) {
      return new Tok(tok);
    }

    @Override
    public void add(DocBuilder builder) {
      builder.add(this);
    }

    @Override
    float computeWidth() {
      int idx = Newlines.firstBreak(tok.getOriginalText());
      // only count the first line of multi-line block comments
      if (tok.isComment()) {
        if (idx > 0) {
          return idx;
        } else if (tok.isSlashSlashComment() && !tok.getOriginalText().startsWith("// ")) {
          // Account for line comments with missing spaces, see computeFlat.
          return tok.length() + 1;
        } else {
          return tok.length();
        }
      }
      return idx != -1 ? Float.POSITIVE_INFINITY : (float) tok.length();
    }

    @Override
    String computeFlat() {
      // TODO(cushon): commentsHelper.rewrite doesn't get called for spans that fit in a single
      // line. That's fine for multi-line comment reflowing, but problematic for adding missing
      // spaces in line comments.
      if (tok.isSlashSlashComment() && !tok.getOriginalText().startsWith("// ")) {
        return "// " + tok.getOriginalText().substring("//".length());
      }
      return tok.getOriginalText();
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
