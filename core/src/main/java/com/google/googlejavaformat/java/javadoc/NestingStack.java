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

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Stack for tracking the level of nesting. In the simplest case, each entry is just the integer 1,
 * and the stack is effectively a counter. In more complex cases, the entries may depend on context.
 * For example, if the stack is keeping track of Javadoc lists, the entries represent indentation
 * levels, and those depend on whether the list is an HTML list or a Markdown list.
 */
final class NestingStack {
  private int total;
  private final Deque<Integer> stack = new ArrayDeque<>();

  int total() {
    return total;
  }

  void push() {
    push(1);
  }

  void push(int value) {
    stack.push(value);
    total += value;
  }

  void incrementIfPositive() {
    if (total > 0) {
      push();
    }
  }

  void popIfNotEmpty() {
    if (!isEmpty()) {
      total -= stack.pop();
    }
  }

  boolean isEmpty() {
    return stack.isEmpty();
  }

  void reset() {
    total = 0;
    stack.clear();
  }
}
