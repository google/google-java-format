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
import static org.junit.Assert.assertTrue;

import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.common.io.CharStreams;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ResourceInfo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Integration test for google-java-format. Format each file in the input directory, and confirm
 * that the result is the same as the file in the output directory.
 */
@RunWith(JUnit4.class)
public final class FormatterTest {
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
        assertEquals("bad testdata file names", 2, subPath.getNameCount());
        String dirName = subPath.getName(0).toString();
        String fileName = subPath.getName(1).toString();
        assertThat(fileName).endsWith(".java");
        final String stringFromStream;
        try (InputStream stream = classLoader.getResourceAsStream(resourceName)) {
          stringFromStream =
              CharStreams.toString(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
        switch (dirName) {
          case "input":
            inputs.put(fileName, stringFromStream);
            break;
          case "output":
            outputs.put(fileName, stringFromStream);
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
      String output = new Formatter().formatSource(input);
      assertEquals("bad output for " + fileName, expectedOutput, output);
    }
  }

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
  public void testFormatNonJavaFiles() throws Exception {
    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();
    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true));

    // should succeed because non-Java files are skipped
    assertThat(main.format("foo.go")).isEqualTo(0);
    assertThat(err.toString()).contains("Skipping non-Java file: " + "foo.go");

    // should fail because the file does not exist
    assertThat(main.format("Foo.java")).isNotEqualTo(0);
    assertThat(err.toString()).contains("could not read file: " + "Foo.java");
  }

  @Test
  public void testFormatStdinStdoutWithDashFlag() throws Exception {
    String input =
        "class Foo{\n"
        + "void f\n"
        + "() {\n"
        + "}\n"
        + "}\n";
    String expectedOutput =
        "class Foo {\n"
        + "  void f() {}\n"
        + "}\n";

    InputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();

    InputStream oldIn = System.in;
    System.setIn(in);

    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true));
    assertThat(main.format("-")).isEqualTo(0);
    assertThat(out.toString()).isEqualTo(expectedOutput);

    System.setIn(oldIn);
  }

  private static String doGetFormatReplacements(String input, int characterILo, int characterIHi)
      throws Exception {
    List<Replacement> replacements =
        new Formatter().getFormatReplacements(
            input, ImmutableList.of(Range.closedOpen(characterILo, characterIHi + 1)));
    // Reformat the source.
    String source = input;
    for (Replacement replacement : replacements) {
      Range<Integer> range = replacement.getReplaceRange().canonical(DiscreteDomain.integers());
      source =
          source.substring(0, range.lowerEndpoint()) + replacement.getReplacementString()
              + source.substring(range.upperEndpoint());
    }
    return source;
  }
}
