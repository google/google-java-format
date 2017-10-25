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
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Joiner;
import com.google.common.io.ByteStreams;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests formatting javadoc. */
@RunWith(JUnit4.class)
public final class JavadocFormattingTest {

  private final Formatter formatter = new Formatter();

  @Test
  public void notJavadoc() {
    String[] input = {
      "/**/", //
      "class Test {}",
    };
    String[] expected = {
      "/**/", //
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void empty() {
    String[] input = {
      "/***/", //
      "class Test {}",
    };
    String[] expected = {
      "/** */", "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void emptyMultipleLines() {
    String[] input = {
      "/**", //
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/** */", "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void simple() {
    String[] input = {
      "/** */", //
      "class Test {}",
    };
    String[] expected = {
      "/** */", "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void commentMostlyUntouched() {
    // This test isn't necessarily what we'd want to do, but it's what we do now, and it's OK-ish.
    String[] input = {
      "/**",
      " * Foo.",
      " *",
      " *  <!--",
      "*abc",
      " *   def   ",
      " * </tr>",
      " *-->bar",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/**",
      " * Foo.",
      " * <!--",
      " *abc",
      " *   def",
      " * </tr>",
      " *-->",
      " * bar",
      " */",
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void moeComments() {
    String[] input = {
      "/**",
      " * Deatomizes the given user.",
      " * <!-- MOE:begin_intracomment_strip -->",
      " * See go/deatomizer-v5 for the design doc.",
      " * <!-- MOE:end_intracomment_strip -->",
      " * To reatomize, call {@link reatomize}.",
      " *",
      " * <!-- MOE:begin_intracomment_strip -->",
      " * <p>This method is used in the Google teleporter.",
      " *",
      " * <p>Yes, we have a teleporter.",
      " * <!-- MOE:end_intracomment_strip -->",
      " *",
      " * @param user the person to teleport.",
      " *     <!-- MOE:begin_intracomment_strip -->",
      " *     Users must sign go/deatomize-waiver ahead of time.",
      " *     <!-- MOE:end_intracomment_strip -->",
      " * <!-- MOE:begin_intracomment_strip -->",
      " * @deprecated Sometimes turns the user into a goat.",
      " * <!-- MOE:end_intracomment_strip -->",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/**",
      " * Deatomizes the given user.",
      " * <!-- MOE:begin_intracomment_strip -->",
      " * See go/deatomizer-v5 for the design doc.",
      " * <!-- MOE:end_intracomment_strip -->",
      " * To reatomize, call {@link reatomize}.",
      " *",
      " * <!-- MOE:begin_intracomment_strip -->",
      " * <p>This method is used in the Google teleporter.",
      " *",
      " * <p>Yes, we have a teleporter.",
      " * <!-- MOE:end_intracomment_strip -->",
      " *",
      " * @param user the person to teleport.",
      " *     <!-- MOE:begin_intracomment_strip -->",
      " *     Users must sign go/deatomize-waiver ahead of time.",
      " *     <!-- MOE:end_intracomment_strip -->",
      " * <!-- MOE:begin_intracomment_strip -->",
      " * @deprecated Sometimes turns the user into a goat.",
      " * <!-- MOE:end_intracomment_strip -->",
      " */",
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void moeCommentBeginOnlyInMiddleOfDoc() {
    // We don't really care what happens here so long as we don't explode.
    String[] input = {
      "/**", //
      " * Foo.",
      " * <!-- MOE:begin_intracomment_strip -->",
      " * Bar.",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/**", //
      " * Foo.",
      " * <!-- MOE:begin_intracomment_strip -->",
      " * Bar.",
      " */",
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void moeCommentBeginOnlyAtEndOfDoc() {
    // We don't really care what happens here so long as we don't explode.
    // TODO(cpovirk): OK, maybe try to leave it in....
    String[] input = {
      "/**", //
      " * Foo.",
      " * <!-- MOE:begin_intracomment_strip -->",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/** Foo. */", //
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void moeCommentEndOnly() {
    // We don't really care what happens here so long as we don't explode.
    String[] input = {
      "/**", //
      " * Foo.",
      " * <!-- MOE:end_intracomment_strip -->",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/**", //
      " * Foo.",
      " * <!-- MOE:end_intracomment_strip -->",
      " */",
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void tableMostlyUntouched() {
    String[] input = {
      "/**",
      " * Foo.",
      " *",
      " *  <table>",
      "*<tr><td>a<td>b</tr>",
      " * <tr>",
      " * <td>A",
      " *     <td>B",
      " * </tr>",
      " *</table>",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/**",
      " * Foo.",
      " *",
      " * <table>",
      " * <tr><td>a<td>b</tr>",
      " * <tr>",
      " * <td>A",
      " *     <td>B",
      " * </tr>",
      " * </table>",
      " */",
      "class Test {}",
    };
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
    String[] input = {
      "/**", //
      " * Example:",
      " *",
      " *  <pre>",
      "*    1 2<br>    3   ",
      " *4 5 6",
      "7 8",
      " *</pre>",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/**", //
      " * Example:",
      " *",
      " * <pre>",
      " *    1 2<br>    3",
      " * 4 5 6",
      " * 7 8",
      " * </pre>",
      " */",
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void preCodeExample() {
    // We should figure out whether we want a newline or blank line before <pre> or not.
    String[] input = {
      "/**",
      " * Example:",
      " *",
      " * <pre>   {@code",
      " *",
      " *   Abc.def(foo, 7, true); // blah}</pre>",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/**",
      " * Example:",
      " *",
      " * <pre>{@code",
      " * Abc.def(foo, 7, true); // blah",
      " * }</pre>",
      " */",
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void preNotWrapped() {
    String[] input = {
      "/**",
      " * Example:",
      " *",
      " * <pre>",
      " * 456789012 456789012 456789012 456789012 456789012 456789012 456789012 456789012 "
          + "456789012 45678901",
      " * </pre>",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/**",
      " * Example:",
      " *",
      " * <pre>",
      " * 456789012 456789012 456789012 456789012 456789012 456789012 456789012 456789012 "
          + "456789012 45678901",
      " * </pre>",
      " */",
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void javaCodeInPre() {
    String[] input = {
      "/**",
      " * Example:",
      " *",
      " *<pre>",
      " * aaaaa    |   a  |   +",
      " * \"bbbb    |   b  |  \"",
      " *</pre>",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/**",
      " * Example:",
      " *",
      " * <pre>",
      " * aaaaa    |   a  |   +",
      " * \"bbbb    |   b  |  \"",
      " * </pre>",
      " */",
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void joinLines() {
    String[] input = {
      "/**", //
      " * foo",
      " * bar",
      " * baz",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/** foo bar baz */", //
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void oneLinerIs100() {
    String[] input = {
      "/**",
      " * 567890123 567890123 567890123 567890123 567890123 567890123 567890123 567890123 "
          + "567890123 567",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/** 567890123 567890123 567890123 567890123 567890123 567890123 567890123 567890123 "
          + "567890123 567 */",
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void oneLinerWouldBe101() {
    String[] input = {
      "/**",
      " * 567890123 567890123 567890123 567890123 567890123 567890123 567890123 567890123 "
          + "567890123 5678",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/**",
      " * 567890123 567890123 567890123 567890123 567890123 567890123 567890123 567890123 "
          + "567890123 5678",
      " */",
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void multilineWrap() {
    String[] input = {
      "/**",
      " * 456789012 456789012 456789012 456789012 456789012 456789012 456789012 456789012 "
          + "456789012 45678901",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/**",
      " * 456789012 456789012 456789012 456789012 456789012 456789012 456789012 456789012 "
          + "456789012",
      " * 45678901",
      " */",
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void tooLong() {
    String[] input = {
      "/**",
      " * abc",
      " *",
      " * <p>789012345678901234567890123456789012345678901234567890123456789012345678901234567"
          + "8901234567890123456",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/**",
      " * abc",
      " *",
      " * <p>789012345678901234567890123456789012345678901234567890123456789012345678901234567"
          + "8901234567890123456",
      " */",
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void joinedTokens() {
    /*
     * Originally, 4, <b>, and 8901 are separate tokens. Test that we join them (and thus don't
     * split them across lines).
     */
    String[] input = {
      "/**",
      " * 456789012 456789012 456789012 456789012 456789012 456789012 456789012 456789012 "
          + "456789012 4<b>8901",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/**",
      " * 456789012 456789012 456789012 456789012 456789012 456789012 456789012 456789012 "
          + "456789012",
      " * 4<b>8901",
      " */",
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void joinedAtSign() {
    /*
     * The last 456789012 would fit on the first line with the others. But putting it there would
     * mean the next line would start with @5678901, which would then be interpreted as a tag.
     */
    String[] input = {
      "/**",
      " * 456789012 456789012 456789012 456789012 456789012 456789012 456789012 456789012 "
          + "456789012 @5678901",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/**",
      " * 456789012 456789012 456789012 456789012 456789012 456789012 456789012 456789012",
      " * 456789012 @5678901",
      " */",
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void joinedMultipleAtSign() {
    // This is the same as above except that it tests multiple consecutive @... tokens.
    String[] input = {
      "/**",
      " * 456789012 456789012 456789012 456789012 456789012 456789012 456789012 456789012 "
          + "@56789012 @5678901",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/**",
      " * 456789012 456789012 456789012 456789012 456789012 456789012 456789012",
      " * 456789012 @56789012 @5678901",
      " */",
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void noAsterisk() {
    String[] input = {
      "/**", //
      " abc<p>def",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/**", //
      " * abc",
      " *",
      " * <p>def",
      " */",
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void significantAsterisks() {
    String[] input = {
      "/** *", //
      " * *",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/** * * */", //
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void links() {
    String[] input = {
      "/**",
      " * 456789012 456789012 456789012 456789012 456789012 456789012 456789012 456789012 "
          + "456789012 4567 <a",
      " * href=foo>foo</a>.",
      " *",
      " * <p>789012 456789012 456789012 456789012 456789012 456789012 456789012 456789 "
          + "<a href=foo>",
      " * foo</a>.",
      " *",
      " * <p>789012 456789012 456789012 456789012 456789012 456789012 456789012 4567890 "
          + "<a href=foo>",
      " * foo</a>.",
      " *",
      " * <p><a href=foo>",
      " * foo</a>.",
      " *",
      " * <p>foo <a href=bar>",
      " * bar</a>.",
      " *",
      " * <p>foo-<a href=bar>",
      " * bar</a>.",
      " *",
      " * <p>foo<a href=bar>",
      " * bar</a>.",
      " *",
      " * <p><a href=foo>foo</a> bar.",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/**",
      " * 456789012 456789012 456789012 456789012 456789012 456789012 456789012 456789012 "
          + "456789012 4567 <a",
      " * href=foo>foo</a>.",
      " *",
      " * <p>789012 456789012 456789012 456789012 456789012 456789012 456789012 456789 "
          + "<a href=foo>foo</a>.",
      " *",
      " * <p>789012 456789012 456789012 456789012 456789012 456789012 456789012 4567890 "
          + "<a href=foo>",
      " * foo</a>.",
      " *",
      " * <p><a href=foo>foo</a>.",
      " *",
      " * <p>foo <a href=bar>bar</a>.",
      " *",
      " * <p>foo-<a href=bar>bar</a>.",
      " *",
      /*
       * In this next case, we've removed a space from the output. Fortunately, the depot doesn't
       * appear to contain any occurrences of this pattern. And if it does, the better fix is to
       * insert a space before <a href> rather than after.
       */
      " * <p>foo<a href=bar>bar</a>.",
      " *",
      " * <p><a href=foo>foo</a> bar.",
      " */",
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void heading() {
    String[] input = {
      "/**", //
      " * abc<h1>def</h1>ghi",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/**", //
      " * abc",
      " *",
      " * <h1>def</h1>",
      " *",
      " * ghi",
      " */",
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void blockquote() {
    String[] input = {
      "/**", //
      " * abc<blockquote><p>def</blockquote>ghi",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/**", //
      " * abc",
      " *",
      " * <blockquote>",
      " *",
      " * <p>def",
      " *",
      " * </blockquote>",
      " *",
      " * ghi",
      " */",
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void lists() {
    String[] input = {
      "/**", //
      "* hi",
      "*",
      "* <ul>",
      "* <li>",
      "* <ul>",
      "* <li>a</li>",
      "* </ul>",
      "* </li>",
      "* </ul>",
      "*/",
      "class Test {}",
    };
    String[] expected = {
      "/**", //
      " * hi",
      " *",
      " * <ul>",
      " *   <li>",
      " *       <ul>",
      " *         <li>a",
      " *       </ul>",
      " * </ul>",
      " */",
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void lists2() {
    String[] input = {
      "/**", //
      " * Foo.",
      " *",
      " * <ul><li>1<ul><li>1a<li>1b</ul>more 1<p>still more 1<li>2</ul>",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/**", //
      " * Foo.",
      " *",
      " * <ul>",
      " *   <li>1",
      " *       <ul>",
      " *         <li>1a",
      " *         <li>1b",
      " *       </ul>",
      " *       more 1",
      " *       <p>still more 1",
      " *   <li>2",
      " * </ul>",
      " */",
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void closeInnerListStillNewline() {
    String[] input = {
      "/**", //
      " * Foo.",
      " *",
      " * <ul><li><ul><li>a</ul>b</ul>",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/**", //
      " * Foo.",
      " *",
      " * <ul>",
      " *   <li>",
      " *       <ul>",
      " *         <li>a",
      " *       </ul>",
      " *       b",
      " * </ul>",
      " */",
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void listItemWrap() {
    String[] input = {
      "/**", //
      " * Foo.",
      " *",
      " * <ul><li>234567890 234567890 234567890 234567890 234567890 234567890 234567890 234567890"
          + " 234567890 234567890</ul>",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/**", //
      " * Foo.",
      " *",
      " * <ul>",
      " *   <li>234567890 234567890 234567890 234567890 234567890 234567890 234567890 234567890"
          + " 234567890",
      " *       234567890",
      " * </ul>",
      " */",
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void unclosedList() {
    String[] input = {
      "/**", //
      " * Foo.",
      " *",
      " * <ul><li>1",
      " * @return blah",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/**", //
      " * Foo.",
      " *",
      " * <ul>",
      " *   <li>1",
      " *",
      " * @return blah",
      " */",
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void br() {
    String[] input = {
      "/**", //
      " * abc<br>def",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/**", //
      " * abc<br>",
      " * def",
      " */",
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void brSpaceBug() {
    // TODO(b/28983091): Remove the space before <br> here.
    String[] input = {
      "/**", //
      " * abc <br>def",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/**", //
      " * abc <br>",
      " * def",
      " */",
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void brAtSignBug() {
    /*
     * This is a bug -- more of a "spec" bug than an implementation bug, and hard to fix.
     * Fortunately, some very quick searching didn't turn up any instances in the Google codebase.
     */
    String[] input = {
      "/**", //
      " * abc<br>@foo ",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/**", //
      " * abc<br>",
      " * @foo", // interpreted as a block tag now!
      " */",
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void unicodeCharacterCountArguableBug() {
    /*
     * We might prefer for multi-char characters like 𝄞 to be treated as taking up one column (or
     * perhaps for all characters to be treated based on their width in monospace fonts). But
     * currently we just count chars.
     */
    String[] input = {
      "/**",
      " * 456789𝄞12 456789𝄞12 456789𝄞12 456789𝄞12 456789𝄞12 456789𝄞12 456789𝄞12 456789𝄞12 "
          + "456789𝄞12 456789𝄞",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/**",
      " * 456789𝄞12 456789𝄞12 456789𝄞12 456789𝄞12 456789𝄞12 456789𝄞12 456789𝄞12 456789𝄞12",
      " * 456789𝄞12 456789𝄞",
      " */",
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void blankLineBeforeParams() {
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
    };
    doFormatTest(input, expected);
  }

  @Test
  public void onlyParams() {
    String[] input = {
      "/**", //
      " *",
      " *",
      " * @param this is a param",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/** @param this is a param */", //
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void paramsContinuationIndented() {
    String[] input = {
      "/**", //
      " * hello world",
      " *",
      " * @param foo 567890123 567890123 567890123 567890123 567890123 567890123 567890123"
          + " 567890123 567890123",
      " * @param bar another",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/**", //
      " * hello world",
      " *",
      " * @param foo 567890123 567890123 567890123 567890123 567890123 567890123 567890123"
          + " 567890123",
      " *     567890123",
      " * @param bar another",
      " */",
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void paramsOtherIndents() {
    String[] input = {
      "/**", //
      " * hello world",
      " *",
      " * @param foo a<p>b<ul><li>a<ul><li>x</ul></ul>",
      " * @param bar another",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/**", //
      " * hello world",
      " *",
      " * @param foo a",
      " *     <p>b",
      " *     <ul>",
      " *       <li>a",
      " *           <ul>",
      " *             <li>x",
      " *           </ul>",
      " *     </ul>",
      " *", // TODO(cpovirk): Ideally we would probably eliminate this.
      " * @param bar another",
      " */",
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void paragraphTag() {
    String[] input = {
      "class Test {",
      "  /**",
      "   * hello<p>world",
      "   */",
      "  void f() {}",
      "",
      "  /**",
      "   * hello",
      "   * <p>",
      "   * world",
      "   */",
      "  void f() {}",
      "}",
    };
    String[] expected = {
      "class Test {",
      "  /**",
      "   * hello",
      "   *",
      "   * <p>world",
      "   */",
      "  void f() {}",
      "",
      "  /**",
      "   * hello",
      "   *",
      "   * <p>world",
      "   */",
      "  void f() {}",
      "}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void xhtmlParagraphTag() {
    String[] input = {
      "class Test {",
      "  /**",
      "   * hello<p/>world",
      "   */",
      "  void f() {}",
      "",
      "}",
    };
    String[] expected = {
      "class Test {",
      "  /**",
      "   * hello",
      "   *",
      "   * <p>world",
      "   */",
      "  void f() {}",
      "}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void removeInitialParagraphTag() {
    String[] input = {
      "/**", //
      " * <p>hello<p>world",
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
    };
    doFormatTest(input, expected);
  }

  @Test
  public void inferParagraphTags() {
    String[] input = {
      "/**",
      " *",
      " *",
      " * foo",
      " * foo",
      " *",
      " *",
      " * foo",
      " *",
      " * bar",
      " *",
      " * <pre>",
      " *",
      " * baz",
      " *",
      " * </pre>",
      " *",
      " * <ul>",
      " * <li>foo",
      " *",
      " * bar",
      " * </ul>",
      " *",
      " *",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/**",
      " * foo foo",
      " *",
      " * <p>foo",
      " *",
      " * <p>bar",
      " *",
      " * <pre>",
      " *",
      " * baz",
      " *",
      " * </pre>",
      " *",
      " * <ul>",
      " *   <li>foo",
      " *       <p>bar",
      " * </ul>",
      " */",
      "class Test {}",
    };
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
    String[] input = {
      "/** abc {@code {} <p> <li> <pre> <table>} def */", //
      "class Test {}",
    };
    String[] expected = {
      "/** abc {@code {} <p> <li> <pre> <table>} def */", //
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void loneBraceDoesNotStartInlineTag() {
    String[] input = {
      "/** {  <p> } */", //
      "class Test {}",
    };
    String[] expected = {
      "/**", //
      " * {",
      " *",
      " * <p>}",
      " */",
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void unicodeEscapesNotReplaced() {
    // Test that we don't replace them with their interpretations.
    String[] input = {
      "/** foo \\u0000 bar \\u6c34 baz */", //
      "class Test {}",
    };
    String[] expected = {
      "/** foo \\u0000 bar \\u6c34 baz */", //
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void unicodeEscapesNotInterpretedBug() {
    /*
     * In theory, \u003C should be treated exactly like <, and so too should the escaped versions of
     * @, *, and other special chars. We don't recognize that, though, so we don't put what is
     * effectively "<p>" on a new line.
     */
    String[] input = {
      "/** a\\u003Cp>b */", //
      "class Test {}",
    };
    String[] expected = {
      "/** a\\u003Cp>b */", //
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void trailingLink() {
    // Eclipse's parser seems to want to discard the line break after {@link}. Test that we see it.
    String[] input = {
      "/**", //
      " * abc {@link Foo}",
      " * def",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/** abc {@link Foo} def */", //
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void codeInCode() {
    // Eclipse's parser seems to get confused at the second {@code}. Test that we handle it.
    String[] input = {
      "/** abc {@code {@code foo}} def */", //
      "class Test {}",
    };
    String[] expected = {
      "/** abc {@code {@code foo}} def */", //
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void quotedTextSplitAcrossLinks() {
    /*
     * This demonstrates one of multiple reasons that we can't hand the Javadoc *content* to
     * Eclipse's lexer as if it were Java code.
     */
    String[] input = {
      "/**", //
      " * abc \"foo",
      " * bar baz\" def",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/** abc \"foo bar baz\" def */", //
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void standardizeTags() {
    String[] input = {
      "/**",
      " * foo",
      " *",
      " * <P>bar",
      " *",
      " * <p class=clazz>baz<BR>",
      " * baz",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/**",
      " * foo",
      " *",
      " * <p>bar",
      " *",
      " * <p class=clazz>baz<br>",
      " * baz",
      " */",
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void removeCloseTags() {
    String[] input = {
      "/**", //
      " * foo</p>",
      " *",
      " * <p>bar</p>",
      " */",
      "class Test {}",
    };
    String[] expected = {
      "/**", //
      " * foo",
      " *",
      " * <p>bar",
      " */",
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void javadocFullSentences() {
    String[] input = {
      "/** In our application, bats are often found hanging from the ceiling, especially on"
          + " Wednesdays.  Sometimes sick bats have issues where their claws do not close entirely."
          + "  This class provides a nice, grippable surface for them to cling to. */",
      "class Grippable {}",
    };
    String[] expected = {
      "/**",
      " * In our application, bats are often found hanging from the ceiling, especially on"
          + " Wednesdays.",
      " * Sometimes sick bats have issues where their claws do not close entirely. This class"
          + " provides a",
      " * nice, grippable surface for them to cling to.",
      " */",
      "class Grippable {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void javadocSentenceFragment() {
    String[] input = {
      "/** Provides a comfy, grippable surface for sick bats with claw-closing problems, which are"
          + " sometimes found hanging from the ceiling on Wednesdays. */",
      "class Grippable {}",
    };
    String[] expected = {
      "/**",
      " * Provides a comfy, grippable surface for sick bats with claw-closing problems, which are"
          + " sometimes",
      " * found hanging from the ceiling on Wednesdays.",
      " */",
      "class Grippable {}",
    };
    doFormatTest(input, expected);
  }

  @Test
  public void javadocCanEndAnywhere() {
    String[] input = {
      "/** foo <pre*/", //
      "class Test {}",
    };
    String[] expected = {
      "/** foo <pre */", //
      "class Test {}",
    };
    doFormatTest(input, expected);
  }

  private void doFormatTest(String[] input, String[] expected) {
    try {
      String actual = formatter.formatSource(Joiner.on('\n').join(input));
      assertThat(actual).isEqualTo(Joiner.on('\n').join(expected) + "\n");
    } catch (FormatterException e) {
      throw new AssertionError(e);
    }
  }

  @Test
  public void windowsLineSeparator() throws FormatterException {
    String[] input = {
      "/**", " * hello", " *", " * <p>world", " */", "class Test {}",
    };
    for (String separator : Arrays.asList("\r", "\r\n")) {
      String actual = formatter.formatSource(Joiner.on(separator).join(input));
      assertThat(actual).isEqualTo(Joiner.on(separator).join(input) + separator);
    }
  }

  @Test
  public void u2028LineSeparator() {
    String[] input = {
      "public class Foo {",
      "  /**\u2028",
      "   * Set and enable something.",
      "   */",
      "  public void setSomething() {}",
      "}",
    };
    String[] expected = {
      "public class Foo {",
      "  /**",
      "   * \u2028 Set and enable something.",
      "   */",
      "  public void setSomething() {}",
      "}",
    };
    doFormatTest(input, expected);
  }
}
