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

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.max;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.googlejavaformat.CommentsHelper;
import com.google.googlejavaformat.Indent;
import com.google.googlejavaformat.Newlines;
import com.google.googlejavaformat.Output;
import java.util.ArrayList;
import java.util.List;

/// Lays out a [Doc].
///
/// Layout is greedy and single-pass, like Wadler's `be`/`better` ([A prettier
/// printer](https://homepages.inf.ed.ac.uk/wadler/papers/prettier/prettier.pdf), §2-3): each group
/// is laid out flat if it fits, and otherwise broken, without revisiting earlier decisions. The fit
/// test is local to the group (see [Doc]), so every decision looks at most `maxWidth` columns ahead
/// and layout takes time linear in the size of the document.
///
/// Layout happens in two phases. The first phase makes every line-breaking decision in document
/// order and records the resulting output operations; it also records whether each
/// [Output.BreakTag] was taken. The second phase replays the operations to the output. Separating
/// the phases lets output decisions that depend on break tags (e.g. conditional blank lines) see
/// the final state of every tag.
public final class LayoutSolver {

  private LayoutSolver() {}

  /// Formats `doc` to a string. Comments are written verbatim.
  public static String format(Doc doc, int maxWidth) {
    StringBuilder sb = new StringBuilder();
    for (Action action : solve(doc, maxWidth, /* commentsHelper= */ null)) {
      switch (action) {
        case Append a -> sb.append(a.text());
        case Newline n -> sb.append('\n');
        case IndentTo i -> sb.repeat(" ", i.spaces());
      }
    }
    return sb.toString();
  }

  /// Formats `doc` to `output`, reformatting comments with `commentsHelper`.
  public static void format(Doc doc, int maxWidth, Output output, CommentsHelper commentsHelper) {
    for (Action action : solve(doc, maxWidth, commentsHelper)) {
      switch (action) {
        case Append a -> output.append(a.text(), a.range());
        case Newline n -> output.append("\n", Doc.EMPTY_RANGE);
        case IndentTo i -> output.indent(i.spaces());
      }
    }
  }

  private sealed interface Action {}

  private record Append(String text, Range<Integer> range) implements Action {}

  private record Newline() implements Action {}

  private record IndentTo(int spaces) implements Action {}

  /// Layout state.
  ///
  /// @param indent the indent for breaks in the current group
  /// @param column the current output column
  private record State(int indent, int column) {
    State withColumn(int column) {
      return new State(indent, column);
    }
  }

  private static List<Action> solve(Doc doc, int maxWidth, CommentsHelper commentsHelper) {
    // Widths saturate at MAX_LINE_WIDTH, which is also how forced breaks are represented, so a
    // document whose width is MAX_LINE_WIDTH must never fit.
    checkArgument(
        maxWidth < Doc.MAX_LINE_WIDTH,
        "maxWidth must be less than %s: %s",
        Doc.MAX_LINE_WIDTH,
        maxWidth);
    Doc.Group root =
        doc instanceof Doc.Group g ? g : new Doc.Group(Indent.Const.ZERO, ImmutableList.of(doc));
    Solver solver = new Solver(maxWidth, commentsHelper);
    var unused = solver.group(root, new State(0, 0));
    return solver.actions;
  }

  private static final class Solver {
    private final int maxWidth;
    private final CommentsHelper commentsHelper;
    private final List<Action> actions = new ArrayList<>();

    Solver(int maxWidth, CommentsHelper commentsHelper) {
      this.maxWidth = maxWidth;
      this.commentsHelper = commentsHelper;
    }

    /// A group that fits is laid out flat (Wadler's `flatten`); otherwise it's split at its direct
    /// breaks. This is Wadler's `group x = flatten x <|> x`, choosing the flat alternative whenever
    /// it fits.
    State group(Doc.Group group, State state) {
      int width = group.width();
      if (state.column() + width <= maxWidth) {
        actions.add(new Append(group.flat(), group.range()));
        return state.withColumn(state.column() + width);
      }
      State broken =
          broken(group, new State(state.indent() + group.plusIndent().eval(), state.column()));
      return state.withColumn(broken.column());
    }

    private State broken(Doc.Group group, State state) {
      List<Doc> segment = new ArrayList<>();
      Doc.Break before = null;
      boolean previousFits = true;
      for (Doc doc : group.docs()) {
        if (doc instanceof Doc.Break b) {
          SegmentResult result = segment(before, segment, previousFits, state);
          state = result.state();
          previousFits = result.fits();
          before = b;
          segment = new ArrayList<>();
        } else {
          segment.add(doc);
        }
      }
      return segment(before, segment, previousFits, state).state();
    }

    /// The result of laying out a segment.
    ///
    /// @param state the state after the segment
    /// @param fits whether the segment fit on the line it started on
    private record SegmentResult(State state, boolean fits) {}

    /// Lays out a break and the segment following it. Unified and forced breaks are taken whenever
    /// their group is broken; a fill break is taken if the segment doesn't fit, or if the previous
    /// segment didn't fit. Fill breaks are a segment-at-a-time version of Wadler's `fill` (§4),
    /// which places a separator flat when the next item fits and breaks otherwise.
    private SegmentResult segment(
        Doc.Break before, List<Doc> segment, boolean previousFits, State state) {
      int segmentWidth = Doc.Group.totalWidth(segment);
      if (before != null) {
        boolean taken =
            switch (before.mode()) {
              case UNIFIED, FORCED -> true;
              case FILL ->
                  !previousFits || state.column() + before.width() + segmentWidth > maxWidth;
            };
        state = breakOp(before, taken, state);
      }
      boolean fits = state.column() + segmentWidth <= maxWidth;
      for (Doc doc : segment) {
        state = leaf(doc, state);
      }
      return new SegmentResult(state, fits);
    }

    private State breakOp(Doc.Break b, boolean taken, State state) {
      b.tag().ifPresent(t -> t.recordBroken(taken));
      if (taken) {
        int indent = max(state.indent() + b.plusIndent().eval(), 0);
        actions.add(new Newline());
        actions.add(new IndentTo(indent));
        return state.withColumn(indent);
      }
      actions.add(new Append(b.flatText(), Doc.EMPTY_RANGE));
      return state.withColumn(state.column() + b.flatText().length());
    }

    private State leaf(Doc doc, State state) {
      return switch (doc) {
        case Doc.Group g -> group(g, state);
        case Doc.Text t -> {
          actions.add(new Append(t.text(), t.range()));
          yield state.withColumn(state.column() + t.width());
        }
        case Doc.Space s -> {
          actions.add(new Append(" ", Doc.EMPTY_RANGE));
          yield state.withColumn(state.column() + 1);
        }
        case Doc.Tok c -> {
          String text =
              commentsHelper != null
                  ? commentsHelper.rewrite(c.tok(), maxWidth, state.column())
                  : c.flat();
          actions.add(new Append(text, c.range()));
          int lastLineLength = text.length() - Newlines.lastLineStart(text);
          yield state.withColumn(state.column() + lastLineLength);
        }
        case Doc.Break b -> throw new AssertionError(b);
      };
    }
  }
}
