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
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.google.common.io.CharStreams;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ResourceInfo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
  public void testNoReflowInitialComment() throws Exception {
    String inputPath =
        "com/google/googlejavaformat/java/testdata/input-other/NoReflowInitialComment.java";
    String outputPath =
        "com/google/googlejavaformat/java/testdata/output-other/NoReflowInitialComment.java";
    ClassLoader classLoader = getClass().getClassLoader();
    final String input;
    try (InputStream stream = classLoader.getResourceAsStream(inputPath)) {
      input = CharStreams.toString(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
    final String expectedOutput;
    try (InputStream stream = classLoader.getResourceAsStream(outputPath)) {
      expectedOutput = CharStreams.toString(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
    JavaInput javaInput = new JavaInput(input);
    JavaOutput javaOutput = new JavaOutput(javaInput, new JavaCommentsHelper(true), false);
    List<String> errors = new ArrayList<>();
    Formatter.format(javaInput, javaOutput, Formatter.MAX_WIDTH, errors, 1);
    if (!errors.isEmpty()) {
      throw new FormatterException(errors.get(0));
    }
    Writer stringWriter = new StringWriter();
    RangeSet<Integer> lines = TreeRangeSet.create();
    lines.add(Range.<Integer>all());
    try {
      javaOutput.writeMerged(stringWriter, lines, Formatter.MAX_WIDTH, errors);
    } catch (IOException ignored) {
      throw new AssertionError("IOException impossible for StringWriter");
    }
    String output = stringWriter.toString();
    assertEquals("bad output", expectedOutput, output);
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
