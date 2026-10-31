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
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.googlejavaformat.java.JavaFormatterOptions.Style;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import org.junit.Test;

/// Tests for [Cleanups].
public class CleanupsTest {

  private static String formatWithCleanups(String input) throws Exception {
    Formatter formatter = new Formatter(JavaFormatterOptions.builder().cleanup(true).build());
    String output = formatter.formatSourceAndFixImports(input);
    assertThat(formatter.formatSourceAndFixImports(output)).isEqualTo(output);
    return output;
  }

  private static String formatWithCleanupsAosp(String input) throws Exception {
    Formatter formatter =
        new Formatter(JavaFormatterOptions.builder().style(Style.AOSP).cleanup(true).build());
    String output = formatter.formatSourceAndFixImports(input);
    assertThat(formatter.formatSourceAndFixImports(output)).isEqualTo(output);
    return output;
  }

  private static String runCli(String input, String... args) throws Exception {
    InputStream in = new ByteArrayInputStream(input.getBytes(UTF_8));
    StringWriter out = new StringWriter();
    Main main =
        new Main(
            new PrintWriter(out, true),
            new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.err, UTF_8)), true),
            in);
    String[] allArgs = Arrays.copyOf(args, args.length + 1);
    allArgs[args.length] = "-";
    assertThat(main.format(allArgs)).isEqualTo(0);
    return out.toString();
  }

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
  public void cleanupFlagAndOptions() throws Exception {
    String input =
        """
        class T {
          void f() {
            int x = 1;
            ;
          }
        }
        """;
    String cleaned =
        """
        class T {
          void f() {
            int x = 1;
          }
        }
        """;
    assertThat(new Formatter().formatSourceAndFixImports(input)).isEqualTo(cleaned);
    assertThat(runCli(input)).isEqualTo(cleaned);
    assertThat(runCli(input, "--cleanup")).isEqualTo(cleaned);
    assertThat(runCli(input, "--cleanup=true")).isEqualTo(cleaned);
    assertThat(
            new Formatter(JavaFormatterOptions.builder().cleanup(false).build())
                .formatSourceAndFixImports(input))
        .isEqualTo(input);
    assertThat(runCli(input, "--cleanup=false")).isEqualTo(input);
  }

  @Test
  public void singleStatementControlFlowNotBraced() throws Exception {
    String input =
        """
        class T {
          void f(boolean cond) {
            if (cond) doA();
          }
        }
        """;
    assertThat(formatWithCleanups(input)).isEqualTo(input);
  }

  @Test
  public void redundantSemicolons() throws Exception {
    String input =
        """
        package p;

        import java.util.List;;

        class T {
          ;
          int x = 1;;
          ;
          void f(List<String> xs) {
            int a = 1;;
            ;\s
        \t;\t
            // line comment
            ; int b = 2;
            switch (a) {
              case 1:
                ;
                break;;
              default:
                ;
            }
            while (xs.isEmpty());
            for (;;);
            if (xs.isEmpty()); ;
            label:
            ;
          };
        };
        """;
    String expected =
        """
        package p;

        import java.util.List;

        class T {
          int x = 1;

          void f(List<String> xs) {
            int a = 1;
            // line comment
            int b = 2;
            switch (a) {
              case 1:
                break;
              default:
            }
            while (xs.isEmpty())
              ;
            for (; ; )
              ;
            if (xs.isEmpty())
              ;
            label:
            ;
          }
        }
        """;
    assertThat(formatWithCleanups(input)).isEqualTo(expected);
    assertThat(formatWithCleanups("class T {}\n;")).isEqualTo("class T {}\n");
    assertThat(formatWithCleanups("package p;;\n")).isEqualTo("package p;\n");
  }

  @Test
  public void enumSemicolons() throws Exception {
    String input =
        """
        enum E1 {
          A;
        }

        enum E2 {
          A;;
        }

        enum E3 {
          A;;
          int x;;
        }

        enum E4 {
          A(() -> {
            int x = 1;
          }) {
            ;
          };;
          E4(Runnable r) {}
        }

        enum E5 {
          A;
          enum Nested {
            B
          }
        }

        enum E6 {
          A;
          int x;
          enum Nested {
            B
          }
        }
        """;
    String expected =
        """
        enum E1 {
          A;
        }

        enum E2 {
          A;
        }

        enum E3 {
          A;
          int x;
        }

        enum E4 {
          A(
              () -> {
                int x = 1;
              }) {};

          E4(Runnable r) {}
        }

        enum E5 {
          A;

          enum Nested {
            B
          }
        }

        enum E6 {
          A;
          int x;

          enum Nested {
            B
          }
        }
        """;
    assertThat(formatWithCleanups(input)).isEqualTo(expected);
  }

  @Test
  public void emptyAnnotationParens() throws Exception {
    String input =
        """
        @Anno()
        @pkg.Qual()
        @WithArg(1)
        @WithComment(/* keep */ )
        @Outer(@Inner())
        class T {
          @Anno()int x;
          @A()int y;
          void f(String @Anno()... xs) {}
        }
        """;
    String expected =
        """
        @Anno
        @pkg.Qual
        @WithArg(1)
        @WithComment(/* keep */ )
        @Outer(@Inner)
        class T {
          @Anno int x;
          @A int y;

          void f(String @Anno ... xs) {}
        }
        """;
    assertThat(formatWithCleanups(input)).isEqualTo(expected);
  }

  @Test
  public void singleParameterLambdaParens() throws Exception {
    String input =
        """
        import java.util.function.Function;
        import java.util.function.BiFunction;
        import java.util.function.Supplier;

        class T {
          Function<String, String> f1 = (x) -> x;
          Function<String, String> f2 = (_) -> "";
          Function<String, String> f3 = (String x) -> x;
          Function<String, String> f4 = (var x) -> x;
          Function<String, String> f5 = (@Anno var x) -> x;
          Function<String, String> f6 = (/* keep */ x) -> x;
          BiFunction<String, String, String> f7 = (x, y) -> x;
          Supplier<String> f8 = () -> "";
          Function<String, Function<String, String>> f9 = (x) -> (y) -> y;

          Function<String, String> g() {
            return(x) -> x;
          }
        }
        """;
    String expected =
        """
        import java.util.function.BiFunction;
        import java.util.function.Function;
        import java.util.function.Supplier;

        class T {
          Function<String, String> f1 = x -> x;
          Function<String, String> f2 = _ -> "";
          Function<String, String> f3 = (String x) -> x;
          Function<String, String> f4 = (var x) -> x;
          Function<String, String> f5 = (@Anno var x) -> x;
          Function<String, String> f6 = (/* keep */ x) -> x;
          BiFunction<String, String, String> f7 = (x, y) -> x;
          Supplier<String> f8 = () -> "";
          Function<String, Function<String, String>> f9 = x -> y -> y;

          Function<String, String> g() {
            return x -> x;
          }
        }
        """;
    assertThat(formatWithCleanups(input)).isEqualTo(expected);
  }

  @Test
  public void singleParameterLambdaParens_outsideRange() throws Exception {
    String input =
        """
        class T {
          Function<String, String> f = (x) -> {
            return   x;
          };
        }
        """;
    // Partial formatting of a lambda body reformats the enclosing statement, but the cleanup only
    // applies to the requested range, so the parentheses on the unrequested line are kept.
    String expected =
        """
        class T {
          Function<String, String> f =
              (x) -> {
                return x;
              };
        }
        """;
    int start = input.indexOf("return");
    int end = input.indexOf(';', start) + 1;
    assertThat(new Formatter().formatSource(input, ImmutableList.of(Range.closedOpen(start, end))))
        .isEqualTo(expected);
  }

  @Test
  public void aospStyleCleanups() throws Exception {
    String input =
        """
        import java.util.function.Function;

        @Anno()
        class T {
            int x;;
            void f() {
                Runnable r = () -> {};
                Function<String, String> fn = (s) -> s;
            }
        }
        """;
    String expected =
        """
        import java.util.function.Function;

        @Anno
        class T {
            int x;

            void f() {
                Runnable r = () -> {};
                Function<String, String> fn = s -> s;
            }
        }
        """;
    assertThat(formatWithCleanupsAosp(input)).isEqualTo(expected);
  }

  @Test
  public void cleanupWithinRequestedRange() throws Exception {
    String input =
        """
        class T {
          int x = 1;
          ;
          int y = 2;
          ;
        }
        """;
    int offset = input.indexOf(";\n  int y");
    String result = runCli(input, "--offset", Integer.toString(offset), "--length", "1");
    String expected =
        """
        class T {
          int x = 1;
          int y = 2;
          ;
        }
        """;
    assertThat(result).isEqualTo(expected);
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
                /* cleanup= */ false,
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
                /* cleanup= */ false,
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
                /* cleanup= */ false,
                /* removeUnusedImports= */ true,
                /* reorderModifiers= */ true,
                /* sortImports= */ true));

    assertThat(result.contents()).doesNotContain("ArrayList");
    assertThat(result.contents()).contains("public static final int X = 1;");
    int expectedStart = result.contents().indexOf("public static final int X = 1;");
    int expectedEnd = result.contents().indexOf(";", expectedStart) + 1;
    assertThat(result.characterRanges()).contains(Range.closedOpen(expectedStart, expectedEnd));
  }

  @Test
  public void parseReusedWhenLaterPassParsesAfterEarlierEdits() throws Exception {
    String input =
        """
        package test;

        import java.util.ArrayList;

        class Test {
          void f(boolean a) {
            if (a) {
              f(false);
            }
          }
        }
        """;
    Cleanups.Result result =
        Cleanups.apply(
            input,
            ImmutableList.of(Range.closedOpen(0, input.length())),
            Style.GOOGLE,
            Cleanups.enabledPasses(
                /* cleanup= */ true,
                /* removeUnusedImports= */ true,
                /* reorderModifiers= */ false,
                /* sortImports= */ false));
    assertThat(result.unit()).isNotNull();
    assertThat(result.contents()).doesNotContain("ArrayList");
    assertThat(result.contents()).contains("if (a) {");
  }

  @Test
  public void emptyString() throws Exception {
    assertThat(new Formatter().formatSource("")).isEqualTo("\n");
    assertThat(new Formatter().formatSource("", ImmutableList.of())).isEmpty();
    assertThat(new Formatter().formatSourceAndFixImports("")).isEqualTo("\n");
    assertThat(new Formatter().formatSourceAndFixImports("import java.util.List;\n"))
        .isEqualTo("\n");
  }
}
