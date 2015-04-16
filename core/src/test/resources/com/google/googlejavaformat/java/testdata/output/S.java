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
 * Tests for SimpleNames, SimpleTypes, SingleMemberAnnotations, SingleVariableDeclarations,
 * Statements, StringLiterals, SuperConstructorInvocations, SuperFieldAccesses,
 * SuperMethodInvocations, SuperMethodReferences, SwitchCases, SwitchStatements, and
 * SynchronizedStatements.
 */
class S {
  // TODO(jdd): Add tests for higher language levels.

  int x = 0;

  @SingleMemberAnnotation(
      0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0
          + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0)
  S() {
    super();
  }

  class SS extends S {
    SS() {
      super();
      super.x = 0;
      super.foo();
    }
  }

  void foo() {
    Object[] object = null;
    synchronized (
        object[
            0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0
                + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0]) {
      switch ("abc") {
        case "one":
          break;
        case "two":
          break;
        case "three":
        default:
          break;
      }
    }
  }
}
