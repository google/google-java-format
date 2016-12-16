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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableRangeSet;

/**
 * Command line options for google-java-format.
 *
 * <p>google-java-format doesn't depend on AutoValue, to allow AutoValue to depend on
 * google-java-format.
 */
final class CommandLineOptions {

  private final ImmutableList<String> files;
  private final boolean inPlace;
  private final ImmutableRangeSet<Integer> lines;
  private final ImmutableList<Integer> offsets;
  private final ImmutableList<Integer> lengths;
  private final boolean aosp;
  private final boolean version;
  private final boolean help;
  private final boolean stdin;
  private final boolean fixImportsOnly;
  private final boolean removeJavadocOnlyImports;
  private final boolean sortImports;
  private final boolean removeUnusedImports;

  CommandLineOptions(
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
      boolean removeJavadocOnlyImports,
      boolean sortImports,
      boolean removeUnusedImports) {
    this.files = files;
    this.inPlace = inPlace;
    this.lines = lines;
    this.offsets = offsets;
    this.lengths = lengths;
    this.aosp = aosp;
    this.version = version;
    this.help = help;
    this.stdin = stdin;
    this.fixImportsOnly = fixImportsOnly;
    this.removeJavadocOnlyImports = removeJavadocOnlyImports;
    this.sortImports = sortImports;
    this.removeUnusedImports = removeUnusedImports;
  }

  /** The files to format. */
  ImmutableList<String> files() {
    return files;
  }

  /** Format files in place. */
  boolean inPlace() {
    return inPlace;
  }

  /** Line ranges to format. */
  ImmutableRangeSet<Integer> lines() {
    return lines;
  }

  /** Character offsets for partial formatting, paired with {@code lengths}. */
  ImmutableList<Integer> offsets() {
    return offsets;
  }

  /** Partial formatting region lengths, paired with {@code offsets}. */
  ImmutableList<Integer> lengths() {
    return lengths;
  }

  /** Use AOSP style instead of Google Style (4-space indentation). */
  boolean aosp() {
    return aosp;
  }

  /** Print the version. */
  boolean version() {
    return version;
  }

  /** Print usage information. */
  boolean help() {
    return help;
  }

  /** Format input from stdin. */
  boolean stdin() {
    return stdin;
  }

  /** Fix imports, but do no formatting. */
  boolean fixImportsOnly() {
    return fixImportsOnly;
  }

  /**
   * When fixing imports, remove imports that are used only in javadoc and fully-qualify any
   * {@code @link} tags referring to the imported types.
   */
  boolean removeJavadocOnlyImports() {
    return removeJavadocOnlyImports;
  }

  /** Sort imports. */
  boolean sortImports() {
    return sortImports;
  }

  /** Remove unused imports. */
  boolean removeUnusedImports() {
    return removeUnusedImports;
  }

  /** Returns true if partial formatting was selected. */
  boolean isSelection() {
    return !lines().isEmpty() || !offsets().isEmpty() || !lengths().isEmpty();
  }

  static Builder builder() {
    return new Builder();
  }

  static class Builder {

    private final ImmutableList.Builder<String> files = ImmutableList.builder();
    private final ImmutableRangeSet.Builder<Integer> lines = ImmutableRangeSet.builder();
    private final ImmutableList.Builder<Integer> offsets = ImmutableList.builder();
    private final ImmutableList.Builder<Integer> lengths = ImmutableList.builder();
    private Boolean inPlace = false;
    private Boolean aosp = false;
    private Boolean version = false;
    private Boolean help = false;
    private Boolean stdin = false;
    private Boolean fixImportsOnly = false;
    private Boolean removeJavadocOnlyImports = false;
    private Boolean sortImports = true;
    private Boolean removeUnusedImports = true;

    ImmutableList.Builder<String> filesBuilder() {
      return files;
    }

    Builder inPlace(boolean inPlace) {
      this.inPlace = inPlace;
      return this;
    }

    ImmutableRangeSet.Builder<Integer> linesBuilder() {
      return lines;
    }

    Builder addOffset(Integer offset) {
      offsets.add(offset);
      return this;
    }

    Builder addLength(Integer length) {
      lengths.add(length);
      return this;
    }

    Builder aosp(boolean aosp) {
      this.aosp = aosp;
      return this;
    }

    Builder version(boolean version) {
      this.version = version;
      return this;
    }

    Builder help(boolean help) {
      this.help = help;
      return this;
    }

    Builder stdin(boolean stdin) {
      this.stdin = stdin;
      return this;
    }

    Builder fixImportsOnly(boolean fixImportsOnly) {
      this.fixImportsOnly = fixImportsOnly;
      return this;
    }

    Builder removeJavadocOnlyImports(boolean removeJavadocOnlyImports) {
      this.removeJavadocOnlyImports = removeJavadocOnlyImports;
      return this;
    }

    Builder sortImports(boolean sortImports) {
      this.sortImports = sortImports;
      return this;
    }

    Builder removeUnusedImports(boolean removeUnusedImports) {
      this.removeUnusedImports = removeUnusedImports;
      return this;
    }

    CommandLineOptions build() {
      return new CommandLineOptions(
          this.files.build(),
          this.inPlace,
          this.lines.build(),
          this.offsets.build(),
          this.lengths.build(),
          this.aosp,
          this.version,
          this.help,
          this.stdin,
          this.fixImportsOnly,
          this.removeJavadocOnlyImports,
          this.sortImports,
          this.removeUnusedImports);
    }
  }
}
