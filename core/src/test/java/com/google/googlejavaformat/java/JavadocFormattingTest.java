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

import com.google.common.base.Joiner;
import com.google.common.truth.Truth;
import com.google.googlejavaformat.java.JavaFormatterOptions.JavadocFormatter;
import com.google.googlejavaformat.java.JavaFormatterOptions.SortImports;
import com.google.googlejavaformat.java.JavaFormatterOptions.Style;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests formatting javadoc. */
@RunWith(JUnit4.class)
public final class JavadocFormattingTest {

  public final Formatter formatter =
      new Formatter(
          new JavaFormatterOptions(JavadocFormatter.ECLIPSE, Style.GOOGLE, SortImports.NO));

  @Test
  public void simple() throws Exception {
    String[] input = {
      "/** */", //
      "class Test {}",
    };
    String[] expected = {
      "/** */", //
      "class Test {}",
      "",
    };
    String actual = formatter.formatSource(Joiner.on('\n').join(input));
    Truth.assertThat(actual).isEqualTo(Joiner.on('\n').join(expected));
  }

  @Test
  public void javaCodeInPre() throws Exception {
    String[] input = {
      "/**",
      " *<pre>",
      " * aaaaa    |   a  |   +",
      " * \"bbbb    |   b  |  \"",
      " *</pre>",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/**",
      " * <pre>",
      " * aaaaa    |   a  |   +",
      " * \"bbbb    |   b  |  \"",
      " *</pre>",
      " */",
      "class Test {}",
      "",
    };
    String actual = formatter.formatSource(Joiner.on('\n').join(input));
    Truth.assertThat(actual).isEqualTo(Joiner.on('\n').join(expected));
  }

  @Test
  public void joinLines() throws Exception {
    String[] input = {
      "/**", //
      " * foo",
      " * bar",
      " * baz",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/**", //
      " * foo bar baz",
      " */",
      "class Test {}",
      "",
    };
    String actual = formatter.formatSource(Joiner.on('\n').join(input));
    Truth.assertThat(actual).isEqualTo(Joiner.on('\n').join(expected));
  }

  @Test
  public void blankLineBeforeParams() throws Exception {
    String[] input = {
      "/**", //
      " * hello world",
      " * @param this is a param",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/**", //
      " * hello world",
      " *",
      " * @param this is a param",
      " */",
      "class Test {}",
      "",
    };
    String actual = formatter.formatSource(Joiner.on('\n').join(input));
    Truth.assertThat(actual).isEqualTo(Joiner.on('\n').join(expected));
  }

  @Test
  public void paragraphTag() throws Exception {
    String[] input = {
      "/**", //
      " * hello<p>world",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/**", //
      " * hello",
      " *",
      " * <p>world",
      " */",
      "class Test {}",
      "",
    };
    String actual = formatter.formatSource(Joiner.on('\n').join(input));
    Truth.assertThat(actual).isEqualTo(Joiner.on('\n').join(expected));
  }
}
