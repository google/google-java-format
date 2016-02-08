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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests formatting parts of files.
 */
@RunWith(JUnit4.class)
public final class PartialFormattingTest {

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  @Test
  public void testGetFormatReplacements0() throws Exception {
    String input =
        ""
            /* line 0 character  0 */ + "class Foo{\n"
            /* line 1 character 11 */ + "void f\n"
            /* line 2 character 18 */ + "() {\n"
            /* line 3 character 23 */ + "}\n"
            /* line 4 character 25 */ + "}\n";
    String expectedOutput =
        ""
            /* line 0 character  0 */ + "class Foo{\n"
            /* line 1 character 11 */ + "  void f() {}\n"
            /* line 2 character 25 */ + "}\n";
    // Claim to have modified the parentheses.
    String output = doGetFormatReplacements(input, 18, 19);
    assertEquals("bad output", expectedOutput, output);
  }

  @Test
  public void testGetFormatReplacements1() throws Exception {
    String input =
        ""
            /* line 0 character  0 */ + "class Foo{\n"
            /* line 1 character 11 */ + "void f\n"
            /* line 2 character 18 */ + "() {\n"
            /* line 3 character 23 */ + "}\n"
            /* line 4 character 25 */ + "}\n";
    String expectedOutput =
        ""
            /* line 0 character  0 */ + "class Foo{\n"
            /* line 1 character 11 */ + "  void f() {}\n"
            /* line 2 character 25 */ + "}\n";
    // Claim to have modified everything after the parentheses.
    String output = doGetFormatReplacements(input, 20, 21);
    assertEquals("bad output", expectedOutput, output);
  }

  @Test
  public void expandToStatement() throws Exception {
    String input =
        "class Foo {{\n"
            + "ImmutableList<Integer> ids = ImmutableList.builder()\n"
            + ".add(1)\n"
            + ".add(2)\n"
            + ".add(3)\n"
            + ".build();\n"
            + "}}\n";
    String expectedOutput =
        "class Foo {{\n"
            + "    ImmutableList<Integer> ids ="
            + " ImmutableList.builder().add(1).add(2).add(3).build();\n"
            + "}}\n";
    int idx = input.indexOf("add(2)");
    String output = doGetFormatReplacements(input, idx, idx + 1);
    assertEquals("bad output", expectedOutput, output);
  }

  @Test
  public void expandToMethodSignature() throws Exception {
    String input =
        "class Foo {\n"
            + "void \n m() \n {\n"
            + "ImmutableList<Integer> ids = ImmutableList.builder()\n"
            + ".add(1)\n"
            + ".add(2)\n"
            + ".build();\n"
            + "}}\n";
    String expectedOutput =
        "class Foo {\n"
            + "  void m() {\n"
            + "ImmutableList<Integer> ids = ImmutableList.builder()\n"
            + ".add(1)\n"
            + ".add(2)\n"
            + ".build();\n"
            + "}}\n";
    int idx = input.indexOf("void");
    String output = doGetFormatReplacements(input, idx, idx + 1);
    assertEquals("bad output", expectedOutput, output);
  }

  @Test
  public void expandToClassSignature() throws Exception {
    String input =
        "class\nFoo<XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX,"
            + "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX,"
            + "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX> {\n"
            + "void \n m() \n {\n"
            + "}}\n";
    String expectedOutput =
        "class Foo<\n"
            + "    XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX,\n"
            + "    XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX,\n"
            + "    XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX> {\n"
            + "void \n"
            + " m() \n"
            + " {\n"
            + "}}\n";
    int idx = input.indexOf("class");
    String output = doGetFormatReplacements(input, idx, idx + 1);
    assertEquals("bad output", expectedOutput, output);
  }

  @Test
  public void ignoreNeighbouringStatements() throws Exception {
    String input = "class Test {\n"
            + "int \n xxx \n = 1;\n"
            + "int \n yyy \n = 1;\n"
            + "int \n zzz \n = 1;\n"
            + "}\n";
    String expectedOutput =
        "class Test {\n"
            + "int \n"
            + " xxx \n"
            + " = 1;\n"
            + "  int yyy = 1;\n"
            + "int \n"
            + " zzz \n"
            + " = 1;\n"
            + "}\n";
    int idx = input.indexOf("yyy");
    String output = doGetFormatReplacements(input, idx, idx + 1);
    assertEquals("bad output", expectedOutput, output);
  }

