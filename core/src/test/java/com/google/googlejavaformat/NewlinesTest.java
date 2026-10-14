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

package com.google.googlejavaformat;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link Newlines}Test */
@RunWith(JUnit4.class)
public class NewlinesTest {
  @Test
  public void offsets() {
    assertThat(ImmutableList.copyOf(Newlines.lineOffsetIterator("foo\nbar\n")))
        .containsExactly(0, 4, 8);
    assertThat(ImmutableList.copyOf(Newlines.lineOffsetIterator("foo\nbar"))).containsExactly(0, 4);

    assertThat(ImmutableList.copyOf(Newlines.lineOffsetIterator("foo\rbar\r")))
        .containsExactly(0, 4, 8);
    assertThat(ImmutableList.copyOf(Newlines.lineOffsetIterator("foo\rbar"))).containsExactly(0, 4);

    assertThat(ImmutableList.copyOf(Newlines.lineOffsetIterator("foo\r\nbar\r\n")))
        .containsExactly(0, 5, 10);
    assertThat(ImmutableList.copyOf(Newlines.lineOffsetIterator("foo\r\nbar")))
        .containsExactly(0, 5);
  }

  @Test
  public void lines() {
    assertThat(ImmutableList.copyOf(Newlines.lineIterator("foo\nbar\n")))
        .containsExactly("foo\n", "bar\n");
    assertThat(ImmutableList.copyOf(Newlines.lineIterator("foo\nbar")))
        .containsExactly("foo\n", "bar");

    assertThat(ImmutableList.copyOf(Newlines.lineIterator("foo\rbar\r")))
        .containsExactly("foo\r", "bar\r");
    assertThat(ImmutableList.copyOf(Newlines.lineIterator("foo\rbar")))
        .containsExactly("foo\r", "bar");

    assertThat(ImmutableList.copyOf(Newlines.lineIterator("foo\r\nbar\r\n")))
        .containsExactly("foo\r\n", "bar\r\n");
    assertThat(ImmutableList.copyOf(Newlines.lineIterator("foo\r\nbar")))
        .containsExactly("foo\r\n", "bar");
  }

  @Test
  public void lastLineStart() {
    assertThat(Newlines.lastLineStart("")).isEqualTo(0);
    assertThat(Newlines.lastLineStart("foo")).isEqualTo(0);
    assertThat(Newlines.lastLineStart("foo\nbar")).isEqualTo(4);
    assertThat(Newlines.lastLineStart("foo\rbar")).isEqualTo(4);
    assertThat(Newlines.lastLineStart("foo\r\nbar")).isEqualTo(5);
    assertThat(Newlines.lastLineStart("foo\n")).isEqualTo(4);
    assertThat(Newlines.lastLineStart("foo\r\n")).isEqualTo(5);
    assertThat(Newlines.lastLineStart("a\n\r\nb")).isEqualTo(4);
    assertThat(Newlines.lastLineStart("a\r\rb")).isEqualTo(3);
  }

  @Test
  public void lastLineStart_matchesLineOffsetIterator() {
    for (String input :
        ImmutableList.of(
            "", "foo", "foo\nbar\n", "foo\rbar", "foo\r\nbar\r\n", "\n", "\r\n\r", "a\n\r\nb")) {
      assertThat(Newlines.lastLineStart(input))
          .isEqualTo(Iterators.getLast(Newlines.lineOffsetIterator(input)));
    }
  }

  @Test
  public void terminalOffset() {
    Iterator<Integer> it = Newlines.lineOffsetIterator("foo\nbar\n");
    it.next();
    it.next();
    it.next();
    try {
      it.next();
      fail();
    } catch (NoSuchElementException e) {
      // expected
    }

    it = Newlines.lineOffsetIterator("foo\nbar");
    it.next();
    it.next();
    try {
      it.next();
      fail();
    } catch (NoSuchElementException e) {
      // expected
    }
  }

  @Test
  public void terminalLine() {
    Iterator<String> it = Newlines.lineIterator("foo\nbar\n");
    it.next();
    it.next();
    try {
      it.next();
      fail();
    } catch (NoSuchElementException e) {
      // expected
    }

    it = Newlines.lineIterator("foo\nbar");
    it.next();
    it.next();
    try {
      it.next();
      fail();
    } catch (NoSuchElementException e) {
      // expected
    }
  }
}
