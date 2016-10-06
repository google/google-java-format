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
import com.google.googlejavaformat.Output.BreakTag;

/**
 * An indent for a {@link Doc.Level} or {@link Doc.Break}. The indent is either a constant {@code
 * int}, or a conditional expression whose value depends on whether or not a {@link Doc.Break} has
 * been broken.
 */
public abstract class Indent {

  abstract int eval();

  /** A constant function, returning a constant indent. */
  public static final class Const extends Indent {
    private final int n;

    public static final Const ZERO = new Const(+0);

    private Const(int n) {
      this.n = n;
    }

    public static Const make(int n, int indentMultiplier) {
      return new Const(n * indentMultiplier);
    }

    @Override
    int eval() {
      return n;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("n", n).toString();
    }
  }

  /** A conditional function, whose value depends on whether a break was taken. */
  public static final class If extends Indent {
    private final BreakTag condition;
    private final Indent thenIndent;
    private final Indent elseIndent;

    private If(BreakTag condition, Indent thenIndent, Indent elseIndent) {
      this.condition = condition;
      this.thenIndent = thenIndent;
      this.elseIndent = elseIndent;
    }

    public static If make(BreakTag condition, Indent thenIndent, Indent elseIndent) {
      return new If(condition, thenIndent, elseIndent);
    }

    @Override
    int eval() {
      return (condition.wasBreakTaken() ? thenIndent : elseIndent).eval();
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("condition", condition)
          .add("thenIndent", thenIndent)
          .add("elseIndent", elseIndent)
          .toString();
    }
  }
}