  @Test
  public void insertLeadingNewlines() throws Exception {
    String input = "class Test { int xxx = 1; int yyy = 1; int zzz = 1; }";
    String expectedOutput =
        "class Test { int xxx = 1;\n"
            + "  int yyy = 1;\n"
            + "  int zzz = 1; }";
    int idx = input.indexOf("yyy");
    String output = doGetFormatReplacements(input, idx, idx + 1);
    assertEquals("bad output", expectedOutput, output);
  }

  @Test
  public void insertLeadingNewlines2() throws Exception {
    String input = "class Test { int xxx = 1;\n\n         int yyy = 1; int zzz = 1; }";
    String expectedOutput =
        "class Test { int xxx = 1;\n\n"
            + "  int yyy = 1;\n"
            + "  int zzz = 1; }";
    int idx = input.indexOf("yyy");
    String output = doGetFormatReplacements(input, idx, idx + 1);
    assertEquals("bad output", expectedOutput, output);
  }

  @Test
  public void insertTrailingNewlines() throws Exception {
    String input = "class Test { int xxx = 1;\n  int yyy = 1;      int zzz = 1; }";
    String expectedOutput =
        "class Test { int xxx = 1;\n"
            + "  int yyy = 1;\n"
            + "  int zzz = 1; }";
    int idx = input.indexOf("yyy");
    String output = doGetFormatReplacements(input, idx, idx + 1);
    assertEquals("bad output", expectedOutput, output);
  }

  @Test
  public void rejoinMethodSignatureLines() throws Exception {
    String input = "class Test { void zzz\n() { int x; } }";
    String expectedOutput =
        "class Test {\n"
            + "  void zzz() {\n"
            + "    int x; } }";
    int idx = input.indexOf("zzz");
    String output = doGetFormatReplacements(input, idx, idx);
    assertEquals("bad output", expectedOutput, output);
  }

  @Test
  public void formatTrailingBrace() throws Exception {
    String input = "class Test { void f() { return; } }\n";
    String expectedOutput =
        "class Test { void f() { return;\n"
            + "  }\n" + "}\n";
    int idx = input.indexOf("}");
    String output = doGetFormatReplacements(input, idx, idx);
    assertEquals("bad output", expectedOutput, output);
  }

  @Test
  public void formatTrailingBraceEmptyMethodBody() throws Exception {
    String input = "class Test { void f() {} }\n";
    String expectedOutput =
        "class Test {\n"
            + "  void f() {}\n" + "}\n";
    int idx = input.indexOf("}");
    String output = doGetFormatReplacements(input, idx, idx);
    assertEquals("bad output", expectedOutput, output);
  }

  @Test
  public void formatTrailingBraceEmptyClassBody() throws Exception {
    String input = "class Test { int x; }\n";
    String expectedOutput = "class Test { int x;\n}\n";
    int idx = input.indexOf("}");
    String output = doGetFormatReplacements(input, idx, idx);
    assertEquals("bad output", expectedOutput, output);
  }

  @Test
  public void formatTrailingBraceEmptyClassBody2() throws Exception {
    String input = "class Test {\n}\n";
    String expectedOutput = "class Test {}\n";
    int idx = input.indexOf("}");
    String output = doGetFormatReplacements(input, idx, idx);
    assertEquals("bad output", expectedOutput, output);
  }

  @Test
  public void onlyPackage() throws Exception {
    String input = "package\ntest\n;\nclass Test {}\n";
    String expectedOutput =
        "package test;\n"
            + "class Test {}\n";
    int idx = input.indexOf("test");
    String output = doGetFormatReplacements(input, idx, idx);
    assertEquals("bad output", expectedOutput, output);
  }

  private static String doGetFormatReplacements(
      String input, int characterILo, int characterIHi)
      throws Exception {
    return new Formatter()
        .formatSource(input, ImmutableList.of(Range.closedOpen(characterILo, characterIHi + 1)));
  }

