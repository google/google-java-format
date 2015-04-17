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

import java.util.List;

/**
 * Tests for IfStatements, ImportDeclarations, InfixExpressions, Initializers,
 * InstanceofExpressions, and IntersectionTypes.
 */
class I {
  interface I0 {}

  interface I1 {}

  interface I2 {}

  interface I3 {}

  interface I4 {}

  interface I5 {}

  interface I6 {}

  interface I7 {}

  interface I8 {}

  interface I9 {}

  interface I10 {}

  interface I11 {}

  interface I12 {}

  interface I13 {}

  interface I14 {}

  interface I15 {}

  interface I16 {}

  interface I17 {}

  interface I18 {}

  interface I19 {}

  class II<
      T extends
          I0 & I1 & I2 & I3 & I4 & I5 & I6 & I7 & I8 & I9 & I10 & I11 & I12 & I13 & I14 & I15 & I16
              & I17 & I18 & I19> {}

  static class CC {
    static {
      int i = 0;
    }
  }

  int x =
      0 >>> 0 + 0 / 0 * 0 - 0 & 0 << 0 * 0 / 0 >> 0 - 0 ^ 0 * 0 / 0 >>> 0 << 0 * 0 - 0 / 0
          | 0 * 0 >> 0 + 0 / 0 * 0 - 0 << 0
              & 0 * 0 / 0 >>> 0 - 0 * 0 >> 0 / 0 << 0 * 0 + 0 - 0 / 0 * 0
          | 0 - 0 * 0 >>> 0 << 0 / 0 * 0 >> 0 - 0 ^ 0 * 0 / 0 & 0 << 0 + 0;

  void f() {
    if (0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0
            + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0
        == 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0
            + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0) {
    } else if (null instanceof List) {
    } else {
    }
    if (true) {}
  }
}
