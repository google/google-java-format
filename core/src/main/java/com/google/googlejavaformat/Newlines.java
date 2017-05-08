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

package com.google.googlejavaformat;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import java.util.Iterator;
import java.util.NoSuchElementException;

/** Platform-independent newline handling. */
public class Newlines {

  /** Returns the number of line breaks in the input. */
  public static int count(String input) {
    return Iterators.size(lineOffsetIterator(input)) - 1;
  }

  /** Returns the index of the first break in the input, or {@code -1}. */
  public static int firstBreak(String input) {
    Iterator<Integer> it = lineOffsetIterator(input);
    it.next();
    return it.hasNext() ? it.next() : -1;
  }

  private static final ImmutableSet<String> BREAKS = ImmutableSet.of("\r\n", "\n", "\r");

  /** Returns true if the entire input string is a recognized line break. */
  public static boolean isNewline(String input) {
    return BREAKS.contains(input);
  }

  /** Returns the length of the newline sequence at the current offset, or {@code -1}. */
  public static int hasNewlineAt(String input, int idx) {
    for (String b : BREAKS) {
      if (input.startsWith(b, idx)) {
        return b.length();
      }
    }
    return -1;
  }

  /**
   * Returns the terminating line break in the input, or {@code null} if the input does not end in a
   * break.
   */
  public static String getLineEnding(String input) {
    for (String b : BREAKS) {
      if (input.endsWith(b)) {
        return b;
      }
    }
    return null;
  }

  /**
   * Returns the first line separator in the text, or {@code "\n"} if the text does not contain a
   * single line separator.
   */
  public static String guessLineSeparator(String text) {
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      switch (c) {
        case '\r':
          if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
            return "\r\n";
          }
          return "\r";
        case '\n':
          return "\n";
        default:
          break;
      }
    }
    return "\n";
  }

  /** Returns true if the input contains any line breaks. */
  public static boolean containsBreaks(String text) {
    return CharMatcher.anyOf("\n\r").matchesAnyOf(text);
  }

  /** Returns an iterator over the start offsets of lines in the input. */
  public static Iterator<Integer> lineOffsetIterator(String input) {
    return new LineOffsetIterator(input);
  }

  /** Returns an iterator over lines in the input, including trailing whitespace. */
  public static Iterator<String> lineIterator(String input) {
    return new LineIterator(input);
  }

  private static class LineOffsetIterator implements Iterator<Integer> {

    private int curr = 0;
    private int idx = 0;
    private final String input;

    private LineOffsetIterator(String input) {
      this.input = input;
    }

    @Override
    public boolean hasNext() {
      return curr != -1;
    }

    @Override
    public Integer next() {
      if (curr == -1) {
        throw new NoSuchElementException();
      }
      int result = curr;
      advance();
      return result;
    }

    private void advance() {
      for (; idx < input.length(); idx++) {
        char c = input.charAt(idx);
        switch (c) {
          case '\r':
            if (idx + 1 < input.length() && input.charAt(idx + 1) == '\n') {
              idx++;
            }
            // falls through
          case '\n':
            idx++;
            curr = idx;
            return;
          default:
            break;
        }
      }
      curr = -1;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("remove");
    }
  }

  private static class LineIterator implements Iterator<String> {

    int idx;
    String curr;

    private final String input;
    private final Iterator<Integer> indices;

    private LineIterator(String input) {
      this.input = input;
      this.indices = lineOffsetIterator(input);
      idx = indices.next(); // read leading 0
    }

    private void advance() {
      int last = idx;
      if (indices.hasNext()) {
        idx = indices.next();
      } else if (hasNext()) {
        // no terminal line break
        idx = input.length();
      } else {
        throw new NoSuchElementException();
      }
      curr = input.substring(last, idx);
    }

    @Override
    public boolean hasNext() {
      return idx < input.length();
    }

    @Override
    public String next() {
      advance();
      return curr;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("remove");
    }
  }
}
