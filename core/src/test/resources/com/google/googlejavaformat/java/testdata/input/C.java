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

package com.google.googlejavaformat.java.test;

/**
 * Tests for CastExpressions, CatchClauses, CharacterLiterals, ClassInstanceCreations,
 * CommentHelper, ConditionalExpressions, ConstructorInvocations, ContinueStatements, and
 * CreationReferences.
 */
class C<T> {
  // TODO(jdd): Test higher-language-level constructs.

  C() {
    this(
        0, 1,
        2, 3,
        4, 5,
        6, 7,
        8, 9,
        10, 11,
        12, 13,
        14, 15,
        16, 17,
        18, 19,
        20, 21,
        22, 23,
        24, 25,
        26, 27,
        28, 29,
        30, 31);
  }

  C(int... x) {}

  void f() {
    try {
    } catch (RuntimeException e) {
      Object x =
          (Pair<
                  Pair<
                      Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>,
                      Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>>,
                  Pair<
                      Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>,
                      Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>>>)
              null;
      C<Integer> c =
          new C<Integer>(
              0, 1,
              2, 3,
              4, 5,
              6, 7,
              8, 9,
              10, 11,
              12, 13,
              14, 15,
              16, 17,
              18, 19,
              20, 21,
              22, 23,
              24, 25,
              26, 27,
              28, 29,
              30, 31);
      int i = 0;
      int j =
          i == 0
              ? 0
              : i == 1
                  ? 1
                  : i == 2 ? 2 : i == 3 ? 3 : i == 4 ? 4 : i == 5 ? 5 : i == 6 ? 6 : i == 7 ? 7 : i;
      LABEL:
      while (true != false) {
        if (false == true)
          continue;
        if (false == false)
          continue LABEL;
        // Comment indented +2
      // Comment indented +0
      }
    }
  }
}
