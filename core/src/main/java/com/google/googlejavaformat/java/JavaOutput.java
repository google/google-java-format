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

import static java.util.Comparator.comparing;

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
          && (javaInput.getRanges(iLine).isEmpty()
              || javaInput.getRanges(iLine).upperEndpoint() <= range.lowerEndpoint())) {
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
      BlankLineWanted wanted = blankLines.getOrDefault(lastK, BlankLineWanted.NO);
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
              // drop leading blank lines
              if (!mutableLines.isEmpty() || lineBuilder.length() > 0) {
                mutableLines.add(lineBuilder.toString());
              }
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
    if (!CharMatcher.whitespace().matchesAllOf(lastLine)) {
      mutableLines.add(lastLine);
    }
    int jN = mutableLines.size();
    Range<Integer> eofRange = Range.closedOpen(kN, kN + 1);
    while (ranges.size() < jN) {
      ranges.add(Formatter.EMPTY_RANGE);
    }
    ranges.add(eofRange);
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
    Map<Integer, Range<Integer>> kToJ = JavaOutput.makeKToIJ(this);

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

      int replaceFrom = startTok.getPosition();
      // Replace leading whitespace in the input with the whitespace from the formatted file
      while (replaceFrom > 0) {
        char previous = javaInput.getText().charAt(replaceFrom - 1);
        if (!CharMatcher.whitespace().matches(previous)) {
          break;
        }
        replaceFrom--;
      }

      int i = kToJ.get(startTok.getIndex()).lowerEndpoint();
      // Include leading blank lines from the formatted output, unless the formatted range
      // starts at the beginning of the file.
      while (i > 0 && getLine(i - 1).isEmpty()) {
        i--;
      }
      // Write out the formatted range.
      for (; i < kToJ.get(endTok.getIndex()).upperEndpoint(); i++) {
        // It's possible to run out of output lines (e.g. if the input ended with
        // multiple trailing newlines).
        if (i < getLineCount()) {
          if (i > 0) {
            replacement.append(lineSeparator);
          }
          replacement.append(getLine(i));
        }
      }

      int replaceTo =
          Math.min(endTok.getPosition() + endTok.length(), javaInput.getText().length());
      // If the formatted ranged ended in the trailing trivia of the last token before EOF,
      // format all the way up to EOF to deal with trailing whitespace correctly.
      if (endTok.getIndex() == javaInput.getkN() - 1) {
        replaceTo = javaInput.getText().length();
      }
      // Replace trailing whitespace in the input with the whitespace from the formatted file.
      // If the trailing whitespace in the input includes one or more line breaks, preserve the
      // whitespace after the last newline to avoid re-indenting the line following the formatted
      // line.
      int newline = -1;
      while (replaceTo < javaInput.getText().length()) {
        char next = javaInput.getText().charAt(replaceTo);
        if (!CharMatcher.whitespace().matches(next)) {
          break;
        }
        int newlineLength = Newlines.hasNewlineAt(javaInput.getText(), replaceTo);
        if (newlineLength != -1) {
          newline = replaceTo;
          // Skip over the entire newline; don't count the second character of \r\n as a newline.
          replaceTo += newlineLength;
        } else {
          replaceTo++;
        }
      }
      if (newline != -1) {
        replaceTo = newline;
      }

      if (newline == -1) {
        // There wasn't an existing trailing newline; add one.
        replacement.append(lineSeparator);
      }
      for (; i < getLineCount(); i++) {
        String after = getLine(i);
        int idx = CharMatcher.whitespace().negate().indexIn(after);
        if (idx == -1) {
          // Write out trailing empty lines from the formatted output.
          replacement.append(lineSeparator);
        } else {
          if (newline == -1) {
            // If there wasn't a trailing newline in the input, indent the next line.
            replacement.append(after.substring(0, idx));
          }
          break;
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
    replacements.sort(comparing((Replacement r) -> r.getReplaceRange().lowerEndpoint()).reversed());
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
