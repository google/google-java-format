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

import static com.google.common.truth.Truth.assertThat;
import static com.google.googlejavaformat.java.LineSeparator.MAC;
import static com.google.googlejavaformat.java.LineSeparator.UNIX;
import static com.google.googlejavaformat.java.LineSeparator.WINDOWS;
import static com.google.googlejavaformat.java.LineSeparator.detect;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Joiner;
import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link LineSeparator}
 */
@RunWith(JUnit4.class)
public class LineSeparatorTest {

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  @Test
  public void detectEmptyTextReturnsDefaultSeparator() throws Exception {
    assertThat(detect("")).isEqualTo(UNIX);
    assertThat(detect("", WINDOWS)).isEqualTo(WINDOWS);
    assertThat(detect("", UNIX)).isEqualTo(UNIX);
    assertThat(detect("", MAC)).isEqualTo(MAC);
  }

  @Test
  public void detectWindowsSeparator() throws Exception {
    assertThat(detect("hello\r\nworld")).isEqualTo(WINDOWS);
    assertThat(detect("hello\r\nworld\r\n")).isEqualTo(WINDOWS);
    assertThat(WINDOWS.matches("hello\r\nworld")).isTrue();
    assertThat(WINDOWS.matches("hello\r\nworld\r\n")).isTrue();
  }

  @Test
  public void detectUnixSeparator() throws Exception {
    assertThat(detect("hello\nworld")).isEqualTo(UNIX);
    assertThat(detect("hello\nworld\n")).isEqualTo(UNIX);
    assertThat(UNIX.matches("hello\nworld")).isTrue();
    assertThat(UNIX.matches("hello\nworld\n")).isTrue();
  }

  @Test
  public void detectMacSeparator() throws Exception {
    assertThat(detect("hello\rworld")).isEqualTo(MAC);
    assertThat(detect("hello\rworld\r")).isEqualTo(MAC);
    assertThat(MAC.matches("hello\rworld")).isTrue();
    assertThat(MAC.matches("hello\rworld\r")).isTrue();
  }

  @Test
  public void detectSeparatorOfMixed() throws Exception {
    assertThat(detect("hello\r\nworld\n")).isEqualTo(WINDOWS);
    assertThat(detect("hello\rworld\r\n")).isEqualTo(WINDOWS);
    assertThat(detect("hello\rworld\n")).isEqualTo(UNIX);
    assertThat(detect("hello\nworld\r")).isEqualTo(UNIX);
  }

  @Test
  public void convertEmptyTextStaysEmpty() throws Exception {
    assertThat(WINDOWS.convert("")).isEmpty();
    assertThat(UNIX.convert("")).isEmpty();
    assertThat(MAC.convert("")).isEmpty();
  }

  @Test
  public void convertToWindows() throws Exception {
    convert123(WINDOWS, "1\r\n2\r\n3\r\n");
  }

  @Test
  public void convertToUnix() throws Exception {
    convert123(UNIX, "1\n2\n3\n");
  }

  @Test
  public void convertToMac() throws Exception {
    convert123(MAC, "1\r2\r3\r");
  }

  private void convert123(LineSeparator target, String expected) {
    assertThat(target.convert("1\n2\n3\n")).isEqualTo(expected);
    assertThat(target.convert("1\r2\r3\r")).isEqualTo(expected);
    assertThat(target.convert("1\r\n2\r\n3\r\n")).isEqualTo(expected);
  }

  @Test
  public void preserveLineSeparators() throws Exception {
    String[] input = {
        "import java.util.ArrayList;",
        "class Test {",
        "ArrayList<String> a = new ArrayList<>();",
        "ArrayList<String> b = new ArrayList<>();",
        "char c = '\\n';",
        "}",
    };
    String[] expected = {
        "import java.util.ArrayList;",
        "",
        "class Test {",
        "  ArrayList<String> a = new ArrayList<>();",
        "  ArrayList<String> b = new ArrayList<>();",
        "  char c = '\\n';",
        "}",
        ""
    };
    for (LineSeparator separator : LineSeparator.values()) {
      Joiner joiner = Joiner.on(separator.getChars());
      String source = joiner.join(input);
      assertThat(detect(source)).isSameAs(separator);
      // stdin
      StringWriter out = new StringWriter();
      Main main =
          new Main(
              new PrintWriter(out, true),
              new PrintWriter(System.err, true),
              new ByteArrayInputStream(source.getBytes(UTF_8)));
      assertThat(main.format("-")).isEqualTo(0);
      assertThat(detect(out.toString())).isSameAs(separator);
      assertThat(out.toString()).isEqualTo(joiner.join(expected));
      // entire file
      Path path = testFolder.newFile("Test" + separator.name() + ".java").toPath();
      Files.write(path, source.getBytes(UTF_8));
      main =
          new Main(new PrintWriter(System.out, true), new PrintWriter(System.err, true), System.in);
      int errorCode = main.format("-replace", path.toAbsolutePath().toString());
      assertThat(errorCode).named("Error Code").isEqualTo(0);
      assertThat(detect(new String(Files.readAllBytes(path), UTF_8))).isSameAs(separator);
    }
  }

  @Test
  public void partialFormatting() throws Exception {
    Path path = testFolder.newFile("Test.java").toPath();
    Files.write(path, "class Test {\r\n\r\n}".getBytes(UTF_8));
    Main main =
        new Main(new PrintWriter(System.out, true), new PrintWriter(System.err, true), System.in);
    int errorCode = main.format("-offset", "16", "-length", "1", path.toAbsolutePath().toString());
    assertThat(errorCode).named("Error Code").isEqualTo(0);
  }
}
