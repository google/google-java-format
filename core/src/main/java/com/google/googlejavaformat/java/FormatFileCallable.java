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

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.google.googlejavaformat.java.RemoveUnusedImports.JavadocOnlyImports;

import java.util.concurrent.Callable;

/**
 * Encapsulates information about a file to be formatted, including which parts of the file to
 * format.
 */
public class FormatFileCallable implements Callable<String> {
  private final String input;
  private final CommandLineOptions parameters;
  private final JavaFormatterOptions options;

  public FormatFileCallable(
      CommandLineOptions parameters, String input, JavaFormatterOptions options) {
    this.input = input;
    this.parameters = parameters;
    this.options = options;
  }

  @Override
  public String call() throws FormatterException {
    String inputString = input;
    inputString =
        RemoveUnusedImports.removeUnusedImports(
            inputString,
            parameters.removeJavadocOnlyImports()
                ? JavadocOnlyImports.REMOVE
                : JavadocOnlyImports.KEEP);
    inputString = ImportOrderer.reorderImports(inputString);
    if (parameters.fixImportsOnly()) {
      return inputString;
    }

    return new Formatter(options)
        .formatSource(inputString, characterRanges(inputString).asRanges());
  }

  private RangeSet<Integer> characterRanges(String input) {
    final RangeSet<Integer> characterRanges = TreeRangeSet.create();

    if (parameters.lines().isEmpty() && parameters.offsets().isEmpty()) {
      characterRanges.add(Range.closedOpen(0, input.length()));
      return characterRanges;
    }

    characterRanges.addAll(Formatter.lineRangesToCharRanges(input, parameters.lines()));

    for (int i = 0; i < parameters.offsets().size(); i++) {
      characterRanges.add(
          Range.closedOpen(
              parameters.offsets().get(i),
              parameters.offsets().get(i) + parameters.lengths().get(i)));
    }

    return characterRanges;
  }
}
