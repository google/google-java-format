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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

      String usage = e.usage();


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
  public void preserveOriginalFile() throws Exception {
    Path from = Paths.get("src/test/resources/com/google/googlejavaformat/java/testdata/");
    Path temp = testFolder.newFolder("preserve").toPath();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(from, "*.output")) {
      for (Path path : stream) {
        Path java = temp.resolve(path.toFile().getName().replace(".output", ".java"));
        Files.copy(path, java);
        preserveOriginalFile(java.toAbsolutePath());
      }
    }
  }

  private void preserveOriginalFile(Path java) throws Exception {
    long expected = Files.getLastModifiedTime(java).toMillis();
    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();
    Main main = new Main(new PrintWriter(out, true), new PrintWriter(err, true), System.in);
    int errorCode = main.format("-replace", java.toAbsolutePath().toString());
    assertThat(errorCode).named("Error Code").isEqualTo(0);
    long actual = Files.getLastModifiedTime(java).toMillis();
    assertThat(actual).named("Last modified time changed: " + java).isEqualTo(expected);
  }
}
