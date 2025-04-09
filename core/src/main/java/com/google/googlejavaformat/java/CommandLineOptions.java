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

import com.google.auto.value.AutoBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableRangeSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Optional;

/**
 * Command line options for google-java-format.
 *
 * @param files The files to format.
 * @param inPlace Format files in place.
 * @param lines Line ranges to format.
 * @param offsets Character offsets for partial formatting, paired with {@code lengths}.
 * @param lengths Partial formatting region lengths, paired with {@code offsets}.
 * @param aosp Use AOSP style instead of Google Style (4-space indentation).
 * @param version Print the version.
 * @param help Print usage information.
 * @param stdin Format input from stdin.
 * @param fixImportsOnly Fix imports, but do no formatting.
 * @param sortImports Sort imports.
 * @param removeUnusedImports Remove unused imports.
 * @param dryRun Print the paths of the files whose contents would change if the formatter were run
 *     normally.
 * @param setExitIfChanged Return exit code 1 if there are any formatting changes.
 * @param assumeFilename Return the name to use for diagnostics when formatting standard input.
 */
record CommandLineOptions(
    ImmutableList<String> files,
    boolean inPlace,
    ImmutableRangeSet<Integer> lines,
    ImmutableList<Integer> offsets,
    ImmutableList<Integer> lengths,
    boolean aosp,
    boolean version,
    boolean help,
    boolean stdin,
    boolean fixImportsOnly,
    boolean sortImports,
    boolean removeUnusedImports,
    boolean dryRun,
    boolean setExitIfChanged,
    Optional<String> assumeFilename,
    boolean reflowLongStrings,
    boolean formatJavadoc,
    Optional<String> profile) {

  /** Returns true if partial formatting was selected. */
  boolean isSelection() {
    return !lines().isEmpty() || !offsets().isEmpty() || !lengths().isEmpty();
  }

  static Builder builder() {
    return new AutoBuilder_CommandLineOptions_Builder()
        .sortImports(true)
        .removeUnusedImports(true)
        .reflowLongStrings(true)
        .formatJavadoc(true)
        .aosp(false)
        .version(false)
        .help(false)
        .stdin(false)
        .fixImportsOnly(false)
        .dryRun(false)
        .setExitIfChanged(false)
        .inPlace(false);
  }

  @AutoBuilder
  interface Builder {

    ImmutableList.Builder<String> filesBuilder();

    Builder inPlace(boolean inPlace);

    Builder lines(ImmutableRangeSet<Integer> lines);

    ImmutableList.Builder<Integer> offsetsBuilder();

    @CanIgnoreReturnValue
    default Builder addOffset(Integer offset) {
      offsetsBuilder().add(offset);
      return this;
    }

    ImmutableList.Builder<Integer> lengthsBuilder();

    @CanIgnoreReturnValue
    default Builder addLength(Integer length) {
      lengthsBuilder().add(length);
      return this;
    }

    Builder aosp(boolean aosp);

    Builder version(boolean version);

    Builder help(boolean help);

    Builder stdin(boolean stdin);

    Builder fixImportsOnly(boolean fixImportsOnly);

    Builder sortImports(boolean sortImports);

    Builder removeUnusedImports(boolean removeUnusedImports);

    Builder dryRun(boolean dryRun);

    Builder setExitIfChanged(boolean setExitIfChanged);

    Builder assumeFilename(String assumeFilename);

    Builder reflowLongStrings(boolean reflowLongStrings);

    Builder formatJavadoc(boolean formatJavadoc);

    Builder profile(String profile);

    CommandLineOptions build();
  }
}
