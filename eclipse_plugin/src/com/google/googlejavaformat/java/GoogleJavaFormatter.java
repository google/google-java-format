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

import com.google.common.base.Preconditions;
import com.google.common.collect.Range;
import com.google.googlejavaformat.java.SnippetFormatter.SnippetKind;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

/** Runs the Google Java formatter on the given code. */
public class GoogleJavaFormatter extends CodeFormatter {

  private static final int INDENTATION_SIZE = 2;

  @Override
  public TextEdit format(
      int kind, String source, int offset, int length, int indentationLevel, String lineSeparator) {
    IRegion[] regions = new IRegion[] {new Region(offset, length)};
    return formatInternal(kind, source, regions, indentationLevel);
  }

  @Override
  public TextEdit format(
      int kind, String source, IRegion[] regions, int indentationLevel, String lineSeparator) {
    return formatInternal(kind, source, regions, indentationLevel);
  }

  @Override
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

  /** Runs the Google Java formatter on the given source, with only the given ranges specified. */
  private TextEdit formatInternal(int kind, String source, IRegion[] regions, int initialIndent) {
    try {
      boolean includeComments =
          (kind & CodeFormatter.F_INCLUDE_COMMENTS) == CodeFormatter.F_INCLUDE_COMMENTS;
      kind &= ~CodeFormatter.F_INCLUDE_COMMENTS;
      SnippetKind snippetKind;
      switch (kind) {
        case ASTParser.K_EXPRESSION:
          snippetKind = SnippetKind.EXPRESSION;
          break;
        case ASTParser.K_STATEMENTS:
          snippetKind = SnippetKind.STATEMENTS;
          break;
        case ASTParser.K_CLASS_BODY_DECLARATIONS:
          snippetKind = SnippetKind.CLASS_BODY_DECLARATIONS;
          break;
        case ASTParser.K_COMPILATION_UNIT:
          snippetKind = SnippetKind.COMPILATION_UNIT;
          break;
        default:
          throw new IllegalArgumentException(String.format("Unknown snippet kind: %d", kind));
      }
      List<Replacement> replacements =
          new SnippetFormatter()
              .format(
                  snippetKind, source, rangesFromRegions(regions), initialIndent, includeComments);
      if (idempotent(source, regions, replacements)) {
        // Do not create edits if there's no diff.
        return null;
      }
      // Convert replacements to text edits.
      return editFromReplacements(replacements);
    } catch (IllegalArgumentException | FormatterException exception) {
      // Do not format on errors.
      return null;
    }
  }

  private List<Range<Integer>> rangesFromRegions(IRegion[] regions) {
    List<Range<Integer>> ranges = new ArrayList<>();
    for (IRegion region : regions) {
      ranges.add(Range.closedOpen(region.getOffset(), region.getOffset() + region.getLength()));
    }
    return ranges;
  }

  /** @return {@code true} if input and output texts are equal, else {@code false}. */
  private boolean idempotent(String source, IRegion[] regions, List<Replacement> replacements) {
    // This implementation only checks for single replacement.
    if (replacements.size() == 1) {
      Replacement replacement = replacements.get(0);
      String output = replacement.getReplacementString();
      // Entire source case: input = output, nothing changed.
      if (output.equals(source)) {
        return true;
      }
      // Single region and single replacement case: if they are equal, nothing changed.
      if (regions.length == 1) {
        Range<Integer> range = replacement.getReplaceRange();
        String snippet = source.substring(range.lowerEndpoint(), range.upperEndpoint());
        if (output.equals(snippet)) {
          return true;
        }
      }
    }
    return false;
  }

  private TextEdit editFromReplacements(List<Replacement> replacements) {
    // Split the replacements that cross line boundaries.
    TextEdit edit = new MultiTextEdit();
    for (Replacement replacement : replacements) {
      Range<Integer> replaceRange = replacement.getReplaceRange();
      edit.addChild(
          new ReplaceEdit(
              replaceRange.lowerEndpoint(),
              replaceRange.upperEndpoint() - replaceRange.lowerEndpoint(),
              replacement.getReplacementString()));
    }
    return edit;
  }
}
