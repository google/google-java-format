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
import static org.junit.Assert.fail;

import com.google.common.collect.Range;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link CommandLineOptionsParser}Test */
@RunWith(JUnit4.class)
public class CommandLineOptionsParserTest {

  @Test
  public void defaults() {
    CommandLineOptions options = CommandLineOptionsParser.parse(Collections.<String>emptyList());
    assertThat(options.files()).isEmpty();
    assertThat(options.stdin()).isFalse();
    assertThat(options.aosp()).isFalse();
    assertThat(options.help()).isFalse();
    assertThat(options.lengths()).isEmpty();
    assertThat(options.lines().asRanges()).isEmpty();
    assertThat(options.offsets()).isEmpty();
    assertThat(options.inPlace()).isFalse();
    assertThat(options.version()).isFalse();
    assertThat(options.sortImports()).isTrue();
    assertThat(options.removeUnusedImports()).isTrue();
  }

  @Test
  public void hello() {
    CommandLineOptions options =
        CommandLineOptionsParser.parse(
            Arrays.asList("-lines=1:10,20:30", "-i", "Hello.java", "Goodbye.java"));
    assertThat(options.lines().asRanges())
        .containsExactly(Range.closedOpen(0, 10), Range.closedOpen(19, 30));
    assertThat(options.inPlace()).isTrue();
    assertThat(options.files()).containsExactly("Hello.java", "Goodbye.java");
  }

  @Test
  public void stdin() {
    assertThat(CommandLineOptionsParser.parse(Arrays.asList("-")).stdin()).isTrue();
  }

  @Test
  public void aosp() {
    assertThat(CommandLineOptionsParser.parse(Arrays.asList("-aosp")).aosp()).isTrue();
  }

  @Test
  public void help() {
    assertThat(CommandLineOptionsParser.parse(Arrays.asList("-help")).help()).isTrue();
  }

  @Test
  public void lengths() {
    assertThat(
            CommandLineOptionsParser.parse(Arrays.asList("-length", "1", "--length", "2"))
                .lengths())
        .containsExactly(1, 2);
  }

  @Test
  public void lines() {
    assertThat(
            CommandLineOptionsParser.parse(
                    Arrays.asList("--lines", "1:2", "-lines=4:5", "--line", "7:8", "-line=10:11"))
                .lines()
                .asRanges())
        .containsExactly(
            Range.closedOpen(0, 2),
            Range.closedOpen(3, 5),
            Range.closedOpen(6, 8),
            Range.closedOpen(9, 11));
  }

  @Test
  public void offset() {
    assertThat(
            CommandLineOptionsParser.parse(Arrays.asList("-offset", "1", "--offset", "2"))
                .offsets())
        .containsExactly(1, 2);
  }

  @Test
  public void inPlace() {
    assertThat(CommandLineOptionsParser.parse(Arrays.asList("-i")).inPlace()).isTrue();
  }

  @Test
  public void version() {
    assertThat(CommandLineOptionsParser.parse(Arrays.asList("-v")).version()).isTrue();
  }

  @Test
  public void skipSortingImports() {
    assertThat(
            CommandLineOptionsParser.parse(Arrays.asList("--skip-sorting-imports")).sortImports())
        .isFalse();
  }

  @Test
  public void skipRemovingUnusedImports() {
    assertThat(
            CommandLineOptionsParser.parse(Arrays.asList("--skip-removing-unused-imports"))
                .removeUnusedImports())
        .isFalse();
  }

  // TODO(cushon): consider handling this in the parser and reporting a more detailed error
  @Test
  public void illegalLines() {
    try {
      CommandLineOptionsParser.parse(Arrays.asList("-lines=1:1", "-lines=1:1"));
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains("overlap");
    }
  }
}
