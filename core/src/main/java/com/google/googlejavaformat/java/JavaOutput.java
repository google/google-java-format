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

package com.google.googlejavaformat.java;

import com.google.common.base.MoreObjects;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeRangeSet;
import com.google.googlejavaformat.CommentsHelper;
import com.google.googlejavaformat.InputOutput;
import com.google.googlejavaformat.Output;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * Throughout this file, {@code i} is an index for input lines, {@code j} is an index for output
 * lines, {@code ij} is an index into either input or output lines, and {@code k} is an index for
 * toks.
 */

/**
 * {@code JavaOutput} extends {@link Output Output} to represent a Java output document. It includes
 * methods to emit the output document.
 */
public final class JavaOutput extends Output {
  /** We merge untouched lines from the input with reformatted lines from the output. */
  enum From {
    INPUT, OUTPUT
  }

  /**
   * A non-empty half-open run of lines in the merged result.
   */
  static final class RunInfo {
    final From from; // Where is the run from?
    final int ij0; // The run's (included) lower bound.
    final int ij1; // The run's (non-included) upper bound.

    /** LinesInfo constructor. */
    RunInfo(From from, int ij0, int ij1) {
      this.from = from;
      this.ij0 = ij0;
      this.ij1 = ij1;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("from", from)
          .add("ij0", ij0)
          .add("ij1", ij1)
          .toString();
    }
  }

  private static final String LINE_TOO_LONG_WARNING =
      "THE FOLLOWING LINE WAS TOO LONG TO FIT. PLEASE CORRECT IT.";

  private final JavaInput javaInput; // Used to follow along while emitting the output.
  private final CommentsHelper commentsHelper; // Used to re-flow comments.
  private final boolean addWarnings; // Add warnings as comments in output.
  private final Map<Integer, Boolean> blankLines = new HashMap<>(); // Info on blank lines.

  private final List<String> mutableLines = new ArrayList<>();
  private final int kN; // The number of tokens or comments in the input, excluding the EOF.
  private int iLine = 0; // Closest corresponding line number on input.
  private int lastK = -1; // Last {@link Tok} index output.
  private int spacesPending = 0;
  private int newlinesPending = 0;
  private StringBuilder lineBuilder = new StringBuilder();

  private final Set<BreakTag> breaksTaken = Sets.newIdentityHashSet();

  /**
   * {@code JavaOutput} constructor.
   * @param javaInput the {@link JavaInput}, used to match up blank lines in the output
   * @param commentsHelper the {@link CommentsHelper}, used to rewrite comments
   */
  public JavaOutput(JavaInput javaInput, CommentsHelper commentsHelper, boolean addWarnings) {
    this.javaInput = javaInput;
    this.commentsHelper = commentsHelper;
    this.addWarnings = addWarnings;
    kN = javaInput.getkN();
  }

  @Override
  public void blankLine(int k, boolean wanted) {
    blankLines.put(k, wanted);
  }

  @Override
  public void breakWasTaken(BreakTag breakTag) {
    breaksTaken.add(breakTag);
  }

  @Override
  public boolean wasBreakTaken(BreakTag breakTag) {
    return breaksTaken.contains(breakTag);
  }

  @Override
  public Set<BreakTag> breaksTaken() {
    return ImmutableSet.copyOf(breaksTaken);
  }

