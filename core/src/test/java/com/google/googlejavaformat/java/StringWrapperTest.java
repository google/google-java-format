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

import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Range;
import com.google.googlejavaformat.java.JavaFormatterOptions.Style;
import java.util.Random;
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

  // https://github.com/google/google-java-format/issues/1186
  @Test
  public void escapedBackslashBeforeT() throws Exception {
    String input =
        """
        class T {
          String t =
           "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789\\\\t";
        }
        """;
    String output =
        """
        class T {
          String t =
              "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789\\\\t";
        }
        """;
    assertThat(new Formatter().formatSourceAndFixImports(input)).isEqualTo(output);
  }

  // https://github.com/google/google-java-format/issues/1439
  @Test
  public void escapedBackslashes() throws Exception {
    String input =
        """
        class T {
          String s = "%s xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\\\\text{C} \\\\n \\\\\\\\";
        }
        """
            .formatted("word ".repeat(20));
    String output =
        """
        class T {
          String s =
              "word word word word word word word word word word word word word word word word word word"
                  + " word word "
                  + " xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\\\\text{C} \\\\n"
                  + " \\\\\\\\";
        }
        """;
    assertThat(new Formatter().formatSourceAndFixImports(input)).isEqualTo(output);
  }

  @Test
  public void escapesRandomized() throws Exception {
    // The source text of string literal characters and escape sequences, including escaped
    // backslashes followed by characters that would otherwise form escapes, and octal and Unicode
    // escapes.
    String[] atoms = {
      "a",
      "bb",
      " ",
      "\\\\",
      "\\t",
      "\\n",
      "\\r\\n",
      "\\\"",
      "\\\\t",
      "\\\\n",
      "\\\\\\t",
      "\\0",
      "\\12",
      "\\377",
      "\\s",
      "\\u0041",
      "\\\\u0041"
    };
    Random random = new Random(42);
    Formatter formatter = new Formatter();
    for (int i = 0; i < 200; i++) {
      StringBuilder literal = new StringBuilder();
      while (literal.length() < 150) {
        literal.append(atoms[random.nextInt(atoms.length)]);
      }
      String input = "class T {\n  String s = \"" + literal + "\";\n}\n";
      // StringWrapper checks that its output parses and has the same AST as its input. (This uses
      // `unused` rather than `_` since google-java-format still supports building with JDK 21.)
      String unused = StringWrapper.wrap(40, formatter.formatSource(input), formatter);
    }
  }

  // https://github.com/google/google-java-format/issues/1114
  @Test
  public void wrap_onlyInCharacterRanges() throws Exception {
    String input =
        """
        class T {
          String s = "one long incredibly unbroken sentence moving from topic to topic so that no one had a chance to interrupt";
          String t = "one long incredibly unbroken sentence moving from topic to topic so that no one had a chance to interrupt";
        }
        """;
    Formatter formatter = new Formatter();
    int tStart = input.indexOf("String t");
    int tEnd = input.indexOf(';', tStart);
    assertThat(
            StringWrapper.wrap(
                100, input, formatter, ImmutableRangeSet.of(Range.closedOpen(tStart, tEnd))))
        .isEqualTo(
            """
            class T {
              String s = "one long incredibly unbroken sentence moving from topic to topic so that no one had a chance to interrupt";
              String t =
                  "one long incredibly unbroken sentence moving from topic to topic so that no one had a chance"
                      + " to interrupt";
            }
            """);
    // A range that does not intersect any string literal returns the input unchanged.
    assertThat(
            StringWrapper.wrap(100, input, formatter, ImmutableRangeSet.of(Range.closedOpen(0, 9))))
        .isEqualTo(input);
  }

  @Test
  public void wrap_mapsCharacterRangesAfterFirstPass() throws Exception {
    // The first pass formats the statement containing `t`, collapsing the extra spaces before the
    // literal so its range in the formatted output is disjoint from its original range in `input`.
    String input =
        """
        class T {
          String s = "one long incredibly unbroken sentence moving from topic to topic so that no one had a chance to interrupt";
          String t = %s"one long incredibly unbroken sentence moving from topic to topic so that no one had a chance to interrupt";
        }
        """
            .formatted(" ".repeat(150));
    int tLiteralStart = input.lastIndexOf("\"one");
    int tLiteralEnd = input.indexOf(';', tLiteralStart);
    assertThat(
            StringWrapper.wrap(
                100,
                input,
                new Formatter(),
                ImmutableRangeSet.of(Range.closedOpen(tLiteralStart, tLiteralEnd))))
        .isEqualTo(
            """
            class T {
              String s = "one long incredibly unbroken sentence moving from topic to topic so that no one had a chance to interrupt";
              String t =
                  "one long incredibly unbroken sentence moving from topic to topic so that no one had a chance"
                      + " to interrupt";
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
