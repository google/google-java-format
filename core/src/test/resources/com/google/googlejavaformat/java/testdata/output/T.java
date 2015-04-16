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
 * Tests for ThisExpressions, ThrowStatements, TryStatements, TypeDeclarationStatements,
 * TypeDeclarations, TypeLiterals, TypeMethodReferences, TypeParameters, and Types.
 */
class T<T1, T2, T3> {
  // TODO(jdd): Add tests for higher language levels.

  T f(int x) throws Exception {
    class TT {}
    if (x == 0 || T.class == null) {
      return this;
    }
    try (AutoCloseable y = null) {
      throw new RuntimeException();
    }
  }
}
