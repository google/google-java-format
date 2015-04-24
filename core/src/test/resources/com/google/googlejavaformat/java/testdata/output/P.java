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
