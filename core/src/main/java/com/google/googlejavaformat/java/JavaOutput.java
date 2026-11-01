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

import static com.google.common.base.Verify.verify;
import static java.lang.Math.max;
import static java.lang.Math.min;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

/*
 * Throughout this file, {@code i} is an index for input lines, {@code j} is an index for output
 * lines, {@code ij} is an index into either input or output lines, and {@code k} is an index for
 * toks.
 */

/// `JavaOutput` extends [Output] to represent a Java output document. It includes methods to emit
/// the output document.
public final class JavaOutput extends Output {
  private final String lineSeparator;
  private final Input javaInput; // Used to follow along while emitting the output.
  private final CommentsHelper commentsHelper; // Used to re-flow comments.
  private final IntFunction<String> indentFunction;
  private final ToIntFunction<CharSequence> visualLength;
  private final Map<Integer, BlankLineWanted> blankLines = new HashMap<>(); // Info on blank lines.
  private final RangeSet<Integer> partialFormatRanges = TreeRangeSet.create();

  private final List<String> mutableLines = new ArrayList<>();
  private final int kN; // The number of tokens or comments in the input, excluding the EOF.
  private int iLine = 0; // Closest corresponding line number on input.
  private int lastK = -1; // Last {@link Tok} index output.
  private int newlinesPending = 0;
  private StringBuilder lineBuilder = new StringBuilder();
  private StringBuilder spacesPending = new StringBuilder();

  private static final int MAX_CACHED_SPACES = 100;
  private static final String[] spaces = new String[MAX_CACHED_SPACES + 1];

  /// Returns an indentation string of the given number of spaces.
  static String spaces(int indent) {
    if (indent <= 0) {
      return "";
    }
    if (indent <= MAX_CACHED_SPACES) {
      String result = spaces[indent];
      if (result == null) {
        result = spaces[indent] = " ".repeat(indent);
      }
      return result;
    }
    return " ".repeat(indent);
  }

  /// `JavaOutput` constructor.
  ///
  /// @param javaInput the [Input], used to match up blank lines in the output
  /// @param commentsHelper the [CommentsHelper], used to rewrite comments
  public JavaOutput(String lineSeparator, Input javaInput, CommentsHelper commentsHelper) {
    this(lineSeparator, javaInput, commentsHelper, JavaOutput::spaces, CharSequence::length);
  }

