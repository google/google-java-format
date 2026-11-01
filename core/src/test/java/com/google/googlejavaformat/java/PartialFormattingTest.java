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

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.google.common.base.Joiner;
import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ObjectArrays;
import com.google.common.collect.Range;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/// Tests formatting parts of files.
@RunWith(Parameterized.class)
public final class PartialFormattingTest {

  @Parameters
  public static Iterable<Object[]> parameters() {
    return ImmutableList.copyOf(new Object[][] {{"\n"}, {"\r"}, {"\r\n"}});
  }

  @Rule public TemporaryFolder testFolder = new TemporaryFolder();

  private final String newline;

  public PartialFormattingTest(String newline) {
    this.newline = newline;
  }

  String lines(String... args) {
    return Joiner.on(newline).join(args);
  }

  @Test
  public void testGetFormatReplacements0() throws Exception {
    String input =
        lines(
            /* line 0 character  0 */ "class Foo{",
            /* line 1 character 11 */ "void f",
            /* line 2 character 18 */ "() {",
            /* line 3 character 23 */ "}",
            /* line 4 character 25 */ "}",
            "");
    String expectedOutput =
        lines(

            /* line 0 character  0 */ "class Foo{",
            /* line 1 character 11 */ "  void f() {}",
            /* line 2 character 25 */ "}",
            "");
    // Claim to have modified the parentheses.
    int start = input.indexOf("() {");
    String output = doGetFormatReplacements(input, start, start + 1);
    assertThat(output).isEqualTo(expectedOutput);
  }

  @Test
  public void testGetFormatReplacements1() throws Exception {
    String input =
        lines(
            /* line 0 character  0 */ "class Foo{",
            /* line 1 character 11 */ "void f",
            /* line 2 character 18 */ "() {",
            /* line 3 character 23 */ "}",
            /* line 4 character 25 */ "}",
            "");
    String expectedOutput =
        lines(
            /* line 0 character  0 */ "class Foo{",
            /* line 1 character 11 */ "  void f() {}",
            /* line 2 character 25 */ "}",
            "");
    // Claim to have modified everything after the parentheses.
    String output = doGetFormatReplacements(input, 20, 21);
    assertThat(output).isEqualTo(expectedOutput);
  }

  @Test
  public void expandToStatement() throws Exception {
    String input =
        lines(
            "class Foo {{",
            "ImmutableList<Integer> ids = ImmutableList.builder()",
            ".add(1)",
            ".add(2)",
            ".add(3)",
            ".build();",
            "}}",
            "");
    String expectedOutput =
        lines(
            "class Foo {{",
            "    ImmutableList<Integer> ids ="
                + " ImmutableList.builder().add(1).add(2).add(3).build();",
            "}}",
            "");
    int idx = input.indexOf("add(2)");
    String output = doGetFormatReplacements(input, idx, idx + 1);
    assertThat(output).isEqualTo(expectedOutput);
  }

  @Test
  public void expandToMethodSignature() throws Exception {
    String input =
        lines(
            "class Foo {",
            "void ",
            " m() ",
            " {",
            "ImmutableList<Integer> ids = ImmutableList.builder()",
            ".add(1)",
            ".add(2)",
            ".build();",
            "}}",
            "");
    String expectedOutput =
        lines(
            "class Foo {",
            "  void m() {",
            "    ImmutableList<Integer> ids = ImmutableList.builder()",
            "    .add(1)",
            "    .add(2)",
            "    .build();",
            "}}",
            "");
    String expectedNoReindent =
        lines(
            "class Foo {",
            "  void m() {",
            "ImmutableList<Integer> ids = ImmutableList.builder()",
            ".add(1)",
            ".add(2)",
            ".build();",
            "}}",
            "");
    int idx = input.indexOf("void");
    assertThat(doGetFormatReplacements(input, idx, idx + 1)).isEqualTo(expectedOutput);
    assertThat(doGetFormatReplacements(input, idx, idx + 1, /* cleanup= */ false))
        .isEqualTo(expectedNoReindent);
  }

  @Test
  public void expandToClassSignature() throws Exception {
    String input =
        lines(
            "class",
            "Foo<XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX,"
                + "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX,"
                + "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX> {",
            "void ",
            " m() ",
            " {",
            "}}",
            "");
    String expectedOutput =
        lines(
            "class Foo<",
            "    XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX,",
            "    XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX,",
            "    XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX> {",
            "  void ",
            "   m() ",
            "   {",
            "  }}",
            "");
    String expectedNoReindent =
        lines(
            "class Foo<",
            "    XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX,",
            "    XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX,",
            "    XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX> {",
            "void ",
            " m() ",
            " {",
            "}}",
            "");
    int idx = input.indexOf("class");
    assertThat(doGetFormatReplacements(input, idx, idx + 1)).isEqualTo(expectedOutput);
    assertThat(doGetFormatReplacements(input, idx, idx + 1, /* cleanup= */ false))
        .isEqualTo(expectedNoReindent);
  }

  @Test
  public void ignoreNeighbouringStatements() throws Exception {
    String input =
        lines(
            "class Test {",
            "int ",
            " xxx ",
            " = 1;",
            "int ",
            " yyy ",
            " = 1;",
            "int ",
            " zzz ",
            " = 1;",
            "}",
            "");
    String expectedOutput =
        lines(
            "class Test {",
            "  int ",
            "   xxx ",
            "   = 1;",
            "  int yyy = 1;",
            "  int ",
            "   zzz ",
            "   = 1;",
            "}",
            "");
    String expectedNoReindent =
        lines(
            "class Test {",
            "int ",
            " xxx ",
            " = 1;",
            "  int yyy = 1;",
            "int ",
            " zzz ",
            " = 1;",
            "}",
            "");
    int idx = input.indexOf("yyy");
    assertThat(doGetFormatReplacements(input, idx, idx + 1)).isEqualTo(expectedOutput);
    assertThat(doGetFormatReplacements(input, idx, idx + 1, /* cleanup= */ false))
        .isEqualTo(expectedNoReindent);
  }

  @Test
  public void insertLeadingNewlines() throws Exception {
    String input =
        lines(
            "class Test { int xxx = 1; int yyy = 1; int zzz = 1; }", //
            "");
    String expectedOutput =
        lines(
            "class Test { int xxx = 1;", //
            "  int yyy = 1;",
            "  int zzz = 1; }",
            "");
    int idx = input.indexOf("yyy");
    String output = doGetFormatReplacements(input, idx, idx + 1);
    assertThat(output).isEqualTo(expectedOutput);
  }

  @Test
  public void insertLeadingNewlines2() throws Exception {
    String input =
        lines(
            "class Test { int xxx = 1;", //
            "",
            "         int yyy = 1; int zzz = 1; }");
    String expectedOutput =
        lines(
            "class Test { int xxx = 1;", //
            "", //
            "  int yyy = 1;",
            "  int zzz = 1; }");
    int idx = input.indexOf("yyy");
    String output = doGetFormatReplacements(input, idx, idx + 1);
    assertThat(output).isEqualTo(expectedOutput);
  }

  @Test
  public void insertTrailingNewlines() throws Exception {
    String input =
        lines(
            "class Test { int xxx = 1;", //
            "  int yyy = 1;      int zzz = 1; }");
    String expectedOutput =
        lines(
            "class Test { int xxx = 1;", //
            "  int yyy = 1;",
            "  int zzz = 1; }");
    int idx = input.indexOf("yyy");
    String output = doGetFormatReplacements(input, idx, idx + 1);
    assertThat(output).isEqualTo(expectedOutput);
  }

  @Test
  public void rejoinMethodSignatureLines() throws Exception {
    String input =
        lines(
            "class Test { void zzz", //
            "() { int x; } }");
    String expectedOutput =
        lines(
            "class Test {", //
            "  void zzz() {",
            "    int x; } }");
    int idx = input.indexOf("zzz");
    String output = doGetFormatReplacements(input, idx, idx);
    assertThat(output).isEqualTo(expectedOutput);
  }

