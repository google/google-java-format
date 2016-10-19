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

import static com.google.common.io.Files.getFileExtension;
import static com.google.common.io.Files.getNameWithoutExtension;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.io.CharStreams;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ResourceInfo;
import com.google.googlejavaformat.Newlines;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Integration test for google-java-format. */
@RunWith(Parameterized.class)
public class FormatterIntegrationTest {

  @Parameters(name = "{index}: {0}")
  public static Iterable<Object[]> data() throws IOException {
    Path testDataPath = Paths.get("com/google/googlejavaformat/java/testdata");
    ClassLoader classLoader = FormatterIntegrationTest.class.getClassLoader();
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
        String contents;
        try (InputStream stream =
            FormatterIntegrationTest.class.getClassLoader().getResourceAsStream(resourceName)) {
          contents = CharStreams.toString(new InputStreamReader(stream, UTF_8));
        }
        switch (extension) {
          case "input":
            inputs.put(baseName, contents);
            break;
          case "output":
            outputs.put(baseName, contents);
            break;
          default:
        }
      }
    }
    List<Object[]> testInputs = new ArrayList<>();
    assertEquals("unmatched inputs and outputs", inputs.size(), outputs.size());
    for (Map.Entry<String, String> entry : inputs.entrySet()) {
      String fileName = entry.getKey();
      String input = inputs.get(fileName);
      assertTrue("unmatched input", outputs.containsKey(fileName));
      String expectedOutput = outputs.get(fileName);
      testInputs.add(new Object[] {fileName, input, expectedOutput});
    }
    return testInputs;
  }

  private final String name;
  private final String input;
  private final String expected;
  private final String separator;

  public FormatterIntegrationTest(String name, String input, String expected) {
    this.name = name;
    this.input = input;
    this.expected = expected;
    this.separator = Newlines.getLineEnding(expected);
  }

  @Test
  public void format() {
    try {
      String output = new Formatter().formatSource(input);
      assertEquals("bad output for " + name, expected, output);
    } catch (FormatterException e) {
      fail(String.format("Formatter crashed on %s: %s", name, e.getMessage()));
    }
  }

  @Test
  public void idempotentLF() {
    try {
      String mangled = expected.replace(separator, "\n");
      String output = new Formatter().formatSource(mangled);
      assertEquals("bad output for " + name, mangled, output);
    } catch (FormatterException e) {
      fail(String.format("Formatter crashed on %s: %s", name, e.getMessage()));
    }
  }

  @Test
  public void idempotentCR() throws IOException {
    try {
      String mangled = expected.replace(separator, "\r");
      String output = new Formatter().formatSource(mangled);
      assertEquals("bad output for " + name, mangled, output);
    } catch (FormatterException e) {
      fail(String.format("Formatter crashed on %s: %s", name, e.getMessage()));
    }
  }

  @Test
  public void idempotentCRLF() {
    try {
      String mangled = expected.replace(separator, "\r\n");
      String output = new Formatter().formatSource(mangled);
      assertEquals("bad output for " + name, mangled, output);
    } catch (FormatterException e) {
      fail(String.format("Formatter crashed on %s: %s", name, e.getMessage()));
    }
  }
}
