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
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.googlejavaformat.CommentsHelper;
import com.google.googlejavaformat.Input;

import java.util.ArrayList;
import java.util.List;

/** {@code JavaCommentsHelper} extends {@link CommentsHelper} to rewrite Java comments. */
public final class JavaCommentsHelper implements CommentsHelper {
  // TODO(jdd): Switch to use formatter's filling logic here too.
  // TODO(jdd): Use Eclipse JavaDoc parser here.

  private final boolean noReflowInitialComment;

  private static final Splitter NEWLINE_SPLITTER = Splitter.on('\n');
  private static final Splitter WHITESPACE_SPLITTER =
      Splitter.on(CharMatcher.WHITESPACE).omitEmptyStrings();

  JavaCommentsHelper(boolean noReflowInitialComment) {
    this.noReflowInitialComment = noReflowInitialComment;
  }

  @Override
  public String rewrite(Input.Tok tok, int maxWidth, int column0) {
    String text = tok.getOriginalText();
    if (!(text.startsWith("/*") && text.contains("\n")) || text.contains("<pre>")
        || text.contains("<code>")
        || noReflowInitialComment && tok.getIndex() == 0 && !text.startsWith("/**")) {
      return text;
    }
    List<String> rawLines = NEWLINE_SPLITTER.splitToList(text);
    int rawLinesN = rawLines.size();
    if (!(rawLinesN >= 3 && (rawLines.get(0).equals("/*") || rawLines.get(0).equals("/**"))
        && rawLines.get(rawLinesN - 1).trim().equals("*/"))) {
      return text;
    }
    String commonPrefix = rawLines.get(1);
    for (int i = 2; i < rawLinesN - 1; i++) {
      commonPrefix = Strings.commonPrefix(commonPrefix, rawLines.get(i));
    }
    if (!commonPrefix.trim().startsWith("*")) {
      return text;
    }
    List<String> lines = new ArrayList<>();
    for (int i = 1; i < rawLinesN - 1; i++) {
      String line = rawLines.get(i).trim();
      if (line.equals("*")) {
        lines.add("");
      } else if (line.startsWith("* ")) {
        lines.add(line.substring(2));
      } else {
        return CharMatcher.WHITESPACE.collapseFrom(text, ' ');
      }
    }
    int availableWidth = maxWidth - (column0 + 3);
    String indentString = Strings.repeat(" ", column0);
    StringBuilder builder = new StringBuilder();
    builder.append(rawLines.get(0)).append('\n').append(indentString);
    int lineI = 0;
    int linesN = lines.size();
    while (lineI < linesN) {
      String line = lines.get(lineI);
      if (isLiteralLine(line)) {
        if (line.isEmpty()) {
          builder.append(" *").append('\n').append(indentString);
        } else {
          builder.append(" * ").append(line).append('\n').append(indentString);
        }
        ++lineI;
      } else {
        List<String> paragraph = new ArrayList<>();
        String extraIndentString = Strings.repeat(" ", extraIndent(lines.get(lineI)));
        do {
          paragraph.add(lines.get(lineI++));
        } while (lineI < linesN
            && !(isEndParagraphLine(lines.get(lineI - 1))
                || isBeginParagraphLine(lines.get(lineI))));
        List<String> initialWords =
            Lists.newArrayList(WHITESPACE_SPLITTER.split(Joiner.on(" ").join(paragraph)));
        List<String> words = new ArrayList<>();
        String pendingWord = "";
        for (String word : initialWords) {
          pendingWord = pendingWord.isEmpty() ? word : pendingWord + ' ' + word;
          if (isGoodJavadoc(pendingWord)) {
            words.add(pendingWord);
            pendingWord = "";
          }
        }
        if (!pendingWord.isEmpty()) {
          words.add(pendingWord);
        }
        int wordI = 0;
        boolean firstOutputLine = true;
        int wordsN = words.size();
        while (wordI < wordsN) {
          StringBuilder lineBuilder = new StringBuilder();
          lineBuilder.append(words.get(wordI));
          int lineWidth = words.get(wordI).length();
          wordI++;
          int extraIndent = extraIndentString.length();
          while (wordI < wordsN
              && lineWidth + 1 + words.get(wordI).length()
                  <= availableWidth - (firstOutputLine ? 0 : extraIndent)) {
            lineBuilder.append(' ').append(words.get(wordI));
            lineWidth += 1 + words.get(wordI).length();
            ++wordI;
          }
          builder.append(" * ");
          if (!firstOutputLine) {
            builder.append(extraIndentString);
          }
          firstOutputLine = false;
          builder.append(lineBuilder).append('\n').append(indentString);
        }
      }
    }
    return builder.append(" */").toString();
  }

  private static boolean isLiteralLine(String line) {
    return line.isEmpty() || line.startsWith(" ");
  }

  private static boolean isBeginParagraphLine(String line) {
    return isLiteralLine(line) || line.startsWith("<") || line.startsWith("@");
  }

  private static boolean isEndParagraphLine(String line) {
    return isLiteralLine(line) || line.endsWith(">");
  }

  private static int extraIndent(String line) {
    return line.startsWith("@") ? 4 : 0;
  }

  private static boolean isGoodJavadoc(String s) {
    int nestingLevel = 0;
    for (char c : s.toCharArray()) {
      switch (c) {
        case '{':
          ++nestingLevel;
          break;
        case '}':
          if (nestingLevel > 0) {
            --nestingLevel;
          }
          break;
        default:
          break;
      }
    }
    return nestingLevel == 0;
  }
}
