package com.google.googlejavaformat.java.test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Tests for EmptyStatements, EnhancedForStatements, EnumConstantDeclarations, EnumDeclarations,
 * ExpressionMethodReferences, ExpressionStatements, Expressions, and ExtendedModifiers.
 */
@MarkerAnnotation
class E<T> {
  // TODO(jdd): Test higher language-level features.

  enum Enum1 {
    A, B, C, D;

    Enum1() {}
  }

  @MarkerAnnotation
  public enum Enum2 {
    A,
    B,
    C,
    D,
    ;

    Enum2() {}
  }

  enum Enum3 {
    A(
        0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0
            + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0),
    B(
        0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0
            + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 1),
    C(
        0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0
            + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 2),
    D(
        0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0
            + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 3);

    Enum3(int x) {}
  }

  enum Enum4 {
    A(
        0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0
            + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0),
    B(
        0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0
            + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 1),
    C(
        0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0
            + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 2),
    D(
        0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0
            + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 0 + 3),
    ;

    Enum4(int x) {}
  }

  int f(int value) {
    ;
    ;
    ;
    ;
    ;
    for (Integer x :
        ImmutableList.<Integer>of(
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)) {}
    for (Pair<
            Pair<Pair<Pair<T, T>, Pair<T, T>>, Pair<Pair<T, T>, Pair<T, T>>>,
            Pair<Pair<Pair<T, T>, Pair<T, T>>, Pair<Pair<T, T>, Pair<T, T>>>>
        x :
            Lists
                .<Pair<
                        Pair<Pair<Pair<T, T>, Pair<T, T>>, Pair<Pair<T, T>, Pair<T, T>>>,
                        Pair<Pair<Pair<T, T>, Pair<T, T>>, Pair<Pair<T, T>, Pair<T, T>>>>>
                    newArrayList()) {}
    f(10);
    return f(20);
  }
}
