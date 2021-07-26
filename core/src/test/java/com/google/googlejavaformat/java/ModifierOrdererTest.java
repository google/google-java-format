/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.google.googlejavaformat.java;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Joiner;
import com.google.common.collect.Range;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link ModifierOrderer}Test */
@RunWith(JUnit4.class)
public class ModifierOrdererTest {

  @Test
  public void simple() throws FormatterException {
    assertThat(ModifierOrderer.reorderModifiers("static abstract class InnerClass {}").getText())
        .isEqualTo("abstract static class InnerClass {}");
  }

  @Test
  public void comment() throws FormatterException {
    assertThat(ModifierOrderer.reorderModifiers("static/*1*/abstract/*2*/public").getText())
        .isEqualTo("public/*1*/abstract/*2*/static");
  }

  @Test
  public void everything() throws FormatterException {
    assertThat(
            ModifierOrderer.reorderModifiers(
                    "strictfp native synchronized volatile transient final static abstract"
                        + " private protected public")
                .getText())
        .isEqualTo(
            "public protected private abstract static final transient volatile synchronized"
                + " native strictfp");
  }

  @Test
  public void everythingIncludingDefault() throws FormatterException {
    assertThat(
            ModifierOrderer.reorderModifiers(
                    "strictfp native synchronized volatile transient final static default abstract"
                        + " private protected public")
                .getText())
        .isEqualTo(
            "public protected private abstract default static final transient volatile synchronized"
                + " native strictfp");
  }

  @Test
  public void subRange() throws FormatterException {
    String[] lines = {
      "class Test {", //
      "  static public int a;",
      "  static public int b;",
      "}",
    };
    String input = Joiner.on('\n').join(lines);
    String substring = "static public int a";
    int start = input.indexOf(substring);
    int end = start + substring.length();
    String output =
        ModifierOrderer.reorderModifiers(
                new JavaInput(input), Arrays.asList(Range.closedOpen(start, end)))
            .getText();
    assertThat(output).contains("public static int a;");
    assertThat(output).contains("static public int b;");
  }

  @Test
  public void whitespace() throws FormatterException {
    String[] lines = {
      "class Test {", //
      "  static",
      "  public int a;",
      "}",
    };
    String input = Joiner.on('\n').join(lines);
    String substring = "static public int a";
    int start = input.indexOf(substring);
    int end = start + substring.length();
    String output =
        ModifierOrderer.reorderModifiers(
                new JavaInput(input), Arrays.asList(Range.closedOpen(start, end)))
            .getText();
    assertThat(output).contains("public\n  static int a;");
  }
}
