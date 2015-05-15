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
import static org.junit.Assert.assertEquals;

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
            + ".build();\n"
            + "}}\n";
    String expectedOutput =
        "class Foo {{\n"
            + "    ImmutableList<Integer> ids =\n"
            + "        ImmutableList.builder()\n"
            + "            .add(1)\n"
            + "            .add(2)\n"
            + "            .build();\n"
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
            + " int zzz = 1; }";
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
            + " int zzz = 1; }";
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
            + "      int zzz = 1; }";
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
            + " int x; } }";
    int idx = input.indexOf("zzz");
    String output = doGetFormatReplacements(input, idx, idx);
    assertEquals("bad output", expectedOutput, output);
  }

  @Test
  public void formatTrailingBrace() throws Exception {
    String input = "class Test { void f() { return; } }\n";
    String expectedOutput =
        "class Test { void f() { return;\n"
            + "  }\n"
            + " }\n";
    int idx = input.indexOf("}");
    String output = doGetFormatReplacements(input, idx, idx);
    assertEquals("bad output", expectedOutput, output);
  }
  
  @Test
  public void formatTrailingBraceEmptyMethodBody() throws Exception {
    String input = "class Test { void f() {} }\n";
    String expectedOutput =
        "class Test {\n"
            + "  void f() {}\n"
            + " }\n";
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

    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true));
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

    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true));
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

    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true));
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

    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true));
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

    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true));
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
    assertThat(replacement.getReplacementString()).isEqualTo("  void f() {}");
    assertThat(replacement.getReplaceRange()).isEqualTo(Range.closedOpen(11, 24));
  }
}