  // TODO(jdd): Add invariant.
  @Override
  public void append(String text, Range<Integer> range) {
    if (!range.isEmpty()) {
      boolean sawNewlines = false;
      // Skip over input line we've passed.
      int iN = javaInput.getLineCount();
      while (iLine < iN
          && (javaInput.getRange1s(iLine).isEmpty()
              || javaInput.getRange1s(iLine).upperEndpoint() <= range.lowerEndpoint())) {
        if (javaInput.getRanges(iLine).isEmpty()) {
          // Skipped over a blank line.
          sawNewlines = true;
        }
        ++iLine;
      }
      /*
       * Output blank line if we've called {@link OpsBuilder#blankLine}{@code (true)} here, or if
       * there's a blank line here and we haven't called {@link OpsBuilder#blankLine}{@code (false)}
       * here, OR if it's a comment.
       */
      Boolean wanted = blankLines.get(lastK);
      if (wanted == null || isComment(text) ? sawNewlines : wanted) {
        ++newlinesPending;
      }
    }
    if (text.equals("\n")) {
      /*
       * Don't update range information, and swallow extra newlines. The case below for '\n' is for
       * block comments.
       */
      if (newlinesPending == 0) {
        ++newlinesPending;
      }
      spacesPending = 0;
    } else {
      boolean range0sSet = false;
      boolean rangesSet = false;
      int textN = text.length();
      for (int i = 0; i < textN; i++) {
        char c = text.charAt(i);
        switch (c) {
          case ' ':
            ++spacesPending;
            break;
          case '\n':
            spacesPending = 0;
            ++newlinesPending;
            break;
          default:
            while (newlinesPending > 0) {
              mutableLines.add(lineBuilder.toString());
              lineBuilder = new StringBuilder();
              rangesSet = false;
              --newlinesPending;
            }
            while (spacesPending > 0) {
              lineBuilder.append(' ');
              --spacesPending;
            }
            lineBuilder.append(c);
            if (!range.isEmpty()) {
              if (!range0sSet) {
                if (!range.isEmpty()) {
                  while (range0s.size() <= mutableLines.size()) {
                    range0s.add(Formatter.EMPTY_RANGE);
                  }
                  range0s.set(mutableLines.size(), union(range0s.get(mutableLines.size()), range));
                  range0sSet = true;
                }
              }
              if (!rangesSet) {
                while (ranges.size() <= mutableLines.size()) {
                  ranges.add(Formatter.EMPTY_RANGE);
                }
                ranges.set(mutableLines.size(), union(ranges.get(mutableLines.size()), range));
                rangesSet = true;
              }
            }
        }
      }
      // TODO(jdd): Move others down here. Use common method for these.
      if (!range.isEmpty()) {
        while (range1s.size() <= mutableLines.size()) {
          range1s.add(Formatter.EMPTY_RANGE);
        }
        range1s.set(mutableLines.size(), union(range1s.get(mutableLines.size()), range));
      }
    }
    if (!range.isEmpty()) {
      lastK = range.upperEndpoint();
    }
  }

  @Override
  public void indent(int indent) {
    spacesPending = indent;
  }

  /** Flush any incomplete last line, then add the EOF token into our data structures. */
  void flush() {
    String lastLine = lineBuilder.toString();
    if (!lastLine.isEmpty()) {
      mutableLines.add(lastLine);
    }
    int jN = mutableLines.size();
    Range<Integer> eofRange = Range.closedOpen(kN, kN + 1);
    while (range0s.size() < jN) {
      range0s.add(Formatter.EMPTY_RANGE);
    }
    range0s.add(eofRange);
    while (ranges.size() < jN) {
      ranges.add(Formatter.EMPTY_RANGE);
    }
    ranges.add(eofRange);
    while (range1s.size() < jN) {
      range1s.add(Formatter.EMPTY_RANGE);
    }
    range1s.add(eofRange);
    setLines(ImmutableList.copyOf(mutableLines));
  }

  // The following methods can be used after the Output has been built.

  @Override
  public CommentsHelper getCommentsHelper() {
    return commentsHelper;
  }

  /**
   * Merge the (un-reformatted) input lines and the (reformatted) output lines. The result will
   * contain all of the toks from the input and output, combining whole lines from the input and
   * output, will contain the specified lines from the output, and will contain as few extra output
   * lines as possible.
   * @param writer the destination {@link Writer}
   * @param iRangeSet0 the canonical {@link Range} of input lines to reformat
   * @param maxWidth the maximum line width
   * @param errors the list of errors to copy to the output
   * @throws IOException on IO error
   */
  public void writeMerged(
      Appendable writer, RangeSet<Integer> iRangeSet0, int maxWidth, List<String> errors)
      throws IOException {
    int mergedLineNumber = 0;
    for (RunInfo runInfo : pickRuns(iRangeSet0)) {
      From from = runInfo.from;
      for (int ij = runInfo.ij0; ij < runInfo.ij1; ij++) {
        String line = (from == From.INPUT ? javaInput : this).getLine(ij);
        if (from == From.OUTPUT && line.length() > maxWidth
            && !(line.startsWith("package ") || line.startsWith("import "))) {
          if (addWarnings) {
            writer.append("// ").append(LINE_TOO_LONG_WARNING).append('\n');
            line = line.trim();
          } else {
            errors.add(String.format("output line %d: line too long", mergedLineNumber));
          }
        }
        writer.append(line).append('\n');
        mergedLineNumber++;
      }
    }
    if (addWarnings) {
      for (String error : errors) {
        writer.append("// ERROR: ").append(error).append('\n');
      }
    }
  }

