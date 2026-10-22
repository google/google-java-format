/*
 * Copyright 2019 Google Inc.
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
import static org.junit.Assume.assumeTrue;

import com.google.googlejavaformat.java.JavaFormatterOptions.Style;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/// [StringWrapper]Test
@RunWith(JUnit4.class)
public class StringWrapperTest {
  @Test
  public void testAwkwardLineEndWrapping() throws Exception {
    String input =
"""
class T {
  String s = someMethodWithQuiteALongNameThatWillGetUsUpCloseToTheColumnLimit() + "foo bar foo bar foo bar";

  String someMethodWithQuiteALongNameThatWillGetUsUpCloseToTheColumnLimit() {
    return null;
  }
}
""";
    String output =
        """
        class T {
          String s =
              someMethodWithQuiteALongNameThatWillGetUsUpCloseToTheColumnLimit()
                  + "foo bar foo bar foo bar";

          String someMethodWithQuiteALongNameThatWillGetUsUpCloseToTheColumnLimit() {
            return null;
          }
        }
        """;

    assertThat(StringWrapper.wrap(100, input, new Formatter())).isEqualTo(output);
  }

  @Test
  public void textBlock() throws Exception {
    assumeTrue(Runtime.version().feature() >= 15);
    String input =
"""
package com.mypackage;
public class ReproBug {
  private String myString;
  private ReproBug() {
    String str =
\"""
{"sourceEndpoint":"ri.something.1-1.object-internal.1","targetEndpoint":"ri.something.1-1.object-internal.2","typeId":"typeId"}\\
\""";
    myString = str;
  }
}
""";
    assertThat(StringWrapper.wrap(100, input, new Formatter())).isEqualTo(input);
  }

  @Test
  public void wrapWithMaxLineLengthAndTabs() throws Exception {
    String input =
        """
        class T {
          String s = "one two three four five six seven eight";
          String tb =
              \"""
              hello
              world
              \""";
        }
        """;
    Formatter formatter =
        new Formatter(
            JavaFormatterOptions.builder()
                .style(Style.GOOGLE.toBuilder().maxLineLength(35).useTabs(true).build())
                .build());
    assertThat(formatter.formatSourceAndFixImports(input))
        .isEqualTo(
            """
            class T {
            \tString s =
            \t\t\t"one two three four five six"
            \t\t\t\t\t+ " seven eight";
            \tString tb =
            \t\t\t\"""
            \t\t\thello
            \t\t\tworld
            \t\t\t\""";
            }
            """);
  }

  /**
   * Wrapping a long string that does not start at the beginning of its statement (e.g. after
   * {@code return}) must produce output that is stable under a subsequent {@link
   * Formatter#formatSource} pass. See https://github.com/google/google-java-format/issues/935.
   */
  @Test
  public void wrapThenFormatIsIdempotentForPrefixedString() throws Exception {
    String input =
        """
        public class JavaFormatterExample {

          public static String veryLongString() {
            return "Very, very, very, very, very, very, very, very, very, very, very long string that will need to be split";
          }
        }
        """;
    Formatter formatter = new Formatter();
    String wrapped = StringWrapper.wrap(100, formatter.formatSource(input), formatter);
    String reformatted = formatter.formatSource(wrapped);
    assertThat(reformatted).isEqualTo(wrapped);
    // And a second wrap+format round trip must not change anything either.
    assertThat(StringWrapper.wrap(100, reformatted, formatter)).isEqualTo(wrapped);
  }
}
