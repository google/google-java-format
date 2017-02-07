/*
 * Copyright 2017 Google Inc.
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.googlejavaformat.java.SnippetFormatter.SnippetKind;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link SnippetFormatter}Test */
@RunWith(JUnit4.class)
public class SnippetFormatterTest {
  @Test
  public void expression() throws FormatterException {
    String input = "x\n=42";
    List<Replacement> replacements =
        new SnippetFormatter()
            .format(
                SnippetKind.EXPRESSION,
                input,
                ImmutableList.of(Range.closedOpen(0, input.length())),
                4,
                false);
    assertThat(replacements)
        .containsExactly(
            Replacement.create(Range.closedOpen(1, 2), " "),
            Replacement.create(Range.closedOpen(3, 3), " "));
  }

  @Test
  public void statement() throws FormatterException {
    String input = "int x\n=42;";
    List<Replacement> replacements =
        new SnippetFormatter()
            .format(
                SnippetKind.STATEMENTS,
                input,
                ImmutableList.of(Range.closedOpen(0, input.length())),
                4,
                false);
    assertThat(replacements)
        .containsExactly(
            Replacement.create(Range.closedOpen(5, 6), " "),
            Replacement.create(Range.closedOpen(7, 7), " "));
  }

  @Test
  public void classMember() throws FormatterException {
    String input = "void f() {\n}";
    List<Replacement> replacements =
        new SnippetFormatter()
            .format(
                SnippetKind.CLASS_BODY_DECLARATIONS,
                input,
                ImmutableList.of(Range.closedOpen(0, input.length())),
                4,
                false);
    assertThat(replacements).containsExactly(Replacement.create(Range.closedOpen(10, 11), ""));
  }

  @Test
  public void compilation() throws FormatterException {
    String input = "/** a\nb*/\nclass Test {\n}";
    List<Replacement> replacements =
        new SnippetFormatter()
            .format(
                SnippetKind.COMPILATION_UNIT,
                input,
                ImmutableList.of(Range.closedOpen(input.indexOf("class"), input.length())),
                4,
                false);
    assertThat(replacements).containsExactly(Replacement.create(Range.closedOpen(22, 23), ""));
  }

  @Test
  public void compilationWithComments() throws FormatterException {
    String input = "/** a\nb*/\nclass Test {\n}";
    List<Replacement> replacements =
        new SnippetFormatter()
            .format(
                SnippetKind.COMPILATION_UNIT,
                input,
                ImmutableList.of(Range.closedOpen(0, input.length())),
                4,
                true);
    assertThat(replacements)
        .containsExactly(
            Replacement.create(Range.closedOpen(0, 24), "/** a b */\nclass Test {}\n"));
  }
}
