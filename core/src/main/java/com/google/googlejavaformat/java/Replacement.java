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

import com.google.auto.value.AutoValue;
import com.google.common.collect.Range;

/**
 * Represents a range in the original source and replacement text for that range.
 */
@AutoValue
public abstract class Replacement {

  /** The range of characters in the original source to replace. */
  public abstract Range<Integer> getReplaceRange();

  /** The string to replace the range of characters with. */
  public abstract String getReplacementString();

  public static Replacement create(int startPosition, int endPosition, String replaceWith) {
    return new AutoValue_Replacement(Range.closedOpen(startPosition, endPosition), replaceWith);
  }
}
