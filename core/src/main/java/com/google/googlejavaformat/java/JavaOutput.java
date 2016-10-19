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

import static com.google.common.base.MoreObjects.firstNonNull;

import com.google.common.base.CharMatcher;
import com.google.common.base.MoreObjects;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.google.googlejavaformat.CommentsHelper;
import com.google.googlejavaformat.Input;
import com.google.googlejavaformat.Input.Token;
import com.google.googlejavaformat.Newlines;
import com.google.googlejavaformat.OpsBuilder.BlankLineWanted;
import com.google.googlejavaformat.Output;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
  private final String lineSeparator;
  private final JavaInput javaInput; // Used to follow along while emitting the output.
  private final CommentsHelper commentsHelper; // Used to re-flow comments.
  private final Map<Integer, BlankLineWanted> blankLines = new HashMap<>(); // Info on blank lines.
  private final RangeSet<Integer> partialFormatRanges = TreeRangeSet.create();

  private final List<String> mutableLines = new ArrayList<>();
  private final int kN; // The number of tokens or comments in the input, excluding the EOF.
  private int iLine = 0; // Closest corresponding line number on input.
  private int lastK = -1; // Last {@link Tok} index output.
  private int spacesPending = 0;
  private int newlinesPending = 0;
  private StringBuilder lineBuilder = new StringBuilder();

  /**
   * {@code JavaOutput} constructor.
   *
   * @param javaInput the {@link JavaInput}, used to match up blank lines in the output
   * @param commentsHelper the {@link CommentsHelper}, used to rewrite comments
   */
  public JavaOutput(String lineSeparator, JavaInput javaInput, CommentsHelper commentsHelper) {
    this.lineSeparator = lineSeparator;
    this.javaInput = javaInput;
    this.commentsHelper = commentsHelper;
    kN = javaInput.getkN();
  }

  @Override
  public void blankLine(int k, BlankLineWanted wanted) {
    if (blankLines.containsKey(k)) {
      blankLines.put(k, blankLines.get(k).merge(wanted));
    } else {
      blankLines.put(k, wanted);
    }
  }

  @Override
  public void markForPartialFormat(Token start, Token end) {
    int lo = JavaOutput.startTok(start).getIndex();
    int hi = JavaOutput.endTok(end).getIndex();
    partialFormatRanges.add(Range.closed(lo, hi));
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
       * there's a blank line here and it's a comment.
       */
      BlankLineWanted wanted = firstNonNull(blankLines.get(lastK), BlankLineWanted.NO);
      if (isComment(text) ? sawNewlines : wanted.wanted().or(sawNewlines)) {
        ++newlinesPending;
      }
    }
    if (Newlines.isNewline(text)) {
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
          case '\r':
            if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
              i++;
            }
            // falls through
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
   * Emit a list of {@link Replacement}s to convert from input to output.
   *
   * @return a list of {@link Replacement}s, sorted by start index, without overlaps
   */
  public ImmutableList<Replacement> getFormatReplacements(RangeSet<Integer> iRangeSet0) {
    ImmutableList.Builder<Replacement> result = ImmutableList.builder();
    Map<Integer, Range<Integer>> kToJ = JavaOutput.makeKToIJ(this, kN);

    // Expand the token ranges to align with re-formattable boundaries.
    RangeSet<Integer> breakableRanges = TreeRangeSet.create();
    RangeSet<Integer> iRangeSet = iRangeSet0.subRangeSet(Range.closed(0, javaInput.getkN()));
    for (Range<Integer> iRange : iRangeSet.asRanges()) {
      Range<Integer> range = expandToBreakableRegions(iRange.canonical(DiscreteDomain.integers()));
      if (range.equals(EMPTY_RANGE)) {
        // the range contains only whitespace
        continue;
      }
      breakableRanges.add(range);
    }

    // Construct replacements for each reformatted region.
    for (Range<Integer> range : breakableRanges.asRanges()) {

      Input.Tok startTok = startTok(javaInput.getToken(range.lowerEndpoint()));
      Input.Tok endTok = endTok(javaInput.getToken(range.upperEndpoint() - 1));

      // Add all output lines in the given token range to the replacement.
      StringBuilder replacement = new StringBuilder();

      boolean needsBreakBefore = false;
      int replaceFrom = startTok.getPosition();
      while (replaceFrom > 0) {
        char previous = javaInput.getText().charAt(replaceFrom - 1);
        if (previous == '\n' || previous == '\r') {
          break;
        }
        if (CharMatcher.whitespace().matches(previous)) {
          replaceFrom--;
          continue;
        }
        needsBreakBefore = true;
        break;
      }

      if (needsBreakBefore) {
        replacement.append(lineSeparator);
      }

      boolean first = true;
      int i;
      for (i = kToJ.get(startTok.getIndex()).lowerEndpoint();
          i < kToJ.get(endTok.getIndex()).upperEndpoint();
          i++) {
        // It's possible to run out of output lines (e.g. if the input ended with
        // multiple trailing newlines).
        if (i < getLineCount()) {
          if (first) {
            first = false;
          } else {
            replacement.append(lineSeparator);
          }
          replacement.append(getLine(i));
        }
      }
      replacement.append(lineSeparator);

      String trailingLine = i < getLineCount() ? getLine(i) : null;

      int replaceTo =
          Math.min(endTok.getPosition() + endTok.length(), javaInput.getText().length());
      // If the formatted ranged ended in the trailing trivia of the last token before EOF,
      // format all the way up to EOF to deal with trailing whitespace correctly.
      if (endTok.getIndex() == javaInput.getkN() - 1) {
        replaceTo = javaInput.getText().length();
      }

      // Expand the partial formatting range to include non-breaking trailing
      // whitespace. If the range ultimately ends in a newline, then preserve
      // whatever original text was on the next line (i.e. don't re-indent
      // the next line after the reformatted range). However, if the partial
      // formatting range doesn't end in a newline, then break and re-indent.
      boolean reIndent = true;
      OUTER:
      while (replaceTo < javaInput.getText().length()) {
        char endChar = javaInput.getText().charAt(replaceTo);
        switch (endChar) {
          case '\r':
            if (replaceTo + 1 < javaInput.getText().length()
                && javaInput.getText().charAt(replaceTo + 1) == '\n') {
              replaceTo++;
            }
            // falls through
          case '\n':
            replaceTo++;
            reIndent = false;
            break OUTER;
          default:
            break;
        }
        if (CharMatcher.whitespace().matches(endChar)) {
          replaceTo++;
          continue;
        }
        break;
      }
      if (reIndent && trailingLine != null) {
        int idx = CharMatcher.whitespace().negate().indexIn(trailingLine);
        if (idx > 0) {
          replacement.append(trailingLine, 0, idx);
        }
      }

      result.add(Replacement.create(replaceFrom, replaceTo, replacement.toString()));
    }

    return result.build();
  }

  /**
   * Expand a token range to start and end on acceptable boundaries for re-formatting.
   *
   * @param iRange the {@link Range} of tokens
   * @return the expanded token range
   */
  private Range<Integer> expandToBreakableRegions(Range<Integer> iRange) {
    // The original line range.
    int loTok = iRange.lowerEndpoint();
    int hiTok = iRange.upperEndpoint() - 1;

    // Expand the token indices to formattable boundaries (e.g. edges of statements).
    if (!partialFormatRanges.contains(loTok) || !partialFormatRanges.contains(hiTok)) {
      return EMPTY_RANGE;
    }
    loTok = partialFormatRanges.rangeContaining(loTok).lowerEndpoint();
    hiTok = partialFormatRanges.rangeContaining(hiTok).upperEndpoint();
    return Range.closedOpen(loTok, hiTok + 1);
  }

  public static String applyReplacements(String input, List<Replacement> replacements) {
    replacements = new ArrayList<>(replacements);
    Collections.sort(
        replacements,
        new Comparator<Replacement>() {
          @Override
          public int compare(Replacement o1, Replacement o2) {
            return Integer.compare(
                o2.getReplaceRange().lowerEndpoint(), o1.getReplaceRange().lowerEndpoint());
          }
        });
    StringBuilder writer = new StringBuilder(input);
    for (Replacement replacement : replacements) {
      writer.replace(
          replacement.getReplaceRange().lowerEndpoint(),
          replacement.getReplaceRange().upperEndpoint(),
          replacement.getReplacementString());
    }
    return writer.toString();
  }

  /** The earliest position of any Tok in the Token, including leading whitespace. */
  public static int startPosition(Token token) {
    int min = token.getTok().getPosition();
    for (Input.Tok tok : token.getToksBefore()) {
      min = Math.min(min, tok.getPosition());
    }
    return min;
  }

  /** The earliest non-whitespace Tok in the Token. */
  public static Input.Tok startTok(Token token) {
    for (Input.Tok tok : token.getToksBefore()) {
      if (tok.getIndex() >= 0) {
        return tok;
      }
    }
    return token.getTok();
  }

  /** The last non-whitespace Tok in the Token. */
  public static Input.Tok endTok(Token token) {
    for (int i = token.getToksAfter().size() - 1; i >= 0; i--) {
      Input.Tok tok = token.getToksAfter().get(i);
      if (tok.getIndex() >= 0) {
        return tok;
      }
    }
    return token.getTok();
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
        .add("blankLines", blankLines)
        .add("super", super.toString())
        .toString();
  }
}
