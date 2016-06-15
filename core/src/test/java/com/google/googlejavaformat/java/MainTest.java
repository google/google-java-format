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

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;

/**
 * Tests for {@link Main}.
 */
@RunWith(JUnit4.class)
public class MainTest {

  @Rule public TemporaryFolder testFolder = new TemporaryFolder();

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
  public void cantImportSortRangeYet() throws UsageException {
    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();
    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true), System.in);
    assertThat(main.format("-sort-imports=only", "-lines=5:10", "-")).isEqualTo(1);
    assertThat(err.toString()).contains(
        "--sort-imports can currently only apply to the whole file");
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
        new Main(new PrintWriter(System.out, true), new PrintWriter(System.err, true), System.in);
    int errorCode = main.format("-replace", path.toAbsolutePath().toString());
    assertThat(errorCode).named("Error Code").isEqualTo(0);
  }

  @Test
  public void testMain() throws Exception {
    Process process =
        new ProcessBuilder(
                ImmutableList.of(
                    Paths.get(System.getProperty("java.home")).resolve("bin/java").toString(),
                    "-cp",
                    System.getProperty("java.class.path"),
                    Main.class.getName()))
            .redirectError(Redirect.PIPE)
            .redirectOutput(Redirect.PIPE)
            .start();
    process.waitFor();
    String err = new String(ByteStreams.toByteArray(process.getErrorStream()), UTF_8);
    assertThat(err).contains("Usage: google-java-format");
    assertThat(process.exitValue()).isEqualTo(0);
  }
}
