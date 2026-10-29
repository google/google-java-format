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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
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
        case Newline n -> sb.repeat("\n", n.count());
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
        case Newline n -> output.append("\n".repeat(n.count()), Doc.EMPTY_RANGE);
        case IndentTo i -> output.indent(i.spaces());
      }
    }
  }

  private sealed interface Action {}

  private record Append(String text, Range<Integer> range) implements Action {}

  /// Emits `count` line breaks; `count` is more than 1 when a [Doc.Lines] contains blank lines.
  private record Newline(int count) implements Action {
    Newline() {
      this(1);
    }
  }

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

    /// Whether the innermost trial layout has reached the end of its first line.
    private boolean firstLineEnded = false;

    /// The column at which the innermost trial's first line ended, if [#firstLineEnded].
    private int firstLineEndColumn;

    /// The indent of the current line.
    private int lineIndent = 0;

    /// The state of a break tag before it was modified during a trial layout.
    ///
    /// @param tag the break tag
    /// @param wasBroken the tag's [Output.BreakTag#wasBreakTaken] value before it was modified
    private record SavedBreakTag(Output.BreakTag tag, boolean wasBroken) {
      void restore() {
        tag.recordBroken(wasBroken);
      }
    }

    /// A stack of active trial layouts (see [#firstLineEnd]). While non-empty, ending the current
    /// line stops the innermost trial: [#endLine] sets [#firstLineEnded], and layout methods return
    /// as soon as they see it set.
    ///
    /// Each entry records the previous state of every [Output.BreakTag] modified during that trial,
    /// so that the tags can be restored when the trial ends.
    private final Deque<List<SavedBreakTag>> trialTagsStack = new ArrayDeque<>();

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
          if (firstLineEnded) {
            return state;
          }
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
        if (firstLineEnded) {
          // The trial is over, so whether the segment fits doesn't matter.
          return new SegmentResult(state, true);
        }
      }
      boolean fits = state.column() + segmentWidth <= maxWidth;
      for (Doc doc : segment) {
        state = leaf(doc, state);
        if (firstLineEnded) {
          return new SegmentResult(state, fits);
        }
      }
      return new SegmentResult(state, fits);
    }

    private State breakOp(Doc.Break b, boolean taken, State state) {
      b.tag().ifPresent(t -> recordTag(t, taken));
      if (taken) {
        endLine(state.column());
        if (firstLineEnded) {
          return state;
        }
        int indent = max(state.indent() + b.plusIndent().eval(), 0);
        lineIndent = indent;
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
        case Doc.Choice c -> choice(c, state);
        case Doc.Text t -> {
          int newline = Newlines.firstBreak(t.text());
          if (newline >= 0) {
            endLine(state.column() + newline);
          }
          actions.add(new Append(t.text(), t.range()));
          yield state.withColumn(state.column() + t.width());
        }
        case Doc.Lines l -> {
          ImmutableList<String> lines = l.lines();
          endLine(state.column() + lines.get(0).length());
          if (firstLineEnded) {
            yield state;
          }
          int indent = lineIndent;
          actions.add(new Append(lines.get(0), l.range()));
          int newlines = 0;
          for (String line : lines.subList(1, lines.size())) {
            newlines++;
            if (line.isEmpty()) {
              continue;
            }
            actions.add(new Newline(newlines));
            newlines = 0;
            if (indent > 0) {
              actions.add(new IndentTo(indent));
            }
            actions.add(new Append(line, l.range()));
          }
          if (newlines > 0) {
            actions.add(new Newline(newlines));
          }
          yield state.withColumn(state.column() + l.width());
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
          int newline = Newlines.firstBreak(text);
          if (newline >= 0) {
            endLine(state.column() + newline);
          }
          actions.add(new Append(text, c.range()));
          int lastLineLength = text.length() - Newlines.lastLineStart(text);
          yield state.withColumn(state.column() + lastLineLength);
        }
        case Doc.Break b -> throw new AssertionError(b);
      };
    }

    /// Lays out a choice. If it doesn't fit flat, the fallback is used if breaking at its direct
    /// break allows the rest of it to fit flat on the next line (e.g. `f(\n x -> g(x))` rather than
    /// breaking inside the lambda). Otherwise, the preferred layout is used when its prefix fits
    /// flat on the current line, its first line fits, and that line contains the whole prefix (e.g.
    /// a hugged lambda isn't preceded by a line comment); otherwise the fallback is used.
    ///
    /// The preferred layout is indented relative to the indent of the current line, rather than the
    /// indent of the enclosing groups. It starts partway along a line, so enclosing groups whose
    /// breaks weren't taken shouldn't contribute to its indentation. For example, a hugged lambda
    /// body is indented relative to the line containing the call.
    private State choice(Doc.Choice choice, State state) {
      int width = choice.width();
      if (state.column() + width <= maxWidth) {
        actions.add(new Append(choice.flat(), choice.range()));
        return state.withColumn(state.column() + width);
      }
      if (choice.fallbackIfFlatOnNextLine() && fallbackFitsOnNextLine(choice.fallback(), state)) {
        return group(choice.fallback(), state);
      }
      State preferredState = new State(lineIndent, state.column());
      int prefixEnd = state.column() + choice.prefixWidth();
      if (prefixEnd <= maxWidth) {
        Trial trial = firstLineEnd(choice.preferred(), preferredState);
        int firstLineEnd = trial.column();
        if (prefixEnd <= firstLineEnd && firstLineEnd <= maxWidth) {
          if (!trialTagsStack.isEmpty() && trial.lineEnded()) {
            // The enclosing trial ends on the line the preferred layout's trial just ended, so
            // laying out the preferred layout again would only rediscover that column. Skipping it
            // keeps the cost of nested choices linear, instead of doubling it at each level.
            endLine(firstLineEnd);
            return state;
          }
          return state.withColumn(group(choice.preferred(), preferredState).column());
        }
      }
      return group(choice.fallback(), state);
    }

    /// Returns whether `fallback` can be laid out by taking a single direct break and fitting
    /// everything after that break flat on the next line.
    private boolean fallbackFitsOnNextLine(Doc.Group fallback, State state) {
      ImmutableList<Doc> docs = fallback.docs();
      int breakIndex = -1;
      for (int i = 0; i < docs.size(); i++) {
        if (docs.get(i) instanceof Doc.Break) {
          if (breakIndex != -1) {
            return false;
          }
          breakIndex = i;
        }
      }
      if (breakIndex == -1
          || state.column() + Doc.Group.totalWidth(docs.subList(0, breakIndex)) > maxWidth) {
        return false;
      }
      Doc.Break theBreak = (Doc.Break) docs.get(breakIndex);
      int indent =
          max(state.indent() + fallback.plusIndent().eval() + theBreak.plusIndent().eval(), 0);
      return indent + Doc.Group.totalWidth(docs.subList(breakIndex + 1, docs.size())) <= maxWidth;
    }

    /// The result of a trial layout.
    ///
    /// @param column the column at which the first line ended, or at which the layout ended if it
    ///   fit on one line
    /// @param lineEnded whether the first line ended before the end of the layout
    private record Trial(int column, boolean lineEnded) {}

    /// Lays out `group` provisionally, up to the end of its first line, and returns the column at
    /// which that line ends. The layout's output and break tags are discarded.
    ///
    /// Only the first line is laid out, so the cost of a trial is bounded by the line width rather
    /// than by the size of `group`. A nested trial consumes [#firstLineEnded] before returning, so
    /// the enclosing trial continues.
    private Trial firstLineEnd(Doc.Group group, State state) {
      int mark = actions.size();
      int savedLineIndent = lineIndent;
      trialTagsStack.push(new ArrayList<>());
      State end = group(group, state);
      Trial trial =
          firstLineEnded ? new Trial(firstLineEndColumn, true) : new Trial(end.column(), false);
      firstLineEnded = false;
      lineIndent = savedLineIndent;
      actions.subList(mark, actions.size()).clear();
      for (SavedBreakTag saved : trialTagsStack.pop().reversed()) {
        saved.restore();
      }
      return trial;
    }

    /// Records that the current line ends at `column`. In a trial layout, this ends the trial;
    /// callers must check [#firstLineEnded] and return.
    private void endLine(int column) {
      if (!trialTagsStack.isEmpty()) {
        firstLineEnded = true;
        firstLineEndColumn = column;
      }
    }

    private void recordTag(Output.BreakTag tag, boolean taken) {
      if (!trialTagsStack.isEmpty()) {
        trialTagsStack.peek().add(new SavedBreakTag(tag, tag.wasBreakTaken()));
      }
      tag.recordBroken(taken);
    }
  }
}