  @Test
  public void formatTrailingBrace() throws Exception {
    String input =
        lines(
            "class Test { void f() { return; } }", //
            "");
    String expectedOutput =
        lines(
            "class Test { void f() { return;", //
            "  }",
            "}",
            "");
    int idx = input.indexOf("}");
    String output = doGetFormatReplacements(input, idx, idx);
    assertThat(output).isEqualTo(expectedOutput);
  }

  @Test
  public void formatTrailingBraceEmptyMethodBody() throws Exception {
    String input =
        lines(
            "class Test { void f() {} }", //
            "");
    String expectedOutput =
        lines(
            "class Test {", //
            "  void f() {}",
            "}",
            "");
    int idx = input.indexOf("}");
    String output = doGetFormatReplacements(input, idx, idx);
    assertThat(output).isEqualTo(expectedOutput);
  }

  @Test
  public void formatTrailingBraceEmptyClassBody() throws Exception {
    String input =
        lines(
            "class Test { int x; }", //
            "");
    String expectedOutput =
        lines(
            "class Test { int x;", //
            "}",
            "");
    int idx = input.indexOf("}");
    String output = doGetFormatReplacements(input, idx, idx);
    assertThat(output).isEqualTo(expectedOutput);
  }

  @Test
  public void formatTrailingBraceEmptyClassBody2() throws Exception {
    String input =
        lines(
            "class Test {", //
            "}",
            "");
    String expectedOutput =
        lines(
            "class Test {}", //
            "");
    int idx = input.indexOf("}");
    String output = doGetFormatReplacements(input, idx, idx);
    assertThat(output).isEqualTo(expectedOutput);
  }

  @Test
  public void onlyPackage() throws Exception {
    String input =
        lines(
            "package", //
            "test",
            ";",
            "class Test {}",
            "");
    String expectedOutput =
        lines(
            "package test;", //
            "",
            "class Test {}",
            "");
    int idx = input.indexOf("test");
    String output = doGetFormatReplacements(input, idx, idx);
    assertThat(output).isEqualTo(expectedOutput);
  }

  private static String doGetFormatReplacements(String input, int characterILo, int characterIHi)
      throws Exception {
    return doGetFormatReplacements(input, characterILo, characterIHi, /* cleanup= */ true);
  }

  private static String doGetFormatReplacements(
      String input, int characterILo, int characterIHi, boolean cleanup) throws Exception {
    return new Formatter(JavaFormatterOptions.builder().cleanup(cleanup).build())
        .formatSource(input, ImmutableList.of(Range.closedOpen(characterILo, characterIHi + 1)));
  }

  @Test
  public void testLength() throws Exception {
    String input =
        lines(
            "class Foo{", //
            "int xxx;",
            "int yyy;",
            "int zzz;",
            "}",
            "");
    String expectedOutput =
        lines(
            "class Foo{", //
            "  int xxx;",
            "  int yyy;",
            "  int zzz;",
            "}",
            "");
    String expectedNoReindent =
        lines(
            "class Foo{", //
            "int xxx;",
            "  int yyy;",
            "int zzz;",
            "}",
            "");

    assertThat(formatMain(input, "-lines", "3")).isEqualTo(expectedOutput);
    assertThat(formatMain(input, "--cleanup=false", "-lines", "3")).isEqualTo(expectedNoReindent);
  }

  @Test
  public void testLengthRange() throws Exception {
    String input =
        lines(
            "class Foo{", //
            "int xxx;",
            "int yyy;",
            "int zzz;",
            "}",
            "");
    String expectedOutput =
        lines(
            "class Foo{", //
            "  int xxx;",
            "  int yyy;",
            "  int zzz;",
            "}",
            "");
    String expectedNoReindent =
        lines(
            "class Foo{", //
            "int xxx;",
            "  int yyy;",
            "  int zzz;",
            "}",
            "");

    assertThat(formatMain(input, "-lines", "3:4")).isEqualTo(expectedOutput);
    assertThat(formatMain(input, "--cleanup=false", "-lines", "3:4")).isEqualTo(expectedNoReindent);
  }

  @Test
  public void statementAndComments() throws Exception {
    String input =
        lines(
            "public class MyTest {",
            "{",
            "// asd",
            "int x = 1;",
            "// asd",
            "int y = 2;",
            "// asd",
            "int z = 3;",
            "// asd",
            "}",
            "}",
            "",
            "");
    String expectedOutput =
        lines(
            "public class MyTest {",
            "  {",
            "    // asd",
            "    int x = 1;",
            "    // asd",
            "    int y = 2;",
            "    // asd",
            "    int z = 3;",
            "    // asd",
            "  }",
            "}",
            "",
            "");
    String expectedNoReindent =
        lines(
            "public class MyTest {",
            "{",
            "// asd",
            "int x = 1;",
            "    // asd",
            "    int y = 2;",
            "// asd",
            "int z = 3;",
            "// asd",
            "}",
            "}",
            "",
            "");

    assertThat(formatMain(input, "-lines", "5")).isEqualTo(expectedOutput);
    assertThat(formatMain(input, "--cleanup=false", "-lines", "5")).isEqualTo(expectedNoReindent);
  }

  @Test
  public void statementAndComments2() throws Exception {
    String input =
        lines(
            "public class MyTest {",
            "{",
            "// asd",
            "int x = 1;",
            "// asd",
            "int y = 2;",
            "// asd",
            "int z = 3;",
            "// asd",
            "}",
            "}",
            "",
            "");
    String expectedOutput =
        lines(
            "public class MyTest {",
            "  {",
            "    // asd",
            "    int x = 1;",
            "    // asd",
            "    int y = 2;",
            "    // asd",
            "    int z = 3;",
            "    // asd",
            "  }",
            "}",
            "",
            "");
    String expectedNoReindent =
        lines(
            "public class MyTest {",
            "{",
            "// asd",
            "int x = 1;",
            "    // asd",
            "    int y = 2;",
            "// asd",
            "int z = 3;",
            "// asd",
            "}",
            "}",
            "",
            "");

    assertThat(formatMain(input, "-lines", "6")).isEqualTo(expectedOutput);
    assertThat(formatMain(input, "--cleanup=false", "-lines", "6")).isEqualTo(expectedNoReindent);
  }

  @Test
  public void statementAndComments3() throws Exception {
    String input =
        lines(
            "public class MyTest {",
            "{",
            "// asd",
            "int x = 1;",
            "// asd",
            "int y = 2;",
            "// asd",
            "int z = 3;",
            "// asd",
            "}",
            "}",
            "",
            "");
    String expectedOutput =
        lines(
            "public class MyTest {",
            "  {",
            "    // asd",
            "    int x = 1;",
            "    // asd",
            "    int y = 2;",
            "    // asd",
            "    int z = 3;",
            "    // asd",
            "  }",
            "}",
            "",
            "");
    String expectedNoReindent =
        lines(
            "public class MyTest {",
            "{",
            "// asd",
            "int x = 1;",
            "// asd",
            "int y = 2;",
            "    // asd",
            "    int z = 3;",
            "// asd",
            "}",
            "}",
            "",
            "");

    assertThat(formatMain(input, "-lines", "7")).isEqualTo(expectedOutput);
    assertThat(formatMain(input, "--cleanup=false", "-lines", "7")).isEqualTo(expectedNoReindent);
  }

