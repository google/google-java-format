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
import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.common.truth.TruthJUnit.assume;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.io.ByteStreams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/// Tests formatting javadoc.
@RunWith(JUnit4.class)
public final class JavadocFormattingTest {
  private static final boolean MARKDOWN_JAVADOC_SUPPORTED = Runtime.version().feature() >= 23;

  private final Formatter formatter = new Formatter();

  /// Tests that the formatter formats the given input string to the given expected string. Also
  /// tests that the formatter is idempotent when formatting an already formatted string.
  private void doFormatTest(String input, String expected) {
    try {
      String actual = formatter.formatSource(input);
      assertThat(actual).isEqualTo(expected);
      String reformatted = formatter.formatSource(actual);
      assertWithMessage("When checking idempotency").that(reformatted).isEqualTo(actual);
    } catch (FormatterException e) {
      throw new AssertionError(e);
    }
  }

  @Test
  public void notJavadoc() {
    String input =
        """
        /**/
        class Test {}\
        """;
    String expected =
        """
        /**/
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void empty() {
    String input =
        """
        /***/
        class Test {}\
        """;
    String expected =
        """
        /***/
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void emptyMultipleLines() {
    String input =
        """
        /**
         */
        class Test {}\
        """;
    String expected =
        """
        /** */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void simple() {
    String input =
        """
        /** */
        class Test {}\
        """;
    String expected =
        """
        /** */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void commentMostlyUntouched() {
    String input =
        """
        /**
         * Foo.
         *
         *  <!--
        *abc
         *   def   \s
         * </tr>
         *-->bar
         */
        class Test {}\
        """;
    String expected =
        """
        /**
         * Foo.
         * <!--
         * abc
         *   def
         * </tr>
         * -->
         * bar
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownHtmlComment() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        /// <!--
        /// abc
        /// -->
        class Test {}
        """;
    String expected = input;
    doFormatTest(input, expected);
  }

  @Test
  public void moeComments() {
    // We replace moe by MOE to avoid triggering actual MOE rewriting.
    String input =
        """
        /**
         * Deatomizes the given user.
         * <!-- moe:begin_intracomment_strip -->
         * See deatomizer-v5 for the design doc.
         * <!-- moe:end_intracomment_strip -->
         * To reatomize, call {@link reatomize}.
         *
         * <!-- moe:begin_intracomment_strip -->
         * <p>This method is used in the Google teleporter.
         *
         * <p>Yes, we have a teleporter.
         * <!-- moe:end_intracomment_strip -->
         *
         * @param user the person to teleport.
         *     <!-- moe:begin_intracomment_strip -->
         *     Users must sign deatomize-waiver ahead of time.
         *     <!-- moe:end_intracomment_strip -->
         * <!-- moe:begin_intracomment_strip -->
         * @deprecated Sometimes turns the user into a goat.
         * <!-- moe:end_intracomment_strip -->
         */
        class Test {}\
        """
            .replace("moe", "MOE");
    String expected =
        """
        /**
         * Deatomizes the given user.
         * <!-- moe:begin_intracomment_strip -->
         * See deatomizer-v5 for the design doc.
         * <!-- moe:end_intracomment_strip -->
         * To reatomize, call {@link reatomize}.
         *
         * <!-- moe:begin_intracomment_strip -->
         * <p>This method is used in the Google teleporter.
         *
         * <p>Yes, we have a teleporter.
         * <!-- moe:end_intracomment_strip -->
         *
         * @param user the person to teleport.
         *     <!-- moe:begin_intracomment_strip -->
         *     Users must sign deatomize-waiver ahead of time.
         *     <!-- moe:end_intracomment_strip -->
         * <!-- moe:begin_intracomment_strip -->
         * @deprecated Sometimes turns the user into a goat.
         * <!-- moe:end_intracomment_strip -->
         */
        class Test {}
        """
            .replace("moe", "MOE");
    doFormatTest(input, expected);
  }

  @Test
  public void moeCommentBeginOnlyInMiddleOfDoc() {
    // We don't really care what happens here so long as we don't explode.
    String input =
        """
        /**
         * Foo.
         * <!-- moe:begin_intracomment_strip -->
         * Bar.
         */
        class Test {}\
        """
            .replace("moe", "MOE");
    String expected =
        """
        /**
         * Foo.
         * <!-- moe:begin_intracomment_strip -->
         * Bar.
         */
        class Test {}
        """
            .replace("moe", "MOE");
    doFormatTest(input, expected);
  }

  @Test
  public void moeCommentBeginOnlyAtEndOfDoc() {
    // We don't really care what happens here so long as we don't explode.
    // TODO(cpovirk): OK, maybe try to leave it in....
    String input =
        """
        /**
         * Foo.
         * <!-- moe:begin_intracomment_strip -->
         */
        class Test {}\
        """
            .replace("moe", "MOE");
    String expected =
        """
        /** Foo. */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void moeCommentEndOnly() {
    // We don't really care what happens here so long as we don't explode.
    String input =
        """
        /**
         * Foo.
         * <!-- moe:end_intracomment_strip -->
         */
        class Test {}\
        """
            .replace("moe", "MOE");
    String expected =
        """
        /**
         * Foo.
         * <!-- moe:end_intracomment_strip -->
         */
        class Test {}
        """
            .replace("moe", "MOE");
    doFormatTest(input, expected);
  }

  @Test
  public void moeCommentAtStartOfDoc() {
    String input =
        """
        /**
         * <!-- moe:begin_intracomment_strip -->
         * Foo.
         * <!-- moe:end_intracomment_strip -->
         */
        class Test {}\
        """
            .replace("moe", "MOE");
    String expected =
        """
        /**
         * <!-- moe:begin_intracomment_strip -->
         * Foo.
         * <!-- moe:end_intracomment_strip -->
         */
        class Test {}
        """
            .replace("moe", "MOE");
    doFormatTest(input, expected);
  }

  @Test
  public void tableMostlyUntouched() {
    String input =
        """
        /**
         * Foo.
         *
         *  <table>
        *<tr><td>a<td>b</tr>
         * <tr>
         * <td>A
         *     <td>B
         * </tr>
         *</table>
         */
        class Test {}\
        """;
    String expected =
        """
        /**
         * Foo.
         *
         * <table>
         * <tr><td>a<td>b</tr>
         * <tr>
         * <td>A
         *     <td>B
         * </tr>
         * </table>
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void preMostlyUntouched() {
    /*
     * Arguably we shouldn't insert the space between "*" and "4," since doing so changes the
     * rendered HTML output (by inserting a space there). However, inserting a space between "*" and
     * "</pre>" (which has no impact on the rendered HTML AFAIK) is a good thing, and inserting
     * similar spaces in the case of <table> is good, too. And if "* 4" breaks a user's intended
     * formatting, that user can fix it up, just as that user would have to fix up the "*4"
     * style violation. The main downside to "* 4" is that the user might not notice that we made
     * the change at all. (We've also slightly complicated NEWLINE_PATTERN and writeNewline to
     * accommodate it.)
     */
    @SuppressWarnings("MisleadingEscapedSpace") // TODO(b/496180372): remove
    String input =
        """
        /**
         * Example:
         *
         *  <pre>
        *    1 2<br>    3   \s
        *4 5 6
        7 8
         *</pre>
         */
        class Test {}\
        """;
    String expected =
        """
        /**
         * Example:
         *
         * <pre>
         *    1 2<br>    3
         * 4 5 6
         * 7 8
         * </pre>
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void preCodeExample() {
    // We should figure out whether we want a newline or blank line before <pre> or not.
    String input =
        """
        /**
         * Example:
         *
         * <pre>   {@code
         *
         *   Abc.def(foo, 7, true); // blah}</pre>
         */
        class Test {}\
        """;
    String expected =
        """
        /**
         * Example:
         *
         * <pre>{@code
         * Abc.def(foo, 7, true); // blah
         * }</pre>
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void preNotWrapped() {
    String input =
        """
        /**
         * Example:
         *
         * <pre>
         * 456789012 456789012 456789012 456789012 456789012 456789012 456789012 456789012 456789012 45678901
         * </pre>
         */
        class Test {}\
        """;
    String expected =
        """
        /**
         * Example:
         *
         * <pre>
         * 456789012 456789012 456789012 456789012 456789012 456789012 456789012 456789012 456789012 45678901
         * </pre>
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void javaCodeInPre() {
    String input =
        """
        /**
         * Example:
         *
         *<pre>
         * aaaaa    |   a  |   +
         * "bbbb    |   b  |  "
         *</pre>
         */
        class Test {}\
        """;
    String expected =
        """
        /**
         * Example:
         *
         * <pre>
         * aaaaa    |   a  |   +
         * "bbbb    |   b  |  "
         * </pre>
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void joinLines() {
    String input =
        """
        /**
         * foo
         * bar
         * baz
         */
        class Test {}\
        """;
    String expected =
        """
        /** foo bar baz */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void oneLinerIs100() {
    String input =
        """
        /**
         * 567890123 567890123 567890123 567890123 567890123 567890123 567890123 567890123 567890123 567
         */
        class Test {}\
        """;
    String expected =
        """
        /** 567890123 567890123 567890123 567890123 567890123 567890123 567890123 567890123 567890123 567 */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void oneLinerWouldBe101() {
    String input =
        """
        /**
         * 567890123 567890123 567890123 567890123 567890123 567890123 567890123 567890123 567890123 5678
         */
        class Test {}\
        """;
    String expected =
        """
        /**
         * 567890123 567890123 567890123 567890123 567890123 567890123 567890123 567890123 567890123 5678
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void multilineWrap() {
    String input =
        """
        /**
         * 456789012 456789012 456789012 456789012 456789012 456789012 456789012 456789012 456789012 45678901
         */
        class Test {}\
        """;
    String expected =
        """
        /**
         * 456789012 456789012 456789012 456789012 456789012 456789012 456789012 456789012 456789012
         * 45678901
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void tooLong() {
    String input =
        """
        /**
         * abc
         *
         * <p>7890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456
         */
        class Test {}\
        """;
    String expected =
        """
        /**
         * abc
         *
         * <p>7890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void joinedTokens() {
    /*
     * Originally, 4, <b>, and 8901 are separate tokens. Test that we join them (and thus don't
     * split them across lines).
     */
    String input =
        """
        /**
         * 456789012 456789012 456789012 456789012 456789012 456789012 456789012 456789012 456789012 4<b>8901
         */
        class Test {}\
        """;
    String expected =
        """
        /**
         * 456789012 456789012 456789012 456789012 456789012 456789012 456789012 456789012 456789012
         * 4<b>8901
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void joinedAtSign() {
    /*
     * The last 456789012 would fit on the first line with the others. But putting it there would
     * mean the next line would start with @5678901, which would then be interpreted as a tag.
     */
    String input =
        """
        /**
         * 456789012 456789012 456789012 456789012 456789012 456789012 456789012 456789012 456789012 @5678901
         */
        class Test {}\
        """;
    String expected =
        """
        /**
         * 456789012 456789012 456789012 456789012 456789012 456789012 456789012 456789012
         * 456789012 @5678901
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void joinedMultipleAtSign() {
    // This is the same as above except that it tests multiple consecutive @... tokens.
    String input =
        """
        /**
         * 456789012 456789012 456789012 456789012 456789012 456789012 456789012 456789012 @56789012 @5678901
         */
        class Test {}\
        """;
    String expected =
        """
        /**
         * 456789012 456789012 456789012 456789012 456789012 456789012 456789012
         * 456789012 @56789012 @5678901
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void noAsterisk() {
    String input =
        """
        /**
         abc<p>def
         */
        class Test {}\
        """;
    String expected =
        """
        /**
         * abc
         *
         * <p>def
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void significantAsterisks() {
    String input =
        """
        /** *
         * *
         */
        class Test {}\
        """;
    String expected =
        """
        /** * * */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void links() {
    String input =
        """
        /**
         * 456789012 456789012 456789012 456789012 456789012 456789012 456789012 456789012 456789012 4567 <a
         * href=foo>foo</a>.
         *
         * <p>789012 456789012 456789012 456789012 456789012 456789012 456789012 456789 <a href=foo>
         * foo</a>.
         *
         * <p>789012 456789012 456789012 456789012 456789012 456789012 456789012 4567890 <a href=foo>
         * foo</a>.
         *
         * <p><a href=foo>
         * foo</a>.
         *
         * <p>foo <a href=bar>
         * bar</a>.
         *
         * <p>foo-<a href=bar>
         * bar</a>.
         *
         * <p>foo<a href=bar>
         * bar</a>.
         *
         * <p><a href=foo>foo</a> bar.
         */
        class Test {}\
        """;
    String expected =
        """
        /**
         * 456789012 456789012 456789012 456789012 456789012 456789012 456789012 456789012 456789012 4567 <a
         * href=foo>foo</a>.
         *
         * <p>789012 456789012 456789012 456789012 456789012 456789012 456789012 456789 <a href=foo>foo</a>.
         *
         * <p>789012 456789012 456789012 456789012 456789012 456789012 456789012 4567890 <a href=foo>
         * foo</a>.
         *
         * <p><a href=foo>foo</a>.
         *
         * <p>foo <a href=bar>bar</a>.
         *
         * <p>foo-<a href=bar>bar</a>.
         *
         * <p>foo<a href=bar>bar</a>.
         *
         * <p><a href=foo>foo</a> bar.
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void heading() {
    String input =
        """
        /**
         * abc<h1>def</h1>ghi
         */
        class Test {}\
        """;
    String expected =
        """
        /**
         * abc
         *
         * <h1>def</h1>
         *
         * ghi
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void blockquote() {
    String input =
        """
        /**
         * abc<blockquote><p>def</blockquote>ghi
         */
        class Test {}\
        """;
    String expected =
        """
        /**
         * abc
         *
         * <blockquote>
         *
         * <p>def
         * </blockquote>
         *
         * ghi
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void lists() {
    String input =
        """
        /**
        * hi
        *
        * <ul>
        * <li>
        * <ul>
        * <li>a</li>
        * </ul>
        * </li>
        * </ul>
        */
        class Test {}\
        """;
    String expected =
        """
        /**
         * hi
         *
         * <ul>
         *   <li>
         *       <ul>
         *         <li>a
         *       </ul>
         * </ul>
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void lists2() {
    String input =
        """
        /**
         * Foo.
         *
         * <ul><li>1<ul><li>1a<li>1b</ul>more 1<p>still more 1<li>2</ul>
         */
        class Test {}\
        """;
    String expected =
        """
        /**
         * Foo.
         *
         * <ul>
         *   <li>1
         *       <ul>
         *         <li>1a
         *         <li>1b
         *       </ul>
         *       more 1
         *       <p>still more 1
         *   <li>2
         * </ul>
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void closeInnerListStillNewline() {
    String input =
        """
        /**
         * Foo.
         *
         * <ul><li><ul><li>a</ul>b</ul>
         */
        class Test {}\
        """;
    String expected =
        """
        /**
         * Foo.
         *
         * <ul>
         *   <li>
         *       <ul>
         *         <li>a
         *       </ul>
         *       b
         * </ul>
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void listItemWrap() {
    String input =
        """
        /**
         * Foo.
         *
         * <ul><li>234567890 234567890 234567890 234567890 234567890 234567890 234567890 234567890 234567890 234567890</ul>
         */
        class Test {}\
        """;
    String expected =
        """
        /**
         * Foo.
         *
         * <ul>
         *   <li>234567890 234567890 234567890 234567890 234567890 234567890 234567890 234567890 234567890
         *       234567890
         * </ul>
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void unclosedList() {
    String input =
        """
        /**
         * Foo.
         *
         * <ul><li>1
         * @return blah
         */
        class Test {}\
        """;
    String expected =
        """
        /**
         * Foo.
         *
         * <ul>
         *   <li>1
         *
         * @return blah
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void br() {
    String input =
        """
        /**
         * abc<br>def
         */
        class Test {}\
        """;
    String expected =
        """
        /**
         * abc<br>
         * def
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void brSpaceBug() {
    // TODO(b/28983091): Remove the space before <br> here.
    String input =
        """
        /**
         * abc <br>def
         */
        class Test {}\
        """;
    String expected =
        """
        /**
         * abc <br>
         * def
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void brAtSignBug() throws FormatterException {
    /*
     * This is a bug -- more of a "spec" bug than an implementation bug, and hard to fix.
     * Fortunately, some very quick searching didn't turn up any instances in the Google codebase.
     */
    String input =
        """
        /**
         * abc<br>@foo
         */
        class Test {}\
        """;
    String expected =
        """
        /**
         * abc<br>
         * @foo
         */
        class Test {}
        """;
    assertThat(formatter.formatSource(input)).isEqualTo(expected);
    // This one is not idempotent since a second formatting round will introduce a blank line before
    // the fake @foo tag. But the test is showing that there's a bug anyway.
  }

  @Test
  public void unicodeCharacterCountArguableBug() {
    /*
     * We might prefer for multi-char characters like 𝄞 to be treated as taking up one column (or
     * perhaps for all characters to be treated based on their width in monospace fonts). But
     * currently we just count chars.
     */
    String input =
        """
        /**
         * 456789𝄞12 456789𝄞12 456789𝄞12 456789𝄞12 456789𝄞12 456789𝄞12 456789𝄞12 456789𝄞12 456789𝄞12 456789𝄞
         */
        class Test {}\
        """;
    String expected =
        """
        /**
         * 456789𝄞12 456789𝄞12 456789𝄞12 456789𝄞12 456789𝄞12 456789𝄞12 456789𝄞12 456789𝄞12
         * 456789𝄞12 456789𝄞
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void blankLinesAroundSnippetAndNoMangling() {
    String input =
        """
        /**
         * hello world
         * {@snippet :
         * public class Foo {
         *   private String s;
         * }
         * }
         * hello again
         */
        class Test {}\
        """;
    String expected =
        """
        /**
         * hello world
         *
         * {@snippet :
         * public class Foo {
         *   private String s;
         * }
         * }
         *
         * hello again
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void notASnippetUnlessOuterTag() {
    String input =
        """
        /** I would like to tell you about the {@code {@snippet ...}} tag. */
        class Test {}\
        """;
    String expected =
        """
        /** I would like to tell you about the {@code {@snippet ...}} tag. */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void blankLineBeforeParams() {
    String input =
        """
        /**
         * hello world
         * @param this is a param
         */
        class Test {}\
        """;
    String expected =
        """
        /**
         * hello world
         *
         * @param this is a param
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void onlyParams() {
    String input =
        """
        /**
         *
         *
         * @param this is a param
         */
        class Test {}\
        """;
    String expected =
        """
        /**
         * @param this is a param
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void paramsContinuationIndented() {
    String input =
        """
        /**
         * hello world
         *
         * @param foo 567890123 567890123 567890123 567890123 567890123 567890123 567890123 567890123 567890123
         * @param bar another
         */
        class Test {}\
        """;
    String expected =
        """
        /**
         * hello world
         *
         * @param foo 567890123 567890123 567890123 567890123 567890123 567890123 567890123 567890123
         *     567890123
         * @param bar another
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void paramsOtherIndents() {
    String input =
        """
        /**
         * hello world
         *
         * @param foo a<p>b<ul><li>a<ul><li>x</ul></ul>
         * @param bar another
         */
        class Test {}\
        """;
    // TODO(cpovirk): Ideally we would probably eliminate the blank line before the second @param.
    String expected =
        """
        /**
         * hello world
         *
         * @param foo a
         *     <p>b
         *     <ul>
         *       <li>a
         *           <ul>
         *             <li>x
         *           </ul>
         *     </ul>
         *
         * @param bar another
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void paragraphTag() {
    String input =
        """
        class Test {
          /**
           * hello<p>world
           */
          void f() {}

          /**
           * hello
           * <p>
           * world
           */
          void f() {}
        }\
        """;
    String expected =
        """
        class Test {
          /**
           * hello
           *
           * <p>world
           */
          void f() {}

          /**
           * hello
           *
           * <p>world
           */
          void f() {}
        }
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void xhtmlParagraphTag() {
    String input =
        """
        class Test {
          /**
           * hello<p/>world
           */
          void f() {}

        }\
        """;
    String expected =
        """
        class Test {
          /**
           * hello
           *
           * <p>world
           */
          void f() {}
        }
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void removeInitialParagraphTag() {
    String input =
        """
        /**
         * <p>hello<p>world
         */
        class Test {}\
        """;
    String expected =
        """
        /**
         * hello
         *
         * <p>world
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void inferParagraphTags() {
    String input =
        """
        /**
         *
         *
         * foo
         * foo
         *
         *
         * foo
         *
         * bar
         *
         * <pre>
         *
         * baz
         *
         * </pre>
         *
         * <ul>
         * <li>foo
         *
         * bar
         * </ul>
         *
         *
         */
        class Test {}\
        """;
    String expected =
        """
        /**
         * foo foo
         *
         * <p>foo
         *
         * <p>bar
         *
         * <pre>
         *
         * baz
         *
         * </pre>
         *
         * <ul>
         *   <li>foo
         *       <p>bar
         * </ul>
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void paragraphTagNewlines() throws Exception {
    String input =
        new String(
            ByteStreams.toByteArray(getClass().getResourceAsStream("testjavadoc/B28750242.input")),
            UTF_8);
    String expected =
        new String(
            ByteStreams.toByteArray(getClass().getResourceAsStream("testjavadoc/B28750242.output")),
            UTF_8);
    String output = formatter.formatSource(input);
    assertThat(output).isEqualTo(expected);
  }

  @Test
  public void listItemSpaces() throws Exception {
    String input =
        new String(
            ByteStreams.toByteArray(getClass().getResourceAsStream("testjavadoc/B31404367.input")),
            UTF_8);
    String expected =
        new String(
            ByteStreams.toByteArray(getClass().getResourceAsStream("testjavadoc/B31404367.output")),
            UTF_8);
    String output = formatter.formatSource(input);
    assertThat(output).isEqualTo(expected);
  }

  @Test
  public void htmlTagsInCode() {
    String input =
        """
        /** abc {@code {} <p> <li> <pre> <table>} def */
        class Test {}\
        """;
    String expected =
        """
        /** abc {@code {} <p> <li> <pre> <table>} def */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void loneBraceDoesNotStartInlineTag() {
    String input =
        """
        /** {  <p> } */
        class Test {}\
        """;
    String expected =
        """
        /**
         * {
         *
         * <p>}
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void unicodeEscapesNotReplaced() {
    // Test that we don't replace them with their interpretations.
    String input =
        """
        /** foo \\u0000 bar \\u6c34 baz */
        class Test {}\
        """;
    String expected =
        """
        /** foo \\u0000 bar \\u6c34 baz */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void unicodeEscapesNotInterpretedBug() {
    /*
     * In theory, ╲u003C should be treated exactly like <, and so too should the escaped versions of
     * @, *, and other special chars. We don't recognize that, though, so we don't put what is
     * effectively "<p>" on a new line.
     */
    String input =
        """
        /** a\\u003Cp>b */
        class Test {}\
        """;
    String expected =
        """
        /** a\\u003Cp>b */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void trailingLink() {
    // Eclipse's parser seems to want to discard the line break after {@link}. Test that we see it.
    String input =
        """
        /**
         * abc {@link Foo}
         * def
         */
        class Test {}\
        """;
    String expected =
        """
        /** abc {@link Foo} def */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void codeInCode() {
    // Eclipse's parser seems to get confused at the second {@code}. Test that we handle it.
    String input =
        """
        /** abc {@code {@code foo}} def */
        class Test {}\
        """;
    String expected =
        """
        /** abc {@code {@code foo}} def */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void quotedTextSplitAcrossLinks() {
    /*
     * This demonstrates one of multiple reasons that we can't hand the Javadoc *content* to
     * Eclipse's lexer as if it were Java code.
     */
    String input =
        """
        /**
         * abc "foo
         * bar baz" def
         */
        class Test {}\
        """;
    String expected =
        """
        /** abc "foo bar baz" def */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void standardizeTags() {
    String input =
        """
        /**
         * foo
         *
         * <P>bar
         *
         * <p class=clazz>baz<BR>
         * baz
         */
        class Test {}\
        """;
    String expected =
        """
        /**
         * foo
         *
         * <p>bar
         *
         * <p class=clazz>baz<br>
         * baz
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void removeCloseTags() {
    String input =
        """
        /**
         * foo</p>
         *
         * <p>bar</p>
         */
        class Test {}\
        """;
    String expected =
        """
        /**
         * foo
         *
         * <p>bar
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void javadocFullSentences() {
    String input =
        """
        /** In our application, bats are often found hanging from the ceiling, especially on Wednesdays.  Sometimes sick bats have issues where their claws do not close entirely.  This class provides a nice, grippable surface for them to cling to. */
        class Grippable {}\
        """;
    String expected =
        """
        /**
         * In our application, bats are often found hanging from the ceiling, especially on Wednesdays.
         * Sometimes sick bats have issues where their claws do not close entirely. This class provides a
         * nice, grippable surface for them to cling to.
         */
        class Grippable {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void javadocSentenceFragment() {
    String input =
        """
        /** Provides a comfy, grippable surface for sick bats with claw-closing problems, which are sometimes found hanging from the ceiling on Wednesdays. */
        class Grippable {}\
        """;
    String expected =
        """
        /**
         * Provides a comfy, grippable surface for sick bats with claw-closing problems, which are sometimes
         * found hanging from the ceiling on Wednesdays.
         */
        class Grippable {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void javadocCanEndAnywhere() {
    String input =
        """
        /** foo <pre*/
        class Test {}\
        """;
    String expected =
        """
        /** foo <pre */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void windowsLineSeparator() throws FormatterException {
    String input =
        """
        /**
         * hello
         *
         * <p>world
         */
        class Test {}\
        """;
    for (String separator : new String[] {"\r", "\r\n"}) {
      String actual = formatter.formatSource(input.replace("\n", separator));
      assertThat(actual).isEqualTo(input.replace("\n", separator) + separator);
    }
  }

  @Test
  public void u2028LineSeparator() {
    // The subterfuge with ␤ here is needed because of https://bugs.openjdk.org/browse/JDK-8380912.
    String input =
        """
        public class Foo {
          /**␤
           * Set and enable something.
           */
          public void setSomething() {}
        }\
        """
            .replace("␤", "\u2028");
    String expected =
        """
        public class Foo {
          /**
           * ␤ Set and enable something.
           */
          public void setSomething() {}
        }
        """
            .replace("␤", "\u2028");
    doFormatTest(input, expected);
  }

  @Test
  public void missingSummaryFragment() {
    String input =
        """
        public class Foo {
          /**
           * @return something.
           */
          public void setSomething() {}

          /**
           * @hide
           */
          public void setSomething() {}
        }\
        """;
    String expected =
        """
        public class Foo {
          /**
           * @return something.
           */
          public void setSomething() {}

          /** @hide */
          public void setSomething() {}
        }
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void simpleMarkdown() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
"""
package com.example;

/// # Heading
///
/// A very long line of text, long enough that it will need to be wrapped to fit within the maximum line length.
///
/// A second paragraph.
class Test {
  /// Another very long line of text, also long enough that it will need to be wrapped to fit within the maximum line length.
  /// @param <T> a generic type
  <T> T method() {
    return null;
  }

  /// This long line of text looks like a javadoc comment, but is not, because it is separated from the actual javadoc comment by a plain comment.
  // This is the plain comment.
  /// A third very long line of text, this time a javadoc comment on a field, which again exceeds the maximum line length.
  String field;

  /// A fourth very long line of text, which however is not a javadoc comment so will be wrapped like a regular // comment.
}\
""";
    String expected =
"""
package com.example;

/// # Heading
///
/// A very long line of text, long enough that it will need to be wrapped to fit within the maximum
/// line length.
///
/// A second paragraph.
class Test {
  /// Another very long line of text, also long enough that it will need to be wrapped to fit within
  /// the maximum line length.
  ///
  /// @param <T> a generic type
  <T> T method() {
    return null;
  }

  /// This long line of text looks like a javadoc comment, but is not, because it is separated from
  // the actual javadoc comment by a plain comment.
  // This is the plain comment.
  /// A third very long line of text, this time a javadoc comment on a field, which again exceeds
  /// the maximum line length.
  String field;

  /// A fourth very long line of text, which however is not a javadoc comment so will be wrapped
  // like a regular // comment.
}
""";
    doFormatTest(input, expected);
  }

  @Test
  public void moduleMarkdown() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
"""
/// A very long line of text, long enough that it will need to be wrapped to fit within the maximum line length.
module com.example {}
""";
    String expected =
"""
/// A very long line of text, long enough that it will need to be wrapped to fit within the maximum
/// line length.
module com.example {}
""";
    doFormatTest(input, expected);
  }

  @Test
  public void markdownLists() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
"""
/// A list that contains:
/// - things
/// - very very long lines that are going to need to be wrapped with appropriate indentation on the next line
/// - item that unnecessarily
///   continues onto the next line
/// - a nested list
///   * nested thing 1
///   * nested thing 2
/// -    a nested numbered list with unnecessary leading whitespace
///      1. nested thing 1
///         on more than one line
///      2. nested thing 2 on only one line but which is long enough that it is going to need to be wrapped
///
///      3. nested thing 3 after a blank line
///
/// A following paragraph.
class Test {}
""";
    String expected =
"""
/// A list that contains:
/// - things
/// - very very long lines that are going to need to be wrapped with appropriate indentation on the
///   next line
/// - item that unnecessarily continues onto the next line
/// - a nested list
///   * nested thing 1
///   * nested thing 2
/// - a nested numbered list with unnecessary leading whitespace
///   1. nested thing 1 on more than one line
///   2. nested thing 2 on only one line but which is long enough that it is going to need to be
///      wrapped
///
///   3. nested thing 3 after a blank line
///
/// A following paragraph.
class Test {}
""";
    doFormatTest(input, expected);
  }

  @Test
  public void markdownIndentedListItem() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
"""
/// A list:
///   - `foo`: enabled by default
///   - `bar`: disabled by default
class Test {}
""";
    String expected =
"""
/// A list:
/// - `foo`: enabled by default
/// - `bar`: disabled by default
class Test {}
""";
    doFormatTest(input, expected);
  }

  @Test
  public void markdownEmptyListItem() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
"""
/// A list with an empty item:
/// - `foo`: enabled by default
/// - `bar`: disabled by default
/// -
class Test {}
""";
    String expected = input;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownFencedCodeBlocks() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    // If fenced code blocks are not supported correctly, the contents of each one will be joined.
    // If the input lines survive as separate lines, that means we identified the code block.
    String input =
"""
/// ```
/// foo
/// bar
/// ```
///
/// -  ```
///    code block
///    in a list
///        with an indented line
///    ```
///
/// - flibbertigibbet
///   ```
///   code block in a list after text with no blank line intervening (one will be inserted)
///   ```
///
/// - flibbertigibbet
///
///   ```
///   code block in a list after text with a blank line intervening
///   ```
///
/// ~~~java
/// code block
/// with tildes and an info string ("java")
/// ~~~
///
///  ````
///  code block
///  with more than three backticks and an extra leading space
///  ````
class Test {}
""";
    String expected =
"""
/// ```
/// foo
/// bar
/// ```
///
/// - ```
///   code block
///   in a list
///       with an indented line
///   ```
///
/// - flibbertigibbet
///
///   ```
///   code block in a list after text with no blank line intervening (one will be inserted)
///   ```
///
/// - flibbertigibbet
///
///   ```
///   code block in a list after text with a blank line intervening
///   ```
///
/// ~~~java
/// code block
/// with tildes and an info string ("java")
/// ~~~
///
/// ````
/// code block
/// with more than three backticks and an extra leading space
/// ````
class Test {}
""";
    doFormatTest(input, expected);
  }

  @Test
  public void markdownMoeComments() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        /// This is the first line.
        ///
        /// <!-- moe:begin_intracomment_strip -->
        /// This is a comment that should be stripped
        ///
        /// ```
        /// this is a code block
        /// ```
        /// <!-- moe:end_intracomment_strip -->
        class Test {}
        """
            .replace("moe", "MOE");
    String expected =
        """
        /// This is the first line.
        ///
        /// <!-- moe:begin_intracomment_strip -->
        /// This is a comment that should be stripped
        ///
        /// ```
        /// this is a code block
        /// ```
        /// <!-- moe:end_intracomment_strip -->
        class Test {}
        """
            .replace("moe", "MOE");
    doFormatTest(input, expected);
  }

  @Test
  public void markdownBackslashes() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    // We write `╲` (a box drawing character) instead of `\\` here and then substitute. That makes
    // the test case a bit easier to read and also means that we can see where the line wrapping
    // should happen. (Having to write \\ instead of \ would make the source text lines wider than
    // the strings they represent.)
    @SuppressWarnings("MisleadingEscapedSpace")
    String input =
"""
/// ╲<br> is not a break.
/// ╲&#42; is not an HTML entity.
/// Backslash does not escape the end of a `code span╲` so <br> is a real break,
/// but backslash does escape the *start* of a ╲`code span so <br> is also a real break.
/// hard╲
/// line╲\t\s
/// breaks
/// - foo ╲
///     bar
/// ╲@param not a param tag
/// ╲╲@param not a param tag either
class Test {}
"""
            .replace('╲', '\\');
    // I don't think anything changes if we do or do not respect the \& backslash so nothing here
    // proves whether we do.
    String expected =
"""
/// ╲<br> is not a break. ╲&#42; is not an HTML entity. Backslash does not escape the end of a `code
/// span╲` so <br>
/// is a real break, but backslash does escape the *start* of a ╲`code span so <br>
/// is also a real break. hard╲
/// line╲
/// breaks
/// - foo ╲
///   bar ╲@param not a param tag ╲╲@param not a param tag either
class Test {}
"""
            .replace('╲', '\\');
    doFormatTest(input, expected);
  }

  @Test
  public void markdownLinkWrapping() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
"""
/// enough text to cause the following link to need to be wrapped [foo](http://very.long/url/that/will/need/to/be/wrapped)
class Test {}
""";
    // It would be wrong to wrap between the ] and the (, and fortunately we don't.
    // https://spec.commonmark.org/0.31.2/#example-511
    String expected =
"""
/// enough text to cause the following link to need to be wrapped
/// [foo](http://very.long/url/that/will/need/to/be/wrapped)
class Test {}
""";
    doFormatTest(input, expected);
  }

  @Test
  public void markdownThematicBreaks() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        /// foo
        /// ***
        /// bar
        /// baz
        ///
        ///
        class Test {}
        """;
    // TODO: thematic breaks are not supported. That means the input is unchanged. We can see this
    // because the blank lines at the end are preserved.
    String expected = input;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownSetextHeadings() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        /// Heading
        /// =======
        /// Phoebe B. Peabody-Beebe
        ///
        ///
        /// Subheading
        /// ----------
        class Test {}
        """;
    // TODO: This kind of heading is not supported. That means the input is unchanged.
    // We can see this because the extra blank line is preserved.
    String expected = input;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownIndentedCodeBlocks() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        ///     code block
        ///     is indented
        /// text after code block
        ///
        ///
        class Test {}
        """;
    // TODO: evil indented code blocks like this are not supported. That means the input is
    // unchanged. We can see this because the blank lines at the end are preserved.
    String expected = input;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownBlockTagContinuationLines() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
"""
/// Something something something.
///
/// @param foo a parameter with a long description that will need to be wrapped onto multiple
/// lines, with each line being indented by the default amount for continuation
/// lines in a block tag.
///
/// There is even a second paragraph which illustrates why the indentation should be +2
/// rather than +4.
record Test(String foo) {}
""";
    String expected =
"""
/// Something something something.
///
/// @param foo a parameter with a long description that will need to be wrapped onto multiple lines,
///   with each line being indented by the default amount for continuation lines in a block tag.
///
///   There is even a second paragraph which illustrates why the indentation should be +2 rather
///   than +4.
record Test(String foo) {}
""";

    doFormatTest(input, expected);
  }

  @Test
  public void markdownLinkReferenceDefinitions() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        /// [foo] [bar]
        ///
        /// [foo]: /url "title"
        /// [bar]: /url2 "title2"
        ///
        ///
        class Test {}
        """;
    // TODO: link reference definitions are not supported. That means the input is unchanged. We can
    // see this because the blank lines at the end are preserved.
    String expected = input;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownLooseLists() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        /// - item 1
        ///
        /// - item 2
        class Test {}
        """;
    String expected = input;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownListPrecedingBlankLine() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        /// Title.
        ///
        /// Some paragraph.
        ///
        /// - Item 1.
        /// - Item 2.
        class Test {}
        """;
    String expected = input;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownBlockQuotes() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        /// > foo
        /// > bar
        ///
        /// baz
        class Test {}
        """;
    String expected =
        """
        /// > foo bar
        ///
        /// baz
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownBlockQuoteWithinListItem() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        /// 1. item one
        /// 2. item two
        ///    - sublist
        ///    - sublist
        ///    - > foo
        ///      > bar
        class Test {}
        """;
    String expected =
        """
        /// 1. item one
        /// 2. item two
        ///    - sublist
        ///    - sublist
        ///    - > foo bar
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownNestedBlockQuotes() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        /// > foo
        /// > > bar
        /// > > baz
        class Test {}
        """;
    String expected =
        """
        /// > foo
        /// > > bar baz
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownBlockQuoteWithCodeBlockInside() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        /// > foo
        /// > ```
        /// > >
        /// > ```
        /// > bar
        class Test {}
        """;
    String expected =
        """
        /// > foo
        /// >
        /// > ```
        /// > >
        /// > ```
        /// >
        /// > bar
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownBlockQuoteInBlockTag() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
"""
/// A test class.
/// @param foo a Foo
/// > In the reign of James the Second
/// > It was generally reckoned
/// > As a very serious crime
/// > To marry two wives at one time.
class Test {}
""";
    String expected =
"""
/// A test class.
///
/// @param foo a Foo
///   > In the reign of James the Second It was generally reckoned As a very serious crime To marry
///   > two wives at one time.
class Test {}
""";
    doFormatTest(input, expected);
  }

  @Test
  public void markdownCodeSpans() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
"""
/// `<ul>` should not trigger list handling.
///
/// `This very long code line should eventually trigger line wrapping because newlines are allowed in code spans.`
///
/// This other long line is carefully crafted to provoke a line break inside a double-backtick `` `<ul>` `` code span.
///
/// There should not be a line break immediately before or after a backtick in an example like this`and`that.
class Test {}
""";
    String expected =
"""
/// `<ul>` should not trigger list handling.
///
/// `This very long code line should eventually trigger line wrapping because newlines are allowed
/// in code spans.`
///
/// This other long line is carefully crafted to provoke a line break inside a double-backtick ``
/// `<ul>` `` code span.
///
/// There should not be a line break immediately before or after a backtick in an example like
/// this`and`that.
class Test {}
""";
    doFormatTest(input, expected);
  }

  @Test
  public void markdownMoeCommentsWithoutBlankLines() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        /// Summary paragraph.
        /// <!-- moe:begin_intracomment_strip -->
        /// Internal-only details.
        /// <!-- moe:end_intracomment_strip -->
        /// Follow-up paragraph on the next line.
        ///
        /// <!-- A regular HTML comment -->
        ///
        /// Paragraph after blank line.
        ///
        /// Paragraph before inline HTML comment.
        /// <!-- A regular HTML comment without blank lines -->
        /// Paragraph immediately after regular HTML comment.
        ///
        /// > Paragraph 1.
        /// >
        /// > <!-- moe:begin_intracomment_strip -->
        /// > Internal.
        /// > <!-- moe:end_intracomment_strip -->
        /// >
        /// > Paragraph 2.
        /// >
        /// > <!-- A regular HTML comment in blockquote -->
        /// >
        /// > Paragraph 3.
        class Test {}
        """
            .replace("moe", "MOE");
    String expected =
        """
        /// Summary paragraph.
        /// <!-- moe:begin_intracomment_strip -->
        /// Internal-only details.
        /// <!-- moe:end_intracomment_strip -->
        /// Follow-up paragraph on the next line.
        ///
        /// <!-- A regular HTML comment -->
        ///
        /// Paragraph after blank line.
        ///
        /// Paragraph before inline HTML comment.
        /// <!-- A regular HTML comment without blank lines -->
        /// Paragraph immediately after regular HTML comment.
        ///
        /// > Paragraph 1.
        /// >
        /// > <!-- moe:begin_intracomment_strip -->
        /// > Internal.
        /// > <!-- moe:end_intracomment_strip -->
        /// >
        /// > Paragraph 2.
        /// >
        /// > <!-- A regular HTML comment in blockquote -->
        /// >
        /// > Paragraph 3.
        class Test {}
        """
            .replace("moe", "MOE");
    doFormatTest(input, expected);
  }

  @Test
  public void markdownReferenceLinks() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        /// Therefore we need to make all assertions in the test method itself, where [Scanner#scan(Tree, VisitorState)] is triggered.
        ///
        /// Also test formatting of full reference links in prose like [the scan method][Scanner#scan(Tree, VisitorState)] here.
        ///
        /// Also test formatting of collapsed reference links in prose right here like [Scanner#scan(Tree, VisitorState)][] ok.
        ///
        /// Check line wrapping of escaped array parameter types here [java.util.Arrays#equals(byte\\[\\], byte\\[\\])] ok.
        ///
        /// Check line wrapping of varargs method reference link targets like [String#format(Locale, String, Object...)] ok.
        ///
        /// Check line wrapping inside a parameterized type reference right here for [ImmutableMap<String, Integer>] ok.
        ///
        /// Check line wrapping inside a wildcard parameterized type reference right here for [List<? extends Number>] ok.
        ///
        /// Check line wrapping of a full reference link with a generic target [the map][Map<String, Integer>] ok.
        ///
        /// Check line wrapping of a method link with generic qualifier like [Map<String, Integer>#put(Object, Object)] ok.
        ///
        /// Check line wrapping inside a Unicode identifier method reference right here [Scanner#café(Tree, VisitorState)] ok.
        ///
        /// Check line wrapping when there is whitespace before parens like in this [Scanner#scan (Tree, VisitorState)] ok.
        ///
        /// - Indented multi-line full reference link [a][Scanner#scan(Tree,
        ///       VisitorState)] here.
        ///
        /// > > Quoted multi-line [Scanner#scan(Tree,
        /// > > VisitorState)] and [a][Scanner#scan(Tree,
        /// > > VisitorState)].
        ///
        /// Ordinary links with a `code:` URI scheme: [foo](code:bar), [foo](code:123), and [a long link text that should still wrap](code:).
        ///
        /// Prose [note(a long remark with `code` inside that should definitely wrap right across column one hundred)] ok.
        ///
        /// Dotted non-member call that is not a valid JEP 467 link should wrap right inside [Foo.bar(alpha, beta)] ok.
        ///
        /// Inline tags and HTML code: {@code arr[foo(a} and b)] and <code>arr[foo(a</code> and b)].
        ///
        /// {@snippet :
        /// int x = arr[foo(
        ///     a, b)];
        /// }
        class Test {}
        """;
    String expected =
        """
        /// Therefore we need to make all assertions in the test method itself, where
        /// [Scanner#scan(Tree, VisitorState)] is triggered.
        ///
        /// Also test formatting of full reference links in prose like [the scan
        /// method][Scanner#scan(Tree, VisitorState)] here.
        ///
        /// Also test formatting of collapsed reference links in prose right here like
        /// [Scanner#scan(Tree, VisitorState)][] ok.
        ///
        /// Check line wrapping of escaped array parameter types here
        /// [java.util.Arrays#equals(byte\\[\\], byte\\[\\])] ok.
        ///
        /// Check line wrapping of varargs method reference link targets like
        /// [String#format(Locale, String, Object...)] ok.
        ///
        /// Check line wrapping inside a parameterized type reference right here for
        /// [ImmutableMap<String, Integer>] ok.
        ///
        /// Check line wrapping inside a wildcard parameterized type reference right here for
        /// [List<? extends Number>] ok.
        ///
        /// Check line wrapping of a full reference link with a generic target [the
        /// map][Map<String, Integer>] ok.
        ///
        /// Check line wrapping of a method link with generic qualifier like
        /// [Map<String, Integer>#put(Object, Object)] ok.
        ///
        /// Check line wrapping inside a Unicode identifier method reference right here
        /// [Scanner#café(Tree, VisitorState)] ok.
        ///
        /// Check line wrapping when there is whitespace before parens like in this
        /// [Scanner#scan (Tree, VisitorState)] ok.
        ///
        /// - Indented multi-line full reference link [a][Scanner#scan(Tree, VisitorState)] here.
        ///
        /// > > Quoted multi-line [Scanner#scan(Tree, VisitorState)] and
        /// > > [a][Scanner#scan(Tree, VisitorState)].
        ///
        /// Ordinary links with a `code:` URI scheme: [foo](code:bar), [foo](code:123), and [a long link
        /// text that should still wrap](code:).
        ///
        /// Prose [note(a long remark with `code` inside that should definitely wrap right across column one
        /// hundred)] ok.
        ///
        /// Dotted non-member call that is not a valid JEP 467 link should wrap right inside [Foo.bar(alpha,
        /// beta)] ok.
        ///
        /// Inline tags and HTML code: {@code arr[foo(a} and b)] and <code>arr[foo(a</code> and b)].
        ///
        /// {@snippet :
        /// int x = arr[foo(
        ///     a, b)];
        /// }
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownAutolinks() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        /// <http://{@code:example.com> <br> is not inside @code}.
        class Test {}
        """;
    // TODO: the <br> should trigger a line break since it is not in fact inside {@code ...}.
    // This is something of a corner case, though.
    String expected = input;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownTables() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
"""
/// Table McTableface
///
/// | foo | bar |
/// | --- | --- |
/// | baz | qux |
///
/// - |foo|bar|
///   |--:|:--|
///   |baz|qux|
///
/// - Another list.
///
///   | which | contains |
///   | ----- | -------- |
///   | a | table |
class Test {}
""";
    // We don't currently try to align the column markers in the rows of the last table.
    String expected = input;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownBlankLinesAroundSnippetAndNoMangling() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        /// hello world
        /// {@snippet :
        /// public class Foo {
        ///   private String s;
        /// }
        /// }
        /// hello again
        class Test {}\
        """;
    String expected =
        """
        /// hello world
        ///
        /// {@snippet :
        /// public class Foo {
        ///   private String s;
        /// }
        /// }
        ///
        /// hello again
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownLongCommentOnEnumConstant() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
"""
enum Foo {
  /// Long long long, longedy long, más fada an lá tig an oíche, chaise longue, caffè lungo, ich bin so lang nicht bei dir gewest
  BAR,
}
""";
    String expected =
"""
enum Foo {
  /// Long long long, longedy long, más fada an lá tig an oíche, chaise longue, caffè lungo, ich bin
  /// so lang nicht bei dir gewest
  BAR,
}
""";
    doFormatTest(input, expected);
  }

  @Test
  public void markdownLongCommentOnPackageDeclaration() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    // A package doc comment is only really valid in package-info.java, but let's assume this is one
    // of those.
    String input =
"""
/// Long long long, longedy long, más fada an lá tig an oíche, chaise longue, caffè lungo, ich bin so lang nicht bei dir gewest
package com.example;
""";
    String expected =
"""
/// Long long long, longedy long, más fada an lá tig an oíche, chaise longue, caffè lungo, ich bin
/// so lang nicht bei dir gewest
package com.example;
""";
    doFormatTest(input, expected);
  }

  @Test
  public void markdownOrderedListWithParenthesis() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        /// Summary paragraph.
        /// 1) first item
        /// 2) second item
        class Test {}
        """;
    String expected = input;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownBulletListWithPlus() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        /// Summary paragraph.
        /// + first item
        /// + second item
        class Test {}
        """;
    String expected = input;
    doFormatTest(input, expected);
  }

  @Test
  public void tableAtStartOfComment() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        /// <table>
        /// <tr><td>Foo</td></tr>
        /// </table>
        class Test {}
        """;
    String expected = input;
    doFormatTest(input, expected);
  }

  @Test
  public void blockquoteAtStartOfComment() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        /// <blockquote>
        /// line 1
        /// line 2
        /// </blockquote>
        class Test {}
        """;
    String expected =
        """
        /// <blockquote>
        /// line 1 line 2
        /// </blockquote>
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void preAtStartOfComment() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        /// <pre>
        /// line 1
        /// line 2
        /// </pre>
        class Test {}
        """;
    String expected = input;
    doFormatTest(input, expected);
  }

  @Test
  public void tableInHtmlListItem() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        /// <ul>
        /// <li>
        /// <table>
        /// <tr><td>Foo</td></tr>
        /// </table>
        /// </li>
        /// </ul>
        class Test {}
        """;
    // requestBlankLine() in writeTableOpen() inserts a blank line after <li> before <table> when
    // inside a list item. Also, we might prefer to eliminate the blank lines around the table
    // element.
    String expected =
        """
        /// <ul>
        /// <li>
        ///
        /// <table>
        /// <tr><td>Foo</td></tr>
        /// </table>
        ///
        /// </ul>
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownHtmlListItemFollowedByParagraph() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        class T {
          /// Updates:
          /// <li>A single [Foo]
          /// <li>One or more [Bar]s
          ///
          /// <p>See [Baz] for more details.
          void m() {}

          /// Updates:
          /// <li>First item
          /// <p>Following paragraph without blank line.
          void n() {}
        }
        """;
    String expected =
        """
        class T {
          /// Updates:
          /// <li>A single [Foo]
          /// <li>One or more [Bar]s
          ///
          /// <p>See [Baz] for more details.
          void m() {}

          /// Updates:
          /// <li>First item
          ///
          /// <p>Following paragraph without blank line.
          void n() {}
        }
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownHtmlUnorderedAndDefinitionLists() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        class T {
          /// Intro:
          /// <ul>
          ///   <li class="x">First item
          ///
          ///   <p>Second paragraph.
          ///   <li>Second item
          /// </ul>
          /// Following text.
          ///
          /// <ul>
          /// text without li
          /// </ul>
          ///
          /// <dl>
          ///   <dt>Term
          ///   <dd>Definition
          ///
          ///   <p>More details.
          /// </dl>
          void m() {}
        }
        """;
    String expected =
        """
        class T {
          /// Intro:
          /// <ul>
          /// <li class="x">First item
          ///
          /// <p>Second paragraph.
          /// <li>Second item
          /// </ul>
          /// Following text.
          ///
          /// <ul>
          /// text without li
          /// </ul>
          ///
          /// <dl>
          /// <dt>Term
          /// <dd>Definition
          ///
          /// <p>More details.
          /// </dl>
          void m() {}
        }
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownNestedHtmlLists() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        class T {
          /// Types:
          /// <li>
          /// <ol>
          ///   <li>First
          ///   <p>Details for first.
          ///   <li>Second
          ///   <p>Details for second.
          /// </ol>
          void m() {}

          /// Works only if:
          /// <li>
          ///   <ul>First condition
          ///   <ul>Second condition
          /// </li>
          ///
          /// <p>Features:
          /// <li>
          ///   <ul>See [Foo].
          /// </li>
          void n() {}
        }
        """;
    String expected =
        """
        class T {
          /// Types:
          /// <li><ol>
          /// <li>First
          ///
          /// <p>Details for first.
          /// <li>Second
          ///
          /// <p>Details for second.
          /// </ol>
          void m() {}

          /// Works only if:
          /// <li><ul>
          /// First condition
          /// <ul>
          /// Second condition
          ///
          /// <p>Features:
          /// <li><ul>
          /// See [Foo].
          void n() {}
        }
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownHtmlListInFooterTag() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        class T {
          /// Summary.
          ///
          /// @param x values:
          ///   <ul>
          ///     <li>one
          ///     <p>more
          ///   </ul>
          /// @param y values: <ul>
          ///     <li>one [String]
          ///     <li>two {@link Object}
          ///     </ul>
          void m(int x, int y) {}
        }
        """;
    // Note: flattening `<ul>`/`<li>` to the 2-space footer continuation indent prevents indented
    // code blocks after blank lines (`<p>more`), at the trade-off that `<li>one [String]` starts
    // a Type 6 HTML block (where `[String]` is literal, while `{@link Object}` still resolves).
    String expected =
        """
        class T {
          /// Summary.
          ///
          /// @param x values:
          ///   <ul>
          ///   <li>one
          ///
          ///   <p>more
          ///   </ul>
          /// @param y values:
          ///   <ul>
          ///   <li>one [String]
          ///   <li>two {@link Object}
          ///   </ul>
          void m(int x, int y) {}
        }
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownInlineHtmlListAndFollowingMarkdownLink() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        class T {
          /// Intro <ul><li>a</ul> trailing words.
          void m() {}

          /// Updates:
          /// <li>First item
          ///
          /// See [String] for more details.
          void n() {}
        }
        """;
    String expected =
        """
        class T {
          /// Intro
          /// <ul>
          /// <li>a
          /// </ul>
          /// trailing words.
          void m() {}

          /// Updates:
          /// <li>First item
          ///
          /// See [String] for more details.
          void n() {}
        }
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownHtmlListInsideMarkdownContainers() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        class T {
          /// - Outer item:
          ///   <ul>
          ///   <li>HTML item
          ///   </ul>
          ///   <li>Bare HTML li
          ///   </ul>
          ///
          ///   Following paragraph in outer item.
          ///
          /// > <ul>
          /// > <li>Quoted HTML item
          /// >
          /// > <p>Quoted paragraph
          /// > </ul>
          void m() {}

          /// <li>Bare HTML li
          ///
          /// - Markdown item 1
          /// - Markdown item 2
          void n() {}
        }
        """;
    doFormatTest(input, input);
  }

  @Test
  public void tableInsideMarkdownListItemAfterText() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        /// - item text
        ///   <table>
        ///   <tr><td>Foo</td></tr>
        ///   </table>
        class Test {}
        """;
    String expected =
        """
        /// - item text
        ///
        ///   <table>
        ///   <tr><td>Foo</td></tr>
        ///   </table>
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownNoFormatJavadoc() throws Exception {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        /// A very long line of text, long enough that it would be wrapped if javadoc formatting were enabled.
        class Test {
          /// Another very long line of text, also long enough that it would be wrapped if javadoc formatting were enabled.
          void method() {}
        }
        """;
    Formatter noJavadocFormatter =
        new Formatter(JavaFormatterOptions.builder().formatJavadoc(false).build());
    assertThat(noJavadocFormatter.formatSource(input)).isEqualTo(input);
  }

  @Test
  public void trailingAsterisksBeforeClose() {
    String input =
        """
        /** Version information for all the QuotaKeys that we have data for **/
        class Test {}
        """;
    String expected =
        """
        /** Version information for all the QuotaKeys that we have data for */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void preCodeWithoutLeadingStarPreservesIndent() {
    String input =
        """
        /**
         * <pre>{@code
        class Demo {
          // Comment
          static final int X = 1;

          public static void example() {
            int y = 2;
          }
        }
         * }</pre>
         */
        class Test {}
        """;
    String expected =
        """
        /**
         * <pre>{@code
         * class Demo {
         *   // Comment
         *   static final int X = 1;
         *
         *   public static void example() {
         *     int y = 2;
         *   }
         * }
         * }</pre>
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void hyphenatedLineBreakJoined() {
    String input =
        """
        /**
         * This calculation requires an infinite-
         * precision number.
         */
        class Test {}
        """;
    String expected =
        """
        /** This calculation requires an infinite-precision number. */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void suspendedHyphenPreserved() {
    String input =
        """
        /**
         * Both pre-
         * and post-processing steps.
         */
        class Test {}
        """;
    String expected =
        """
        /** Both pre- and post-processing steps. */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownHyphenatedLineBreakJoined() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        /// This calculation requires an infinite-
        /// precision number.
        class Test {}
        """;
    String expected =
        """
        /// This calculation requires an infinite-precision number.
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void unclosedInlineTagInList() {
    String input =
        """
        /**
         * <ul>
         *   <li>{@link #validating() sets (allowInvalid=false, mayDissolve=false, snapFunction=null): Do not modify the input geometry. Validate it instead.
         *   <li>3. Check if the input geometry is valid as is.
         * </ul>
         */
        class Test {}
        """;
    // TODO(b/407787200): Lexing fails due to unclosed inline tag, leaving comment unformatted.
    String expected =
        """
        /**
         * <ul>
         *   <li>{@link #validating() sets (allowInvalid=false, mayDissolve=false, snapFunction=null): Do not modify the input geometry. Validate it instead.
         *   <li>3. Check if the input geometry is valid as is.
         * </ul>
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void unclosedInlineTagAtEndOfComment() {
    String input =
        """
        /**
         * Return the child position of this cell within its parent. For more details, see {@link com.google.common.geometry.S2CellId
         */
        class Test {}
        """;
    // TODO(b/407787200): Lexing fails due to unclosed inline tag, leaving comment unformatted.
    String expected =
        """
        /**
         * Return the child position of this cell within its parent. For more details, see {@link com.google.common.geometry.S2CellId
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void unclosedInlineTagBeforeBlankLine() {
    String input =
        """
        /**
         * Some description with {@link #validating() sets (allowInvalid=false, mayDissolve=false, snapFunction=null): Do not modify the input geometry.
         *
         * <p>Next paragraph that is also long enough to exceed the line length limit and require line wrapping.
         */
        class Test {}
        """;
    // TODO(b/407787200): Lexing fails due to unclosed inline tag, leaving comment unformatted.
    String expected =
        """
        /**
         * Some description with {@link #validating() sets (allowInvalid=false, mayDissolve=false, snapFunction=null): Do not modify the input geometry.
         *
         * <p>Next paragraph that is also long enough to exceed the line length limit and require line wrapping.
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void unclosedInlineTagBeforeFooterTag() {
    String input =
        """
        /**
         * Some description with {@link #validating() sets (allowInvalid=false, mayDissolve=false, snapFunction=null): Do not modify the input geometry.
         *
         * @param x a parameter description that is also long enough to exceed the line length limit and require line wrapping
         */
        class Test {}
        """;
    // TODO(b/407787200): Lexing fails due to unclosed inline tag, leaving comment unformatted.
    String expected =
        """
        /**
         * Some description with {@link #validating() sets (allowInvalid=false, mayDissolve=false, snapFunction=null): Do not modify the input geometry.
         *
         * @param x a parameter description that is also long enough to exceed the line length limit and require line wrapping
         */
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownWrapBeforeBulletListMarkers() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        class T {
          /// Returns the number of elements in the given range of the array, which is always equal to `end - start`.
          void a() {}

          /// Implementations can implement any variation of `#transform()` and must override only a single - depending on config.
          void b() {}

          /// @param activeCount the 0-based index of the last active id (total active items is `activeCount + 1`)
          void c(int activeCount) {}

          /// @return a result proto containing all elements for the corresponding namespace and the corpus + keyPrefix
          void d() {}

          /// Computes the total area of the rectangle, which is always guaranteed to be equal to `widthVal * height`.
          void e() {}

          /// - Returns the number of elements in the given range of an array, which is always equal to `end - start`.
          void f() {}
        }
        """;
    String expected =
        """
        class T {
          /// Returns the number of elements in the given range of the array, which is always equal to
          /// `end - start`.
          void a() {}

          /// Implementations can implement any variation of `#transform()` and must override only a
          /// single - depending on config.
          void b() {}

          /// @param activeCount the 0-based index of the last active id (total active items is
          ///   `activeCount + 1`)
          void c(int activeCount) {}

          /// @return a result proto containing all elements for the corresponding namespace and the
          ///   corpus + keyPrefix
          void d() {}

          /// Computes the total area of the rectangle, which is always guaranteed to be equal to
          /// `widthVal * height`.
          void e() {}

          /// - Returns the number of elements in the given range of an array, which is always equal to
          ///   `end - start`.
          void f() {}
        }
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownWrapBeforeBlockQuoteMarker() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        class T {
          /// Throws an exception if the requested indices are out of bounds for the array, i.e., if `start > end`.
          void a() {}

          /// Throws an exception if the requested indices are out of bounds for the array, i.e., if `start >= end`.
          void b() {}

          /// Throws an exception if the requested indices are out of bounds for the array, i.e., if `start >` end.
          void c() {}

          /// @throws IndexOutOfBoundsException if `fromIndex < 0`, `toIndex > array.length`, or `fromIndex > toIndex`
          void d() {}

          /// > Throws an exception if the requested indices are out of bounds for an array, i.e., if `start > end`.
          void e() {}
        }
        """;
    String expected =
        """
        class T {
          /// Throws an exception if the requested indices are out of bounds for the array, i.e., if
          /// `start > end`.
          void a() {}

          /// Throws an exception if the requested indices are out of bounds for the array, i.e., if
          /// `start >= end`.
          void b() {}

          /// Throws an exception if the requested indices are out of bounds for the array, i.e., if
          /// `start >` end.
          void c() {}

          /// @throws IndexOutOfBoundsException if `fromIndex < 0`, `toIndex > array.length`, or
          ///   `fromIndex > toIndex`
          void d() {}

          /// > Throws an exception if the requested indices are out of bounds for an array, i.e., if
          /// > `start > end`.
          void e() {}
        }
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownWrapBeforeHeadingAndOrderedListMarkers() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        class T {
          /// Returns the formatted identifier for the element, which is always prefixed by the section mark # 1 in output.
          void a() {}

          /// Returns the formatted identifier for the element, which is always prefixed by the heading tag ### title here.
          void b() {}

          /// When validating the configured options in order, the processor always checks the first option 1. first here.
          void c() {}

          /// When validating the configured options in order, the processor always checks the first option 1) first here.
          void d() {}

          /// When validating the configured options in order, the processor always checks the first item 01. first here.
          void e() {}
        }
        """;
    String expected =
        """
        class T {
          /// Returns the formatted identifier for the element, which is always prefixed by the section
          /// mark # 1 in output.
          void a() {}

          /// Returns the formatted identifier for the element, which is always prefixed by the heading
          /// tag ### title here.
          void b() {}

          /// When validating the configured options in order, the processor always checks the first
          /// option 1. first here.
          void c() {}

          /// When validating the configured options in order, the processor always checks the first
          /// option 1) first here.
          void d() {}

          /// When validating the configured options in order, the processor always checks the first
          /// item 01. first here.
          void e() {}
        }
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownWrapBeforeSetextHeadingAndThematicBreakMarkers() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        class T {
          /// Returns true if the computed value of the first argument equals the second argument when value =
          void a() {}

          /// Returns true if the computed value of the first argument equals the second argument when val == veryLongIdentifierNameThatExceedsTheRemainingLineLengthAndWrapsOntoItsOwnLineForTestingCase12345
          void b() {}

          /// Separates the header section from the body section using a standard marker token sequence mark ---
          void c() {}

          /// Separates the header section from the body section using a standard marker token sequence mark ***
          void d() {}

          /// Separates the header section from the body section using a standard marker token sequence mark ___
          void e() {}

          /// Separates a header | column from a body section using a standard marker token sequence pattern |-|
          void f() {}

          /// Separates a header | column from a body section using a standard marker token sequence pattern | --- |
          void g() {}

          /// Separates a header | column from a body section using a standard marker token sequence pattern :-- |
          void h() {}
        }
        """;
    String expected =
        """
        class T {
          /// Returns true if the computed value of the first argument equals the second argument when
          /// value =
          void a() {}

          /// Returns true if the computed value of the first argument equals the second argument when
          /// val ==
          /// veryLongIdentifierNameThatExceedsTheRemainingLineLengthAndWrapsOntoItsOwnLineForTestingCase12345
          void b() {}

          /// Separates the header section from the body section using a standard marker token sequence
          /// mark ---
          void c() {}

          /// Separates the header section from the body section using a standard marker token sequence
          /// mark ***
          void d() {}

          /// Separates the header section from the body section using a standard marker token sequence
          /// mark ___
          void e() {}

          /// Separates a header | column from a body section using a standard marker token sequence
          /// pattern |-|
          void f() {}

          /// Separates a header | column from a body section using a standard marker token sequence
          /// pattern | --- |
          void g() {}

          /// Separates a header | column from a body section using a standard marker token sequence
          /// pattern :-- |
          void h() {}
        }
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownWrapBeforeFencedCodeAndHtmlBlockMarkers() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        class T {
          /// Returns the delimiter string used for separating fenced sections in the output, such as `a val ~~~ b`.
          void a() {}

          /// Returns the delimiter string used for separating fenced sections in the output, such as ````a ``` b````.
          void b() {}

          /// Returns a formatted snippet showing the nested backtick syntax inside a code span: ```` `foo` ````.
          void c() {}

          /// Returns an HTML fragment representing the rendered widget content, such as `foo bar baz value <div>bar</div>`.
          void d() {}

          /// Returns an HTML fragment representing the rendered widget content, such as `foo bar baz value <p>bar</p>`.
          void e() {}

          /// Returns an HTML fragment representing the rendered widget content, such as `foo bar baz value <!-- comment -->`.
          void f() {}

          /// Use this helper method when rendering an inline element rather than a full standalone block <div> element.
          void g() {}

          /// Returns an HTML fragment representing the rendered widget content, such as `foo bar baz value </div>`.
          void h() {}

          /// Returns an HTML fragment representing the rendered widget content, such as `foo bar baz value <div class="x">`.
          void i() {}

          /// Returns an HTML fragment representing the rendered widget content, such as `foo bar baz value <script>x</script>`.
          void j() {}

          /// Returns an HTML fragment representing the rendered widget content, such as `foo bar baz value <!DOCTYPE html>`.
          void k() {}
        }
        """;
    String expected =
        """
        class T {
          /// Returns the delimiter string used for separating fenced sections in the output, such as `a
          /// val ~~~ b`.
          void a() {}

          /// Returns the delimiter string used for separating fenced sections in the output, such
          /// as ````a ``` b````.
          void b() {}

          /// Returns a formatted snippet showing the nested backtick syntax inside a code span: ````
          /// `foo` ````.
          void c() {}

          /// Returns an HTML fragment representing the rendered widget content, such as `foo bar baz
          /// value <div>bar</div>`.
          void d() {}

          /// Returns an HTML fragment representing the rendered widget content, such as `foo bar baz
          /// value <p>bar</p>`.
          void e() {}

          /// Returns an HTML fragment representing the rendered widget content, such as `foo bar baz
          /// value <!-- comment -->`.
          void f() {}

          /// Use this helper method when rendering an inline element rather than a full standalone
          /// block <div> element.
          void g() {}

          /// Returns an HTML fragment representing the rendered widget content, such as `foo bar baz
          /// value </div>`.
          void h() {}

          /// Returns an HTML fragment representing the rendered widget content, such as `foo bar baz
          /// value <div class="x">`.
          void i() {}

          /// Returns an HTML fragment representing the rendered widget content, such as `foo bar baz
          /// value <script>x</script>`.
          void j() {}

          /// Returns an HTML fragment representing the rendered widget content, such as `foo bar baz
          /// value <!DOCTYPE html>`.
          void k() {}
        }
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownWrapBeforeBlockMarkerAfterHtmlCodeTag() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        class T {
          /// Returns the number of elements in the full range of an array, always equal to <code>end</code> - <code>start</code>.
          void a() {}
        }
        """;
    String expected =
        """
        class T {
          /// Returns the number of elements in the full range of an array, always equal to
          /// <code>end</code> - <code>start</code>.
          void a() {}
        }
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownWrapMultipleConsecutiveBlockMarkers() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        class T {
          /// Returns the combined result of evaluating the sequence of operators in the given expression `a - + > b`.
          void a() {}
        }
        """;
    String expected =
        """
        class T {
          /// Returns the combined result of evaluating the sequence of operators in the given expression
          /// `a - + > b`.
          void a() {}
        }
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownWrapAllowsNonBlockMarkers() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        class T {
          /// Returns the index of the first occurrence of the specified target element, or returns a value -1 if absent.
          void a() {}

          /// Returns the index of the first occurrence of the specified target element, or adds an offset +1 if present.
          void b() {}

          /// Returns the number of elements in the range when the maximum allowed capacity is capped up to 2. Next sentence.
          void c() {}

          /// Returns the number of elements in the range when the maximum allowed capacity is set up to 100. Next line.
          void d() {}

          /// Returns the formatted identifier for the element, which is described in detail at the link #method() below.
          void e() {}

          /// Returns the value produced by applying the mapping function from the given input argument `x -> y` in the stream.
          void f() {}

          /// Returns an inline formatted element rendered inside the enclosing paragraph using a simple <span> tag here.
          void g() {}

          /// Returns the formatted output containing an escaped hyphen marker written explicitly with char \\- in the prose.
          void h() {}

          /// Returns the bitwise union produced by combining the first flag mask with the second flag id `a | b`.
          void i() {}

          /// Returns the mapping between the input key and its corresponding computed output value labeled : value.
          void j() {}
        }
        """;
    String expected =
        """
        class T {
          /// Returns the index of the first occurrence of the specified target element, or returns a value
          /// -1 if absent.
          void a() {}

          /// Returns the index of the first occurrence of the specified target element, or adds an offset
          /// +1 if present.
          void b() {}

          /// Returns the number of elements in the range when the maximum allowed capacity is capped up to
          /// 2. Next sentence.
          void c() {}

          /// Returns the number of elements in the range when the maximum allowed capacity is set up to
          /// 100. Next line.
          void d() {}

          /// Returns the formatted identifier for the element, which is described in detail at the link
          /// #method() below.
          void e() {}

          /// Returns the value produced by applying the mapping function from the given input argument `x
          /// -> y` in the stream.
          void f() {}

          /// Returns an inline formatted element rendered inside the enclosing paragraph using a simple
          /// <span> tag here.
          void g() {}

          /// Returns the formatted output containing an escaped hyphen marker written explicitly with char
          /// \\- in the prose.
          void h() {}

          /// Returns the bitwise union produced by combining the first flag mask with the second flag id `a
          /// | b`.
          void i() {}

          /// Returns the mapping between the input key and its corresponding computed output value labeled
          /// : value.
          void j() {}
        }
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownWrapBeforeEndParaTriggers() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        class T {
          /// Command for local testing: `bazel test --test_strategy=local --nocache_test_results --test_arg='--env=QA' //foo/bar:BazTest`
          void a() {}

          /// Records failure summary artifacts when pre-run setup (such as `workspace_setup_cli
          /// --mode=prepare`) throws `cause`.
          void b() {}

          /// Produces a query of the form (**OR** (**EQ** search_term *id*) (**Q_SUBSTRING** search_term *name*)).
          void c() {}

          /// Returns the normalized status value when the input record matches a known condition or **nothing** otherwise.
          void d() {}

          /// Returns the formatted identifier for the element matching the requested search key against _name_ below.
          void e() {}

          /// Returns the internal sentinel key reserved by the runtime for tracking generated metadata _internal_id here.
          void f() {}

          /// Evaluates the spreadsheet cell formula expression represented by the canonical example text =A1+B1 here.
          void g() {}

          /// Returns the broad category of attribute type (e.g., bool, unique enum, repeatable enum, url, ...).
          void h() {}

          /// Generates the certificate chain by interleaving ordered entries `[prod_0, dev_0, prod_1, ...]`.
          void i() {}

          /// Convenience helper for `ServiceRegistry.getDefaultInstanceForTesting() .useDefaultAuthentication(policy)`.
          void j() {}

          /// Summary.
          ///
          /// @deprecated Use the replacement option configured via the command line flag --replacement_flag_name instead.
          @Deprecated
          void k() {}

          /// Summary.
          ///
          /// @deprecated --very_long_flag_name_that_exceeds_the_remaining_line_length_and_would_otherwise_wrap_onto_line_two
          @Deprecated
          void l() {}

          /// > Produces a query of the form (**OR** (**EQ** search_term *id*) (**Q_SUBSTRING** search_term
          /// > *name*)).
          void m() {}
        }
        """;
    String expected =
        """
        class T {
          /// Command for local testing: `bazel test --test_strategy=local --nocache_test_results
          /// --test_arg='--env=QA' //foo/bar:BazTest`
          void a() {}

          /// Records failure summary artifacts when pre-run setup (such as `workspace_setup_cli
          /// --mode=prepare`) throws `cause`.
          void b() {}

          /// Produces a query of the form (**OR** (**EQ** search_term *id*) (**Q_SUBSTRING** search_term
          /// *name*)).
          void c() {}

          /// Returns the normalized status value when the input record matches a known condition or
          /// **nothing** otherwise.
          void d() {}

          /// Returns the formatted identifier for the element matching the requested search key against
          /// _name_ below.
          void e() {}

          /// Returns the internal sentinel key reserved by the runtime for tracking generated metadata
          /// _internal_id here.
          void f() {}

          /// Evaluates the spreadsheet cell formula expression represented by the canonical example text
          /// =A1+B1 here.
          void g() {}

          /// Returns the broad category of attribute type (e.g., bool, unique enum, repeatable enum, url,
          /// ...).
          void h() {}

          /// Generates the certificate chain by interleaving ordered entries `[prod_0, dev_0, prod_1,
          /// ...]`.
          void i() {}

          /// Convenience helper for `ServiceRegistry.getDefaultInstanceForTesting()
          /// .useDefaultAuthentication(policy)`.
          void j() {}

          /// Summary.
          ///
          /// @deprecated Use the replacement option configured via the command line flag
          ///   --replacement_flag_name instead.
          @Deprecated
          void k() {}

          /// Summary.
          ///
          /// @deprecated
          ///   --very_long_flag_name_that_exceeds_the_remaining_line_length_and_would_otherwise_wrap_onto_line_two
          @Deprecated
          void l() {}

          /// > Produces a query of the form (**OR** (**EQ** search_term *id*) (**Q_SUBSTRING** search_term
          /// > *name*)).
          void m() {}
        }
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownWrapAllowsNonEndParaTokens() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        class T {
          /// Returns the configured option value when the user explicitly supplies the command line flag `--flag` here.
          void a() {}

          /// Returns the formatted output containing an escaped emphasis marker written explicitly as \\*name\\* here.
          void b() {}

          /// Returns the formatted output containing an escaped underscore marker written explicitly as \\_name\\_ here.
          void c() {}

          /// Returns the formatted output containing an escaped leading period written explicitly as \\.proto here.
          void d() {}

          /// First paragraph.
          ///
          /// *name* starts the second paragraph.
          void e() {}

          /**
           * Command for local testing: {@code bazel test --test_strategy=local --nocache_test_results --test_arg='--env=QA' //foo/bar:BazTest}
           */
          void f() {}

          /**
           * Returns the formatted identifier for the element matching the requested search key against *name* below.
           */
          void g() {}

          /**
           * Convenience helper for {@code ServiceRegistry.getDefaultInstanceForTesting() .useDefaultAuthentication(policy)}.
           */
          void h() {}
        }
        """;
    String expected =
        """
        class T {
          /// Returns the configured option value when the user explicitly supplies the command line flag
          /// `--flag` here.
          void a() {}

          /// Returns the formatted output containing an escaped emphasis marker written explicitly as
          /// \\*name\\* here.
          void b() {}

          /// Returns the formatted output containing an escaped underscore marker written explicitly as
          /// \\_name\\_ here.
          void c() {}

          /// Returns the formatted output containing an escaped leading period written explicitly as
          /// \\.proto here.
          void d() {}

          /// First paragraph.
          ///
          /// *name* starts the second paragraph.
          void e() {}

          /**
           * Command for local testing: {@code bazel test --test_strategy=local --nocache_test_results
           * --test_arg='--env=QA' //foo/bar:BazTest}
           */
          void f() {}

          /**
           * Returns the formatted identifier for the element matching the requested search key against
           * *name* below.
           */
          void g() {}

          /**
           * Convenience helper for {@code ServiceRegistry.getDefaultInstanceForTesting()
           * .useDefaultAuthentication(policy)}.
           */
          void h() {}
        }
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownCodeSpanInsideBlockQuoteAndBeforeBlockTag() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        class T {
          /// > `> foo`
          void a() {}

          /// > `foo
          /// > ` > bar
          void b() {}

          /// `foo
          /// ` @param bar
          void c() {}

          /// > `first
          /// > start > end`
          void d() {}
        }
        """;
    String expected =
        """
        class T {
          /// > `> foo`
          void a() {}

          /// > `foo ` > bar
          void b() {}

          /// `foo ` @param bar
          void c() {}

          /// > `first start > end`
          void d() {}
        }
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownWrapBeforeBlockMarkerAfterFooterTag() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        class T {
          /// Summary.
          ///
          /// @return >veryLongIdentifierNameThatExceedsTheRemainingLineLengthAndWrapsOntoItsOwnLineForTestingCase12345
          void a() {}

          /// Summary.
          ///
          /// @return ~~~veryLongIdentifierNameThatExceedsTheRemainingLineLengthAndWrapsOntoItsOwnLineForTestingCase12345
          void b() {}

          /// Summary.
          ///
          /// @param <VeryLongTypeParameterNameThatAlmostFillsTheEntireOneHundredColumnLineLimitForJavadoc> - description
          <VeryLongTypeParameterNameThatAlmostFillsTheEntireOneHundredColumnLineLimitForJavadoc> void c() {}
        }
        """;
    String expected =
        """
        class T {
          /// Summary.
          ///
          /// @return >veryLongIdentifierNameThatExceedsTheRemainingLineLengthAndWrapsOntoItsOwnLineForTestingCase12345
          void a() {}

          /// Summary.
          ///
          /// @return ~~~veryLongIdentifierNameThatExceedsTheRemainingLineLengthAndWrapsOntoItsOwnLineForTestingCase12345
          void b() {}

          /// Summary.
          ///
          /// @param <VeryLongTypeParameterNameThatAlmostFillsTheEntireOneHundredColumnLineLimitForJavadoc> -
          ///   description
          <VeryLongTypeParameterNameThatAlmostFillsTheEntireOneHundredColumnLineLimitForJavadoc> void c() {}
        }
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownWrapBlockMarkerAcrossBlockQuoteLines() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        class T {
          /// > Throws an exception if the requested indices are out of bounds for an array, i.e., if `start
          /// > +
          /// > end`.
          void a() {}

          /// > Separates a header | column from a body section using a standard marker token sequence word |
          /// > :-:
          void b() {}

          /// > First paragraph in a block quote
          /// >
          /// > =
          void c() {}

          /// > <div>foo</div>
          /// >
          /// > <div>bar</div>
          void d() {}
        }
        """;
    String expected =
        """
        class T {
          /// > Throws an exception if the requested indices are out of bounds for an array, i.e., if
          /// > `start + end`.
          void a() {}

          /// > Separates a header | column from a body section using a standard marker token sequence
          /// > word | :-:
          void b() {}

          /// > First paragraph in a block quote
          /// >
          /// > =
          void c() {}

          /// > <div>foo</div>
          /// >
          /// > <div>bar</div>
          void d() {}
        }
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void classicJavadocWrapBeforeMarkdownMarkersUnchanged() {
    String input =
        """
        class T {
          /**
           * Returns the number of elements in the given range of the array, which is equal to {@code endIdx - start}.
           */
          void a() {}
        }
        """;
    String expected =
        """
        class T {
          /**
           * Returns the number of elements in the given range of the array, which is equal to {@code endIdx
           * - start}.
           */
          void a() {}
        }
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownEmptyListItemBeforeFencedCodeBlock() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        class T {
          /// Items:
          ///
          /// - First item.
          /// -
          ///
          /// ```
          /// Foo x = new Foo();
          /// ```
          void m() {}
        }
        """;
    doFormatTest(input, input);
  }

  @Test
  public void markdownEmptyListItemBeforeFencedCodeBlockNoBlankLine() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        class T {
          /// Items:
          ///
          /// - First item.
          /// -
          /// ```
          /// Foo x = new Foo();
          /// ```
          void m() {}
        }
        """;
    String expected =
        """
        class T {
          /// Items:
          ///
          /// - First item.
          /// -
          ///
          /// ```
          /// Foo x = new Foo();
          /// ```
          void m() {}
        }
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownEmptyListItemBeforeParagraphAndFooterTag() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        class T {
          /// - First item.
          /// -
          ///
          /// Paragraph after.
          ///
          /// @return value
          int m() {
            return 0;
          }
        }
        """;
    doFormatTest(input, input);
  }

  @Test
  public void markdownEmptyListItemWithTrailingSpacesAndHtmlBlock() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        class T {
          /// - First item.
          /// -  \s
          /// <div>After.</div>
          void m() {}
        }
        """;
    String expected =
        """
        class T {
          /// - First item.
          /// -
          /// <div>After.</div>
          void m() {}
        }
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownListItemContentOnNextLine() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        class T {
          /// -
          ///   foo
          /// -
          ///   ```
          ///   bar();
          ///   ```
          /// -
          ///   - nested
          /// -
          ///   > quote
          /// -
          ///   | a | b |
          ///   | - | - |
          ///   | 1 | 2 |
          /// -\ttabbed
          void m() {}
        }
        """;
    // TODO: The blank lines after the fenced code block and table come from the pre-existing
    // requestBlankLine() at the end of writeMarkdownFencedCodeBlock/writeMarkdownTable, which
    // turns this tight list into a loose list.
    String expected =
        """
        class T {
          /// - foo
          /// - ```
          ///   bar();
          ///   ```
          ///
          /// -
          ///   - nested
          /// - > quote
          /// - | a | b |
          ///   | - | - |
          ///   | 1 | 2 |
          ///
          /// - tabbed
          void m() {}
        }
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownFencedCodeBlockInFirstListItem() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        class T {
          /// - ```
          ///   foo();
          ///   ```
          void m() {}
        }
        """;
    doFormatTest(input, input);
  }

  @Test
  public void markdownFencedCodeBlockInFirstBlockQuote() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        class T {
          /// > ```
          /// > foo();
          /// > ```
          void m() {}
        }
        """;
    doFormatTest(input, input);
  }

  @Test
  public void markdownEmptyBlockQuoteBeforeFencedCodeBlock() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        class T {
          /// Summary.
          ///
          /// >
          /// ```
          /// foo();
          /// ```
          void m() {}
        }
        """;
    String expected =
        """
        class T {
          /// Summary.
          ///
          /// >
          ///
          /// ```
          /// foo();
          /// ```
          void m() {}
        }
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownBlockQuoteBeforeHtmlBlock() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        class T {
          /// > quote
          /// <div>After.</div>
          void m() {}
        }
        """;
    doFormatTest(input, input);
  }

  @Test
  public void markdownListItemLineLengthBoundaryAndPreTrailingSpaces() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        class T {
          /// - aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa bbbbb
          ///
          /// <pre>
          /// foo  \s
          /// </pre>
          void m() {}
        }
        """;
    String expected =
        """
        class T {
          /// - aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
          ///   bbbbb
          ///
          /// <pre>
          /// foo
          /// </pre>
          void m() {}
        }
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownUnclosedHtmlTagAcrossNewlines() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        class T {
          /// <pre>
          /// foo();
          /// </pre
          ///
          /// <p>
          /// After.
          void m() {}
        }
        """;
    String expected =
        """
        class T {
          /// <pre>
          /// foo();
          /// </pre
          ///
          /// <p>
          ///
          /// After.
          void m() {}
        }
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownUnclosedOpenTagAcrossNewlines() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        class T {
          /// Summary.
          ///
          /// <p
          ///
          /// <b>After.</b>
          void m() {}
        }
        """;
    doFormatTest(input, input);
  }

  @Test
  public void multilineOpenHtmlTag() {
    String classicInput =
        """
        class T {
          /**
           * <pre
           *     class="x">
           *   foo(   a,   b);
           *     bar();
           * </pre>
           *
           * <table
           *     border="1">
           *   <tr><td>a</td></tr>
           * </table>
           */
          void classic() {}
        }
        """;
    doFormatTest(classicInput, classicInput);

    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String markdownInput =
        """
        class T {
          /// <pre
          ///     class="x">
          ///   foo(   a,   b);
          ///     bar();
          /// </pre>
          ///
          /// <p
          ///     class="aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa">bbbbb ccccc
          void markdown() {}
        }
        """;
    String markdownExpected =
        """
        class T {
          /// <pre
          ///     class="x">
          ///   foo(   a,   b);
          ///     bar();
          /// </pre>
          ///
          /// <p
          ///     class="aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa">bbbbb
          /// ccccc
          void markdown() {}
        }
        """;
    doFormatTest(markdownInput, markdownExpected);
  }

  @Test
  public void markdownMultilineFooterTagAndMoeComment() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        class T {
          /// Summary.
          ///
          /// <!--
          /// moe:begin_intracomment_strip
          /// -->
          /// Internal.
          /// <!--
          /// moe:end_intracomment_strip
          /// -->
          ///
          /// @param
          /// <T> description
          <T> void m() {}
        }
        """
            .replace("moe", "MOE");
    String expected =
        """
        class T {
          /// Summary.
          ///
          /// <!-- moe:begin_intracomment_strip -->
          /// Internal.
          /// <!-- moe:end_intracomment_strip -->
          ///
          /// @param <T> description
          <T> void m() {}
        }
        """
            .replace("moe", "MOE");
    doFormatTest(input, expected);
  }

  @Test
  public void multilineParamNamedLikeHtmlTag() {
    String classicInput =
        """
        class T {
          /**
           * Summary.
           *
           * @param
           *     <P> the proto type
           */
          <P> void classic() {}
        }
        """;
    String classicExpected =
        """
        class T {
          /**
           * Summary.
           *
           * @param <P> the proto type
           */
          <P> void classic() {}
        }
        """;
    doFormatTest(classicInput, classicExpected);

    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String markdownInput =
        """
        class T {
          /// Summary.
          ///
          /// @param
          /// <P> the proto type
          <P> void markdown() {}
        }
        """;
    String markdownExpected =
        """
        class T {
          /// Summary.
          ///
          /// @param <P> the proto type
          <P> void markdown() {}
        }
        """;
    doFormatTest(markdownInput, markdownExpected);
  }

  @Test
  public void markdownListItemStartingWithBlockElement() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        class T {
          /// - <pre>
          ///   x   y
          ///   </pre>
          /// - # Heading
          /// - <p>para
          /// - <table>
          ///   <tr><td>a</td></tr>
          ///   </table>
          /// - <blockquote>
          ///   quoted
          ///   </blockquote>
          /// - {@snippet :
          ///   code();
          ///   }
          /// -
          /// - </ul>
          /// - b
          void m() {}

          /// -
          ///   @return value
          int n() {
            return 0;
          }

          /// - <!-- moe:end_intracomment_strip -->
          void o() {}
        }
        """
            .replace("moe", "MOE");
    // TODO: As in markdownListItemContentOnNextLine, the blank lines after `<pre>`, heading,
    // `<table>`, `<blockquote>`, and `{@snippet` come from pre-existing `requestBlankLine()`
    // calls on their closing tokens, which turns this tight list into a loose list.
    String expected =
        """
        class T {
          /// - <pre>
          ///   x   y
          ///   </pre>
          ///
          /// - # Heading
          ///
          /// - <p>para
          /// - <table>
          ///   <tr><td>a</td></tr>
          ///   </table>
          ///
          /// - <blockquote>
          ///   quoted
          ///   </blockquote>
          ///
          /// - {@snippet :
          ///   code();
          ///   }
          ///
          /// -
          /// - </ul>
          /// - b
          void m() {}

          /// -
          ///
          /// @return value
          int n() {
            return 0;
          }

          /// -
          /// <!-- moe:end_intracomment_strip -->
          void o() {}
        }
        """
            .replace("moe", "MOE");
    doFormatTest(input, expected);
  }

  @Test
  public void markdownListItemInsideCodeTagForcedNewline() throws Exception {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        class T {
          /// <code>
          ///
          /// -
          ///   b
          /// </code>
          void m() {}
        }
        """;
    // Pre-existing `writeListItemOpen` adds a newline after `ForcedNewline`, so this input is not
    // idempotent; verify `writeNewlineStart` clears the pending marker space so `b` stays at two
    // spaces rather than three.
    String expected =
        """
        class T {
          /// <code>
          ///
          ///
          /// -
          ///   b
          /// </code>
          void m() {}
        }
        """;
    assertThat(formatter.formatSource(input)).isEqualTo(expected);
  }

  // TODO: b/346668798 - Test the following Markdown constructs, and make the tests work as needed.
  // We can assume that the CommonMark parser correctly handles Markdown, so the question is whether
  // they are subsequently mishandled by our formatting logic. So for example the CommonMark parser
  // already recognizes <pre>...</pre> and doesn't look for Markdown constructs within such a block,
  // so we should not need to check that that is handled correctly, given that we already check
  // <pre> handling elsewhere. On the other hand, if we don't handle Markdown code spans (`...`)
  // correctly then we might incorrectly recognize HTML tags like `<ul>` inside them.
  //
  // - Thematic breaks: ---, ***, ___, which are all rendered as <hr> and should presumably have a
  //   line break before and after. https://spec.commonmark.org/0.31.2/#thematic-breaks
  //
  // - Setext headings: text, not necessarily all on one line, followed by a line with only hyphens
  //   or equals signs. We need to preserve the line breaks before and after that line.
  //   https://spec.commonmark.org/0.31.2/#setext-headings
  //
  // - Indented code blocks
  //   Clearly evil, but we should not mangle them. *Maybe* rewrite them as fenced code blocks? But
  //   I'm sure there are lots of tricky cases, like if the indented code block includes ```.
  //   https://spec.commonmark.org/0.31.2/#indented-code-blocks
  //
  // - Link reference definitions should not be joined onto previous lines.
  //   [foo]: /url "title"
  //   https://spec.commonmark.org/0.31.2/#link-reference-definitions
  //
  // - Block quotes
  //   > foo
  //   > bar
  //   We need to ensure that each > stays at the start of its line with appropriate indentation if
  //   inside a list. https://spec.commonmark.org/0.31.2/#block-quotes
  //
  // - Autolinks
  //   <http://example.com> should be preserved. https://spec.commonmark.org/0.31.2/#autolink
}
