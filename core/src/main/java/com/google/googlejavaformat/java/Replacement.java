/*
 * Copyright 2014 Google Inc. All rights reserved.
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Range;
import java.util.Objects;

/**
 * Represents a range in the original source and replacement text for that range.
 *
 * <p>google-java-format doesn't depend on AutoValue, to allow AutoValue to depend on
 * google-java-format.
 */
public final class Replacement {

  public static Replacement create(int startPosition, int endPosition, String replaceWith) {
    checkArgument(startPosition >= 0, "startPosition must be non-negative");
    checkArgument(startPosition <= endPosition, "startPosition cannot be after endPosition");
    return new Replacement(Range.closedOpen(startPosition, endPosition), replaceWith);
  }

  private final Range<Integer> replaceRange;
  private final String replacementString;

  private Replacement(Range<Integer> replaceRange, String replacementString) {
    this.replaceRange = checkNotNull(replaceRange, "Null replaceRange");
    this.replacementString = checkNotNull(replacementString, "Null replacementString");
  }

  /** The range of characters in the original source to replace. */
  public Range<Integer> getReplaceRange() {
    return replaceRange;
  }

  /** The string to replace the range of characters with. */
  public String getReplacementString() {
    return replacementString;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof Replacement) {
      Replacement that = (Replacement) o;
      return replaceRange.equals(that.getReplaceRange())
          && replacementString.equals(that.getReplacementString());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(replaceRange, replacementString);
  }
}
