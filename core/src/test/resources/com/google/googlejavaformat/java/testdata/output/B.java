package com.google.googlejavaformat.java.test;

/** Tests for Blocks, BodyDeclarations, BooleanLiterals, and BreakStatements. */
class B {
  int x;
  private int y;
  public int z;

  void f() {
    LABEL:
    while (true != false) {
      if (false == true) break;
      if (false == false) break LABEL;
    }
  }
}
