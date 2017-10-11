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

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests for array dimension handling, especially mixed array notation and type annotations on
 * dimensions.
 */
@RunWith(Parameterized.class)
public class ArrayDimensionTest {
  @Parameters
  public static Iterable<Object[]> parameters() {
    String[] inputs = {
      // mixed array notation multi-variable declarations
      "int a[], b @B [], c @B [][] @C [];",
      "int a @A [], b @B [], c @B [] @C [];",
      "int a[] @A [], b @B [], c @B [] @C [];",
      "int a, b @B [], c @B [] @C [];",
      "int @A [] a, b @B [], c @B [] @C [];",
      "int @A [] a = {}, b @B [] = {{}}, c @B [] @C [] = {{{}}};",
      // mixed array notation methods
      "int[][][][][] argh()[] @A @B [] @C @D [][] {}",
      "int[][] @T [] @U [] @V @W [][][] argh() @A @B [] @C @D [] {}",
      "int e1() @A [] {}",
      "int f1()[] @A [] {}",
      "int g1() @A [] @B [] {}",
      "int h1() @A @B [] @C @B [] {}",
      "int[] e2() @A [] {}",
      "int @X [] f2()[] @A [] {}",
      "int[] g2() @A [] @B [] {}",
      "int @X [] h2() @A @B [] @C @B [] {}",
      "@X int[] e3() @A [] {}",
      "@X int @Y [] f3()[] @A [] {}",
      "@X int @Y [] g3() @A [] @B [] {}",
      "@X int[] h3() @A @B [] @C @B [] {}",
      // mixed array notation single-variable declarations
      "int[] e2() @A [] {}",
      "int @I [] f2()[] @A [] {}",
      "int[] @J [] g2() @A [] @B [] {}",
      "int @I [] @J [] h2() @A @B [] @C @B [] {}",
      "int a1[];",
      "int b1 @A [];",
      "int c1[] @A [];",
      "int d1 @A [] @B [];",
      "int[] a2[];",
      "int @A [] b2 @B [];",
      "int[] c2[] @A [];",
      "int @A [] d2 @B [] @C [];",
      // array dimension expressions
      "int[][] a0 = new @A int[0];",
      "int[][] a1 = new int @A [0] @B [];",
      "int[][] a2 = new int[0] @A [] @B [];",
      "int[][] a4 = new int[0] @A [][] @B [];",
      // nested array type uses
      "List<int @A [] @B []> xs;",
      "List<int[] @A [][] @B []> xs;",
    };
    return Iterables.transform(Arrays.asList(inputs), input -> new Object[] {input});
  }

  private final String input;

  public ArrayDimensionTest(String input) {
    this.input = input;
  }

  @Test
  public void format() throws Exception {
    String source = "class T {" + input + "}";
    String formatted = new Formatter().formatSource(source);
    String statement =
        formatted.substring("class T {".length(), formatted.length() - "}\n".length());
    // ignore line breaks after declaration-style annotations
    statement =
        Joiner.on(' ').join(Splitter.on('\n').omitEmptyStrings().trimResults().split(statement));
    assertThat(statement).isEqualTo(input);
  }
}
