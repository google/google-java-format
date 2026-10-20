/*
 * Copyright 2026 Google Inc.
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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.googlejavaformat.java.JavaFormatterOptions.Style;
import org.junit.Test;

/// Tests for [Cleanups].
public class CleanupsTest {

  @Test
  public void formatSource_rangePastEndOfInput_throwsFormatterException() {
    String input =
        """
        class Test {
          void f() {}
        }
        """;
    assertThrows(
        FormatterException.class,
        () ->
            new Formatter()
                .formatSource(
                    input,
                    ImmutableList.of(Range.closedOpen(input.length() + 1, input.length() + 1)),
                    /* removeUnusedImports= */ true,
                    /* sortImports= */ false,
                    /* reflowLongStrings= */ false));
  }

  @Test
  public void removeUnusedImports_wholeFile_noExtraBlankLine() throws Exception {
    // b/30464184: removing unused imports before formatting leaves no extra blank line
    String input =
        """
        package test;

        import java.util.ArrayList;
        import java.util.List;

        class Test {
          String xs;
        }
        """;
    String expected =
        """
        package test;

        class Test {
          String xs;
        }
        """;
    String formatted = new Formatter().formatSourceAndFixImports(input);
    assertThat(formatted).isEqualTo(expected);
  }

  @Test
  public void formatSource_noPassesEnabled() throws Exception {
    Formatter formatter =
        new Formatter(JavaFormatterOptions.builder().reorderModifiers(false).build());
    String input =
        """
        class Test {
          final static public int X = 1;
        }
        """;
    String formatted = formatter.formatSource(input);
    assertThat(formatted)
        .isEqualTo(
            """
            class Test {
              final static public int X = 1;
            }
            """);
  }

  @Test
  public void removeUnusedImports_partialFormat_noExtraBlankLine() throws Exception {
    String input =
        """
        package test;
        import java.util.ArrayList;
        import java.util.List;
        class Test {
          void f() {
            int x = 1;
          }
        }
        """;
    int start = input.indexOf("int x = 1;");
    int end = input.indexOf(";", start) + 1;
    String formatted =
        new Formatter()
            .formatSource(
                input,
                ImmutableList.of(Range.closedOpen(start, end)),
                /* removeUnusedImports= */ true,
                /* sortImports= */ true,
                /* reflowLongStrings= */ false);
    assertThat(formatted)
        .isEqualTo(
            """
            package test;

            class Test {
              void f() {
                int x = 1;
              }
            }
            """);
  }

  @Test
  public void fixImports_removesUnusedAndSorts() throws Exception {
    String input =
        """
        package test;

        import java.util.List;
        import java.util.ArrayList;
        import java.util.Collections;

        class Test {
          List<String> xs = Collections.emptyList();
        }
        """;
    String expected =
        """
        package test;

        import java.util.Collections;
        import java.util.List;

        class Test {
          List<String> xs = Collections.emptyList();
        }
        """;
    String result =
        Formatter.fixImports(
            input, Style.GOOGLE, /* removeUnusedImports= */ true, /* sortImports= */ true);
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void reorderModifiers_withinRequestedRange() throws Exception {
    String input =
        """
        class Test {
          final static public int X = 1;
          final static public int Y = 2;
        }
        """;
    // Target only the X declaration line
    int start = input.indexOf("final static public int X");
    int end = input.indexOf(";", start) + 1;
    String formatted =
        new Formatter().formatSource(input, ImmutableList.of(Range.closedOpen(start, end)));
    assertThat(formatted)
        .isEqualTo(
            """
            class Test {
              public static final int X = 1;
              final static public int Y = 2;
            }
            """);
  }

  @Test
  public void getFormatReplacements_isWhitespaceOnly_doesNotReorderModifiers() throws Exception {
    String input =
        """
        class Test {
          final static public int X = 1;
        }
        """;
    ImmutableList<Replacement> replacements =
        new Formatter()
            .getFormatReplacements(input, ImmutableList.of(Range.closedOpen(0, input.length())));
    String applied = JavaOutput.applyReplacements(input, replacements);
    assertThat(applied).contains("final static public int X = 1;");
  }

  @Test
  public void parseReuseWhenNoEdits() throws Exception {
    String input =
        """
        class Test {
          int x;
        }
        """;
    Cleanups.Result result =
        Cleanups.apply(
            input,
            ImmutableList.of(Range.closedOpen(0, input.length())),
            Style.GOOGLE,
            Cleanups.enabledPasses(
                /* removeUnusedImports= */ true,
                /* reorderModifiers= */ false,
                /* sortImports= */ false));
    assertThat(result.unit()).isNotNull();
    assertThat(result.contents()).isEqualTo(input);
  }

  @Test
  public void parseNotReusedWhenEditsOccur() throws Exception {
    String input =
        """
        class Test {
          static public int x;
        }
        """;
    Cleanups.Result result =
        Cleanups.apply(
            input,
            ImmutableList.of(Range.closedOpen(0, input.length())),
            Style.GOOGLE,
            Cleanups.enabledPasses(
                /* removeUnusedImports= */ true,
                /* reorderModifiers= */ true,
                /* sortImports= */ false));
    assertThat(result.unit()).isNull();
    assertThat(result.contents()).contains("public static int x;");
  }

  @Test
  public void rangeRemappingThroughSeveralPasses() throws Exception {
    String input =
        """
        package test;

        import java.util.ArrayList;
        import java.util.List;

        class Test {
          final static public int X = 1;
        }
        """;
    int start = input.indexOf("final static public int X");
    int end = input.indexOf(";", start) + 1;
    Cleanups.Result result =
        Cleanups.apply(
            input,
            ImmutableList.of(Range.closedOpen(start, end)),
            Style.GOOGLE,
            Cleanups.enabledPasses(
                /* removeUnusedImports= */ true,
                /* reorderModifiers= */ true,
                /* sortImports= */ true));

    assertThat(result.contents()).doesNotContain("ArrayList");
    assertThat(result.contents()).contains("public static final int X = 1;");
    int expectedStart = result.contents().indexOf("public static final int X = 1;");
    int expectedEnd = result.contents().indexOf(";", expectedStart) + 1;
    assertThat(result.characterRanges()).contains(Range.closedOpen(expectedStart, expectedEnd));
  }
}