  /// `JavaOutput` constructor.
  ///
  /// @param javaInput the [Input], used to match up blank lines in the output
  /// @param commentsHelper the [CommentsHelper], used to rewrite comments
  /// @param indentFunction function mapping a visual column indent to an indentation string
  /// @param visualLength function returning the visual width of a string of indentation
  public JavaOutput(
      String lineSeparator,
      Input javaInput,
      CommentsHelper commentsHelper,
      IntFunction<String> indentFunction,
      ToIntFunction<CharSequence> visualLength) {
    this.lineSeparator = lineSeparator;
    this.javaInput = javaInput;
    this.commentsHelper = commentsHelper;
    this.indentFunction = indentFunction;
    this.visualLength = visualLength;
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

  // TODO(user): Add invariant.
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
      if ((sawNewlines && isComment(text)) || wanted.wanted().orElse(sawNewlines)) {
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
      spacesPending = new StringBuilder();
    } else {
      boolean rangesSet = false;
      int textN = text.length();
      for (int i = 0; i < textN; i++) {
        char c = text.charAt(i);
        switch (c) {
          case ' ':
            spacesPending.append(' ');
            break;
          case '\t':
            spacesPending.append('\t');
            break;
          case '\r':
            if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
              i++;
            }
          // falls through
          case '\n':
            spacesPending = new StringBuilder();
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
            if (spacesPending.length() > 0) {
              lineBuilder.append(spacesPending);
              spacesPending = new StringBuilder();
            }
            lineBuilder.append(c);
            if (!range.isEmpty()) {
              if (!rangesSet) {
                while (ranges.size() <= mutableLines.size()) {
                  ranges.add(EMPTY_RANGE);
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
    spacesPending.append(indentFunction.apply(indent));
  }

  /// Flush any incomplete last line, then add the EOF token into our data structures.
  public void flush() {
    String lastLine = lineBuilder.toString();
    if (!CharMatcher.whitespace().matchesAllOf(lastLine)) {
      mutableLines.add(lastLine);
    }
    int jN = mutableLines.size();
    Range<Integer> eofRange = Range.closedOpen(kN, kN + 1);
    while (ranges.size() < jN) {
      ranges.add(EMPTY_RANGE);
    }
    ranges.add(eofRange);
    setLines(ImmutableList.copyOf(mutableLines));
  }

  // The following methods can be used after the Output has been built.

  @Override
  public CommentsHelper getCommentsHelper() {
    return commentsHelper;
  }

  private static final CharMatcher NOT_WHITESPACE = CharMatcher.whitespace().negate();

  /// Emit a list of [Replacement]s to convert from input to output.
  ///
  /// @return a list of [Replacement]s, sorted by start index, without overlaps
  public ImmutableList<Replacement> getFormatReplacements(RangeSet<Integer> iRangeSet0) {
    return getFormatReplacements(iRangeSet0, /* reindent= */ false);
  }

  /// Emit a list of [Replacement]s to convert from input to output.
  ///
  /// @param reindent whether to also shift unformatted statements to their formatted indentation
  /// @return a list of [Replacement]s, sorted by start index, without overlaps
  ImmutableList<Replacement> getFormatReplacements(RangeSet<Integer> iRangeSet0, boolean reindent) {
    List<Replacement> result = new ArrayList<>();
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

      int replaceTo = min(endTok.getPosition() + endTok.length(), javaInput.getText().length());
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
        int idx = NOT_WHITESPACE.indexIn(after);
        if (idx == -1) {
          // Write out trailing empty lines from the formatted output.
          replacement.append(lineSeparator);
        } else {
          if (newline == -1) {
            // If there wasn't a trailing newline in the input, indent the next line.
            replacement.append(after, 0, idx);
          }
          break;
        }
      }

      result.add(Replacement.create(replaceFrom, replaceTo, replacement.toString()));
    }

    if (reindent && !breakableRanges.isEmpty()) {
      List<Replacement> reindentReplacements = new ArrayList<>();
      addReindentReplacements(breakableRanges, kToJ, reindentReplacements);
      result.addAll(reindentReplacements);
      result.sort(comparing(r -> r.replaceRange().lowerEndpoint()));
      verifyReindentReplacements(javaInput.getText(), reindentReplacements, result);
    }

    return ImmutableList.copyOf(result);
  }

  private static final CharMatcher HORIZONTAL_WHITESPACE = CharMatcher.anyOf(" \t");

  /// Checks that re-indenting only changes indentation: each re-indent replacement must replace
  /// horizontal whitespace with horizontal whitespace, and no replacements may overlap. Throws
  /// `VerifyException` otherwise, since applying the replacements could change the program.
  static void verifyReindentReplacements(
      String text, List<Replacement> reindentReplacements, List<Replacement> sortedReplacements) {
    for (Replacement r : reindentReplacements) {
      Range<Integer> range = r.replaceRange();
      verify(
          HORIZONTAL_WHITESPACE.matchesAllOf(
                  text.subSequence(range.lowerEndpoint(), range.upperEndpoint()))
              && HORIZONTAL_WHITESPACE.matchesAllOf(r.replacementString()),
          "re-indent replacement %s is not whitespace-only",
          r);
    }
    for (int i = 1; i < sortedReplacements.size(); i++) {
      Replacement prev = sortedReplacements.get(i - 1);
      Replacement next = sortedReplacements.get(i);
      verify(
          prev.replaceRange().upperEndpoint() <= next.replaceRange().lowerEndpoint(),
          "overlapping replacements %s and %s",
          prev,
          next);
    }
  }

  private record ReindentChunk(Input.Tok startTok, Input.Tok endTok) {}

  private void addReindentReplacements(
      RangeSet<Integer> breakableRanges,
      Map<Integer, Range<Integer>> kToJ,
      List<Replacement> result) {
    String text = javaInput.getText();
    for (List<ReindentChunk> group : reindentGroups(text, reindentChunks(breakableRanges))) {
      int wsEnd = group.getFirst().startTok().getPosition();
      int wsStart = lineIndentStart(text, wsEnd);
      if (wsStart < 0) {
        continue;
      }
      int outIndent = groupOutputIndent(text, group, kToJ);
      if (outIndent < 0) {
        continue;
      }
      int delta = outIndent - visualLength.applyAsInt(text.subSequence(wsStart, wsEnd));
      if (delta == 0) {
        continue;
      }
      reindentLines(text, wsStart, chunkEnd(text, group.getLast()), delta, result);
    }
  }

  private ImmutableList<ReindentChunk> reindentChunks(RangeSet<Integer> breakableRanges) {
    ImmutableList.Builder<ReindentChunk> chunks = ImmutableList.builder();
    for (Range<Integer> partialRange : partialFormatRanges.asRanges()) {
      if (breakableRanges.contains(partialRange.lowerEndpoint())) {
        continue;
      }
      Token startToken = javaInput.getToken(partialRange.lowerEndpoint());
      Token endToken = javaInput.getToken(partialRange.upperEndpoint());
      for (Input.Tok tok : startToken.getToksBefore()) {
        if (tok.isComment()) {
          chunks.add(new ReindentChunk(tok, tok));
        }
      }
      if (startToken.getTok().getIndex() < javaInput.getkN()) {
        chunks.add(new ReindentChunk(startToken.getTok(), endTok(endToken)));
      }
    }
    return chunks.build();
  }

  /// Groups chunks that must be shifted together: each chunk after the first in a group starts on
  /// the last line of the chunk before it.
  private static ImmutableList<ImmutableList<ReindentChunk>> reindentGroups(
      String text, List<ReindentChunk> chunks) {
    ImmutableList.Builder<ImmutableList<ReindentChunk>> groups = ImmutableList.builder();
    List<ReindentChunk> group = new ArrayList<>();
    for (ReindentChunk chunk : chunks) {
      if (!group.isEmpty()
          && Newlines.containsBreaks(
              text, chunkEnd(text, group.getLast()), chunk.startTok().getPosition())) {
        groups.add(ImmutableList.copyOf(group));
        group.clear();
      }
      group.add(chunk);
    }
    if (!group.isEmpty()) {
      groups.add(ImmutableList.copyOf(group));
    }
    return groups.build();
  }

  /// Returns the formatted indentation of a group's first line: the smallest output indentation of
  /// the chunks that start on that line, or `-1` if the first chunk has none.
  private int groupOutputIndent(
      String text, List<ReindentChunk> group, Map<Integer, Range<Integer>> kToJ) {
    int firstLineStart = group.getFirst().startTok().getPosition();
    int outIndent = outputIndent(group.getFirst().startTok(), kToJ);
    if (outIndent < 0) {
      return -1;
    }
    for (ReindentChunk chunk : group.subList(1, group.size())) {
      if (Newlines.containsBreaks(text, firstLineStart, chunk.startTok().getPosition())) {
        break;
      }
      int chunkOutIndent = outputIndent(chunk.startTok(), kToJ);
      if (chunkOutIndent >= 0) {
        outIndent = min(outIndent, chunkOutIndent);
      }
    }
    return outIndent;
  }

  private static int chunkEnd(String text, ReindentChunk chunk) {
    Input.Tok endTok = chunk.endTok();
    return min(endTok.getPosition() + endTok.length(), text.length());
  }

  private void reindentLines(String text, int start, int end, int delta, List<Replacement> result) {
    Iterator<Integer> lineOffsets = Newlines.lineOffsetIterator(text.substring(start, end));
    while (lineOffsets.hasNext()) {
      int pos = start + lineOffsets.next();
      if (pos >= end) {
        break;
      }
      if (pos > start && isInsideMultiLineToken(pos)) {
        continue;
      }
      int lineWsEnd = skipHorizontalWhitespace(text, pos, end);
      if (Newlines.hasNewlineAt(text, lineWsEnd) != -1) {
        continue;
      }
      int lineInIndent = visualLength.applyAsInt(text.subSequence(pos, lineWsEnd));
      int lineOutIndent = max(0, lineInIndent + delta);
      emitIndentReplacement(text, pos, lineWsEnd, lineOutIndent, result);
    }
  }

  private boolean isInsideMultiLineToken(int pos) {
    Token tokAtNewline = javaInput.getPositionTokenMap().get(pos - 1);
    if (tokAtNewline == null) {
      return false;
    }
    Input.Tok realTok = tokAtNewline.getTok();
    return pos - 1 >= realTok.getPosition() && pos < realTok.getPosition() + realTok.length();
  }

  private static int lineIndentStart(String text, int pos) {
    int wsStart = pos;
    while (wsStart > 0 && isHorizontalWhitespace(text.charAt(wsStart - 1))) {
      wsStart--;
    }
    return Newlines.isLineStart(text, wsStart) ? wsStart : -1;
  }

  private static int skipHorizontalWhitespace(String text, int pos, int end) {
    while (pos < end && isHorizontalWhitespace(text.charAt(pos))) {
      pos++;
    }
    return pos;
  }

  private void emitIndentReplacement(
      String text, int wsStart, int wsEnd, int targetIndent, List<Replacement> result) {
    String newWs = indentFunction.apply(targetIndent);
    if (wsEnd - wsStart != newWs.length()
        || !text.regionMatches(wsStart, newWs, 0, newWs.length())) {
      result.add(Replacement.create(wsStart, wsEnd, newWs));
    }
  }

  private int outputIndent(Input.Tok tok, Map<Integer, Range<Integer>> kToJ) {
    int k = tok.getIndex();
    Range<Integer> jRange = kToJ.get(k);
    if (jRange == null) {
      return -1;
    }
    int jLine = jRange.lowerEndpoint();
    if (jLine >= getLineCount() || getRanges(jLine).lowerEndpoint() != k) {
      return -1;
    }
    String outLine = getLine(jLine);
    int nonWs = NOT_WHITESPACE.indexIn(outLine);
    if (nonWs == -1) {
      return -1;
    }
    return visualLength.applyAsInt(outLine.subSequence(0, nonWs));
  }

  private static boolean isHorizontalWhitespace(char c) {
    return c == ' ' || c == '\t';
  }

  /// Expand a token range to start and end on acceptable boundaries for re-formatting.
  ///
  /// @param iRange the [Range] of tokens
  /// @return the expanded token range
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
    replacements.sort(comparing((Replacement r) -> r.replaceRange().lowerEndpoint()).reversed());
    StringBuilder writer = new StringBuilder(input);
    for (Replacement replacement : replacements) {
      writer.replace(
          replacement.replaceRange().lowerEndpoint(),
          replacement.replaceRange().upperEndpoint(),
          replacement.replacementString());
    }
    return writer.toString();
  }

  /// The earliest non-whitespace Tok in the Token.
  public static Input.Tok startTok(Token token) {
    for (Input.Tok tok : token.getToksBefore()) {
      if (tok.getIndex() >= 0) {
        return tok;
      }
    }
    return token.getTok();
  }

  /// The last non-whitespace Tok in the Token.
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
        .add("spacesPending", spacesPending.toString().replace("\t", "\\t"))
        .add("newlinesPending", newlinesPending)
        .add("blankLines", blankLines)
        .add("super", super.toString())
        .toString();
  }
}
