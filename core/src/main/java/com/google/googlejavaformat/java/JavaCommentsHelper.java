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

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.google.googlejavaformat.CommentsHelper;
import com.google.googlejavaformat.Input.Tok;
import com.google.googlejavaformat.Newlines;
import com.google.googlejavaformat.java.javadoc.JavadocFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** {@code JavaCommentsHelper} extends {@link CommentsHelper} to rewrite Java comments. */
public final class JavaCommentsHelper implements CommentsHelper {

  private final JavaFormatterOptions options;
  private String lineSeparator = "\n"; // non-final, determined by rewrite

  public JavaCommentsHelper(JavaFormatterOptions options) {
    this.options = options;
  }

  @Override
  public String rewrite(Tok tok, int maxWidth, int column0) {
    if (!tok.isComment()) {
      return tok.getOriginalText();
    }
    String text = tok.getOriginalText();
    this.lineSeparator = options.outputSeparator().chars(text);
    if (tok.isJavadocComment()) {
      text = JavadocFormatter.formatJavadoc(text, column0, options);
    }
    List<String> lines = new ArrayList<>();
    Iterator<String> it = Newlines.lineIterator(text);
    while (it.hasNext()) {
      lines.add(CharMatcher.whitespace().trimTrailingFrom(it.next()));
    }
    if (tok.isSlashSlashComment()) {
      return indentLineComments(lines, column0);
    } else if (javadocShaped(lines)) {
      return indentJavadoc(lines, column0);
    } else {
      return preserveIndentation(lines, column0);
    }
  }

  // For non-javadoc-shaped block comments, shift the entire block to the correct
  // column, but do not adjust relative indentation.
  private String preserveIndentation(List<String> lines, int column0) {
    StringBuilder builder = new StringBuilder();

    // find the leftmost non-whitespace character in all trailing lines
    int startCol = -1;
    for (int i = 1; i < lines.size(); i++) {
      int lineIdx = CharMatcher.whitespace().negate().indexIn(lines.get(i));
      if (lineIdx >= 0 && (startCol == -1 || lineIdx < startCol)) {
        startCol = lineIdx;
      }
    }

    // output the first line at the current column
    builder.append(lines.get(0));

    // output all trailing lines with plausible indentation
    for (int i = 1; i < lines.size(); ++i) {
      builder.append(lineSeparator).append(Strings.repeat(" ", column0));
      // check that startCol is valid index, e.g. for blank lines
      if (lines.get(i).length() >= startCol) {
        builder.append(lines.get(i).substring(startCol));
      } else {
        builder.append(lines.get(i));
      }
    }
    return builder.toString();
  }

  // Remove leading and trailing whitespace, and re-indent each line.
  private String indentLineComments(List<String> lines, int column0) {
    StringBuilder builder = new StringBuilder();
    builder.append(lines.get(0).trim());
    String indentString = Strings.repeat(" ", column0);
    for (int i = 1; i < lines.size(); ++i) {
      builder.append(lineSeparator).append(indentString).append(lines.get(i).trim());
    }
    return builder.toString();
  }

  // Remove leading and trailing whitespace, and re-indent each line.
  // Add a +1 indent before '*', and add the '*' if necessary.
  private String indentJavadoc(List<String> lines, int column0) {
    StringBuilder builder = new StringBuilder();
    builder.append(lines.get(0).trim());
    int indent = column0 + 1;
    String indentString = Strings.repeat(" ", indent);
    for (int i = 1; i < lines.size(); ++i) {
      builder.append(lineSeparator).append(indentString);
      String line = lines.get(i).trim();
      if (!line.startsWith("*")) {
        builder.append("* ");
      }
      builder.append(line);
    }
    return builder.toString();
  }

  // Returns true if the comment looks like javadoc
  private static boolean javadocShaped(List<String> lines) {
    Iterator<String> it = lines.iterator();
    if (!it.hasNext()) {
      return false;
    }
    String first = it.next().trim();
    // if it's actually javadoc, we're done
    if (first.startsWith("/**")) {
      return true;
    }
    // if it's a block comment, check all trailing lines for '*'
    if (!first.startsWith("/*")) {
      return false;
    }
    while (it.hasNext()) {
      if (!it.next().trim().startsWith("*")) {
        return false;
      }
    }
    return true;
  }
}
