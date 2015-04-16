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

/** Tests for Blocks, BodyDeclarations, BooleanLiterals, and BreakStatements. */
class B {
  int x;
  private int y;
  public int z;

  void f() {
    LABEL:
    while (true != false) {
      if (false == true)
        break;
      if (false == false)
        break LABEL;
    }
  }
}
