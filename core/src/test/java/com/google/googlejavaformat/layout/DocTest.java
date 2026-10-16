/*
 * Copyright 2026 Google Inc.
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

package com.google.googlejavaformat.layout;

import static com.google.common.truth.Truth.assertThat;
import static com.google.googlejavaformat.layout.Docs.group;
import static com.google.googlejavaformat.layout.Docs.hardline;
import static com.google.googlejavaformat.layout.Docs.line;
import static com.google.googlejavaformat.layout.Docs.space;
import static com.google.googlejavaformat.layout.Docs.text;

import com.google.common.collect.Range;
import com.google.googlejavaformat.Input;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/// Unit tests for [Doc].
@RunWith(JUnit4.class)
public class DocTest {

  /// A minimal [Input.Tok] with the given text.
  private record FakeTok(int index, String text) implements Input.Tok {
    @Override
    public int getIndex() {
      return index;
    }

    @Override
    public int getPosition() {
      return 0;
    }

    @Override
    public int getColumn() {
      return 0;
    }

    @Override
    public String getText() {
      return text;
    }

    @Override
    public String getOriginalText() {
      return text;
    }

    @Override
    public int length() {
      return text.length();
    }

    @Override
    public boolean isNewline() {
      return text.equals("\n");
    }

    @Override
    public boolean isSlashSlashComment() {
      return text.startsWith("//");
    }

    @Override
    public boolean isSlashStarComment() {
      return text.startsWith("/*");
    }

    @Override
    public boolean isJavadocComment() {
      return text.startsWith("/**");
    }

    @Override
    public boolean isComment() {
      return isSlashSlashComment() || isSlashStarComment();
    }
  }

  private static Doc.Tok tok(String text) {
    return new Doc.Tok(new FakeTok(0, text));
  }

  @Test
  public void textWidth() {
    assertThat(text("hello").width()).isEqualTo(5);
    assertThat(text("a\nb").width()).isEqualTo(Doc.MAX_LINE_WIDTH);
  }

  @Test
  public void commentWidth() {
    assertThat(tok("// foo").width()).isEqualTo(6);
    assertThat(tok("/* foo */").width()).isEqualTo(9);
  }

  @Test
  public void multiLineCommentWidthIsFirstLine() {
    // The width is the offset of the second line, i.e. the first line including its terminator.
    assertThat(tok("/* foo\n * bar\n */").width()).isEqualTo("/* foo\n".length());
  }

  @Test
  public void slashSlashCommentFlatAddsSpace() {
    Doc.Tok tok = tok("//foo");
    assertThat(tok.flat()).isEqualTo("// foo");
    assertThat(tok.width()).isEqualTo(6);
  }

  @Test
  public void whitespaceTokWidth() {
    assertThat(tok("  ").width()).isEqualTo(2);
    assertThat(tok("  ").flat()).isEqualTo("  ");
  }

  @Test
  public void newlineTokNeverFits() {
    assertThat(tok("\n").width()).isEqualTo(Doc.MAX_LINE_WIDTH);
    assertThat(tok(" \n ").width()).isEqualTo(Doc.MAX_LINE_WIDTH);
  }

  @Test
  public void tokRange() {
    assertThat(new Doc.Tok(new FakeTok(7, "// x")).range()).isEqualTo(Range.closedOpen(7, 8));
  }

  @Test
  public void spaceAndBreakWidth() {
    assertThat(space().width()).isEqualTo(1);
    assertThat(space().flat()).isEqualTo(" ");
    assertThat(line().width()).isEqualTo(1);
    assertThat(line("").width()).isEqualTo(0);
    assertThat(hardline().width()).isEqualTo(Doc.MAX_LINE_WIDTH);
  }

  @Test
  public void groupWidthAndFlat() {
    Doc doc = group(text("a"), line(), group(text("b"), space(), text("c")));
    assertThat(doc.width()).isEqualTo(5);
    assertThat(doc.flat()).isEqualTo("a b c");
  }

  @Test
  public void groupWithForcedBreakNeverFits() {
    Doc doc = group(text("a"), hardline(), text("b"));
    assertThat(doc.width()).isEqualTo(Doc.MAX_LINE_WIDTH);
  }

  @Test
  public void groupWidthSaturates() {
    Doc doc = group(text("a".repeat(Doc.MAX_LINE_WIDTH)), text("b"));
    assertThat(doc.width()).isEqualTo(Doc.MAX_LINE_WIDTH);
  }

  @Test
  public void groupRangeSpansChildren() {
    Doc doc =
        group(
            text("("),
            new Doc.Text("a", Range.closedOpen(3, 4)),
            line(),
            new Doc.Text("b", Range.closedOpen(6, 7)),
            text(")"));
    assertThat(doc.range()).isEqualTo(Range.closedOpen(3, 7));
    assertThat(group(text("a"), line(), text("b")).range()).isEqualTo(Doc.EMPTY_RANGE);
  }

  @Test
  public void choiceWidthFlatAndRange() {
    Doc.Group preferred =
        (Doc.Group) group(text("a"), text(" "), new Doc.Text("b", Range.closedOpen(2, 3)));
    Doc.Group fallback =
        (Doc.Group) group(text("a"), line(), new Doc.Text("b", Range.closedOpen(2, 3)));
    Doc choice = new Doc.Choice(preferred, 1, fallback);
    assertThat(choice.width()).isEqualTo(3);
    assertThat(choice.flat()).isEqualTo("a b");
    assertThat(choice.range()).isEqualTo(Range.closedOpen(2, 3));
  }
}
