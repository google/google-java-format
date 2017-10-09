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

import com.google.common.base.Joiner;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for error reporting. */
@RunWith(JUnit4.class)
public class DiagnosticTest {
  @Rule public TemporaryFolder testFolder = new TemporaryFolder();

  private Locale backupLocale;

  @Before
  public void setUpLocale() throws Exception {
    backupLocale = Locale.getDefault();
    Locale.setDefault(Locale.ROOT);
  }

  @After
  public void restoreLocale() throws Exception {
    Locale.setDefault(backupLocale);
  }

  @Test
  public void parseError() throws Exception {
    String input =
        Joiner.on('\n')
            .join(
                "public class InvalidSyntax {",
                "  private static NumPrinter {",
                "    public static void print(int n) {",
                "      System.out.printf(\"%d%n\", n);",
                "    }",
                "  }",
                "",
                "  public static void main(String[] args) {",
                "    NumPrinter.print(args.length);",
                "  }",
                "}");

    StringWriter stdout = new StringWriter();
    StringWriter stderr = new StringWriter();
    Main main = new Main(new PrintWriter(stdout, true), new PrintWriter(stderr, true), System.in);

    Path tmpdir = testFolder.newFolder().toPath();
    Path path = tmpdir.resolve("InvalidSyntax.java");
    Files.write(path, input.getBytes(UTF_8));

    int result = main.format(path.toString());
    assertThat(stdout.toString()).isEmpty();
    assertThat(stderr.toString()).contains("InvalidSyntax.java:2:29: error: <identifier> expected");
    assertThat(result).isEqualTo(1);
  }

  @Test
  public void lexError() throws Exception {
    String input = "\\uuuuuuuuuuuuuuuuuuuuuuuuuuuuuu00not-actually-a-unicode-escape-sequence";

    StringWriter stdout = new StringWriter();
    StringWriter stderr = new StringWriter();
    Main main = new Main(new PrintWriter(stdout, true), new PrintWriter(stderr, true), System.in);

    Path tmpdir = testFolder.newFolder().toPath();
    Path path = tmpdir.resolve("InvalidSyntax.java");
    Files.write(path, input.getBytes(UTF_8));

    int result = main.format(path.toString());
    assertThat(stdout.toString()).isEmpty();
    assertThat(stderr.toString())
        .contains("InvalidSyntax.java:1:35: error: illegal unicode escape");
    assertThat(result).isEqualTo(1);
  }

  @Test
  public void oneFileParseError() throws Exception {
    String one = "class One {\n";
    String two = "class Two {}\n";

    StringWriter stdout = new StringWriter();
    StringWriter stderr = new StringWriter();
    Main main = new Main(new PrintWriter(stdout, true), new PrintWriter(stderr, true), System.in);

    Path tmpdir = testFolder.newFolder().toPath();
    Path pathOne = tmpdir.resolve("One.java");
    Files.write(pathOne, one.getBytes(UTF_8));

    Path pathTwo = tmpdir.resolve("Two.java");
    Files.write(pathTwo, two.getBytes(UTF_8));

    int result = main.format(pathOne.toString(), pathTwo.toString());
    assertThat(stdout.toString()).isEqualTo(two);
    assertThat(stderr.toString()).contains("One.java:1:13: error: reached end of file");
    assertThat(result).isEqualTo(1);
  }

  @Test
  public void oneFileParseErrorReplace() throws Exception {
    String one = "class One {}}\n";
    String two = "class Two {\n}\n";

    StringWriter stdout = new StringWriter();
    StringWriter stderr = new StringWriter();
    Main main = new Main(new PrintWriter(stdout, true), new PrintWriter(stderr, true), System.in);

    Path tmpdir = testFolder.newFolder().toPath();
    Path pathOne = tmpdir.resolve("One.java");
    Files.write(pathOne, one.getBytes(UTF_8));

    Path pathTwo = tmpdir.resolve("Two.java");
    Files.write(pathTwo, two.getBytes(UTF_8));

    int result = main.format("-i", pathOne.toString(), pathTwo.toString());
    assertThat(stdout.toString()).isEmpty();
    assertThat(stderr.toString())
        .contains("One.java:1:14: error: class, interface, or enum expected");
    assertThat(result).isEqualTo(1);
    // don't edit files with parse errors
    assertThat(Files.readAllLines(pathOne, UTF_8)).containsExactly("class One {}}");
    assertThat(Files.readAllLines(pathTwo, UTF_8)).containsExactly("class Two {}");
  }

  @Test
  public void parseError2() throws FormatterException, IOException, UsageException {
    String input = "class Foo { void f() {\n g() } }";

    Path tmpdir = testFolder.newFolder().toPath();
    Path path = tmpdir.resolve("A.java");
    Files.write(path, input.getBytes(StandardCharsets.UTF_8));

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();

    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true), System.in);
    String[] args = {path.toString()};
    int exitCode = main.format(args);

    assertThat(exitCode).isEqualTo(1);
    assertThat(err.toString()).contains("A.java:2:6: error: ';' expected");
  }

  @Test
  public void parseErrorStdin() throws FormatterException, IOException, UsageException {
    String input = "class Foo { void f() {\n g() } }";

    InputStream inStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();
    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true), inStream);
    String[] args = {"-"};
    int exitCode = main.format(args);

    assertThat(exitCode).isEqualTo(1);
    assertThat(err.toString()).contains("<stdin>:2:6: error: ';' expected");
  }

  @Test
  public void lexError2() throws FormatterException, IOException, UsageException {
    String input = "class Foo { void f() {\n g('foo'); } }";

    Path tmpdir = testFolder.newFolder().toPath();
    Path path = tmpdir.resolve("A.java");
    Files.write(path, input.getBytes(StandardCharsets.UTF_8));

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();

    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true), System.in);
    String[] args = {path.toString()};
    int exitCode = main.format(args);

    assertThat(exitCode).isEqualTo(1);
    assertThat(err.toString()).contains("A.java:2:5: error: unclosed character literal");
  }

  @Test
  public void lexErrorStdin() throws FormatterException, IOException, UsageException {
    String input = "class Foo { void f() {\n g('foo'); } }";
    InputStream inStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();
    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true), inStream);
    String[] args = {"-"};
    int exitCode = main.format(args);

    assertThat(exitCode).isEqualTo(1);
    assertThat(err.toString()).contains("<stdin>:2:5: error: unclosed character literal");
  }
}
