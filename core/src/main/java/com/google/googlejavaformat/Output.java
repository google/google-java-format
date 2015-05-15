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

import com.google.common.base.MoreObjects;
import com.google.common.collect.Range;

import java.util.Set;

/**
 * An output from the formatter.
 */
public abstract class Output extends InputOutput {
  /**
   * Unique identifier for a break.
   */
  public static final class BreakTag {}

  /**
   * Indent by outputting {@code indent} spaces.
   * @param indent the current indent
   */
  public abstract void indent(int indent);

  /**
   * Output a string.
   * @param text the string
   * @param range the {@link Range} corresponding to the string
   */
  public abstract void append(String text, Range<Integer> range);

  /**
   * Note a break as taken.
   * @param breakTag the unique break tag
   */
  public abstract void breakWasTaken(BreakTag breakTag);

  /**
   * Was a break taken?
   * @param breakTag the unique break tag
   * @return whether the break was taken
   */
  protected abstract boolean wasBreakTaken(BreakTag breakTag);

  /**
   * Return all breaks taken.
   * @return the set of breaks taken
   */
  public abstract Set<BreakTag> breaksTaken();

  /**
   * A blank line is or is not wanted here.
   * @param k the {@link Input.Tok} index
   * @param wanted whether a blank line is wanted here
   */
  public abstract void blankLine(int k, boolean wanted);

  /**
   * Marks the boundary of a region that can be partially formatted.
   * @param k the token index
   */
  public abstract void markForPartialFormat(int k);

  /**
   * Get the {@link CommentsHelper}.
   * @return the {@link CommentsHelper}
   */
  public abstract CommentsHelper getCommentsHelper();

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("super", super.toString()).toString();
  }
}
