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

package com.google.googlejavaformat.java;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.RangeSet;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Encapsulates information about a file to be formatted, including which parts of the file to
 * format.
 */
abstract class FileToFormat {
  private final ImmutableRangeSet<Integer> lineRanges;
  private final ImmutableList<Integer> offsets;
  private final ImmutableList<Integer> lengths;

  public FileToFormat(RangeSet<Integer> lineRanges, List<Integer> offsets, List<Integer> lengths) {
    this.lineRanges = ImmutableRangeSet.copyOf(lineRanges);
    this.offsets = ImmutableList.copyOf(offsets);
    this.lengths = ImmutableList.copyOf(lengths);
  }

  /**
   * The name of the file.  May be a relative path, and may contain symlinks.
   */
  public abstract String fileName();

  /**
   * An {@link InputStream} to read from the file.
   */
  public abstract InputStream inputStream() throws IOException;

  /**
   * A set of line ranges to format.
   */
  public ImmutableRangeSet<Integer> lineRanges() {
    return lineRanges;
  }

  /**
   * A list of offsets at which to start formatting.  Must match up with {@link #lengths()}.
   */
  public ImmutableList<Integer> offsets() {
    return offsets;
  }

  /**
   * A list of lengths to format, starting at the corresponding offsets.  Must match up with
   * {@link #offsets()}.
   */
  public ImmutableList<Integer> lengths() {
    return lengths;
  }
}
