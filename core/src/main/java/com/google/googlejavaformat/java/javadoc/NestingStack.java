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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import org.jspecify.annotations.Nullable;

/**
 * Stack for tracking the level of nesting. In the simplest case, we have a stack of {@link Integer}
 * where each entry is just the integer 1, and the stack is effectively a counter. In more complex
 * cases, the entries may depend on context. For example, if the stack is keeping track of Javadoc
 * lists, the entries represent indentation levels, and those depend on whether the list is an HTML
 * list or a Markdown list.
 *
 * @param <E> The type of the elements in the stack.
 */
sealed class NestingStack<E> {
  private final Deque<E> stack = new ArrayDeque<>();

  void push(E value) {
    stack.push(value);
  }

  @CanIgnoreReturnValue
  @Nullable E popIfNotEmpty() {
    return isEmpty() ? null : stack.pop();
  }

  /**
   * If the stack contains the given element, pop it and everything above it. Otherwise, do nothing.
   */
  void popUntil(E value) {
    if (stack.contains(value)) {
      E popped;
      do {
        popped = stack.pop();
      } while (!popped.equals(value));
    }
  }

  boolean contains(E value) {
    return stack.contains(value);
  }

  boolean containsAny(Collection<E> values) {
    return stack.stream().anyMatch(values::contains);
  }

  boolean isEmpty() {
    return stack.isEmpty();
  }

  void reset() {
    stack.clear();
  }

  static final class Int extends NestingStack<Integer> {
    private int total;

    int total() {
      return total;
    }

    @Override
    void push(Integer value) {
      super.push(value);
      total += value;
    }

    void push() {
      push(1);
    }

    @Override
    Integer popIfNotEmpty() {
      Integer popped = super.popIfNotEmpty();
      if (popped != null) {
        total -= popped;
      }
      return popped;
    }

    @Override
    void reset() {
      super.reset();
      total = 0;
    }
  }
}