  @Test
  public void blankAndComment() throws Exception {
    String input =
        lines(
            "public class MyTest {",
            "  public void testListDefinitions() throws Exception {",
            "    definitionService.insert(createDefinition(1));",
            "    definitionService.insert(createIncrementalDefinition(2));",
            "    definitionService.insert(createDefinition(3));",
            "    definitionService.insert(createIncrementalDefinition(4));",
            "",
            "    // No maxResults",
            "    assertThat(achievementFirstPartyHelper.listDefinitionsByApplication(",
            "            STUB_GAIA_ID, STUB_APPLICATION_ID, Optional.<Integer>absent(),",
            "           "
                + " Optional.<String>absent()).getAchievements()).containsExactly(createExpectedDefinition(1),"
                + " createIncrementalExpectedDefinition(2), createExpectedDefinition(3),"
                + " createIncrementalExpectedDefinition(4)).inOrder();",
            "  }",
            "}",
            "",
            "");
    String expectedOutput =
        lines(
            "public class MyTest {",
            "  public void testListDefinitions() throws Exception {",
            "    definitionService.insert(createDefinition(1));",
            "    definitionService.insert(createIncrementalDefinition(2));",
            "    definitionService.insert(createDefinition(3));",
            "    definitionService.insert(createIncrementalDefinition(4));",
            "",
            "    // No maxResults",
            "    assertThat(",
            "            achievementFirstPartyHelper",
            "                .listDefinitionsByApplication(",
            "                    STUB_GAIA_ID,",
            "                    STUB_APPLICATION_ID,",
            "                    Optional.<Integer>absent(),",
            "                    Optional.<String>absent())",
            "                .getAchievements())",
            "        .containsExactly(",
            "            createExpectedDefinition(1),",
            "            createIncrementalExpectedDefinition(2),",
            "            createExpectedDefinition(3),",
            "            createIncrementalExpectedDefinition(4))",
            "        .inOrder();",
            "  }",
            "}",
            "",
            "");

    String toFormat =
        lines(
            "    assertThat(achievementFirstPartyHelper.listDefinitionsByApplication(", //
            "");
    int idx = input.indexOf(toFormat);
    String output = doGetFormatReplacements(input, idx, idx + toFormat.length());
    assertThat(output).isEqualTo(expectedOutput);
  }

  @Test
  public void emptyFile() throws Exception {
    new Formatter().formatSource("");
    new Formatter()
        .formatSource(
            lines(
                "", //
                ""),
            ImmutableList.of(Range.closedOpen(0, 1)));
  }

  @Test
  public void testGetFormatReplacementRanges() throws Exception {
    String input =
        lines(
            /* line 0 character  0 */ "class Foo{",
            /* line 1 character 11 */ "void f",
            /* line 2 character 18 */ "() {",
            /* line 3 character 23 */ "}",
            /* line 4 character 25 */ "}",
            "");
    // Claim to have modified the parentheses.
    int start = input.indexOf("() {");
    ImmutableList<Replacement> ranges =
        new Formatter()
            .getFormatReplacements(input, ImmutableList.of(Range.closedOpen(start, start + 1)));
    assertThat(ranges).hasSize(1);
    Replacement replacement = ranges.get(0);
    assertThat(replacement.replacementString())
        .isEqualTo(
            lines(
                "", //
                "  void f() {}"));
    int replaceFrom = input.indexOf("void f") - newline.length();
    assertThat(replacement.replaceRange().lowerEndpoint()).isEqualTo(replaceFrom);
  }

