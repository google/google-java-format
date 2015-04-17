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
 * Tests for PackageDeclarations, ParameterizedTypes, ParenthesizedExpressions, PostfixExpressions,
 * PrefixExpressions, and PrimitiveTypes.
 */
class P<
    T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21,
    T22, T23, T24> {
  void f() {
    int x = (1 + 2) * 3;
    ++x;
    x++;
    int j = + + +x;
    int k = + + ++x;
    int jj = - - -x;
    int kk = - - --x;
    boolean b = false;
    boolean bb = !!b;
  }
}
