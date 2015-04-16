/*
 * Copyright 2015 Google Inc.
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

import com.google.common.base.CharMatcher;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;

import java.util.Set;

/**
 * A fake Output class, used for counting how many lines a Level would require in filled mode.
 */
final class FakeOutput extends Output {
  private final int maxWidth;
  private final CommentsHelper commentsHelper;

  private int lineI = 0;

  private final Set<BreakTag> breaksTaken = Sets.newIdentityHashSet();

  /**
   * Constructor.
   * @param maxWidth the width information
   */
  FakeOutput(int maxWidth, CommentsHelper commentsHelper, Set<BreakTag> breaksTaken) {
    this.maxWidth = maxWidth;
    this.commentsHelper = commentsHelper;
    this.breaksTaken.addAll(breaksTaken);
  }

  /**
   * Get the current line number.
   * @return the current line number
   */
  int getLineI() {
    return lineI;
  }

  @Override
  public void breakWasTaken(BreakTag breakTag) {
    breaksTaken.add(breakTag);
  }

  @Override
  public boolean wasBreakTaken(BreakTag breakTag) {
    return breaksTaken.contains(breakTag);
  }

  @Override
  public Set<BreakTag> breaksTaken() {
    return ImmutableSet.copyOf(breaksTaken);
  }

  @Override
  public void append(String text, Range<Integer> range) {
    lineI += CharMatcher.is('\n').countIn(text);
  }

  @Override
  public void indent(int indent) {}

  @Override
  public CommentsHelper getCommentsHelper() {
    return commentsHelper;
  }

  @Override
  public void blankLine(int k, boolean wanted) {}

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("maxWidth", maxWidth)
        .add("lineI", lineI)
        .toString();
  }
}
