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

package com.google.googlejavaformat.java;

import java.util.regex.Pattern;

/**
 * Line separator constants and utilities.
 */
public enum LineSeparator {

  /**
   * Windows line separator: CRLF or as String {@code "\r\n"}.
   */
  WINDOWS("\r\n", Pattern.compile("(?s).*(\\r\\n).*")),

  /**
   * Unix/Linux line separator: LF or as a String {@code "\n"}.
   */
  UNIX("\n", Pattern.compile("(?s).*(\\n).*")),

  /**
   * Legacy Mac OS line separator: CR or as a String {@code "\r"}.
   */
  MAC("\r", Pattern.compile("(?s).*(\\r).*"));

  /**
   * Convenient for {@code detect(text, LineSeparator.UNIX)}.
   */
  public static LineSeparator detect(String text) {
    return detect(text, UNIX);
  }

  /**
   * Identify which line delimiter is used by the underlying text.
   *
   * <p>The enum constants are iterated in declaration order by this implementation. That means for
   * texts using mixed separators, the first hit determines the returned separator of this method.
   *
   * <p>If no pattern matches, the passed default separator is returned.
   */
  public static LineSeparator detect(String text, LineSeparator defaultSeparator) {
    for (LineSeparator separator : values()) {
      if (separator.pattern.matcher(text).matches()) {
        return separator;
      }
    }
    return defaultSeparator;
  }

  private final String chars;
  private final Pattern pattern;

  LineSeparator(String chars, Pattern pattern) {
    this.chars = chars;
    this.pattern = pattern;
  }

  public String getChars() {
    return chars;
  }

  /**
   * Converts the given text by replacing all line separators with this line separator chars.
   *
   * <p>For the sake of speed, conversion result of texts using mixed line separators are not
   * always the same. Only a single pattern is replaced with this line separator chars.
   *
   * @param text to convert
   * @return the converted text
   */
  public String convert(String text) {
    return convert(text, detect(text));
  }

  String convert(String text, LineSeparator lineSeparatorUsedInText) {
    if (this == lineSeparatorUsedInText) {
      return text;
    }
    return text.replace(lineSeparatorUsedInText.chars, this.chars);
  }

  /** Predicate function comparing this with the result of {@link #detect(String)}. */
  public boolean matches(String text) {
    return this == detect(text);
  }
}
