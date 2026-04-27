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
import static com.google.common.truth.TruthJUnit.assume;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.io.ByteStreams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests formatting javadoc. */
@RunWith(JUnit4.class)
public final class JavadocFormattingTest {
  private static final boolean MARKDOWN_JAVADOC_SUPPORTED = Runtime.version().feature() >= 23;

  private final Formatter formatter = new Formatter();

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
    // This test isn't necessarily what we'd want to do, but it's what we do now, and it's OK-ish.
    @SuppressWarnings("MisleadingEscapedSpace") // TODO(b/496180372): remove
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
         *abc
         *   def
         * </tr>
         *-->
         * bar
         */
        class Test {}
        """;
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
         *
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
  public void brAtSignBug() {
    /*
     * This is a bug -- more of a "spec" bug than an implementation bug, and hard to fix.
     * Fortunately, some very quick searching didn't turn up any instances in the Google codebase.
     */
    @SuppressWarnings("MisleadingEscapedSpace") // TODO(b/496180372): remove
    String input =
        """
        /**
         * abc<br>@foo\s
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
    doFormatTest(input, expected);
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
///   3. nested thing 3 after a blank line
///
/// A following paragraph.
class Test {}
""";
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
  public void markdownThematicBreaks() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        /// foo
        /// ***
        /// bar
        class Test {}
        """;
    // TODO: the line break before `***` should be preserved.
    // It's OK to introduce a blank line before `bar` since it is a new paragraph.
    String expected =
        """
        /// foo ***
        ///
        /// bar
        class Test {}
        """;
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
        /// Subheading
        /// ----------
        class Test {}
        """;
    // TODO: the line breaks before the lines of repeated characters should be preserved.
    //    Or, we could rewrite this style of heading as `# Heading`.
    String expected =
        """
        /// Heading =======
        ///
        /// Phoebe B. Peabody-Beebe
        ///
        /// Subheading ----------
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownIndentedCodeBlocks() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        ///     code block
        ///     is indented
        class Test {}
        """;
    // TODO: the evil indented code block should be preserved.
    String expected =
        """
        /// code block is indented
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownLinkReferenceDefinitions() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        /// [foo]
        /// [foo]: /url "title"
        class Test {}
        """;
    String expected =
        """
        /// [foo] [foo]: /url "title"
        class Test {}
        """;
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
    // TODO: the line break between items should be preserved, and there should not be a blank line
    //   before the list.
    String expected =
        """
        ///
        /// - item 1
        /// - item 2
        class Test {}
        """;
    doFormatTest(input, expected);
  }

  @Test
  public void markdownBlockQuotes() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        /// > foo
        /// > bar
        class Test {}
        """;
    // TODO: the block quote should be preserved, and ideally bar would be joined to foo.
    String expected =
        """
        /// >
        ///
        /// foo > bar
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
  public void markdownAutolinks() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
    String input =
        """
        /// <http://example.com> should be preserved.
        class Test {}
        """;
    // TODO: find a test case that will break if autolinks are not handled correctly.
    // Probably something like: <http://{@code>this should not be handled like a code span}
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

  private void doFormatTest(String input, String expected) {
    try {
      String actual = formatter.formatSource(input);
      assertThat(actual).isEqualTo(expected);
    } catch (FormatterException e) {
      throw new AssertionError(e);
    }
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
  // - Loose lists
  //   "A list is loose if any of its constituent list items are separated by blank lines, or if any
  //   of its constituent list items directly contain two block-level elements with a blank line
  //   between them."
  //   We should test that we do not remove blank lines from a loose list, which would make it a
  //   tight one. https://spec.commonmark.org/0.31.2/#loose
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
