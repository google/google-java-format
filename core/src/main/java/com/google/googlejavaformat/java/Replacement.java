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

import com.google.common.base.MoreObjects;
import com.google.common.collect.Range;

/**
 * Represents a range in the original source and replacement text for that range.
 */
public final class Replacement {
  private final Range<Integer> replaceRange;
  private final String replaceString;

  private Replacement(Range<Integer> replaceRange, String replaceString) {
    this.replaceRange = replaceRange;
    this.replaceString = replaceString;
  }

  public static Replacement create(Range<Integer> replaceRange, String replaceString) {
    return new Replacement(replaceRange, replaceString);
  }

  /**
   * Returns the range of characters in the original source to replace.
   */
  public Range<Integer> getReplaceRange() {
    return replaceRange;
  }

  /**
   * Returns the string to replace the range of characters with.
   */
  public String getReplacementString() {
    return replaceString;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    Replacement other = (Replacement) obj;
    return replaceRange.equals(other.replaceRange) && replaceString.equals(other.replaceString);
  }

  @Override
  public int hashCode() {
    return 31 * replaceRange.hashCode() + replaceString.hashCode();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("replaceRange", replaceRange)
        .add("replaceString", replaceString)
        .toString();
  }
}
