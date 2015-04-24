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

/** Tests for NameQualifiedTypes, Names, NormalAnnotations, NullLiterals, and NumberLiterals. */
class N {
  @NormalAnnotation(
      a = 1,
      b = 2,
      c = 3,
      d = 4,
      e = 5,
      f = 6,
      g = 7,
      h = 8,
      i = 9,
      j = 10,
      k = 11,
      l = 12,
      m = 13,
      n = 14,
      o = 15,
      p = 16,
      q = 17,
      r = 18,
      s = 19,
      t = 20,
      u = 21,
      v = 22,
      w = 23,
      x = 24,
      y = 25,
      z = 26)
  void f() {
    assert 00 == 0 && 0x0 == 0 && 1.0e0f == 1.00e0f && 0.2D == .2D;
    java.lang. /* @MarkerAnnotation */String s = null;
  }
}
