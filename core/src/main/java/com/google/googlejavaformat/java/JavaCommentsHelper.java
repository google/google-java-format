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

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.googlejavaformat.CommentsHelper;
import com.google.googlejavaformat.Input;

import java.util.List;

/** {@code JavaCommentsHelper} extends {@link CommentsHelper} to rewrite Java comments. */
public final class JavaCommentsHelper implements CommentsHelper {
  private static final Splitter NEWLINE_SPLITTER = Splitter.on('\n').trimResults();

  @Override
  public String rewrite(Input.Tok tok, int maxWidth, int column0) {
    List<String> rawLines = NEWLINE_SPLITTER.splitToList(tok.getOriginalText());
    StringBuilder builder = new StringBuilder();
    String firstLine = rawLines.get(0);
    builder.append(firstLine).append("\n");
    int indent = column0;
    if (firstLine.startsWith("/*")) {
      // For block and javadoc comments, add an extra space to trailing lines
      // to align the "*" at the beginning of each line with the first "/*".
      indent++;
    }
    String indentString = Strings.repeat(" ", indent);
    for (int i = 1; i < rawLines.size(); ++i) {
      builder.append(indentString).append(rawLines.get(i)).append("\n");
    }
    return builder.toString();
  }
}
