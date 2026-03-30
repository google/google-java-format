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
import com.google.errorprone.annotations.InlineMe;

/**
 * Represents a range in the original source and replacement text for that range.
 *
 * @param replaceRange The range of characters in the original source to replace.
 * @param replacementString The string to replace the range of characters with.
 */
public record Replacement(Range<Integer> replaceRange, String replacementString) {

  public static Replacement create(int startPosition, int endPosition, String replaceWith) {
    checkArgument(startPosition >= 0, "startPosition must be non-negative");
    checkArgument(startPosition <= endPosition, "startPosition cannot be after endPosition");
    return new Replacement(Range.closedOpen(startPosition, endPosition), replaceWith);
  }

  public Replacement {
    checkNotNull(replaceRange, "Null replaceRange");
    checkNotNull(replacementString, "Null replacementString");
  }

  @InlineMe(replacement = "this.replaceRange()")
  public Range<Integer> getReplaceRange() {
    return replaceRange();
  }

  @InlineMe(replacement = "this.replacementString()")
  public String getReplacementString() {
    return replacementString();
  }
}
