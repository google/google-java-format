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

package com.google.googlejavaformat;

import com.google.common.base.MoreObjects;
import java.util.ArrayDeque;
import java.util.List;

/// A `DocBuilder` converts a sequence of [Op]s into a [Doc].
public final class DocBuilder {
  private final Doc.Level base = Doc.Level.make(Indent.Const.ZERO);
  private final ArrayDeque<Doc.Level> stack = new ArrayDeque<>();

  /// A possibly earlier [Doc.Level] for appending text, à la Philip Wadler.
  ///
  /// Processing [Doc]s presents a subtle problem. Suppose we have a [Doc] for to an assignment
  /// node, `a = b`, with an optional [Doc.Break] following the `=`. Suppose we have 5 characters to
  /// write it, so that we think we don't need the break. Unfortunately, this [Doc] lies in an
  /// expression statement [Doc] for the statement `a = b;` and this statement does not fit in 3
  /// characters. This is why many formatters sometimes emit lines that are too long, or cheat by
  /// using a narrower line length to avoid such problems.
  ///
  /// One solution to this problem is not to decide whether a [Doc.Level] should be broken until
  /// later (in this case, after the semicolon has been seen). A simpler approach is to rewrite the
  /// [Doc] as here, so that the semicolon moves inside the inner [Doc], and we can decide whether
  /// to break that [Doc] without seeing later text.
  private Doc.Level appendLevel = base;

  /// Start to build a `DocBuilder`.
  public DocBuilder() {
    stack.addLast(base);
  }

  /// Add a list of [Op]s to the [OpsBuilder].
  ///
  /// @param ops the [Op]s
  /// @return the [OpsBuilder]
  public DocBuilder withOps(List<Op> ops) {
    for (Op op : ops) {
      op.add(this); // These operations call the operations below to build the doc.
    }
    return this;
  }

  /// Open a new [Doc.Level].
  ///
  /// @param plusIndent the extra indent for the [Doc.Level]
  void open(Indent plusIndent) {
    Doc.Level level = Doc.Level.make(plusIndent);
    stack.addLast(level);
  }

  /// Close the current [Doc.Level].
  void close() {
    Doc.Level top = stack.removeLast();
    stack.peekLast().add(top);
  }

  /// Add a [Doc] to the current [Doc.Level].
  ///
  /// @param doc the [Doc]
  void add(Doc doc) {
    appendLevel.add(doc);
  }

  /// Add a [Doc.Break] to the current [Doc.Level].
  ///
  /// @param breakDoc the [Doc.Break]
  void breakDoc(Doc.Break breakDoc) {
    appendLevel = stack.peekLast();
    appendLevel.add(breakDoc);
  }

  /// Return the [Doc].
  ///
  /// @return the [Doc]
  public Doc build() {
    return base;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("base", base)
        .add("stack", stack)
        .add("appendLevel", appendLevel)
        .toString();
  }
}
