/*
 * Copyright 2016 Google Inc.
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

package com.google.googlejavaformat.java.javadoc;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * String reader designed for use from the lexer. Callers invoke the {@link #tryConsume tryConsume*}
 * methods to specify what characters they expect and then {@link #readAndResetRecorded} to retrieve
 * and consume the matched characters. This is a slightly odd API -- why not just return the matched
 * characters from tryConsume? -- but it is convenient for the lexer.
 */
final class CharStream {
  private final String input;
  private int start;
  private int tokenEnd = -1; // Negative value means no token, and will cause an exception if used.

  CharStream(String input) {
    this.input = checkNotNull(input);
  }

  boolean tryConsume(String expected) {
    if (!input.startsWith(expected, start)) {
      return false;
    }
    tokenEnd = start + expected.length();
    return true;
  }

  /**
   * Tries to consume characters from the current position that match the given pattern.
   *
   * @param pattern the pattern to search for, which must be anchored to match only at position 0
   */
  boolean tryConsumeRegex(Pattern pattern) {
    Matcher matcher = pattern.matcher(input).region(start, input.length());
    if (!matcher.find()) {
      return false;
    }
    checkArgument(matcher.start() == start);
    tokenEnd = matcher.end();
    return true;
  }

  String readAndResetRecorded() {
    String result = input.substring(start, tokenEnd);
    start = tokenEnd;
    tokenEnd = -1;
    return result;
  }

  boolean isExhausted() {
    return start == input.length();
  }
}
