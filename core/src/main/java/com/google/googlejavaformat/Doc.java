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
import com.google.common.base.Optional;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import com.google.googlejavaformat.Output.BreakTag;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link com.google.googlejavaformat.java.JavaInputAstVisitor JavaInputAstVisitor} outputs a
 * sequence of {@link Op}s using {@link OpsBuilder}. This linear sequence is then transformed by
 * {@link DocBuilder} into a tree-structured {@code Doc}. The top-level {@code Doc} is a
 * {@link Level}, which contains a sequence of {@code Doc}s, including other {@link Level}s. Leaf
 * {@code Doc}s are {@link Token}s, representing language-level tokens; {@link Tok}s, which may also
 * represent non-token {@link Input.Tok}s, including comments and other white-space; {@link Space}s,
 * representing single spaces; and {@link Break}s, which represent optional line-breaks.
 */
public abstract class Doc {
  /**
   * Each {@link Break} in a {@link Level} is either {@link FillMode#UNIFIED} or
   * {@link FillMode#INDEPENDENT}.
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
    final int indent0;
    final int lastIndent;
    final int indent;
    final int column;
    final boolean mustBreak;

    State(int indent0, int lastIndent, int indent, int column, boolean mustBreak) {
      this.indent0 = indent0;
      this.lastIndent = lastIndent;
      this.indent = indent;
      this.column = column;
      this.mustBreak = mustBreak;
    }

    public State(int indent0, int column0) {
      this(indent0, indent0, indent0, column0, false);
    }

    State withColumn(int column) {
      return new State(indent0, lastIndent, indent, column, mustBreak);
    }

    State withIndent(int indent) {
      return new State(indent0, lastIndent, indent, column, mustBreak);
    }

    State withLastIndent(int lastIndent) {
      return new State(indent0, lastIndent, indent, column, mustBreak);
    }

    State withMustBreak(boolean mustBreak) {
      return new State(indent0, lastIndent, indent, column, mustBreak);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("indent0", indent0)
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
   * @return the width, or {@code Float.POSITIVE_INFINITY} if it must be broken
   */
  abstract float computeWidth();

  /**
   * Compute the {@code Doc}'s flat value. Not defined (and never called) if contains forced breaks.
   * @return the flat value
   */
  abstract String computeFlat();

  /**
   * Compute the {@code Doc}'s {@link Range} of {@link Input.Token}s.
   * @return the {@link Range}
   */
  abstract Range<Integer> computeRange();

  /**
   * Output a {@code Doc} to an {@link Output}.
   * @param output the {@link Output}
   * @param maxWidth the maximum line width
   * @param state the current output state
   * @return the new output state
   */
  public abstract State write(Output output, int maxWidth, State state);

  /** A {@code Level} inside a {@link Doc}. */
  static final class Level extends Doc {
    private final Indent plusIndent; // The extra indent following breaks.
    private final int maxLinesFilled; // If positive, max lines for filled format, otherwise no max.
    private final List<Doc> docs = new ArrayList<>(); // The elements of the level.

    private Level(Indent plusIndent, int maxLinesFilled) {
      this.plusIndent = plusIndent;
      this.maxLinesFilled = maxLinesFilled;
    }

    /**
     * Factory method for {@code Level}s.
     * @param plusIndent the extra indent inside the {@code Level}
     * @param maxLinesFilled if positive, cannot be in filled mode if it takes more lines
     * @return the new {@code Level}
     */
    static Level make(Indent plusIndent, int maxLinesFilled) {
      return new Level(plusIndent, maxLinesFilled);
    }

