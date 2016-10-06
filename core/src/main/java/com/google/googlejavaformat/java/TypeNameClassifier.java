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

package com.google.googlejavaformat.java;

import com.google.common.base.Verify;
import java.util.List;

/** Heuristics for classifying qualified names as types. */
public final class TypeNameClassifier {

  private TypeNameClassifier() {}

  /** A state machine for classifying qualified names. */
  private enum TyParseState {

    /** The start state. */
    START(false) {
      @Override
      public TyParseState next(JavaCaseFormat n) {
        switch (n) {
          case UPPERCASE:
            // if we see an UpperCamel later, assume this was a class
            // e.g. com.google.FOO.Bar
            return TyParseState.AMBIGUOUS;
          case LOWER_CAMEL:
            return TyParseState.REJECT;
          case LOWERCASE:
            // could be a package
            return TyParseState.START;
          case UPPER_CAMEL:
            return TyParseState.TYPE;
        }
        throw new AssertionError();
      }
    },

    /** The current prefix is a type. */
    TYPE(true) {
      @Override
      public TyParseState next(JavaCaseFormat n) {
        switch (n) {
          case UPPERCASE:
          case LOWER_CAMEL:
          case LOWERCASE:
            return TyParseState.FIRST_STATIC_MEMBER;
          case UPPER_CAMEL:
            return TyParseState.TYPE;
        }
        throw new AssertionError();
      }
    },

    /** The current prefix is a type, followed by a single static member access. */
    FIRST_STATIC_MEMBER(true) {
      @Override
      public TyParseState next(JavaCaseFormat n) {
        return TyParseState.REJECT;
      }
    },

    /** Anything not represented by one of the other states. */
    REJECT(false) {
      @Override
      public TyParseState next(JavaCaseFormat n) {
        return TyParseState.REJECT;
      }
    },

    /** An ambiguous type prefix. */
    AMBIGUOUS(false) {
      @Override
      public TyParseState next(JavaCaseFormat n) {
        switch (n) {
          case UPPERCASE:
            return AMBIGUOUS;
          case LOWER_CAMEL:
          case LOWERCASE:
            return TyParseState.REJECT;
          case UPPER_CAMEL:
            return TyParseState.TYPE;
        }
        throw new AssertionError();
      }
    };

    private final boolean isSingleUnit;

    TyParseState(boolean isSingleUnit) {
      this.isSingleUnit = isSingleUnit;
    }

    public boolean isSingleUnit() {
      return isSingleUnit;
    }

    /** Transition function. */
    public abstract TyParseState next(JavaCaseFormat n);
  }

  /**
   * Returns the end index (inclusive) of the longest prefix that matches the naming conventions of
   * a type or static field access, or -1 if no such prefix was found.
   *
   * <p>Examples:
   *
   * <ul>
   *   <li>ClassName
   *   <li>ClassName.staticMemberName
   *   <li>com.google.ClassName.InnerClass.staticMemberName
   * </ul>
   */
  static int typePrefixLength(List<String> nameParts) {
    TyParseState state = TyParseState.START;
    int typeLength = -1;
    for (int i = 0; i < nameParts.size(); i++) {
      state = state.next(JavaCaseFormat.from(nameParts.get(i)));
      if (state == TyParseState.REJECT) {
        break;
      }
      if (state.isSingleUnit()) {
        typeLength = i;
      }
    }
    return typeLength;
  }

  /** Case formats used in Java identifiers. */
  public enum JavaCaseFormat {
    UPPERCASE,
    LOWERCASE,
    UPPER_CAMEL,
    LOWER_CAMEL;

    /** Classifies an identifier's case format. */
    static JavaCaseFormat from(String name) {
      Verify.verify(!name.isEmpty());
      boolean firstUppercase = false;
      boolean hasUppercase = false;
      boolean hasLowercase = false;
      boolean first = true;
      for (int i = 0; i < name.length(); i++) {
        char c = name.charAt(i);
        if (!Character.isAlphabetic(c)) {
          continue;
        }
        if (first) {
          firstUppercase = Character.isUpperCase(c);
          first = false;
        }
        hasUppercase |= Character.isUpperCase(c);
        hasLowercase |= Character.isLowerCase(c);
      }
      if (firstUppercase) {
        return hasLowercase ? UPPER_CAMEL : UPPERCASE;
      } else {
        return hasUppercase ? LOWER_CAMEL : LOWERCASE;
      }
    }
  }
}
