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
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.fail;

import com.google.common.base.Joiner;
import com.google.common.io.CharStreams;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Integration test for google-java-format. */
@RunWith(JUnit4.class)
public final class FormatterTest {

  @Rule public TemporaryFolder testFolder = new TemporaryFolder();

  @Test
  public void testFormatAosp() throws Exception {
    // don't forget to misspell "long", or you will be mystified for a while
    String input =
        "class A{void b(){while(true){weCanBeCertainThatThisWillEndUpGettingWrapped("
            + "because, it, is, just, so, very, very, very, very, looong);}}}";
    String expectedOutput =
        Joiner.on("\n")
            .join(
                "class A {",
                "    void b() {",
                "        while (true) {",
                "            weCanBeCertainThatThisWillEndUpGettingWrapped(",
                "                    because, it, is, just, so, very, very, very, very, looong);",
                "        }",
                "    }",
                "}",
                "");

    Path tmpdir = testFolder.newFolder().toPath();
    Path path = tmpdir.resolve("A.java");
    Files.write(path, input.getBytes(StandardCharsets.UTF_8));

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();

    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true), System.in);
    String[] args = {"--aosp", path.toString()};
    assertThat(main.format(args)).isEqualTo(0);
    assertThat(out.toString()).isEqualTo(expectedOutput);
  }

  @Test
  public void testFormatNonJavaFiles() throws Exception {
    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();
    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true), System.in);

    // should succeed because non-Java files are skipped
    assertThat(main.format("foo.go")).isEqualTo(0);
    assertThat(err.toString()).contains("Skipping non-Java file: " + "foo.go");

    // should fail because the file does not exist
    assertThat(main.format("Foo.java")).isNotEqualTo(0);
    assertThat(err.toString()).contains("Foo.java: could not read file: ");
  }

  @Test
  public void testFormatStdinStdoutWithDashFlag() throws Exception {
    String input = "class Foo{\n" + "void f\n" + "() {\n" + "}\n" + "}\n";
    String expectedOutput = "class Foo {\n" + "  void f() {}\n" + "}\n";

    InputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();

    InputStream oldIn = System.in;
    System.setIn(in);

    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true), System.in);
    assertThat(main.format("-")).isEqualTo(0);
    assertThat(out.toString()).isEqualTo(expectedOutput);

    System.setIn(oldIn);
  }

  @Test
  public void testFormatLengthUpToEOF() throws Exception {
    String input = "class Foo{\n" + "void f\n" + "() {\n" + "}\n" + "}\n\n\n\n\n\n";
    String expectedOutput = "class Foo {\n" + "  void f() {}\n" + "}\n";

    Path tmpdir = testFolder.newFolder().toPath();
    Path path = tmpdir.resolve("Foo.java");
    Files.write(path, input.getBytes(StandardCharsets.UTF_8));

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();

    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true), System.in);
    String[] args = {"--offset", "0", "--length", String.valueOf(input.length()), path.toString()};
    assertThat(main.format(args)).isEqualTo(0);
    assertThat(out.toString()).isEqualTo(expectedOutput);
  }

  @Test
  public void testFormatLengthOutOfRange() throws Exception {
    String input = "class Foo{}\n";

    Path tmpdir = testFolder.newFolder().toPath();
    Path path = tmpdir.resolve("Foo.java");
    Files.write(path, input.getBytes(StandardCharsets.UTF_8));

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();

    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true), System.in);
    String[] args = {"--offset", "0", "--length", "9999", path.toString()};
    assertThat(main.format(args)).isEqualTo(1);
    assertThat(err.toString())
        .contains("error: invalid length 9999, offset + length (9999) is outside the file");
  }

  @Test
  public void blankInClassBody() throws FormatterException {
    String input = "package test;\nclass T {\n\n}\n";
    String output = new Formatter().formatSource(input);
    String expect = "package test;\n\nclass T {}\n";
    assertThat(output).isEqualTo(expect);
  }

  @Test
  public void blankInClassBodyNoTrailing() throws FormatterException {
    String input = "package test;\nclass T {\n\n}";
    String output = new Formatter().formatSource(input);
    String expect = "package test;\n\nclass T {}\n";
    assertThat(output).isEqualTo(expect);
  }

  @Test
  public void docCommentTrailingBlank() throws FormatterException {
    String input = "class T {\n/** asd */\n\nint x;\n}";
    String output = new Formatter().formatSource(input);
    String expect = "class T {\n  /** asd */\n  int x;\n}\n";
    assertThat(output).isEqualTo(expect);
  }

  @Test
  public void blockCommentInteriorTrailingBlank() throws FormatterException {
    String input = "class T {\n/*\n* asd \n* fgh\n*/ \n\nint x;\n}";
    String output = new Formatter().formatSource(input);
    String expect = "class T {\n  /*\n   * asd\n   * fgh\n   */\n\n  int x;\n}\n";
    assertThat(output).isEqualTo(expect);
  }

  @Test
  public void blockCommentTrailingBlank() throws FormatterException {
    String input = "class T {\n/* asd */ \n\nint x;\n}";
    String output = new Formatter().formatSource(input);
    String expect = "class T {\n  /* asd */\n\n  int x;\n}\n";
    assertThat(output).isEqualTo(expect);
  }

  @Test
  public void lineCommentTrailingBlank() throws FormatterException {
    String input = "class T {\n// asd \n\nint x;\n}";
    String output = new Formatter().formatSource(input);
    String expect = "class T {\n  // asd\n\n  int x;\n}\n";
    assertThat(output).isEqualTo(expect);
  }

  @Test
  public void lineCommentTrailingThinSpace() throws FormatterException {
    // The Unicode thin space is matched by CharMatcher.whitespace() but not trim().
    String input = "class T {\n  // asd\u2009\n}\n";
    String output = new Formatter().formatSource(input);
    String expect = "class T {\n  // asd\n}\n";
    assertThat(output).isEqualTo(expect);
  }

  @Test
  public void noBlankAfterLineCommentWithInteriorBlankLine() throws FormatterException {
    String input = "class T {\n// asd \n\n// dsa \nint x;\n}";
    String output = new Formatter().formatSource(input);
    String expect = "class T {\n  // asd\n\n  // dsa\n  int x;\n}\n";
    assertThat(output).isEqualTo(expect);
  }

  @Test
  public void badConstructor() throws FormatterException {
    String input = "class X { Y() {} }";
    String output = new Formatter().formatSource(input);
    String expect = "class X {\n  Y() {}\n}\n";
    assertThat(output).isEqualTo(expect);
  }

  @Test
  public void voidMethod() throws FormatterException {
    String input = "class X { void Y() {} }";
    String output = new Formatter().formatSource(input);
    String expect = "class X {\n  void Y() {}\n}\n";
    assertThat(output).isEqualTo(expect);
  }

  private static final String UNORDERED_IMPORTS =
      Joiner.on('\n')
          .join(
              "import com.google.common.base.Preconditions;",
              "",
              "import static org.junit.Assert.fail;",
              "import static com.google.truth.Truth.assertThat;",
              "",
              "import org.junit.runners.JUnit4;",
              "import org.junit.runner.RunWith;",
              "",
              "import java.util.List;",
              "",
              "import javax.annotations.Nullable;");

  @Test
  public void importsNotReorderedByDefault() throws FormatterException {
    String input =
        "package com.google.example;\n" + UNORDERED_IMPORTS + "\npublic class ExampleTest {}\n";
    String output = new Formatter().formatSource(input);
    String expect =
        "package com.google.example;\n\n" + UNORDERED_IMPORTS + "\n\npublic class ExampleTest {}\n";
    assertThat(output).isEqualTo(expect);
  }

  @Test
  public void importsFixedIfRequested() throws FormatterException {
    String input =
        "package com.google.example;\n"
            + UNORDERED_IMPORTS
            + "\npublic class ExampleTest {\n"
            + "  @Nullable List<?> xs;\n"
            + "}\n";
    String output = new Formatter().formatSourceAndFixImports(input);
    String expect =
        "package com.google.example;\n\n"
            + "import java.util.List;\n"
            + "import javax.annotations.Nullable;\n\n"
            + "public class ExampleTest {\n"
            + "  @Nullable List<?> xs;\n"
            + "}\n";
    assertThat(output).isEqualTo(expect);
  }

  @Test
  public void importOrderingWithoutFormatting() throws IOException, UsageException {
    importOrdering(
        "--fix-imports-only", "com/google/googlejavaformat/java/testimports/A.imports-only");
  }

  @Test
  public void importOrderingAndFormatting() throws IOException, UsageException {
    importOrdering(null, "com/google/googlejavaformat/java/testimports/A.imports-and-formatting");
  }

  @Test
  public void formattingWithoutImportOrdering() throws IOException, UsageException {
    importOrdering(
        "--skip-sorting-imports",
        "com/google/googlejavaformat/java/testimports/A.formatting-and-unused-import-removal");
  }

  @Test
  public void formattingWithoutRemovingUnusedImports() throws IOException, UsageException {
    importOrdering(
        "--skip-removing-unused-imports",
        "com/google/googlejavaformat/java/testimports/A.formatting-and-import-sorting");
  }

  private void importOrdering(String sortArg, String outputResourceName)
      throws IOException, UsageException {
    Path tmpdir = testFolder.newFolder().toPath();
    Path path = tmpdir.resolve("Foo.java");

    String inputResourceName = "com/google/googlejavaformat/java/testimports/A.input";
    String input = getResource(inputResourceName);
    String expectedOutput = getResource(outputResourceName);
    Files.write(path, input.getBytes(StandardCharsets.UTF_8));

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();
    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true), System.in);
    String[] args =
        sortArg != null
            ? new String[] {sortArg, "-i", path.toString()}
            : new String[] {"-i", path.toString()};
    main.format(args);

    assertThat(err.toString()).isEmpty();
    assertThat(out.toString()).isEmpty();
    String output = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    assertThat(output).isEqualTo(expectedOutput);
  }

  private String getResource(String resourceName) throws IOException {
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
      assertWithMessage("Missing resource: " + resourceName).that(stream).isNotNull();
      return CharStreams.toString(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
  }

  // regression test for google-java-format#47
  @Test
  public void testTrailingCommentWithoutTerminalNewline() throws Exception {
    assertThat(new Formatter().formatSource("/*\n * my comment */"))
        .isEqualTo("/*\n * my comment */\n");
  }

  @Test
  public void testEmptyArray() throws Exception {
    assertThat(new Formatter().formatSource("class T { int x[] = {,}; }"))
        .isEqualTo("class T {\n  int x[] = {,};\n}\n");
  }

  @Test
  public void stringEscapeLength() throws Exception {
    assertThat(new Formatter().formatSource("class T {{ f(\"\\\"\"); }}"))
        .isEqualTo("class T {\n  {\n    f(\"\\\"\");\n  }\n}\n");
  }

  @Test
  public void wrapLineComment() throws Exception {
    assertThat(
            new Formatter()
                .formatSource(
                    "class T {\n"
                        + "  public static void main(String[] args) { // one long incredibly"
                        + " unbroken sentence moving from topic to topic so that no-one had a"
                        + " chance to interrupt;\n"
                        + "  }\n"
                        + "}\n"))
        .isEqualTo(
            "class T {\n"
                + "  public static void main(\n"
                + "      String[]\n"
                + "          args) { // one long incredibly unbroken sentence moving"
                + " from topic to topic so that no-one\n"
                + "                  // had a chance to interrupt;\n"
                + "  }\n"
                + "}\n");
  }

  @Test
  public void onlyWrapLineCommentOnWhitespace() throws Exception {
    assertThat(
            new Formatter()
                .formatSource(
                    "class T {\n"
                        + "  public static void main(String[] args) { // one_long_incredibly"
                        + "_unbroken_sentence_moving_from_topic_to_topic_so_that_no-one_had_a"
                        + "_chance_to_interrupt;\n"
                        + "  }\n"
                        + "}\n"))
        .isEqualTo(
            "class T {\n"
                + "  public static void main(\n"
                + "      String[]\n"
                + "          args) { // one_long_incredibly"
                + "_unbroken_sentence_moving_from_topic_to_topic_so_that_no-one_had_a"
                + "_chance_to_interrupt;\n"
                + "  }\n"
                + "}\n");
  }

  @Test
  public void onlyWrapLineCommentOnWhitespace_noLeadingWhitespace() throws Exception {
    assertThat(
            new Formatter()
                .formatSource(
                    "class T {\n"
                        + "  public static void main(String[] args) { //one_long_incredibly"
                        + "_unbroken_sentence_moving_from_topic_to_topic_so_that_no-one_had_a"
                        + "_chance_to_interrupt;\n"
                        + "  }\n"
                        + "}\n"))
        .isEqualTo(
            "class T {\n"
                + "  public static void main(\n"
                + "      String[]\n"
                + "          args) { // one_long_incredibly"
                + "_unbroken_sentence_moving_from_topic_to_topic_so_that_no-one_had_a"
                + "_chance_to_interrupt;\n"
                + "  }\n"
                + "}\n");
  }

  @Test
  public void throwsFormatterException() throws Exception {
    try {
      new Formatter().formatSourceAndFixImports("package foo; public class {");
      fail();
    } catch (FormatterException expected) {
    }
  }

  @Test
  public void blankLinesImportComment() throws FormatterException {
    String withBlank =
        "package p;\n"
            + "\n"
            + "/** test */\n"
            + "\n"
            + "import a.A;\n"
            + "\n"
            + "class T {\n"
            + "  A a;\n"
            + "}\n";
    String withoutBlank =
        "package p;\n"
            + "\n"
            + "/** test */\n"
            + "import a.A;\n"
            + "\n"
            + "class T {\n"
            + "  A a;\n"
            + "}\n";

    // Formatting deletes the blank line between the "javadoc" and the first import.
    assertThat(new Formatter().formatSource(withBlank)).isEqualTo(withoutBlank);
    assertThat(new Formatter().formatSourceAndFixImports(withBlank)).isEqualTo(withoutBlank);
    assertThat(new Formatter().formatSource(withoutBlank)).isEqualTo(withoutBlank);
    assertThat(new Formatter().formatSourceAndFixImports(withoutBlank)).isEqualTo(withoutBlank);

    // Just fixing imports preserves whitespace around imports.
    assertThat(RemoveUnusedImports.removeUnusedImports(withBlank)).isEqualTo(withBlank);
    assertThat(ImportOrderer.reorderImports(withBlank)).isEqualTo(withBlank);
    assertThat(RemoveUnusedImports.removeUnusedImports(withoutBlank)).isEqualTo(withoutBlank);
    assertThat(ImportOrderer.reorderImports(withoutBlank)).isEqualTo(withoutBlank);
  }
}