    /**
     * Add a {@link Doc} to the {@code Level}.
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

    @Override
    public State write(Output output, int maxWidth, State state) {
      float thisWidth = getWidth();
      if ((float) state.column + thisWidth <= (float) maxWidth) {
        output.append(getFlat(), range()); // This is defined because width is finite.
        return state.withColumn(state.column + (int) thisWidth);
      }
      return state.withColumn(
          writeMaybeFilled(
                  output, maxWidth, new State(state.indent + plusIndent.eval(output), state.column))
              .column);
    }

    private State writeMaybeFilled(Output output, int maxWidth, State state) {
      if (maxLinesFilled > 0) {
        FakeOutput fakeOutput =
            new FakeOutput(maxWidth, output.getCommentsHelper(), output.breaksTaken());
        writeBroken(fakeOutput, maxWidth, state, false);
        return writeBroken(output, maxWidth, state, fakeOutput.getLineI() >= maxLinesFilled);
      } else {
        return writeBroken(output, maxWidth, state, false);
      }
    }

    private State writeBroken(Output output, int maxWidth, State state0, boolean breakAll) {
      List<List<Doc>> splits = new ArrayList<>();
      List<Break> breaks = new ArrayList<>();
      int n = splitByBreaks(docs, splits, breaks);
      // Handle first split.
      State state =
          writeBreakAndSplit(
              output, maxWidth, state0, Optional.<Break>absent(), splits.get(0), breakAll);
      // Handle following breaks and split.
      for (int i = 0; i < n; i++) {
        state =
            writeBreakAndSplit(
                output, maxWidth, state, Optional.of(breaks.get(i)), splits.get(i + 1), breakAll);
      }
      return state;
    }

    private static State writeBreakAndSplit(
        Output output, int maxWidth, State state0, Optional<Break> optBreakDoc, List<Doc> split,
        boolean breakAll) {
      float breakWidth = optBreakDoc.isPresent() ? optBreakDoc.get().getWidth() : 0.0F;
      float splitWidth = getWidth(split);
      State state = state0;
      if (!breakAll && !(optBreakDoc.isPresent() && optBreakDoc.get().fillMode == FillMode.UNIFIED)
          && !state.mustBreak
          && (float) state.column + breakWidth + splitWidth <= (float) maxWidth) {
        // Unbroken break, followed by flat text.
        if (breakWidth > 0.0F) {
          output.append(optBreakDoc.get().getFlat(), EMPTY_RANGE);
        }
        state =
            writeSplit(output, maxWidth, split, state.withColumn(state.column + (int) breakWidth));
      } else {
        // Break.
        if (optBreakDoc.isPresent()) {
          Break breakDoc = optBreakDoc.get();
          state = state.withColumn(breakDoc.writeBroken(output, state.lastIndent));
        }
        state = state.withMustBreak(false);
        boolean enoughRoom = (float) state.column + splitWidth <= (float) maxWidth;
        state = writeSplit(output, maxWidth, split, state);
        if (!enoughRoom) {
          state = state.withMustBreak(true); // Break after, too.
        }
      }
      return state;
    }

    private static State writeSplit(Output output, int maxWidth, List<Doc> docs, State state0) {
      State state = state0;
      for (Doc doc : docs) {
        state = doc.write(output, maxWidth, state);
      }
      return state;
    }

    private static int splitByBreaks(List<Doc> docs, List<List<Doc>> splits, List<Break> breaks) {
      splits.clear();
      breaks.clear();
      splits.add(new ArrayList<Doc>());
      for (Doc doc : docs) {
        if (doc instanceof Break) {
          breaks.add((Break) doc);
          splits.add(new ArrayList<Doc>());
        } else {
          splits.get(splits.size() - 1).add(doc);
        }
      }
      return breaks.size();
    }

    /**
     * Get the width of a sequence of {@link Doc}s.
     * @param docs the {@link Doc}s
     * @return the width, or {@code Float.POSITIVE_INFINITY} if any {@link Doc} must br broken
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
          .add("maxLinesFilled", maxLinesFilled)
          .add("docs", docs)
          .toString();
    }
  }

  /** A leaf {@link Doc} for a token. */
  public static final class Token extends Doc implements Op {
    /**
     * Is a Token a real token, or imaginary (e.g., a token generated incorrectly, or an EOF)?
     */
    public enum RealOrImaginary {
      REAL, IMAGINARY;

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
     * @return the extra indent
     */
    Indent getPlusIndentCommentsBefore() {
      return plusIndentCommentsBefore;
    }

