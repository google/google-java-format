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

import static com.google.common.base.StandardSystemProperty.JAVA_CLASS_PATH;
import static com.google.common.base.StandardSystemProperty.JAVA_HOME;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Locale;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link Main}. */
@RunWith(JUnit4.class)
public class MainTest {

  @Rule public TemporaryFolder testFolder = new TemporaryFolder();

  // PrintWriter instances used below are hard-coded to use system-default line separator.
  private final Joiner joiner = Joiner.on(System.lineSeparator());

  private static final ImmutableList<String> ADD_EXPORTS =
      ImmutableList.of(
          "--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
          "--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
          "--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
          "--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
          "--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
          "--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED");

  @Test
  public void testUsageOutput() {
    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();
    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true), System.in);

    try {
      main.format("--help");
      throw new AssertionError("Expected UsageException to be thrown");
    } catch (UsageException e) {

      String usage = e.getMessage();

      // Check that doc links are included.
      assertThat(usage).contains("https://github.com/google/google-java-format");
      assertThat(usage).contains("Usage: google-java-format");

      // Sanity check that a flag and description is in included.
      assertThat(usage).contains("--length");
      assertThat(usage).contains("Character length to format.");

      // Check that some of the additional text is included.
      assertThat(usage).contains("the result is sent to stdout");
    }
  }

  @Test
  public void version() throws UsageException {
    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();
    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true), System.in);
    assertThat(main.format("-version")).isEqualTo(0);
    assertThat(err.toString()).contains("google-java-format: Version ");
  }

  @Test
  public void preserveOriginalFile() throws Exception {
    Path path = testFolder.newFile("Test.java").toPath();
    Files.write(path, "class Test {}\n".getBytes(UTF_8));
    try {
      Files.setPosixFilePermissions(path, EnumSet.of(PosixFilePermission.OWNER_READ));
    } catch (UnsupportedOperationException e) {
      return;
    }
    Main main =
        new Main(
            new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out, UTF_8)), true),
            new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.err, UTF_8)), true),
            System.in);
    int errorCode = main.format("-replace", path.toAbsolutePath().toString());
    assertWithMessage("Error Code").that(errorCode).isEqualTo(0);
  }

  @Test
  public void testMain() throws Exception {
    Process process =
        new ProcessBuilder(
                ImmutableList.<String>builder()
                    .add(Paths.get(JAVA_HOME.value()).resolve("bin/java").toString())
                    .addAll(ADD_EXPORTS)
                    .add("-cp")
                    .add(JAVA_CLASS_PATH.value())
                    .add(Main.class.getName())
                    .build())
            .redirectError(Redirect.PIPE)
            .redirectOutput(Redirect.PIPE)
            .start();
    process.waitFor();
    String err = new String(ByteStreams.toByteArray(process.getErrorStream()), UTF_8);
    assertThat(err).contains("Usage: google-java-format");
    assertThat(process.exitValue()).isEqualTo(2);
  }

  // end to end javadoc formatting test
  @Test
  public void javadoc() throws Exception {
    String[] input = {
      "/**",
      " * graph",
      " *",
      " * graph",
      " *",
      " * @param foo lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do"
          + " eiusmod tempor incididunt ut labore et dolore magna aliqua",
      " */",
      "class Test {",
      "  /**",
      "   * creates entropy",
      "   */",
      "  public static void main(String... args) {}",
      "}",
    };
    String[] expected = {
      "/**",
      " * graph",
      " *",
      " * <p>graph",
      " *",
      " * @param foo lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do"
          + " eiusmod tempor",
      " *     incididunt ut labore et dolore magna aliqua",
      " */",
      "class Test {",
      "  /** creates entropy */",
      "  public static void main(String... args) {}",
      "}",
      "",
    };
    InputStream in = new ByteArrayInputStream(joiner.join(input).getBytes(UTF_8));
    StringWriter out = new StringWriter();
    Main main =
        new Main(
            new PrintWriter(out, true),
            new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.err, UTF_8)), true),
            in);
    assertThat(main.format("-")).isEqualTo(0);
    assertThat(out.toString()).isEqualTo(joiner.join(expected));
  }

  // end to end import fixing test
  @Test
  public void imports() throws Exception {
    String[] input = {
      "import java.util.LinkedList;",
      "import java.util.List;",
      "import java.util.ArrayList;",
      "class Test {",
      "  /**",
      "   * May be an {@link ArrayList}.",
      "   */",
      "  public static List<String> names;",
      "}",
    };
    String[] expected = {
      "import java.util.ArrayList;",
      "import java.util.List;",
      "",
      "class Test {",
      "  /**",
      "   * May be an {@link ArrayList}.",
      "   */",
      "  public static List<String> names;",
      "}",
    };
    InputStream in = new ByteArrayInputStream(joiner.join(input).getBytes(UTF_8));
    StringWriter out = new StringWriter();
    Main main =
        new Main(
            new PrintWriter(out, true),
            new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.err, UTF_8)), true),
            in);
    assertThat(main.format("-", "--fix-imports-only")).isEqualTo(0);
    assertThat(out.toString()).isEqualTo(joiner.join(expected));
  }

  @Test
  public void optimizeImportsDoesNotLeaveEmptyLines() throws Exception {
    String[] input = {
      "package abc;",
      "",
      "import java.util.LinkedList;",
      "import java.util.List;",
      "import java.util.ArrayList;",
      "",
      "import static java.nio.charset.StandardCharsets.UTF_8;",
      "",
      "import java.util.EnumSet;",
      "",
      "class Test ",
      "extends ArrayList {",
      "}"
    };
    String[] expected = {
      "package abc;", //
      "",
      "import java.util.ArrayList;",
      "",
      "class Test extends ArrayList {}",
      ""
    };

    // pre-check expectation with local formatter instance
    String optimized = new Formatter().formatSourceAndFixImports(joiner.join(input));
    assertThat(optimized).isEqualTo(joiner.join(expected));

    InputStream in = new ByteArrayInputStream(joiner.join(input).getBytes(UTF_8));
    StringWriter out = new StringWriter();
    Main main =
        new Main(
            new PrintWriter(out, true),
            new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.err, UTF_8)), true),
            in);
    assertThat(main.format("-")).isEqualTo(0);
    assertThat(out.toString()).isEqualTo(joiner.join(expected));
  }

  // test that -lines handling works with import removal
  @Test
  public void importRemovalLines() throws Exception {
    String[] input = {
      "import java.util.ArrayList;",
      "import java.util.List;",
      "class Test {",
      "ArrayList<String> a = new ArrayList<>();",
      "ArrayList<String> b = new ArrayList<>();",
      "}",
    };
    String[] expected = {
      "import java.util.ArrayList;",
      "",
      "class Test {",
      "  ArrayList<String> a = new ArrayList<>();",
      "ArrayList<String> b = new ArrayList<>();",
      "}",
    };
    StringWriter out = new StringWriter();
    Main main =
        new Main(
            new PrintWriter(out, true),
            new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.err, UTF_8)), true),
            new ByteArrayInputStream(joiner.join(input).getBytes(UTF_8)));
    assertThat(main.format("-", "-lines", "4")).isEqualTo(0);
    assertThat(out.toString()).isEqualTo(joiner.join(expected));
  }

  // test that errors are reported on the right line when imports are removed
  @Test
  public void importRemoveErrorParseError() throws Exception {
    Locale backupLocale = Locale.getDefault();
    try {
      Locale.setDefault(Locale.ROOT);

      String[] input = {
        "import java.util.ArrayList;", //
        "import java.util.List;",
        "class Test {",
        "}}",
      };
      StringWriter out = new StringWriter();
      StringWriter err = new StringWriter();
      Main main =
          new Main(
              new PrintWriter(out, true),
              new PrintWriter(err, true),
              new ByteArrayInputStream(joiner.join(input).getBytes(UTF_8)));
      assertThat(main.format("-")).isEqualTo(1);
      assertThat(err.toString()).contains("<stdin>:4:3: error: class, interface");

    } finally {
      Locale.setDefault(backupLocale);
    }
  }

  @Test
  public void packageInfo() throws Exception {
    String[] input = {
      "@CheckReturnValue",
      "@ParametersAreNonnullByDefault",
      "package com.google.common.labs.base;",
      "",
      "import com.google.errorprone.annotations.CheckReturnValue;",
      "import javax.annotation.ParametersAreNonnullByDefault;",
      "",
    };
    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();
    Main main =
        new Main(
            new PrintWriter(out, true),
            new PrintWriter(err, true),
            new ByteArrayInputStream(joiner.join(input).getBytes(UTF_8)));
    assertThat(main.format("-")).isEqualTo(0);
    assertThat(out.toString()).isEqualTo(joiner.join(input));
  }

  @Test
  public void newline() throws Exception {
    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();
    Main main =
        new Main(
            new PrintWriter(out, true),
            new PrintWriter(err, true),
            new ByteArrayInputStream("class T {}\n\t".getBytes(UTF_8)));
    assertThat(main.format("-")).isEqualTo(0);
    assertThat(out.toString()).isEqualTo("class T {}\n");
  }

  @Test
  public void dryRunStdinUnchanged() throws Exception {
    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();
    Main main =
        new Main(
            new PrintWriter(out, true),
            new PrintWriter(err, true),
            new ByteArrayInputStream("class Test {}\n".getBytes(UTF_8)));
    assertThat(main.format("-n", "-")).isEqualTo(0);
    assertThat(out.toString()).isEmpty();
    assertThat(err.toString()).isEmpty();
  }

  @Test
  public void dryRunStdinChanged() throws Exception {
    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();
    String input = "class Test {\n}\n";
    Main main =
        new Main(
            new PrintWriter(out, true),
            new PrintWriter(err, true),
            new ByteArrayInputStream(input.getBytes(UTF_8)));
    assertThat(main.format("-n", "-")).isEqualTo(0);
    assertThat(out.toString()).isEqualTo("<stdin>" + System.lineSeparator());
    assertThat(err.toString()).isEmpty();
  }

  @Test
  public void dryRunFiles() throws Exception {
    Path a = testFolder.newFile("A.java").toPath();
    Path b = testFolder.newFile("B.java").toPath();
    Path c = testFolder.newFile("C.java").toPath();
    Files.write(a, "class A {}\n".getBytes(UTF_8));
    Files.write(b, "class B {\n}\n".getBytes(UTF_8));
    Files.write(c, "class C {\n}\n".getBytes(UTF_8));

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();
    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true), System.in);
    int exitCode =
        main.format(
            "-n",
            a.toAbsolutePath().toAbsolutePath().toString(),
            b.toAbsolutePath().toString(),
            c.toAbsolutePath().toString());

    assertThat(exitCode).isEqualTo(0);

    assertThat(out.toString())
        .isEqualTo(
            b.toAbsolutePath()
                + System.lineSeparator()
                + c.toAbsolutePath()
                + System.lineSeparator());
    assertThat(err.toString()).isEmpty();
  }

  @Test
  public void keepGoingWhenFilesDontExist() throws Exception {
    Path a = testFolder.newFile("A.java").toPath();
    Path b = testFolder.newFile("B.java").toPath();
    File cFile = testFolder.newFile("C.java");
    Path c = cFile.toPath();
    cFile.delete();

    Files.write(a, "class A{}\n".getBytes(UTF_8));
    Files.write(b, "class B{}\n".getBytes(UTF_8));

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();
    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true), System.in);

    int exitCode =
        main.format(
            "",
            a.toAbsolutePath().toString(),
            c.toAbsolutePath().toString(),
            b.toAbsolutePath().toString());

    // Formatter returns failure if a file was not present.
    assertThat(exitCode).isEqualTo(1);

    // Present files were correctly formatted.
    assertThat(out.toString()).isEqualTo("class A {}\nclass B {}\n");

    // File not found still showed error.
    assertThat(err.toString()).isNotEmpty();
  }

  @Test
  public void exitIfChangedStdin() throws Exception {
    Path path = testFolder.newFile("Test.java").toPath();
    Files.write(path, "class Test {\n}\n".getBytes(UTF_8));
    Process process =
        new ProcessBuilder(
                ImmutableList.<String>builder()
                    .add(Paths.get(JAVA_HOME.value()).resolve("bin/java").toString())
                    .addAll(ADD_EXPORTS)
                    .add("-cp")
                    .add(JAVA_CLASS_PATH.value())
                    .add(Main.class.getName())
                    .add("-n")
                    .add("--set-exit-if-changed")
                    .add("-")
                    .build())
            .redirectInput(path.toFile())
            .redirectError(Redirect.PIPE)
            .redirectOutput(Redirect.PIPE)
            .start();
    process.waitFor();
    String err = new String(ByteStreams.toByteArray(process.getErrorStream()), UTF_8);
    String out = new String(ByteStreams.toByteArray(process.getInputStream()), UTF_8);
    assertThat(err).isEmpty();
    assertThat(out).isEqualTo("<stdin>" + System.lineSeparator());
    assertThat(process.exitValue()).isEqualTo(1);
  }

  @Test
  public void exitIfChangedFiles() throws Exception {
    Path path = testFolder.newFile("Test.java").toPath();
    Files.write(path, "class Test {\n}\n".getBytes(UTF_8));
    Process process =
        new ProcessBuilder(
                ImmutableList.<String>builder()
                    .add(Paths.get(JAVA_HOME.value()).resolve("bin/java").toString())
                    .addAll(ADD_EXPORTS)
                    .add("-cp")
                    .add(JAVA_CLASS_PATH.value())
                    .add(Main.class.getName())
                    .add("-n")
                    .add("--set-exit-if-changed")
                    .add(path.toAbsolutePath().toString())
                    .build())
            .redirectError(Redirect.PIPE)
            .redirectOutput(Redirect.PIPE)
            .start();
    process.waitFor();
    String err = new String(ByteStreams.toByteArray(process.getErrorStream()), UTF_8);
    String out = new String(ByteStreams.toByteArray(process.getInputStream()), UTF_8);
    assertThat(err).isEmpty();
    assertThat(out).isEqualTo(path.toAbsolutePath() + System.lineSeparator());
    assertThat(process.exitValue()).isEqualTo(1);
  }

  @Test
  public void assumeFilename_error() throws Exception {
    String[] input = {
      "class Test {}}",
    };
    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();
    Main main =
        new Main(
            new PrintWriter(out, true),
            new PrintWriter(err, true),
            new ByteArrayInputStream(joiner.join(input).getBytes(UTF_8)));
    assertThat(main.format("--assume-filename=Foo.java", "-")).isEqualTo(1);
    assertThat(err.toString()).contains("Foo.java:1:15: error: class, interface");
  }

  @Test
  public void assumeFilename_dryRun() throws Exception {
    String[] input = {
      "class Test {", //
      "}",
    };
    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();
    Main main =
        new Main(
            new PrintWriter(out, true),
            new PrintWriter(err, true),
            new ByteArrayInputStream(joiner.join(input).getBytes(UTF_8)));
    assertThat(main.format("--dry-run", "--assume-filename=Foo.java", "-")).isEqualTo(0);
    assertThat(out.toString()).isEqualTo("Foo.java" + System.lineSeparator());
  }

  @Test
  public void reflowLongStrings() throws Exception {
    String[] input = {
      "class T {", //
      "  String s = \"one long incredibly unbroken sentence moving from topic to topic so that no"
          + " one had a chance to interrupt\";",
      "}"
    };
    String[] expected = {
      "class T {",
      "  String s =",
      "      \"one long incredibly unbroken sentence moving from topic to topic so that no one had"
          + " a chance\"",
      "          + \" to interrupt\";",
      "}",
      "",
    };
    InputStream in = new ByteArrayInputStream(joiner.join(input).getBytes(UTF_8));
    StringWriter out = new StringWriter();
    Main main =
        new Main(
            new PrintWriter(out, true),
            new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.err, UTF_8)), true),
            in);
    assertThat(main.format("-")).isEqualTo(0);
    assertThat(out.toString()).isEqualTo(joiner.join(expected));
  }

  @Test
  public void noReflowLongStrings() throws Exception {
    String[] input = {
      "class T {", //
      "  String s = \"one long incredibly unbroken sentence moving from topic to topic so that no"
          + " one had a chance to interrupt\";",
      "}"
    };
    String[] expected = {
      "class T {",
      "  String s =",
      "      \"one long incredibly unbroken sentence moving from topic to topic so that no one had"
          + " a chance to interrupt\";",
      "}",
      "",
    };
    InputStream in = new ByteArrayInputStream(joiner.join(input).getBytes(UTF_8));
    StringWriter out = new StringWriter();
    Main main =
        new Main(
            new PrintWriter(out, true),
            new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.err, UTF_8)), true),
            in);
    assertThat(main.format("--skip-reflowing-long-strings", "-")).isEqualTo(0);
    assertThat(out.toString()).isEqualTo(joiner.join(expected));
  }

  @Test
  public void noFormatJavadoc() throws Exception {
    String[] input = {
      "/**",
      " * graph",
      " *",
      " * graph",
      " *",
      " * @param foo lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do"
          + " eiusmod tempor incididunt ut labore et dolore magna aliqua",
      " */",
      "class Test {",
      "  /**",
      "   * creates entropy",
      "   */",
      "  public static void main(String... args) {}",
      "}",
      "",
    };
    InputStream in = new ByteArrayInputStream(joiner.join(input).getBytes(UTF_8));
    StringWriter out = new StringWriter();
    Main main =
        new Main(
            new PrintWriter(out, true),
            new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.err, UTF_8)), true),
            in);
    assertThat(main.format("--skip-javadoc-formatting", "-")).isEqualTo(0);
    assertThat(out.toString()).isEqualTo(joiner.join(input));
  }

  @Test
  public void reorderModifiersOptionTest() throws Exception {
    String[] input = {
      "class Test {", //
      "  static public void main(String... args) {}",
      "}",
      "",
    };
    String[] fixed = {
      "class Test {", //
      "  public static void main(String... args) {}",
      "}",
      "",
    };
    String source = joiner.join(input);
    assertThat(new Formatter(JavaFormatterOptions.builder().build()).formatSource(source))
        .isEqualTo(joiner.join(fixed));
    assertThat(
            new Formatter(JavaFormatterOptions.builder().reorderModifiers(false).build())
                .formatSource(source))
        .isEqualTo(source);
  }
}