  @Test
  public void noTokensOnLine() throws Exception {
    String input =
        lines(
            "    package com.google.googlejavaformat.java;",
            "",
            "/*",
            " * Copyright 2015 Google Inc.",
            " *",
            " * Licensed under the Apache License, Version 2.0 (the \"License\"); you may not",
            " * in compliance with the License. You may obtain a copy of the License at",
            " *",
            " *     http://www.apache.org/licenses/LICENSE-2.0",
            " *",
            " * Unless required by applicable law or agreed to in writing, software distribute",
            " * is distributed on an \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY",
            " * or implied. See the License for the specific language governing permissions an",
            " * the License.",
            " */",
            "",
            "import com.google.googlejavaformat.FormatterDiagnostic;",
            "import java.util.List;",
            "",
            "/** Checked exception class for formatter errors. */",
            "public final class FormatterException extends Exception {",
            "",
            "  FormatterException(String message) {",
            "    super(message);",
            "  }",
            "",
            "  /**",
            "   * @param errors",
            "   */",
            "  public FormatterException(List<FormatterDiagnostic> errors) {",
            "    // TODO(cushon): Auto-generated constructor stub",
            "  }",
            "}");

    Path tmpdir = testFolder.newFolder().toPath();
    Path path = tmpdir.resolve("FormatterException.java");
    Files.write(path, input.getBytes(UTF_8));

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();

    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true), System.in);
    String[] args = {"-lines", "3:4", path.toString()};
    assertThat(main.format(args)).isEqualTo(0);
    assertThat(out.toString()).isEqualTo(input.substring(4));

    StringWriter outNoReindent = new StringWriter();
    Main mainNoReindent =
        new Main(new PrintWriter(outNoReindent, true), new PrintWriter(err, true), System.in);
    assertThat(mainNoReindent.format("-lines", "3:4", "--cleanup=false", path.toString()))
        .isEqualTo(0);
    assertThat(outNoReindent.toString()).isEqualTo(input);
  }

  @Test
  public void nestedStatement1() throws Exception {
    String input =
        lines(
            "public class MyTest {{",
            "int x = ",
            " 1;",
            "int y = new Runnable() {",
            "  void run() {",
            "    System.err.println(42);",
            "  }",
            "};",
            "int z = ",
            " 1;",
            "}}",
            "",
            "");
    String expectedOutput =
        lines(
            "public class MyTest {{",
            "    int x = ",
            "     1;",
            "    int y =",
            "        new Runnable() {",
            "          void run() {",
            "            System.err.println(42);",
            "          }",
            "        };",
            "    int z = ",
            "     1;",
            "}}",
            "",
            "");
    String expectedOutputNoReindent =
        lines(
            "public class MyTest {{",
            "int x = ",
            " 1;",
            "    int y =",
            "        new Runnable() {",
            "          void run() {",
            "            System.err.println(42);",
            "          }",
            "        };",
            "int z = ",
            " 1;",
            "}}",
            "",
            "");

    String toFormat = "Runnable";
    int idx = input.indexOf(toFormat);
    assertThat(doGetFormatReplacements(input, idx, idx + toFormat.length()))
        .isEqualTo(expectedOutput);
    assertThat(doGetFormatReplacements(input, idx, idx + toFormat.length(), /* cleanup= */ false))
        .isEqualTo(expectedOutputNoReindent);
  }

  @Test
  public void nestedStatement2() throws Exception {
    String input =
        lines(
            "public class MyTest {",
            "int x = ",
            " 1;",
            "int y = new Runnable() {",
            "  void run() {",
            "    System.err.println(42);",
            "  }",
            "};",
            "int z = ",
            " 1;",
            "}",
            "",
            "");
    String expectedOutput =
        lines(
            "public class MyTest {",
            "  int x = ",
            "   1;",
            "  int y =",
            "      new Runnable() {",
            "        void run() {",
            "          System.err.println(42);",
            "        }",
            "      };",
            "  int z = ",
            "   1;",
            "}",
            "",
            "");
    String expectedOutputNoReindent =
        lines(
            "public class MyTest {",
            "int x = ",
            " 1;",
            "  int y =",
            "      new Runnable() {",
            "        void run() {",
            "          System.err.println(42);",
            "        }",
            "      };",
            "int z = ",
            " 1;",
            "}",
            "",
            "");

    String toFormat = "Runnable";
    int idx = input.indexOf(toFormat);
    assertThat(doGetFormatReplacements(input, idx, idx + toFormat.length()))
        .isEqualTo(expectedOutput);
    assertThat(doGetFormatReplacements(input, idx, idx + toFormat.length(), /* cleanup= */ false))
        .isEqualTo(expectedOutputNoReindent);
  }

  @Test
  public void blankLine() throws Exception {
    String input =
        lines(
            "public class MyTest {", //
            "int x = 1;",
            "",
            "int y = 1;",
            "}",
            "",
            "");
    String expectedOutput = input;

    testFormatLine(input, expectedOutput, 3);
  }

  @Test
  public void lineWithIdentifier() throws Exception {
    String input =
        lines(
            "public class MyTest {", //
            "int",
            "y",
            "= 1;",
            "}",
            "",
            "");
    String expectedOutput =
        lines(
            "public class MyTest {", //
            "  int y = 1;",
            "}",
            "",
            "");

    testFormatLine(input, expectedOutput, 3);
  }

  // formatted region expands to include entire comment
  @Test
  public void lineInsideComment() throws Exception {
    String input =
        lines(
            "public class MyTest {",
            "/* This is a",
            "            poorly indented",
            "                       comment*/",
            "int x;",
            "}",
            "",
            "");
    String expectedOutput =
        lines(
            "public class MyTest {",
            "  /* This is a",
            "  poorly indented",
            "             comment*/",
            "  int x;",
            "}",
            "",
            "");

    testFormatLine(input, expectedOutput, 3);
  }

  @Test
  public void testReplacementsSorted() throws Exception {
    String input =
        lines(
            "class Test {",
            "int a = 1;",
            "int b = 2;",
            "int c = 3;",
            "int d = 4;",
            "int e = 5;",
            "}");
    List<Range<Integer>> ranges = new ArrayList<>();
    for (int i = 1; i <= 5; i += 2) {
      int idx = input.indexOf(String.valueOf(i));
      ranges.add(Range.closedOpen(idx, idx + 1));
    }

    ImmutableList<Replacement> replacements = new Formatter().getFormatReplacements(input, ranges);

    // expect replacements in ascending order, by start position
    List<Integer> startPositions = new ArrayList<>();
    for (Replacement replacement : replacements) {
      startPositions.add(replacement.replaceRange().lowerEndpoint());
    }
    assertThat(startPositions).hasSize(3);
    assertThat(startPositions).isInStrictOrder();

    ImmutableList<Replacement> replacementsWithReindent =
        new Formatter().getFormatReplacements(input, ranges, /* reindent= */ true);
    List<Integer> startPositionsWithReindent = new ArrayList<>();
    for (Replacement replacement : replacementsWithReindent) {
      startPositionsWithReindent.add(replacement.replaceRange().lowerEndpoint());
    }
    assertThat(startPositionsWithReindent).hasSize(5);
    assertThat(startPositionsWithReindent).isInStrictOrder();
  }

  @Test
  public void testReplacementsSorted_DescendingInput() throws Exception {
    String input =
        lines(
            "class Test {",
            "int a = 1;",
            "int b = 2;",
            "int c = 3;",
            "int d = 4;",
            "int e = 5;",
            "}");
    List<Range<Integer>> ranges = new ArrayList<>();
    for (int i = 5; i >= 1; i -= 2) {
      int idx = input.indexOf(String.valueOf(i));
      ranges.add(Range.closedOpen(idx, idx + 1));
    }

    ImmutableList<Replacement> replacements = new Formatter().getFormatReplacements(input, ranges);

    // expect replacements in ascending order, by start position
    List<Integer> startPositions = new ArrayList<>();
    for (Replacement replacement : replacements) {
      startPositions.add(replacement.replaceRange().lowerEndpoint());
    }
    assertThat(startPositions).hasSize(3);
    assertThat(startPositions).isInStrictOrder();

    ImmutableList<Replacement> replacementsWithReindent =
        new Formatter().getFormatReplacements(input, ranges, /* reindent= */ true);
    List<Integer> startPositionsWithReindent = new ArrayList<>();
    for (Replacement replacement : replacementsWithReindent) {
      startPositionsWithReindent.add(replacement.replaceRange().lowerEndpoint());
    }
    assertThat(startPositionsWithReindent).hasSize(5);
    assertThat(startPositionsWithReindent).isInStrictOrder();
  }

  private void testFormatLine(String input, String expectedOutput, int i) throws Exception {
    Path tmpdir = testFolder.newFolder().toPath();
    Path path = tmpdir.resolve("Foo.java");
    Files.write(path, input.getBytes(UTF_8));

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();

    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true), System.in);
    String[] args = {"-lines", Integer.toString(i), path.toString()};
    assertThat(main.format(args)).isEqualTo(0);
    assertThat(out.toString()).isEqualTo(expectedOutput);
  }

  @Test
  public void lineWithTrailingComment() throws Exception {
    String input =
        lines(
            "class Foo{", //
            "int xxx; // asd",
            "}",
            "");
    String expectedOutput =
        lines(
            "class Foo{", //
            "  int xxx; // asd",
            "}",
            "");

    Path tmpdir = testFolder.newFolder().toPath();
    Path path = tmpdir.resolve("Foo.java");
    Files.write(path, input.getBytes(UTF_8));

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();

    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true), System.in);
    String[] args = {"-lines", "2", path.toString()};
    assertThat(main.format(args)).isEqualTo(0);
    assertThat(out.toString()).isEqualTo(expectedOutput);
  }

  // Nested statements are OK as long as they're nested inside "block-like" constructs.
  @Test
  public void nestedStatement_allowPartial() throws Exception {
    String input =
        lines(
            "public class MyTest {{",
            "if (true) {",
            "if (true) {",
            "System.err.println(\"Hello\");",
            "} else {",
            "System.err.println(\"Goodbye\");",
            "}",
            "}",
            "}}",
            "");
    String expectedOutput =
        lines(
            "public class MyTest {{",
            "    if (true) {",
            "      if (true) {",
            "        System.err.println(\"Hello\");",
            "      } else {",
            "        System.err.println(\"Goodbye\");",
            "      }",
            "    }",
            "}}",
            "");
    String expectedOutputNoReindent =
        lines(
            "public class MyTest {{",
            "if (true) {",
            "if (true) {",
            "        System.err.println(\"Hello\");",
            "} else {",
            "System.err.println(\"Goodbye\");",
            "}",
            "}",
            "}}",
            "");

    String toFormat = "Hello";
    int idx = input.indexOf(toFormat);
    assertThat(doGetFormatReplacements(input, idx, idx + toFormat.length()))
        .isEqualTo(expectedOutput);
    assertThat(doGetFormatReplacements(input, idx, idx + toFormat.length(), /* cleanup= */ false))
        .isEqualTo(expectedOutputNoReindent);
  }

  // regression test for b/b22196513
  @Test
  public void noTrailingWhitespace() throws Exception {
    String input =
        lines(
            "", //
            "class Test {",
            "  {",
            "    {",
            "      {",
            "      }",
            "    }",
            "}}",
            "");
    String expected =
        lines(
            "", //
            "class Test {",
            "  {",
            "    {",
            "      {",
            "      }",
            "    }",
            "  }",
            "}",
            "");
    int start = input.indexOf(newline + "}}");
    ImmutableList<Range<Integer>> ranges =
        ImmutableList.of(Range.closedOpen(start, start + newline.length() + 2));
    String output = new Formatter().formatSource(input, ranges);
    assertEquals("bad output", expected, output);
  }

  // regression test for b/b22196513
  @Test
  public void trailingNonBreakingWhitespace() throws Exception {
    String input =
        lines(
            "", //
            "class Test {",
            "  {",
            "    int x;int y;",
            "  }",
            "}",
            "");
    String expected =
        lines(
            "", //
            "class Test {",
            "  {",
            "    int x;",
            "    int y;",
            "  }",
            "}",
            "");
    String match = "int x;";
    int start = input.indexOf(match);
    int end = start + match.length();
    ImmutableList<Range<Integer>> ranges = ImmutableList.of(Range.closedOpen(start, end));
    String output = new Formatter().formatSource(input, ranges);
    assertEquals("bad output", expected, output);
  }

  @Test
  public void outOfRangeStartLine() throws Exception {
    String input =
        lines(
            "class Foo {", //
            "int x = 1;",
            "}");
    String expectedOutput =
        lines(
            "class Foo {", //
            "  int x = 1;",
            "}",
            "");

    Path tmpdir = testFolder.newFolder().toPath();
    Path path = tmpdir.resolve("Foo.java");
    Files.write(path, input.getBytes(UTF_8));

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();

    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true), System.in);
    String[] args = {"-lines", "-1:3", path.toString()};
    assertThat(main.format(args)).isEqualTo(0);
    assertThat(out.toString()).isEqualTo(expectedOutput);
  }

  @Test
  public void outOfRangeEndLine() throws Exception {
    String input =
        lines(
            "class Foo {", //
            "int x = 1;",
            "}");
    String expectedOutput =
        lines(
            "class Foo {", //
            "  int x = 1;",
            "}",
            "");

    Path tmpdir = testFolder.newFolder().toPath();
    Path path = tmpdir.resolve("Foo.java");
    Files.write(path, input.getBytes(UTF_8));

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();

    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true), System.in);
    String[] args = {"-lines", "1:5", path.toString()};
    assertThat(main.format(args)).isEqualTo(0);
    assertThat(out.toString()).isEqualTo(expectedOutput);
  }

  @Test
  public void testOutOfRangeLines() throws Exception {
    String input =
        lines(
            "class Foo {", //
            "}",
            "");
    String expectedOutput =
        lines(
            "class Foo {}", //
            "");

    Path tmpdir = testFolder.newFolder().toPath();
    Path path = tmpdir.resolve("Foo.java");
    Files.write(path, input.getBytes(UTF_8));

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();

    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true), System.in);
    String[] args = {"-lines=23:27", "-lines=31:35", "-lines=52:63", "-lines=1:1", path.toString()};
    assertThat(main.format(args)).isEqualTo(0);
    assertThat(out.toString()).isEqualTo(expectedOutput);
  }

  @Test
  public void testEmptyFirstLine() throws Exception {
    String input =
        lines(
            "", //
            "",
            "class Foo {",
            "}",
            "");

    Path tmpdir = testFolder.newFolder().toPath();
    Path path = tmpdir.resolve("Foo.java");
    Files.write(path, input.getBytes(UTF_8));

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();

    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true), System.in);
    String[] args = {"-lines=1:1", path.toString()};
    assertThat(main.format(args)).isEqualTo(0);
  }

  @Test
  public void testEmptyLastLine() throws Exception {
    String input =
        lines(
            "class Foo {", //
            "}",
            "",
            "");

    Path tmpdir = testFolder.newFolder().toPath();
    Path path = tmpdir.resolve("Foo.java");
    Files.write(path, input.getBytes(UTF_8));

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();

    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true), System.in);
    String[] args = {"-lines=5:5", path.toString()};
    assertThat(main.format(args)).isEqualTo(0);
  }

  // Regression test for b/22872933
  // Don't extend partial formatting ranges across switch cases.
  @Test
  public void switchCase() throws Exception {
    String input =
        lines(
            "class Test {",
            "  {",
            "    switch (foo) {",
            "      case FOO:",
            "      f();",
            "      break;",
            "      case BAR:",
            "      g();",
            "      break;",
            "    }",
            "  }",
            "}");
    String expectedOutput =
        lines(
            "class Test {",
            "  {",
            "    switch (foo) {",
            "      case FOO:",
            "        f();",
            "        break;",
            "      case BAR:",
            "        g();",
            "        break;",
            "    }",
            "  }",
            "}");
    String expectedOutputNoReindent =
        lines(
            "class Test {",
            "  {",
            "    switch (foo) {",
            "      case FOO:",
            "        f();",
            "      break;",
            "      case BAR:", // we deliberately only format the first case
            "      g();",
            "      break;",
            "    }",
            "  }",
            "}");

    int idx = input.indexOf("f()");
    assertThat(doGetFormatReplacements(input, idx, idx + 1)).isEqualTo(expectedOutput);
    assertThat(doGetFormatReplacements(input, idx, idx + 1, /* cleanup= */ false))
        .isEqualTo(expectedOutputNoReindent);
  }

  // regression test for b/23349153
  @Test
  public void emptyStatement() throws Exception {
    String input =
        lines(
            "class Test {{", //
            "Object o = f();;",
            "}}",
            "");
    String expectedOutput =
        lines(
            "class Test {{", //
            "    Object o = f();",
            "    ;",
            "}}",
            "");
    int idx = input.indexOf("Object o");
    String output = doGetFormatReplacements(input, idx, idx + 1);
    assertThat(output).isEqualTo(expectedOutput);
  }

  @Test
  public void preserveTrailingWhitespaceAfterNewline() throws Exception {
    String input =
        lines(
            "class Test {{", //
            "Object o = f();       ",
            "            int x;",
            "}}",
            "");
    String expectedOutput =
        lines(
            "class Test {{", //
            "    Object o = f();",
            "    int x;",
            "}}",
            "");
    String expectedOutputNoReindent =
        lines(
            "class Test {{", //
            "    Object o = f();",
            "            int x;",
            "}}",
            "");
    int idx = input.indexOf("Object o");
    assertThat(doGetFormatReplacements(input, idx, idx + 1)).isEqualTo(expectedOutput);
    assertThat(doGetFormatReplacements(input, idx, idx + 1, /* cleanup= */ false))
        .isEqualTo(expectedOutputNoReindent);
  }

  @Test
  public void trailingWhitespace() throws Exception {
    String input =
        lines(
            "class Test {{", //
            "Object o = f();       ",
            "            ;",
            "}}",
            "");
    String expectedOutput =
        lines(
            "class Test {{", //
            "    Object o = f();",
            "    ;",
            "}}",
            "");
    String expectedOutputNoReindent =
        lines(
            "class Test {{", //
            "    Object o = f();",
            "            ;",
            "}}",
            "");
    int idx = input.indexOf("Object o");
    assertThat(doGetFormatReplacements(input, idx, idx + 1)).isEqualTo(expectedOutput);
    assertThat(doGetFormatReplacements(input, idx, idx + 1, /* cleanup= */ false))
        .isEqualTo(expectedOutputNoReindent);
  }

  // Regression test for b/18479811
  @Test
  public void onNewline() throws Exception {

    String line1 = "for (Integer x : Arrays.asList(1, 2, 3)) {";
    String line2 = "System.err.println(x);";
    String input =
        lines(
            "class Test {{", //
            line1,
            line2,
            "}}}",
            "");

    int startOffset = input.indexOf(line1);
    int length = 1;

    String expectedFormatLine1 =
        lines(
            "class Test {{",
            "    for (Integer x : Arrays.asList(1, 2, 3)) {",
            "System.err.println(x);",
            "}}}",
            "");

    String expectedFormatLine1And2 =
        lines(
            "class Test {{",
            "    for (Integer x : Arrays.asList(1, 2, 3)) {",
            "      System.err.println(x);",
            "}}}",
            "");

    Formatter noReindentFormatter =
        new Formatter(JavaFormatterOptions.builder().cleanup(false).build());
    for (; length <= line1.length() + newline.length(); length++) {
      Range<Integer> range = Range.closedOpen(startOffset, startOffset + length);
      assertEquals(
          "bad output",
          expectedFormatLine1And2,
          new Formatter().formatSource(input, ImmutableList.of(range)));
      assertEquals(
          "bad output",
          expectedFormatLine1,
          noReindentFormatter.formatSource(input, ImmutableList.of(range)));
    }

    for (; length <= line1.length() + line2.length() + 2 * newline.length(); length++) {
      Range<Integer> range = Range.closedOpen(startOffset, startOffset + length);
      String output = new Formatter().formatSource(input, ImmutableList.of(range));
      assertEquals("bad output", expectedFormatLine1And2, output);
      assertEquals(
          "bad output",
          expectedFormatLine1And2,
          noReindentFormatter.formatSource(input, ImmutableList.of(range)));
    }
  }

  @Test
  public void afterNewline() throws Exception {

    String line1 = "for (Integer x : Arrays.asList(1, 2, 3)) {";
    String line2 = "                  System.err.println(x);";
    String input =
        lines(
            "class Test {{", //
            line1,
            line2,
            "}}}",
            "");

    String expectedFormatLine1 =
        lines(
            "class Test {{", //
            "    for (Integer x : Arrays.asList(1, 2, 3)) {", //
            line2,
            "}}}",
            "");

    String expectedFormatLine2 =
        lines(
            "class Test {{", //
            line1,
            "      System.err.println(x);",
            "}}}",
            "");

    String expectedFormatLine1And2 =
        lines(
            "class Test {{", //
            "    for (Integer x : Arrays.asList(1, 2, 3)) {", //
            "      System.err.println(x);",
            "}}}",
            "");

    Formatter noReindentFormatter =
        new Formatter(JavaFormatterOptions.builder().cleanup(false).build());
    int line2Start = input.indexOf(line2);
    int nonWhitespaceLine2Start = input.indexOf("System.err");
    int start;
    // formatting a range that touches non-whitespace characters in line2 should format line2
    for (start = nonWhitespaceLine2Start; start > (line2Start - newline.length()); start--) {
      Range<Integer> range = Range.closedOpen(start, nonWhitespaceLine2Start + newline.length());
      assertThat(new Formatter().formatSource(input, ImmutableList.of(range)))
          .isEqualTo(expectedFormatLine1And2);
      assertThat(noReindentFormatter.formatSource(input, ImmutableList.of(range)))
          .isEqualTo(expectedFormatLine2);
    }
    // formatting a range that touches whitespace characters between line1 and line2 should
    // not result in any formatting
    assertThat(input.substring(start, start + newline.length())).isEqualTo(newline);
    int line1End = input.indexOf(line1) + line1.length();
    for (; start >= line1End; start--) {
      Range<Integer> range = Range.closedOpen(start, line2Start);
      assertThat(new Formatter().formatSource(input, ImmutableList.of(range))).isEqualTo(input);
      assertThat(noReindentFormatter.formatSource(input, ImmutableList.of(range))).isEqualTo(input);
    }
    // formatting a range that touches non-whitespace characters in line1 should format line1
    assertThat(input.substring(start + 1, start + 1 + newline.length())).isEqualTo(newline);
    int line1Start = input.indexOf(line1);
    for (; start >= line1Start; start--) {
      Range<Integer> range = Range.closedOpen(start, line2Start);
      assertThat(new Formatter().formatSource(input, ImmutableList.of(range)))
          .isEqualTo(expectedFormatLine1And2);
      assertThat(noReindentFormatter.formatSource(input, ImmutableList.of(range)))
          .isEqualTo(expectedFormatLine1);
    }
  }

  @Test
  public void commentBeforeBadConstructor() throws Exception {
    String[] lines = {
      "class D {", //
      "  /** */",
      "  F() {}",
      "}",
    };
    String output = new Formatter().formatSource(lines(lines));
    String[] expected = {
      "class D {", //
      "  /** */",
      "  F() {}",
      "}",
      "",
    };
    assertThat(output).isEqualTo(lines(expected));
  }

  @Test
  public void partialEnum() throws Exception {
    String[] input = {
      "enum E {", //
      "ONE,",
      "TWO,",
      "THREE;",
      "}",
    };
    String[] expected = {
      "enum E {", //
      "  ONE,",
      "  TWO,",
      "  THREE;",
      "}",
    };
    String[] expectedNoReindent = {
      "enum E {", //
      "ONE,",
      "  TWO,",
      "THREE;",
      "}",
    };

    Path tmpdir = testFolder.newFolder().toPath();
    Path path = tmpdir.resolve("Foo.java");
    Files.write(path, lines(input).getBytes(UTF_8));

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();

    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true), System.in);
    String[] args = {"-lines", "3", path.toString()};
    assertThat(main.format(args)).isEqualTo(0);
    assertThat(out.toString()).isEqualTo(lines(expected));

    StringWriter outNoReindent = new StringWriter();
    Main mainNoReindent =
        new Main(new PrintWriter(outNoReindent, true), new PrintWriter(err, true), System.in);
    assertThat(mainNoReindent.format("-lines", "3", "--cleanup=false", path.toString()))
        .isEqualTo(0);
    assertThat(outNoReindent.toString()).isEqualTo(lines(expectedNoReindent));
  }

  @Test
  public void partialModifierOrder() throws Exception {
    String[] input = {
      "class T {", //
      "final private int a = 0;",
      "final private int b = 0;",
      "final private int c = 0;",
      "}",
    };
    String[] expected = {
      "class T {", //
      "  final private int a = 0;",
      "  private final int b = 0;",
      "  final private int c = 0;",
      "}",
    };
    String[] expectedNoReindent = {
      "class T {", //
      "final private int a = 0;",
      "  private final int b = 0;",
      "final private int c = 0;",
      "}",
    };

    Path tmpdir = testFolder.newFolder().toPath();
    Path path = tmpdir.resolve("Foo.java");
    Files.write(path, lines(input).getBytes(UTF_8));

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();

    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true), System.in);
    String[] args = {"-lines", "3", path.toString()};
    assertThat(main.format(args)).isEqualTo(0);
    assertThat(out.toString()).isEqualTo(lines(expected));

    StringWriter outNoReindent = new StringWriter();
    Main mainNoReindent =
        new Main(new PrintWriter(outNoReindent, true), new PrintWriter(err, true), System.in);
    assertThat(mainNoReindent.format("-lines", "3", "--cleanup=false", path.toString()))
        .isEqualTo(0);
    assertThat(outNoReindent.toString()).isEqualTo(lines(expectedNoReindent));
  }

  @Test
  public void endOfLine() throws Exception {
    String[] input = {
      "class foo {",
      "  foo(",
      "      int aaaaaaaaaaaaaaa,",
      "      int ccccccccccccc) {",
      "    int a = 0;",
      "    int c = 0;",
      "  }",
      "}",
    };
    String[] expected = {
      "class foo {",
      "  foo(int aaaaaaaaaaaaaaa, int ccccccccccccc) {",
      "    int a = 0;",
      "    int c = 0;",
      "  }",
      "}",
    };
    String in = lines(input);
    // request partial formatting of the end of the first parameter
    int start = in.indexOf(lines(",", "      int ccccccccccccc"));
    assertThat(in.substring(start, start + 1)).isEqualTo(",");

    assertThat(new Formatter().formatSource(in, ImmutableList.of(Range.closedOpen(start, start))))
        .isEqualTo(lines(expected));

    assertThat(formatMain(lines(input), "-offset", String.valueOf(start), "-length", "0"))
        .isEqualTo(lines(expected));
  }

  private String formatMain(String input, String... args) throws Exception {
    Path tmpdir = testFolder.newFolder().toPath();
    Path path = tmpdir.resolve("Test.java");
    Files.write(path, input.getBytes(UTF_8));

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();

    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true), System.in);
    assertThat(main.format(ObjectArrays.concat(args, path.toString()))).isEqualTo(0);
    return out.toString();
  }

  // formatting the newlinea after a statement is a no-op
  @Test
  public void endOfLineStatement() throws Exception {
    String[] input = {
      "class foo {{", //
      "  int a = 0; ",
      "  int c = 0;",
      "}}",
    };
    String[] expected = {
      "class foo {{", //
      "    int a = 0;",
      "    int c = 0;",
      "}}",
    };
    String[] expectedNoReindent = {
      "class foo {{", //
      "    int a = 0;",
      "  int c = 0;",
      "}}",
    };
    String in = lines(input);
    int idx = in.indexOf(';');
    assertThat(new Formatter().formatSource(in, ImmutableList.of(Range.closedOpen(idx, idx))))
        .isEqualTo(lines(expected));
    assertThat(
            new Formatter(JavaFormatterOptions.builder().cleanup(false).build())
                .formatSource(in, ImmutableList.of(Range.closedOpen(idx, idx))))
        .isEqualTo(lines(expectedNoReindent));
  }

  // formatting trailing whitespace at the end of the line doesn't format the line on either side
  @Test
  public void endOfLineStatementNewline() throws Exception {
    String[] input = {
      "class foo {{", //
      "  int a = 0; ",
      "  int c = 0;",
      "}}",
    };
    String in = lines(input);
    int idx = in.indexOf(';');
    assertThat(
            new Formatter().formatSource(in, ImmutableList.of(Range.closedOpen(idx + 1, idx + 1))))
        .isEqualTo(in);
  }

  @Test
  public void importNewlines() throws Exception {
    String input =
        lines(
            "package p;",
            "import java.util.ArrayList;",
            "class Foo {",
            "  ArrayList<String> xs = new ArrayList<>();",
            "}",
            "");
    String expectedOutput =
        lines(
            "package p;",
            "",
            "import java.util.ArrayList;",
            "",
            "class Foo {",
            "  ArrayList<String> xs = new ArrayList<>();",
            "}",
            "");

    String output = runFormatter(input, new String[] {"-lines", "2"});
    assertThat(output).isEqualTo(expectedOutput);
  }

  @Test
  public void b36458607() throws Exception {
    String input =
        lines(
            "// copyright",
            "",
            "package p;",
            "import static c.g.I.c;",
            "",
            "/** */",
            "class Foo {{ c(); }}",
            "");
    String expectedOutput =
        lines(
            "// copyright",
            "",
            "package p;",
            "",
            "import static c.g.I.c;",
            "",
            "/** */",
            "class Foo {{ c(); }}",
            "");

    String output = runFormatter(input, new String[] {"-lines", "4"});
    assertThat(output).isEqualTo(expectedOutput);
  }

  @Test
  public void b32159971() throws Exception {
    String input =
        lines(
            "", //
            "",
            "package p;",
            "class X {}",
            "");
    String expectedOutput =
        lines(
            "package p;", //
            "",
            "class X {}",
            "");

    String output = runFormatter(input, new String[] {"-lines", "3"});
    assertThat(output).isEqualTo(expectedOutput);
  }

  @Test
  public void b21668189() throws Exception {
    String input =
        lines(
            "class Foo {", //
            "  {",
            "    int x = 1;",
            "    ",
            "    int y = 2;",
            "  }",
            "}",
            "");
    String expectedOutput =
        lines(
            "class Foo {", //
            "  {",
            "    int x = 1;",
            "",
            "    int y = 2;",
            "  }",
            "}",
            "");

    String output = runFormatter(input, new String[] {"-lines", "4:5"});
    assertThat(output).isEqualTo(expectedOutput);
  }

  private String runFormatter(String input, String[] args) throws IOException, UsageException {
    Path tmpdir = testFolder.newFolder().toPath();
    Path path = tmpdir.resolve("Foo.java");
    Files.write(path, input.getBytes(UTF_8));

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();

    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true), System.in);
    assertThat(main.format(ObjectArrays.concat(args, path.toString()))).isEqualTo(0);
    return out.toString();
  }

  @Test
  public void trailing() throws Exception {
    String input =
        lines(
            "package foo.bar.baz;",
            "",
            "public class B {",
            "  public void f() {",
            "    int a = 7 +4;",
            "    int b = 7 +4;",
            "    int c = 7 +4;",
            "    int d = 7 +4;",
            "",
            "    int e = 7 +4;",
            "  }",
            "}");
    String expected =
        lines(
            "package foo.bar.baz;",
            "",
            "public class B {",
            "  public void f() {",
            "    int a = 7 +4;",
            "    int b = 7 +4;",
            "    int c = 7 + 4;",
            "    int d = 7 +4;",
            "",
            "    int e = 7 +4;",
            "  }",
            "}");
    String actual =
        new Formatter().formatSource(input, ImmutableList.of(rangeOf(input, "int c = 7 +4")));
    assertThat(actual).isEqualTo(expected);
  }

  private Range<Integer> rangeOf(String input, String needle) {
    int idx = input.indexOf(needle);
    return Range.closedOpen(idx, idx + needle.length());
  }

  @Test
  public void importJavadocNewlines() throws Exception {
    String input =
        lines(
            "package p;",
            "import java.util.ArrayList;",
            "/** */",
            "class Foo {",
            "  ArrayList<String> xs = new ArrayList<>();",
            "}",
            "");
    String expectedOutput =
        lines(
            "package p;",
            "",
            "import java.util.ArrayList;",
            "",
            "/** */",
            "class Foo {",
            "  ArrayList<String> xs = new ArrayList<>();",
            "}",
            "");

    String output = runFormatter(input, new String[] {"-lines", "2"});
    assertThat(output).isEqualTo(expectedOutput);
  }

  @Test
  public void nestedSwitchCase() throws Exception {
    String input =
        lines(
            "class Test {",
            "  {",
            "    switch (foo) {",
            "      case FOO:",
            "      f();",
            "      break;",
            "      case BAR:",
            "      switch (bar) {",
            "        case BAZ:",
            "        h();",
            "        break;",
            "        case BOZ:",
            "        i();",
            "        break;",
            "      }",
            "    }",
            "  }",
            "}");
    String expectedOutput =
        lines(
            "class Test {",
            "  {",
            "    switch (foo) {",
            "      case FOO:",
            "        f();",
            "        break;",
            "      case BAR:",
            "        switch (bar) {",
            "          case BAZ:",
            "            h();",
            "            break;",
            "          case BOZ:",
            "            i();",
            "            break;",
            "        }",
            "    }",
            "  }",
            "}");
    String expectedOutputNoReindent =
        lines(
            "class Test {",
            "  {",
            "    switch (foo) {",
            "      case FOO:",
            "      f();",
            "      break;",
            "      case BAR:",
            "      switch (bar) {",
            "        case BAZ:",
            "            h();",
            "        break;",
            "        case BOZ:",
            "        i();",
            "        break;",
            "      }",
            "    }",
            "  }",
            "}");

    int idx = input.indexOf("h()");
    assertThat(doGetFormatReplacements(input, idx, idx + 1)).isEqualTo(expectedOutput);
    assertThat(doGetFormatReplacements(input, idx, idx + 1, /* cleanup= */ false))
        .isEqualTo(expectedOutputNoReindent);
  }

  @Test
  public void b117602702() throws Exception {
    String input =
        lines(
            "class Foo {", //
            "private Foo () {};",
            "}",
            "");
    String expectedOutput =
        lines(
            "class Foo {", //
            "  private Foo() {}",
            "  ;",
            "}",
            "");

    String output = runFormatter(input, new String[] {"-offset", "13", "-length", "1"});
    assertThat(output).isEqualTo(expectedOutput);
  }

  // b/30464184: remove unused imports in partial formatting leaves at most one blank line
  @Test
  public void unusedImportDeletionNoDoubleBlankLine() throws Exception {
    String input =
        lines("package com.example;", "", "import java.util.List;", "", "class Test {}", "");
    String expectedOutput = lines("package com.example;", "", "class Test {}", "");
    assertThat(
            new Formatter()
                .formatSource(
                    input,
                    ImmutableList.of(rangeOf(input, "import java.util.List;")),
                    /* removeUnusedImports= */ true,
                    /* sortImports= */ true,
                    /* reflowLongStrings= */ false))
        .isEqualTo(expectedOutput);

    String noPackageInput = lines("import java.util.List;", "", "class Test {}", "");
    String noPackageExpected = lines("class Test {}", "");
    assertThat(
            new Formatter()
                .formatSource(
                    noPackageInput,
                    ImmutableList.of(rangeOf(noPackageInput, "import java.util.List;")),
                    /* removeUnusedImports= */ true,
                    /* sortImports= */ true,
                    /* reflowLongStrings= */ false))
        .isEqualTo(noPackageExpected);
  }

  // b/150227347: trailing blank lines at EOF removed when range touches EOF
  @Test
  public void trailingBlankLinesAtEofRemoved() throws Exception {
    String input = lines("class Test {", "  void f() {}", "}", "", "", "");
    String expectedOutput = lines("class Test {", "  void f() {}", "}", "");
    assertThat(runFormatter(input, new String[] {"-lines", "3:5"})).isEqualTo(expectedOutput);
    assertThat(runFormatter(input, new String[] {"-lines", "4:5"})).isEqualTo(expectedOutput);
    assertThat(runFormatter(input, new String[] {"-lines", "4:6"})).isEqualTo(expectedOutput);

    String commentInput = lines("class Test {} // comment", "", "", "");
    String commentExpected = lines("class Test {} // comment", "");
    assertThat(runFormatter(commentInput, new String[] {"-lines", "2:3"}))
        .isEqualTo(commentExpected);
  }

  @Test
  public void verifyReindentReplacements() {
    String text = "  a\n    b\n";
    Replacement reindentA = Replacement.create(0, 2, "    ");
    Replacement reindentB = Replacement.create(4, 8, "");
    JavaOutput.verifyReindentReplacements(
        text, ImmutableList.of(reindentA, reindentB), ImmutableList.of(reindentA, reindentB));

    Replacement nonWhitespaceInput = Replacement.create(0, 3, "  ");
    assertThrows(
        VerifyException.class,
        () ->
            JavaOutput.verifyReindentReplacements(
                text, ImmutableList.of(nonWhitespaceInput), ImmutableList.of(nonWhitespaceInput)));

    Replacement nonWhitespaceOutput = Replacement.create(0, 2, "  x");
    assertThrows(
        VerifyException.class,
        () ->
            JavaOutput.verifyReindentReplacements(
                text,
                ImmutableList.of(nonWhitespaceOutput),
                ImmutableList.of(nonWhitespaceOutput)));

    Replacement format = Replacement.create(1, 6, "a\n  b");
    assertThrows(
        VerifyException.class,
        () ->
            JavaOutput.verifyReindentReplacements(
                text, ImmutableList.of(reindentA), ImmutableList.of(reindentA, format)));
  }

  /// Returns a text block with its `\n` line separators replaced by the separator under test.
  private String text(String textBlock) {
    return textBlock.replace("\n", newline);
  }

  // b/24140798: re-indent enclosing blocks and statements while preserving line breaks outside
  // -lines.
  @Test
  public void reindentBracesAndBlocks() throws Exception {
    String input =
        text(
            """
            class T {
              void f() {
                if (true) {
              if (true) {
                /* comment */ int x =
                    1 + 2;
                int y = 3   +   4;
              } else {
                int z =
                    5 + 6;
              }
                }
              }
            }
            """);
    String expectedOutput =
        text(
            """
            class T {
              void f() {
                if (true) {
                  if (true) {
                    /* comment */ int x =
                        1 + 2;
                    int y = 3 + 4;
                  } else {
                    int z =
                        5 + 6;
                  }
                }
              }
            }
            """);

    assertThat(runFormatter(input, new String[] {"-lines", "7"})).isEqualTo(expectedOutput);
  }

  @Test
  public void reindentSwitchEnumModule() throws Exception {
    String switchInput =
        text(
            """
            class T {
              void f(int x) {
                switch (x) {
                  case 1:
                    int y = 1   +   2;
                    break;
              }
              }
            }
            """);
    String switchExpected =
        text(
            """
            class T {
              void f(int x) {
                switch (x) {
                  case 1:
                    int y = 1 + 2;
                    break;
                }
              }
            }
            """);
    assertThat(runFormatter(switchInput, new String[] {"-lines", "5"})).isEqualTo(switchExpected);

    String enumInput =
        text(
            """
            class T {
              enum E {
                ONE,
                TWO;
                }
            }
            """);
    String enumExpected =
        text(
            """
            class T {
              enum E {
                ONE,
                TWO;
              }
            }
            """);
    assertThat(runFormatter(enumInput, new String[] {"-lines", "3"})).isEqualTo(enumExpected);

    String moduleInput =
        text(
            """
            module com.example.foo {
              requires java.base;
              requires java.sql;
              }
            """);
    String moduleExpected =
        text(
            """
            module com.example.foo {
              requires java.base;
              requires java.sql;
            }
            """);
    assertThat(runFormatter(moduleInput, new String[] {"-lines", "2"})).isEqualTo(moduleExpected);
  }

  @Test
  public void reindentStatementStartingOnPreviousStatementsLastLine() throws Exception {
    String input =
        text(
            """
            class T {
              void f() {
                if (true) {
              foo(
                  1); bar(
                  2);
              int y = 1   +   2;
                }
              }
            }
            """);
    String expectedOutput =
        text(
            """
            class T {
              void f() {
                if (true) {
                  foo(
                      1); bar(
                      2);
                  int y = 1 + 2;
                }
              }
            }
            """);
    assertThat(new Formatter().formatSource(input, ImmutableList.of(rangeOf(input, "int y"))))
        .isEqualTo(expectedOutput);
  }

  @Test
  public void reindentTrailingMultiLineComment() throws Exception {
    String input =
        text(
            """
            class T {
              void f() {
                if (true) {
              foo(); /* a
                     b */
              int y = 1   +   2;
                }
              }
            }
            """);
    String expectedOutput =
        text(
            """
            class T {
              void f() {
                if (true) {
                  foo(); /* a
                         b */
                  int y = 1 + 2;
                }
              }
            }
            """);
    assertThat(new Formatter().formatSource(input, ImmutableList.of(rangeOf(input, "int y"))))
        .isEqualTo(expectedOutput);
  }

  @Test
  public void reindentTextBlockPreserved() throws Exception {
    String input =
        text(
            """
            class T {
              void f() {
              String s = \"""
                  hello
                  world\""";
                int y = 1   +   2;
              }
            }
            """);
    String expectedOutput =
        text(
            """
            class T {
              void f() {
                String s = \"""
                  hello
                  world\""";
                int y = 1 + 2;
              }
            }
            """);
    assertThat(new Formatter().formatSource(input, ImmutableList.of(rangeOf(input, "int y"))))
        .isEqualTo(expectedOutput);
  }

  @Test
  public void reindentTabs() throws Exception {
    String input =
        text(
            """
            class T {
            \tvoid f() {
            \tint x =
            \t\t1;
            \t\tint y = 2   +   3;
            \t}
            }
            """);
    String expectedOutput =
        text(
            """
            class T {
            \tvoid f() {
            \t\tint x =
            \t\t\t1;
            \t\tint y = 2 + 3;
            \t}
            }
            """);
    Formatter tabFormatter =
        new Formatter(
            JavaFormatterOptions.builder()
                .style(JavaFormatterOptions.Style.builder().useTabs(true).build())
                .build());
    assertThat(tabFormatter.formatSource(input, ImmutableList.of(rangeOf(input, "int y"))))
        .isEqualTo(expectedOutput);
  }
}