    /**
     * Force a line break and indent trailing javadoc or block comments.
     */
    Optional<Indent> breakAndIndentTrailingComment() {
      return breakAndIndentTrailingComment;
    }

    /**
     * Make a {@code Token}.
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
     * @return the {@link Input.Token}
     */
    Input.Token getToken() {
      return token;
    }

    /**
     * Is the token good? That is, does it match an {@link Input.Token}?
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
      return (float) token.getTok().getOriginalText().length();
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
    public State write(Output output, int maxWidth, State state) {
      String text = token.getTok().getOriginalText();
      output.append(text, range());
      return state.withColumn(state.column + text.length());
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
    public State write(Output output, int maxWidth, State state) {
      output.append(" ", range());
      return state.withColumn(state.column + 1);
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

    private Break(
        FillMode fillMode, String flat, Indent plusIndent,
        Optional<BreakTag> optTag) {
      this.fillMode = fillMode;
      this.flat = flat;
      this.plusIndent = plusIndent;
      this.optTag = optTag;
    }

    /**
     * Make a {@code Break}.
     * @param fillMode the {@link FillMode}
     * @param flat the the text when not broken
     * @param plusIndent extra indent if taken
     * @return the new {@code Break}
     */
    public static Break make(FillMode fillMode, String flat, Indent plusIndent) {
      return new Break(
          fillMode, flat, plusIndent, Optional.<BreakTag>absent());
    }

    /**
     * Make a {@code Break}.
     * @param fillMode the {@link FillMode}
     * @param flat the the text when not broken
     * @param plusIndent extra indent if taken
     * @param optTag an optional tag for remembering whether the break was taken
     * @return the new {@code Break}
     */
    static Break make(
        FillMode fillMode, String flat, Indent plusIndent, Optional<BreakTag> optTag) {
      return new Break(fillMode, flat, plusIndent, optTag);
    }

    /**
     * Make a forced {@code Break}.
     * @return the new forced {@code Break}
     */
    public static Break makeForced() {
      return make(FillMode.FORCED, "", Indent.Const.ZERO);
    }

    /**
     * Return the {@code Break}'s extra indent.
     * @return the extra indent
     */
    int getPlusIndent(Output output) {
      return plusIndent.eval(output);
    }

    /**
     * Is the {@code Break} forced?
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

    @Override
    public State write(Output output, int maxWidth, State state) {
      output.append(flat, range());
      return state.withColumn(state.column + flat.length());
    }

    /**
     * Write a broken {@code Break}.
     * @param output the {@link Output} target
     * @param indent the current indent
     * @return the new column
     */
    int writeBroken(Output output, int indent) {
      output.append("\n", EMPTY_RANGE);
      int newIndent = Math.max(indent + plusIndent.eval(output), 0);
      output.indent(newIndent);
      if (optTag.isPresent()) {
        output.breakWasTaken(optTag.get());
      }
      return newIndent;
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
      return tok.getOriginalText().contains("\n")
          ? Float.POSITIVE_INFINITY
          : (float) tok.getOriginalText().length();
    }

    @Override
    String computeFlat() {
      return tok.getOriginalText();
    }

    @Override
    Range<Integer> computeRange() {
      return Range.singleton(tok.getIndex()).canonical(INTEGERS);
    }

    @Override
    public State write(Output output, int maxWidth, State state) {
      int column = state.column;
      String text = output.getCommentsHelper().rewrite(tok, maxWidth, column);
      output.append(text, range());
      // TODO(lowasser): use lastIndexOf('\n')
      for (char c : text.toCharArray()) {
        column = c == '\n' ? 0 : column + 1;
      }
      return state.withColumn(column);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("tok", tok).toString();
    }
  }
}
