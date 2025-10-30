/*
 * Copyright 2016 Google Inc.
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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Collection;

import static com.google.common.truth.Truth.assertThat;
import static com.google.googlejavaformat.java.RemoveUnusedDeclarations.removeUnusedDeclarations;

@RunWith(Parameterized.class)
public record RemoveUnusedDeclarationsTest(String input, String expected) {

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> parameters() {
    return ImmutableList.<Object[]>builder()
      .addAll(workingCases())
      //.addAll(todoCases())
      .build();
  }

  private static Collection<Object[]> workingCases() {
    String[][][] inputsOutputs = {
      // Interface members
      {
        {
          """
          interface TestInterface {
            public static final int CONSTANT = 1;
            public abstract void method();
            public static class InnerClass {}
          }
          """
        },
        {
          """
          interface TestInterface {
            int CONSTANT = 1;
            void method();
            class InnerClass {}
          }
          """
        }
      },

      // Class with redundant modifiers
      {
        {
          """
          public class TestClass {
            public final static String VALUE = "test";
            public abstract void doSomething();
          }
          """
        },
        {
          """
          public class TestClass {
            public static final String VALUE = "test";
            public abstract void doSomething();
          }
          """
        }
      },

      // Final parameters (should be preserved)
      {
        {
          """
          class Test {
            void method(final String param1, @Nullable final String param2) {}
          }
          """
        },
        {
          """
          class Test {
            void method(final String param1, @Nullable final String param2) {}
          }
          """
        }
      },

      // Code that shouldn't change
      {
        {
          """
          class NoChanges {
            private int field;
            void method(String param) {}
            static final class Inner {}
          }
          """
        },
        {
          """
          class NoChanges {
            private int field;
            void method(String param) {}
            static final class Inner {}
          }
          """
        }
      }
    };

    return buildTestCases(inputsOutputs);
  }

  private static Collection<Object[]> todoCases() {
    String[][][] inputsOutputs = {
      // Enum constants
      {
        {
          """
          public enum TestEnum {
            public static final VALUE1, VALUE2;
            public static void doSomething() {}
          }
          """
        },
        {
          """
          public enum TestEnum {
            VALUE1, VALUE2;
            static void doSomething() {}
          }
          """
        }
      },

      // Annotation declarations
      {
        {
          """
          @public @interface TestAnnotation {
            public abstract String value();
            public static final int DEFAULT = 0;
          }
          """
        },
        {
          """
          @public @interface TestAnnotation {
            String value();
            int DEFAULT = 0;
          }
          """
        }
      },

      // Nested interfaces and classes
      {
        {
          """
          class Outer {
            public static interface InnerInterface {
              public static final int VAL = 1;
            }
            public static class InnerClass {
              public static final int VAL = 1;
            }
          }
          """
        },
        {
          """
          class Outer {
            interface InnerInterface {
              int VAL = 1;
            }
            static class InnerClass {
              static final int VAL = 1;
            }
          }
          """
        }
      },

      // Static interfaces in abstract classes
      {
        {
          """
          public abstract class Test {
            public static final int CONST1 = 1;
            private static final int CONST2 = 2;
            protected abstract void doSomething(final String param);
            public static interface Inner {
              public static final int INNER_CONST = 3;
            }
          }
          """
        },
        {
          """
          public abstract class Test {
            public static final int CONST1 = 1;
            private static final int CONST2 = 2;
            protected abstract void doSomething(final String param);
            interface Inner {
              int INNER_CONST = 3;
            }
          }
          """
        }
      },

      // Records
      {
        {
          """
          public record TestRecord(
            public final String name, 
            public static final int MAX = 100
          ) {
            public static void doSomething() {}
          }
          """
        },
        {
          """
          public record TestRecord(
            String name, 
            int MAX = 100
          ) {
            static void doSomething() {}
          }
          """
        }
      },

      // Sealed classes
      {
        {
          """
          public sealed abstract class Shape 
            permits public final class Circle, public non-sealed class Rectangle {
            public abstract double area();
          }
          """
        },
        {
          """
          public sealed abstract class Shape 
            permits Circle, Rectangle {
            abstract double area();
          }
          """
        }
      }
    };

    return buildTestCases(inputsOutputs);
  }

  private static Collection<Object[]> buildTestCases(String[][][] inputsOutputs) {
    ImmutableList.Builder<Object[]> builder = ImmutableList.builder();
    for (String[][] inputAndOutput : inputsOutputs) {
      assertThat(inputAndOutput).hasLength(2);
      builder.add(new String[]{
        Joiner.on('\n').join(inputAndOutput[0]) + '\n',
        Joiner.on('\n').join(inputAndOutput[1]) + '\n',
      });
    }
    return builder.build();
  }

  @Test
  public void removeUnused() throws Exception {
    assertThat(removeUnusedDeclarations(input)).isEqualTo(expected);
  }
}