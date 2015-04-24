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