  /**
   * Pick how to merge the (un-reformatted) input lines and the (reformatted) output lines. The
   * merge will contain all of the toks from the input and output, combining whole lines from each,
   * the specified lines from the output, and as few extra output lines as possible. The
   * {@link RunInfo} elements of the result will alternate {@link From#INPUT} and
   * {@link From#OUTPUT}.
   * @param iRangeSet0 the empty or canonical {@link Range} of input lines to reformat
   * @return the list of {@link RunInfo} defining the output
   */
  public ImmutableList<RunInfo> pickRuns(RangeSet<Integer> iRangeSet0) {
    ImmutableList.Builder<RunInfo> runInfos = ImmutableList.builder();
    int iN = javaInput.getLineCount(); // Number of input lines.
    int jN = getLineCount(); // Number of output lines.
    if (iN > 0 && getLineCount() > 0) {
      RangeSet<Integer> iRangeSet = iRangeSet0.subRangeSet(Range.closedOpen(0, iN));
      RangeSet<Integer> kRangeSet = TreeRangeSet.create();
      Map<Integer, Range<Integer>> kToI = makeKToIJ(javaInput, javaInput.getkN());
      Map<Integer, Range<Integer>> kToJ = makeKToIJ(this, kN);
      for (Range<Integer> iRange : iRangeSet.asRanges()) {
        kRangeSet.add(
            expandIRangeToKRange(iRange.canonical(DiscreteDomain.integers()), kToI, kToJ, iN, jN));
      }
      int k = 0; // We've output all toks up to but not including {@code k}.
      for (Range<Integer> kRange0 : kRangeSet.asRanges()) {
        Range<Integer> kRange = kRange0.canonical(DiscreteDomain.integers());
        int kLo = kRange.lowerEndpoint();
        int kHi = kRange.upperEndpoint() - 1;
        if (k <= kLo - 1) {
          runInfos.addAll(pickRuns(javaInput, kToI, k, kLo - 1, iN, From.INPUT));
          k = kLo;
        }
        if (k <= kHi) {
          runInfos.addAll(pickRuns(this, kToJ, k, kHi, jN, From.OUTPUT));
          k = Math.max(k, kHi + 1);
        }
      }
      if (k <= kN) {
        runInfos.addAll(pickRuns(javaInput, kToI, k, kN, iN, From.INPUT));
      }
    }
    return runInfos.build();
  }

  //TODO(jdd): Fix.
  /**
   * Given a {@link Range} of input lines, minimally expand the range so that the first line begins
   * with a numbered tok that also begins a line in the output, and so that the last line ends with
   * a numbered tok that also ends a line in the output, returning the range of
   * {@link JavaInput.Tok}s.
   * @param iRange the {@link Range} of input lines
   * @param kToI the map from numbered toks to input line ranges
   * @param kToJ the map from numbered toks to output line ranges
   * @param iN the number of input lines
   * @param jN the number of output lines
   * @return the minimally expanded range of toks
   */
  private Range<Integer> expandIRangeToKRange(
      Range<Integer> iRange, Map<Integer, Range<Integer>> kToI, Map<Integer, Range<Integer>> kToJ,
      int iN, int jN) {
    int iLo = iRange.lowerEndpoint();
    int iHi = iRange.upperEndpoint() - 1;
    // Expand input line range until it begins and ends at a tok, or is at the beginning or end.
    for (; 0 < iLo && javaInput.getRange0s(iLo).isEmpty(); --iLo) {}
    for (; iHi < iN - 1 && javaInput.getRange1s(iHi).isEmpty(); ++iHi) {}
    // Compute input tok range.
    int kILo = ijBackToK0(javaInput, iLo);
    int kIHi = ijForwardToK1(javaInput, iHi, iN);
    int jLo = kToJ.get(kILo).lowerEndpoint();
    int jHi = kToJ.get(kIHi).canonical(DiscreteDomain.integers()).upperEndpoint() - 1;
    // TODO(jdd): Is this unneeded?
    // Expand output line range until it begins and ends at a tok, or is at the beginning or end.
    for (; 0 < jLo && javaInput.getRange0s(jLo).isEmpty(); --jLo) {}
    for (; jHi < jN - 1 && javaInput.getRange1s(iHi).isEmpty(); ++jHi) {}
    int kJLo = ijBackToK0(this, jLo);
    int kJHi = ijForwardToK1(this, jHi, iN);
    // Iterate over both index ranges.
    while (kILo != kJLo) {
      if (kILo < kJLo) {
        kJLo = kBackToK0(this, kJLo - 1, kToJ);
      } else {
        kILo = kBackToK0(javaInput, kILo - 1, kToI);
      }
    }
    while (kIHi != kJHi) {
      if (kIHi < kJHi) {
        kIHi = kForwardToK1(javaInput, kIHi + 1, kToI, iN);
      } else {
        kJHi = kForwardToK1(this, kJHi + 1, kToJ, jN);
      }
    }
    return Range.closedOpen(kJLo, kJHi + 1);
  }

