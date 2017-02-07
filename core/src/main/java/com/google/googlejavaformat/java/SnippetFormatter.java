/*
 * Copyright 2017 Google Inc.
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

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import java.util.ArrayList;
import java.util.List;

/** Formats a subset of a compilation unit. */
public class SnippetFormatter {

  /** The kind of snippet to format. */
  public enum SnippetKind {
    COMPILATION_UNIT,
    CLASS_BODY_DECLARATIONS,
    STATEMENTS,
    EXPRESSION
  }

  private class SnippetWrapper {
    int offset;
    final StringBuilder contents = new StringBuilder();

    public SnippetWrapper append(String str) {
      contents.append(str);
      return this;
    }

    public SnippetWrapper appendSource(String source) {
      this.offset = contents.length();
      contents.append(source);
      return this;
    }

    public void closeBraces(int initialIndent) {
      for (int i = initialIndent; --i >= 0; ) {
        contents.append("\n").append(createIndentationString(i)).append("}");
      }
    }
  }

  private static final int INDENTATION_SIZE = 2;
  private final Formatter formatter = new Formatter();
  private static final CharMatcher NOT_WHITESPACE = CharMatcher.whitespace().negate();

  public String createIndentationString(int indentationLevel) {
    Preconditions.checkArgument(
        indentationLevel >= 0,
        "Indentation level cannot be less than zero. Given: %s",
        indentationLevel);
    int spaces = indentationLevel * INDENTATION_SIZE;
    StringBuilder buf = new StringBuilder(spaces);
    for (int i = 0; i < spaces; i++) {
      buf.append(' ');
    }
    return buf.toString();
  }

  private static Range<Integer> offsetRange(Range<Integer> range, int offset) {
    range = range.canonical(DiscreteDomain.integers());
    return Range.closedOpen(range.lowerEndpoint() + offset, range.upperEndpoint() + offset);
  }

  private static List<Range<Integer>> offsetRanges(List<Range<Integer>> ranges, int offset) {
    List<Range<Integer>> result = new ArrayList<>();
    for (Range<Integer> range : ranges) {
      result.add(offsetRange(range, offset));
    }
    return result;
  }

  /** Runs the Google Java formatter on the given source, with only the given ranges specified. */
  public List<Replacement> format(
      SnippetKind kind,
      String source,
      List<Range<Integer>> ranges,
      int initialIndent,
      boolean includeComments)
      throws FormatterException {
    RangeSet<Integer> rangeSet = TreeRangeSet.create();
    for (Range<Integer> range : ranges) {
      rangeSet.add(range);
    }
    if (includeComments) {
      if (kind != SnippetKind.COMPILATION_UNIT) {
        throw new IllegalArgumentException(
            "comment formatting is only supported for compilation units");
      }
      return formatter.getFormatReplacements(source, ranges);
    }
    SnippetWrapper wrapper = snippetWrapper(kind, source, initialIndent);
    ranges = offsetRanges(ranges, wrapper.offset);

    String replacement = formatter.formatSource(wrapper.contents.toString(), ranges);
    replacement =
        replacement.substring(
            wrapper.offset,
            replacement.length() - (wrapper.contents.length() - wrapper.offset - source.length()));

    List<Replacement> replacements = toReplacements(source, replacement);
    List<Replacement> filtered = new ArrayList<>();
    for (Replacement r : replacements) {
      if (rangeSet.encloses(r.getReplaceRange())) {
        filtered.add(r);
      }
    }
    return filtered;
  }

  /**
   * Generates {@code Replacement}s rewriting {@code source} to {@code replacement}, under the
   * assumption that they differ in whitespace alone.
   */
  private static List<Replacement> toReplacements(String source, String replacement) {
    if (!NOT_WHITESPACE.retainFrom(source).equals(NOT_WHITESPACE.retainFrom(replacement))) {
      throw new IllegalArgumentException(
          "source = \"" + source + "\", replacement = \"" + replacement + "\"");
    }
    /*
     * In the past we seemed to have problems touching non-whitespace text in the formatter, even
     * just replacing some code with itself.  Retrospective attempts to reproduce this have failed,
     * but this may be an issue for future changes.
     */
    List<Replacement> replacements = new ArrayList<>();
    int i = NOT_WHITESPACE.indexIn(source);
    int j = NOT_WHITESPACE.indexIn(replacement);
    if (i != 0 || j != 0) {
      replacements.add(Replacement.create(Range.closedOpen(0, i), replacement.substring(0, j)));
    }
    while (i != -1 && j != -1) {
      int i2 = NOT_WHITESPACE.indexIn(source, i + 1);
      int j2 = NOT_WHITESPACE.indexIn(replacement, j + 1);
      if (i2 == -1 || j2 == -1) {
        break;
      }
      if ((i2 - i) != (j2 - j)
          || !source.substring(i + 1, i2).equals(replacement.substring(j + 1, j2))) {
        replacements.add(
            Replacement.create(Range.closedOpen(i + 1, i2), replacement.substring(j + 1, j2)));
      }
      i = i2;
      j = j2;
    }
    return replacements;
  }

  private SnippetWrapper snippetWrapper(SnippetKind kind, String source, int initialIndent) {
    /*
     * Synthesize a dummy class around the code snippet provided by Eclipse.  The dummy class is
     * correctly formatted -- the blocks use correct indentation, etc.
     */
    switch (kind) {
      case COMPILATION_UNIT:
        {
          SnippetWrapper wrapper = new SnippetWrapper();
          for (int i = 1; i <= initialIndent; i++) {
            wrapper.append("class Dummy {\n").append(createIndentationString(i));
          }
          wrapper.appendSource(source);
          wrapper.closeBraces(initialIndent);
          return wrapper;
        }
      case CLASS_BODY_DECLARATIONS:
        {
          SnippetWrapper wrapper = new SnippetWrapper();
          for (int i = 1; i <= initialIndent; i++) {
            wrapper.append("class Dummy {\n").append(createIndentationString(i));
          }
          wrapper.appendSource(source);
          wrapper.closeBraces(initialIndent);
          return wrapper;
        }
      case STATEMENTS:
        {
          SnippetWrapper wrapper = new SnippetWrapper();
          wrapper.append("class Dummy {\n").append(createIndentationString(1));
          for (int i = 2; i <= initialIndent; i++) {
            wrapper.append("{\n").append(createIndentationString(i));
          }
          wrapper.appendSource(source);
          wrapper.closeBraces(initialIndent);
          return wrapper;
        }
      case EXPRESSION:
        {
          SnippetWrapper wrapper = new SnippetWrapper();
          wrapper.append("class Dummy {\n").append(createIndentationString(1));
          for (int i = 2; i <= initialIndent; i++) {
            wrapper.append("{\n").append(createIndentationString(i));
          }
          wrapper.append("Object o = ");
          wrapper.appendSource(source);
          wrapper.append(";");
          wrapper.closeBraces(initialIndent);
          return wrapper;
        }
      default:
        throw new IllegalArgumentException("Unknown snippet kind: " + kind);
    }
  }
}
