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
package com.google.googlejavaformat.eclipse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jface.text.IRegion;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.statushandlers.StatusManager;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import com.google.googlejavaformat.java.Replacement;

/**
 * Google Java Style Code Formatter.
 * <p>
 * Uses <a href= "https://github.com/google/google-java-format">google-java-format</a>, a program
 * that reformats Java source code to comply with
 * <a href="https://google.github.io/styleguide/javaguide.html">Google Java Style</a>, to format
 * Java source code in Eclipse.
 *
 * @author Christian Stein
 * @see <a href= "https://github.com/google/google-java-format">google-java-format</a>
 */
public class GoogleJavaFormatter extends CodeFormatter {

  /**
   * Plug-in ID.
   */
  private static final String PLUGIN_ID = GoogleJavaFormatter.class.getCanonicalName();

  /**
   * The google-java-format formatter instance.
   */
  private final Formatter formatter = new Formatter();

  /**
   * Block indentation: +2 spaces
   * <p>
   * Each time a new block or block-like construct is opened, the indent increases by two spaces.
   * When the block ends, the indent returns to the previous indent level. The indent level applies
   * to both code and comments throughout the block.
   *
   * @see <a href= "https://google.github.io/styleguide/javaguide.html#s4.2-block-indentation">Block
   *      indentation</a>
   */
  @Override
  public String createIndentationString(int indentationLevel) {
    return new String(new char[indentationLevel]).replace("\0", "  ");
  }

  @Override
  public TextEdit format(
      int kind, String source, IRegion[] regions, int indentationLevel, String lineSeparator) {
    List<Range<Integer>> ranges = new ArrayList<>();
    for (IRegion region : regions) {
      ranges.add(Range.open(region.getOffset(), region.getOffset() + region.getLength()));
    }
    return format(source, ranges);
  }

  @Override
  public TextEdit format(
      int kind, String source, int offset, int length, int indentationLevel, String lineSeparator) {
    List<Range<Integer>> ranges = Arrays.asList(Range.open(offset, offset + length));
    return format(source, ranges);
  }

  protected TextEdit format(String input, List<Range<Integer>> ranges) {
    try {
      ImmutableList<Replacement> replacements = formatter.getFormatReplacements(input, ranges);
      MultiTextEdit multi = new MultiTextEdit();
      for (Replacement replacement : replacements) {
        int replaceOffset = replacement.getReplaceRange().lowerEndpoint();
        int replaceLength = replacement.getReplaceRange().upperEndpoint();
        String replaceStr = replacement.getReplacementString();
        String oldString = input.substring(replaceOffset, replaceLength);
        if (replaceStr.equals(oldString)) {
          continue;
        }
        multi.addChild(new ReplaceEdit(replaceOffset, replaceLength - replaceOffset, replaceStr));
      }
      return multi.hasChildren() ? multi : null;
    } catch (FormatterException e) {
      String message = "Formatting failed: " + e.getLocalizedMessage();
      Status status = new Status(Status.ERROR, PLUGIN_ID, message, e);
      StatusManager.getManager().handle(status, StatusManager.LOG | StatusManager.SHOW);
      return null;
    }
  }
}