  @Test
  public void testLength() throws Exception {
    String input =
        "class Foo{\n"
            + "int xxx;\n"
            + "int yyy;\n"
            + "int zzz;\n"
            + "}\n";
    String expectedOutput =
        "class Foo{\n"
            + "int xxx;\n"
            + "  int yyy;\n"
            + "int zzz;\n"
            + "}\n";

    Path tmpdir = testFolder.newFolder().toPath();
    Path path = tmpdir.resolve("Foo.java");
    Files.write(path, input.getBytes(StandardCharsets.UTF_8));

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();

    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true), System.in);
    String[] args = {"-lines", "3", path.toString()};
    assertThat(main.format(args)).isEqualTo(0);
    assertThat(out.toString()).isEqualTo(expectedOutput);
  }

  @Test
  public void testLengthRange() throws Exception {
    String input =
        "class Foo{\n"
            + "int xxx;\n"
            + "int yyy;\n"
            + "int zzz;\n"
            + "}\n";
    String expectedOutput =
        "class Foo{\n"
            + "int xxx;\n"
            + "  int yyy;\n"
            + "  int zzz;\n"
            + "}\n";

    Path tmpdir = testFolder.newFolder().toPath();
    Path path = tmpdir.resolve("Foo.java");
    Files.write(path, input.getBytes(StandardCharsets.UTF_8));

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();

    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true), System.in);
    String[] args = {"-lines", "3:4", path.toString()};
    assertThat(main.format(args)).isEqualTo(0);
    assertThat(out.toString()).isEqualTo(expectedOutput);
  }

  @Test
  public void statementAndComments() throws Exception {
    String input =
        "public class MyTest {\n"
            + "{\n"
            + "// asd\n"
            + "int x = 1;\n"
            + "// asd\n"
            + "int y = 2;\n"
            + "// asd\n"
            + "int z = 3;\n"
            + "// asd\n"
            + "}\n"
            + "}\n"
            + "\n";
    String expectedOutput =
        "public class MyTest {\n"
            + "{\n"
            + "// asd\n"
            + "int x = 1;\n"
            + "    // asd\n"
            + "    int y = 2;\n"
            + "// asd\n"
            + "int z = 3;\n"
            + "// asd\n"
            + "}\n"
            + "}\n"
            + "\n";

    Path tmpdir = testFolder.newFolder().toPath();
    Path path = tmpdir.resolve("Foo.java");
    Files.write(path, input.getBytes(StandardCharsets.UTF_8));

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();

    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true), System.in);
    String[] args = {"-lines", "5", path.toString()};
    assertThat(main.format(args)).isEqualTo(0);
    assertThat(out.toString()).isEqualTo(expectedOutput);
  }

  @Test
  public void statementAndComments2() throws Exception {
    String input =
        "public class MyTest {\n"
            + "{\n"
            + "// asd\n"
            + "int x = 1;\n"
            + "// asd\n"
            + "int y = 2;\n"
            + "// asd\n"
            + "int z = 3;\n"
            + "// asd\n"
            + "}\n"
            + "}\n"
            + "\n";
    String expectedOutput =
        "public class MyTest {\n"
            + "{\n"
            + "// asd\n"
            + "int x = 1;\n"
            + "    // asd\n"
            + "    int y = 2;\n"
            + "// asd\n"
            + "int z = 3;\n"
            + "// asd\n"
            + "}\n"
            + "}\n"
            + "\n";

    Path tmpdir = testFolder.newFolder().toPath();
    Path path = tmpdir.resolve("Foo.java");
    Files.write(path, input.getBytes(StandardCharsets.UTF_8));

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();

    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true), System.in);
    String[] args = {"-lines", "6", path.toString()};
    assertThat(main.format(args)).isEqualTo(0);
    assertThat(out.toString()).isEqualTo(expectedOutput);
  }

  @Test
  public void statementAndComments3() throws Exception {
    String input =
        "public class MyTest {\n"
            + "{\n"
            + "// asd\n"
            + "int x = 1;\n"
            + "// asd\n"
            + "int y = 2;\n"
            + "// asd\n"
            + "int z = 3;\n"
            + "// asd\n"
            + "}\n"
            + "}\n"
            + "\n";
    String expectedOutput =
        "public class MyTest {\n"
            + "{\n"
            + "// asd\n"
            + "int x = 1;\n"
            + "// asd\n"
            + "int y = 2;\n"
            + "    // asd\n"
            + "    int z = 3;\n"
            + "// asd\n"
            + "}\n"
            + "}\n"
            + "\n";

    Path tmpdir = testFolder.newFolder().toPath();
    Path path = tmpdir.resolve("Foo.java");
    Files.write(path, input.getBytes(StandardCharsets.UTF_8));

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();

    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true), System.in);
    String[] args = {"-lines", "7", path.toString()};
    assertThat(main.format(args)).isEqualTo(0);
    assertThat(out.toString()).isEqualTo(expectedOutput);
  }

  @Test
  public void blankAndComment() throws Exception {
    String input =
        "public class MyTest {\n"
            + "  public void testListDefinitions() throws Exception {\n"
            + "    definitionService.insert(createDefinition(1));\n"
            + "    definitionService.insert(createIncrementalDefinition(2));\n"
            + "    definitionService.insert(createDefinition(3));\n"
            + "    definitionService.insert(createIncrementalDefinition(4));\n"
            + "\n"
            + "    // No maxResults\n"
            + "    assertThat(achievementFirstPartyHelper.listDefinitionsByApplication(\n"
            + "            STUB_GAIA_ID, STUB_APPLICATION_ID, Optional.<Integer>absent(),\n"
            + "            Optional.<String>absent()).getAchievements()).containsExactly(createExpectedDefinition(1), createIncrementalExpectedDefinition(2), createExpectedDefinition(3), createIncrementalExpectedDefinition(4)).inOrder();\n"
            + "  }\n"
            + "}\n"
            + "\n";
    String expectedOutput =
        "public class MyTest {\n"
            + "  public void testListDefinitions() throws Exception {\n"
            + "    definitionService.insert(createDefinition(1));\n"
            + "    definitionService.insert(createIncrementalDefinition(2));\n"
            + "    definitionService.insert(createDefinition(3));\n"
            + "    definitionService.insert(createIncrementalDefinition(4));\n"
            + "\n"
            + "    // No maxResults\n"
            + "    assertThat(\n"
            + "            achievementFirstPartyHelper\n"
            + "                .listDefinitionsByApplication(\n"
            + "                    STUB_GAIA_ID,\n"
            + "                    STUB_APPLICATION_ID,\n"
            + "                    Optional.<Integer>absent(),\n"
            + "                    Optional.<String>absent())\n"
            + "                .getAchievements())\n"
            + "        .containsExactly(\n"
            + "            createExpectedDefinition(1),\n"
            + "            createIncrementalExpectedDefinition(2),\n"
            + "            createExpectedDefinition(3),\n"
            + "            createIncrementalExpectedDefinition(4))\n"
            + "        .inOrder();\n"
            + "  }\n"
            + "}\n"
            + "\n";

    String toFormat = "    assertThat(achievementFirstPartyHelper.listDefinitionsByApplication(\n";
    int idx = input.indexOf(toFormat);
    String output = doGetFormatReplacements(input, idx, idx + toFormat.length());
    assertEquals("bad output", expectedOutput, output);
  }

  @Test
  public void emptyFile() throws Exception {
    new Formatter().formatSource("");
    new Formatter().formatSource("\n", ImmutableList.of(Range.closedOpen(0, 1)));
  }

  @Test
  public void testGetFormatReplacementRanges() throws Exception {
    String input =
        ""
            /* line 0 character  0 */ + "class Foo{\n"
            /* line 1 character 11 */ + "void f\n"
            /* line 2 character 18 */ + "() {\n"
            /* line 3 character 23 */ + "}\n"
            /* line 4 character 25 */ + "}\n";
    // Claim to have modified the parentheses.
    ImmutableList<Replacement> ranges =
        new Formatter().getFormatReplacements(input, ImmutableList.of(Range.closedOpen(18, 19)));
    assertThat(ranges).hasSize(1);
    Replacement replacement = ranges.get(0);
    assertThat(replacement.getReplacementString()).isEqualTo("  void f() {}\n");
    assertThat(replacement.getReplaceRange()).isEqualTo(Range.closedOpen(11, 25));
  }

  @Test
  public void noTokensOnLine() throws Exception {
    String input =
        Joiner.on('\n')
            .join(
                "    package com.google.googlejavaformat.java;",
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
                "",
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
    Files.write(path, input.getBytes(StandardCharsets.UTF_8));

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();

    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true), System.in);
    String[] args = {"-lines", "3:4", path.toString()};
    assertThat(main.format(args)).isEqualTo(0);
    assertThat(out.toString()).isEqualTo(input);
  }

  @Test
  public void nestedStatement1() throws Exception {
    String input =
        "public class MyTest {{\n"
            + "int x = \n 1;\n"
            + "int y = new Runnable() {\n"
            + "  void run() {\n"
            + "    System.err.println(42);\n"
            + "  }\n"
            + "};\n"
            + "int z = \n 1;\n"
            + "}}\n"
            + "\n";
    String expectedOutput =
        "public class MyTest {{\n"
            + "int x = \n 1;\n"
            + "    int y =\n"
            + "        new Runnable() {\n"
            + "          void run() {\n"
            + "            System.err.println(42);\n"
            + "          }\n"
            + "        };\n"
            + "int z = \n 1;\n"
            + "}}\n"
            + "\n";

    String toFormat = "Runnable";
    int idx = input.indexOf(toFormat);
    String output = doGetFormatReplacements(input, idx, idx + toFormat.length());
    assertEquals("bad output", expectedOutput, output);
  }

  @Test
  public void nestedStatement2() throws Exception {
    String input =
        "public class MyTest {\n"
            + "int x = \n 1;\n"
            + "int y = new Runnable() {\n"
            + "  void run() {\n"
            + "    System.err.println(42);\n"
            + "  }\n"
            + "};\n"
            + "int z = \n 1;\n"
            + "}\n"
            + "\n";
    String expectedOutput =
        "public class MyTest {\n"
            + "int x = \n 1;\n"
            + "  int y =\n"
            + "      new Runnable() {\n"
            + "        void run() {\n"
            + "          System.err.println(42);\n"
            + "        }\n"
            + "      };\n"
            + "int z = \n 1;\n"
            + "}\n"
            + "\n";

    String toFormat = "Runnable";
    int idx = input.indexOf(toFormat);
    String output = doGetFormatReplacements(input, idx, idx + toFormat.length());
    assertEquals("bad output", expectedOutput, output);
  }

  @Test
  public void blankLine() throws Exception {
    String input =
        "public class MyTest {\n"
            + "int x = 1;\n"
            + "\n"
            + "int y = 1;\n"
            + "}\n"
            + "\n";
    String expectedOutput = input;

    testFormatLine(input, expectedOutput, 3);
  }

  @Test
  public void lineWithIdentifier() throws Exception {
    String input =
        "public class MyTest {\n"
            + "int\n"
            + "y\n"
            + "= 1;\n"
            + "}\n"
            + "\n";
    String expectedOutput =
        "public class MyTest {\n"
            + "  int y = 1;\n"
            + "}\n"
            + "\n";

    testFormatLine(input, expectedOutput, 3);
  }

  // formatted region doesn't expand to include entire comment
  @Test
  public void lineInsideComment() throws Exception {
    String input =
        "public class MyTest {\n"
            + "/* This is a\n"
            + "            poorly indented\n"
            + "                       comment*/\n"
            + "}\n"
            + "\n";

    testFormatLine(input, input, 3);
  }

  @Test
  public void testReplacementsSorted() throws Exception {
    String input =
        Joiner.on('\n')
            .join(
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
      startPositions.add(replacement.getReplaceRange().lowerEndpoint());
    }
    assertThat(startPositions).hasSize(3);
    assertThat(startPositions).isStrictlyOrdered();
  }

  @Test
  public void testReplacementsSorted_DescendingInput() throws Exception {
    String input =
        Joiner.on('\n')
            .join(
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
      startPositions.add(replacement.getReplaceRange().lowerEndpoint());
    }
    assertThat(startPositions).hasSize(3);
    assertThat(startPositions).isStrictlyOrdered();
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
        "class Foo{\n"
            + "int xxx; // asd\n"
            + "}\n";
    String expectedOutput =
        "class Foo{\n"
            + "  int xxx; // asd\n"
            + "}\n";

    Path tmpdir = testFolder.newFolder().toPath();
    Path path = tmpdir.resolve("Foo.java");
    Files.write(path, input.getBytes(StandardCharsets.UTF_8));

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
        "public class MyTest {{\n"
            + "if (true) {\n"
            + "if (true) {\n"
            + "System.err.println(\"Hello\");\n"
            + "} else {\n"
            + "System.err.println(\"Goodbye\");\n"
            + "}\n"
            + "}\n"
            + "}}\n";
    String expectedOutput =
        "public class MyTest {{\n"
            + "if (true) {\n"
            + "if (true) {\n"
            + "        System.err.println(\"Hello\");\n"
            + "} else {\n"
            + "System.err.println(\"Goodbye\");\n"
            + "}\n"
            + "}\n"
            + "}}\n";

    String toFormat = "Hello";
    int idx = input.indexOf(toFormat);
    String output = doGetFormatReplacements(input, idx, idx + toFormat.length());
    assertEquals("bad output", expectedOutput, output);
  }

  // regression test for b/b22196513
  @Test
  public void noTrailingWhitespace() throws Exception {
    String input =
        ""
            + "class Test {\n"
            + "  {\n"
            + "    {\n"
            + "      {\n"
            + "      }\n"
            + "    }\n"
            + "}}\n";
    String expected =
        ""
            + "class Test {\n"
            + "  {\n"
            + "    {\n"
            + "      {\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";
    ImmutableList<Range<Integer>> ranges = ImmutableList.of(Range.closedOpen(45, 48));
    String output = new Formatter().formatSource(input, ranges);
    assertEquals("bad output", expected, output);
  }

  // regression test for b/b22196513
  @Test
  public void trailingNonBreakingWhitespace() throws Exception {
    String input =
        ""
            + "class Test {\n"
            + "  {\n"
            + "    int x;int y;\n"
            + "  }\n"
            + "}\n";
    String expected =
        ""
            + "class Test {\n"
            + "  {\n"
            + "    int x;\n"
            + "    int y;\n"
            + "  }\n"
            + "}\n";
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
        "class Foo {\n"
            + "int x = 1;\n"
            + "}";
    String expectedOutput =
        "class Foo {\n"
            + "  int x = 1;\n"
            + "}\n";

    Path tmpdir = testFolder.newFolder().toPath();
    Path path = tmpdir.resolve("Foo.java");
    Files.write(path, input.getBytes(StandardCharsets.UTF_8));

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
        "class Foo {\n"
            + "int x = 1;\n"
            + "}";
    String expectedOutput =
        "class Foo {\n"
            + "  int x = 1;\n"
            + "}\n";

    Path tmpdir = testFolder.newFolder().toPath();
    Path path = tmpdir.resolve("Foo.java");
    Files.write(path, input.getBytes(StandardCharsets.UTF_8));

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();

    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true), System.in);
    String[] args = {"-lines", "1:5", path.toString()};
    assertThat(main.format(args)).isEqualTo(0);
    assertThat(out.toString()).isEqualTo(expectedOutput);
  }

  @Test
  public void testOutOfRangeLines() throws Exception {
    String input = "class Foo {\n" + "}\n";
    String expectedOutput = "class Foo {}\n";

    Path tmpdir = testFolder.newFolder().toPath();
    Path path = tmpdir.resolve("Foo.java");
    Files.write(path, input.getBytes(StandardCharsets.UTF_8));

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();

    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true), System.in);
    String[] args = {
      "-lines=23:27", "-lines=1:1", "-lines=31:35", "-lines=52:63", "-lines=1:1", path.toString()
    };
    assertThat(main.format(args)).isEqualTo(0);
    assertThat(out.toString()).isEqualTo(expectedOutput);
  }

  @Test
  public void testEmptyFirstLine() throws Exception {
    String input = "\n" + "\n" + "class Foo {\n" + "}\n";

    Path tmpdir = testFolder.newFolder().toPath();
    Path path = tmpdir.resolve("Foo.java");
    Files.write(path, input.getBytes(StandardCharsets.UTF_8));

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();

    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true), System.in);
    String[] args = {"-lines=1:1", path.toString()};
    assertThat(main.format(args)).isEqualTo(0);
  }

  @Test
  public void testEmptyLastLine() throws Exception {
    String input = "class Foo {\n" + "}\n" + "\n" + "\n";

    Path tmpdir = testFolder.newFolder().toPath();
    Path path = tmpdir.resolve("Foo.java");
    Files.write(path, input.getBytes(StandardCharsets.UTF_8));

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
        Joiner.on('\n').join(
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
        Joiner.on('\n').join(
            "class Test {",
            "  {",
            "    switch (foo) {",
            "      case FOO:",
            "        f();",
            "        break;",
            "      case BAR:", // we deliberately only format the first case
            "      g();",
            "      break;",
            "    }",
            "  }",
            "}");

    int idx = input.indexOf("f()");
    String output = doGetFormatReplacements(input, idx, idx + 1);
    assertEquals("bad output", expectedOutput, output);
  }

  // regression test for b/23349153
  @Test
  public void emptyStatement() throws Exception {
    String input =
        "class Test {{\n"
            + "Object o = f();;\n"
            + "}}\n";
    String expectedOutput = "class Test {{\n"
            + "    Object o = f();\n"
            + "    ;\n"
            + "}}\n";
    int idx = input.indexOf("Object o");
    String output = doGetFormatReplacements(input, idx, idx + 1);
    assertEquals("bad output", expectedOutput, output);
  }

  @Test
  public void preserveTrailingWhitespaceAfterNewline() throws Exception {
    String input = "class Test {{\n"
            + "Object o = f();       \n"
            + "            ;\n"
            + "}}\n";
    String expectedOutput =
        "class Test {{\n"
            + "    Object o = f();\n"
            + "            ;\n"
            + "}}\n";
    int idx = input.indexOf("Object o");
    String output = doGetFormatReplacements(input, idx, idx + 1);
    assertEquals("bad output", expectedOutput, output);
  }

  @Test
  public void trailingWhitespace() throws Exception {
    String input = "class Test {{\n"
            + "Object o = f();       \n"
            + "            ;\n"
            + "}}\n";
    String expectedOutput =
        "class Test {{\n"
            + "    Object o = f();\n"
            + "            ;\n"
            + "}}\n";
    int idx = input.indexOf("Object o");
    String output = doGetFormatReplacements(input, idx, idx + 1);
    assertEquals("bad output", expectedOutput, output);
  }

  // Regression test for b/18479811
  @Test
  public void onNewline() throws Exception {

    String line1 = "for (Integer x : Arrays.asList(1, 2, 3)) {\n";
    String line2 = "System.err.println(x);\n";
    String input = "class Test {{\n" + line1 + line2 + "}}}\n";

    int startOffset = input.indexOf(line1);
    int length = 1;

    String expectedFormatLine1 =
        "class Test {{\n"
            + "    for (Integer x : Arrays.asList(1, 2, 3)) {\n"
            + "System.err.println(x);\n"
            + "}}}\n";

    for (; length <= line1.length(); length++) {
      Range<Integer> range = Range.closedOpen(startOffset, startOffset + length);
      String output = new Formatter().formatSource(input, ImmutableList.of(range));
      assertEquals("bad output", expectedFormatLine1, output);
    }

    String expectedFormatLine1And2 =
        "class Test {{\n"
            + "    for (Integer x : Arrays.asList(1, 2, 3)) {\n"
            + "      System.err.println(x);\n"
            + "}}}\n";

    for (; length <= line1.length() + line2.length(); length++) {
      Range<Integer> range = Range.closedOpen(startOffset, startOffset + length);
      String output = new Formatter().formatSource(input, ImmutableList.of(range));
      assertEquals("bad output", expectedFormatLine1And2, output);
    }
  }

  @Test
  public void afterNewline() throws Exception {

    String line1 = "for (Integer x : Arrays.asList(1, 2, 3)) {\n";
    String line2 = "                  System.err.println(x);\n";
    String input = "class Test {{\n" + line1 + line2 + "}}}\n";

    String expectedFormatLine1 =
        "class Test {{\n" + "    for (Integer x : Arrays.asList(1, 2, 3)) {\n" + line2 + "}}}\n";

    String expectedFormatLine2 =
        "class Test {{\n" + line1 + "      System.err.println(x);\n" + "}}}\n";

    int line2Start = input.indexOf(line2);
    int nonWhitespaceLine2Start = input.indexOf("System.err");
    int start = -1;
    for (start = nonWhitespaceLine2Start; start >= line2Start; start--) {
      Range<Integer> range = Range.closedOpen(start, nonWhitespaceLine2Start + 1);
      String output = new Formatter().formatSource(input, ImmutableList.of(range));
      assertEquals("bad output", expectedFormatLine2, output);
    }
    assertThat(input.charAt(start)).isEqualTo('\n');
    int line1Start = input.indexOf(line1);
    for (; start >= line1Start; start--) {
      Range<Integer> range = Range.closedOpen(start, line2Start);
      String output = new Formatter().formatSource(input, ImmutableList.of(range));
      assertEquals("bad output", expectedFormatLine1, output);
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
    String output = new Formatter().formatSource(Joiner.on('\n').join(lines));
    String[] expected = {
      "class D {", //
      "  /** */",
      "  F() {}",
      "}",
      "",
    };
    assertThat(output).isEqualTo(Joiner.on('\n').join(expected));
  }
}