  /**
   * Given a line number, move backward, if needed, until we reach a tok that begins a line, and
   * return the tok index. If this fails, return {@code 0}.
   * @param put the {@link InputOutput}
   * @param ij0 the line number
   * @return the resulting tok index
   */
  private static int ijBackToK0(InputOutput put, int ij0) {
    for (int ij = ij0;; --ij) {
      if (ij <= 0) {
        return 0;
      }
      Range<Integer> kRange0 = put.getRange0s(ij);
      if (!kRange0.isEmpty()) {
        Range<Integer> kRange = put.getRanges(ij);
        if (!kRange.isEmpty()
            && kRange.lowerEndpoint().intValue() == kRange0.lowerEndpoint().intValue()) {
          return kRange0.lowerEndpoint();
        }
      }
    }
  }

  /**
   * Given a tok index, move backward, if needed, until we reach a tok that begins a line, and
   * return the tok index. If this fails, return {@code 0}.
   * @param put the {@link InputOutput}
   * @param k the tok index
   * @param kToI the map from token indices to ranges of {@link InputOutput} lines
   * @return the resulting tok index
   */
  private static int kBackToK0(InputOutput put, int k, Map<Integer, Range<Integer>> kToI) {
    return 0 < k ? ijBackToK0(put, kToI.get(k).lowerEndpoint()) : 0;
  }

  /**
   * Given a line number, move forward, if needed, until we reach a tok that ends a line in the
   * {@link InputOutput}, or until we reach the last tok.
   * @param put the {@link InputOutput}
   * @param ij0 the line number
   * @param ijN the number of lines
   * @return the resulting tok index
   */
  private int ijForwardToK1(InputOutput put, int ij0, int ijN) {
    for (int ij = ij0;; ++ij) {
      if (ij >= ijN - 1) {
        return kN - 1;
      }
      Range<Integer> kRange1 = put.getRange1s(ij);
      if (!kRange1.isEmpty()) {
        Range<Integer> kRange = put.getRanges(ij);
        if (!kRange.isEmpty()
            && kRange.upperEndpoint().intValue() == kRange1.upperEndpoint().intValue()) {
          return Math.min(kRange1.upperEndpoint() - 1, kN - 1);
        }
      }
    }
  }

  /**
   * Given a tok index, move forward, if needed, until we reach a tok that ends a line in the
   * {@link InputOutput}, or until we reach the last tok.
   * @param put the {@link InputOutput}
   * @param k the tok index
   * @param kToI the map from token indices to ranges of {@link InputOutput} lines
   * @return the resulting tok index
   */
  private int kForwardToK1(InputOutput put, int k, Map<Integer, Range<Integer>> kToI, int ijN) {
    return k < kN ? ijForwardToK1(put, kToI.get(k).upperEndpoint() - 1, ijN) : kN - 1;
  }

  /**
   * Pick which lines to write from the input or output.
   * @param put the {@link InputOutput}
   * @param kTiIJ the map from tok indices to line ranges in the {@link InputOutput}
   * @param kLo the first tok index, which begins a line in the input and in the output
   * @param kHi0 the last tok index, which ends a line in the input and in the output
   * @param ijN the number of lines in the {@link InputOutput}
   * @param from whether we are picking input lines or output lines
   */
  private List<RunInfo> pickRuns(
      InputOutput put, Map<Integer, Range<Integer>> kTiIJ, int kLo, int kHi0, int ijN, From from) {
    List<RunInfo> result = new ArrayList<>();
    if (kLo <= kHi0) {
      int kHi = Math.min(kHi0, kN - 1);
      if (kLo <= kHi) {
        int ijLo = kTiIJ.get(kLo).lowerEndpoint();
        int ijHi = kTiIJ.get(kHi).upperEndpoint() - 1;
        if (from == From.OUTPUT) {
          // Expand output range to include blank lines.
          while (0 < ijLo && put.getRanges(ijLo - 1).isEmpty()) {
            --ijLo;
          }
          while (ijHi < ijN - 1 && put.getRanges(ijHi + 1).isEmpty()) {
            ++ijHi;
          }
        }
        if (ijHi > ijN) {
          ijHi = ijN;
        }
        if (ijLo <= ijHi) {
          result.add(new RunInfo(from, ijLo, ijHi + 1));
        }
      }
    }
    return result;
  }

  private boolean isComment(String text) {
    return text.startsWith("//") || text.startsWith("/*");
  }

  private static Range<Integer> union(Range<Integer> x, Range<Integer> y) {
    return x.isEmpty() ? y : y.isEmpty() ? x : x.span(y).canonical(DiscreteDomain.integers());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("iLine", iLine)
        .add("lastK", lastK)
        .add("spacesPending", spacesPending)
        .add("newlinesPending", newlinesPending)
        .add("breaksTaken", breaksTaken)
        .add("blankLines", blankLines)
        .add("super", super.toString())
        .toString();
  }
}
