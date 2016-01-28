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

import static com.google.common.io.Files.getFileExtension;
import static com.google.common.io.Files.getNameWithoutExtension;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.base.Joiner;
import com.google.common.io.CharStreams;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ResourceInfo;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;

/**
 * Integration test for google-java-format. Format each file in the input directory, and confirm
 * that the result is the same as the file in the output directory.
 */
@RunWith(JUnit4.class)
public final class FormatterTest {

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  @Test
  public void testFormatter() throws Exception {
    Path testDataPath = Paths.get("com/google/googlejavaformat/java/testdata");
    ClassLoader classLoader = getClass().getClassLoader();
    Map<String, String> inputs = new TreeMap<>();
    Map<String, String> outputs = new TreeMap<>();
    for (ResourceInfo resourceInfo : ClassPath.from(classLoader).getResources()) {
      String resourceName = resourceInfo.getResourceName();
      Path resourceNamePath = Paths.get(resourceName);
      if (resourceNamePath.startsWith(testDataPath)) {
        Path subPath = testDataPath.relativize(resourceNamePath);
        assertEquals("bad testdata file names", 1, subPath.getNameCount());
        String baseName = getNameWithoutExtension(subPath.getFileName().toString());
        String extension = getFileExtension(subPath.getFileName().toString());
        final String stringFromStream;
        try (InputStream stream = classLoader.getResourceAsStream(resourceName)) {
          stringFromStream =
              CharStreams.toString(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
        switch (extension) {
          case "input":
            inputs.put(baseName, stringFromStream);
            break;
          case "output":
            outputs.put(baseName, stringFromStream);
            break;
          default:
        }
      }
    }
    assertEquals("unmatched inputs and outputs", inputs.size(), outputs.size());

    for (Map.Entry<String, String> entry : inputs.entrySet()) {
      String fileName = entry.getKey();
      String input = inputs.get(fileName);
      assertTrue("unmatched input", outputs.containsKey(fileName));
      String expectedOutput = outputs.get(fileName);
      try {
        String output = new Formatter().formatSource(input);
        assertEquals("bad output for " + fileName, expectedOutput, output);
      } catch (FormatterException e) {
        fail(String.format("Formatter crashed on %s: %s", fileName, e.getMessage()));
      }
    }
  }

  @Test
  public void testFormatAosp() throws Exception {
    // don't forget to misspell "long", or you will be mystified for a while
    String input =
        "class A{void b(){while(true){weCanBeCertainThatThisWillEndUpGettingWrapped("
            + "because, it, is, just, so, very, very, very, very, looong);}}}";
    String expectedOutput =
        Joiner.on("\n").join(
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
    assertThat(err.toString()).contains("could not read file: " + "Foo.java");
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
  public void blockCommentTrailingBlank() throws FormatterException {
    String input = "class T {\n/* asd */\n\nint x;\n}";
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
}
