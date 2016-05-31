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
  String remaining;
  int toConsume;

  CharStream(String input) {
    this.remaining = checkNotNull(input);
  }

  boolean tryConsume(String expected) {
    if (!remaining.startsWith(expected)) {
      return false;
    }
    toConsume = expected.length();
    return true;
  }

  /*
   * @param pattern the pattern to search for, which must be anchored to match only at position 0
   */
  boolean tryConsumeRegex(Pattern pattern) {
    Matcher matcher = pattern.matcher(remaining);
    if (!matcher.find()) {
      return false;
    }
    checkArgument(matcher.start() == 0);
    toConsume = matcher.end();
    return true;
  }

  String readAndResetRecorded() {
    String result = remaining.substring(0, toConsume);
    remaining = remaining.substring(toConsume);
    toConsume = 0; // TODO(cpovirk): Set this to a bogus value here and in the constructor.
    return result;
  }

  boolean isExhausted() {
    return remaining.isEmpty();
  }
}
