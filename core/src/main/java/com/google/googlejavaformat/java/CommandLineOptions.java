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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableRangeSet;

/** Command line options for google-java-format. */
@AutoValue
abstract class CommandLineOptions {

  enum SortImports {
    NO,
    /** Sort imports, but perform no formatting. */
    ONLY,
    /** Sort imports in addition to regular formatting. */
    ALSO
  }

  /** The files to format. */
  abstract ImmutableList<String> files();

  /** Format files in place. */
  abstract boolean inPlace();

  /** Line ranges to format. */
  abstract ImmutableRangeSet<Integer> lines();

  /** Character offsets for partial formatting, paired with {@code lengths}. */
  abstract ImmutableList<Integer> offsets();

  /** Partial formatting region lengths, paired with {@code offsets}. */
  abstract ImmutableList<Integer> lengths();

  /** Use AOSP style instead of Google Style (4-space indentation). */
  abstract boolean aosp();

  /** Print the version. */
  abstract boolean version();

  /** Print usage information. */
  abstract boolean help();

  /** Sort imports. */
  abstract SortImports sortImports();

  /** Format input from stdin. */
  abstract boolean stdin();

  /** Returns true if partial formatting was selected. */
  boolean isSelection() {
    return !lines().isEmpty() || !offsets().isEmpty() || !lengths().isEmpty();
  }

  static Builder builder() {
    return new AutoValue_CommandLineOptions.Builder()
        .inPlace(false)
        .aosp(false)
        .version(false)
        .help(false)
        .sortImports(SortImports.NO)
        .stdin(false);
  }

  @AutoValue.Builder
  abstract static class Builder {
    abstract Builder inPlace(boolean inPlace);

    abstract ImmutableList.Builder<String> filesBuilder();

    abstract ImmutableRangeSet.Builder<Integer> linesBuilder();

    abstract ImmutableList.Builder<Integer> offsetsBuilder();

    abstract ImmutableList.Builder<Integer> lengthsBuilder();

    abstract Builder aosp(boolean aosp);

    abstract Builder version(boolean version);

    abstract Builder help(boolean help);

    abstract Builder sortImports(SortImports sortImports);

    abstract Builder stdin(boolean stdin);

    abstract CommandLineOptions build();
  }
}
