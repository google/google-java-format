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
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.google.googlejavaformat.FormatterDiagnostic;
import com.google.googlejavaformat.java.JavaFormatterOptions.SortImports;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Encapsulates information about a file to be formatted, including which parts of the file to
 * format.
 */
public class FormatFileCallable implements Callable<String> {
  private final String fileName;
  private final String input;
  private final ImmutableRangeSet<Integer> lineRanges;
  private final ImmutableList<Integer> offsets;
  private final ImmutableList<Integer> lengths;
  private final JavaFormatterOptions options;

  public FormatFileCallable(
      String fileName,
      RangeSet<Integer> lineRanges,
      List<Integer> offsets,
      List<Integer> lengths,
      String input,
      JavaFormatterOptions options) {
    this.fileName = fileName;
    this.input = input;
    this.lineRanges = ImmutableRangeSet.copyOf(lineRanges);
    this.offsets = ImmutableList.copyOf(offsets);
    this.lengths = ImmutableList.copyOf(lengths);
    this.options = options;
  }

  @Override
  public String call() throws FormatterException {
    String inputString = input;
    if (options.sortImports() != SortImports.NO) {
      inputString = ImportOrderer.reorderImports(fileName, inputString);
      if (options.sortImports() == SortImports.ONLY) {
        return inputString;
      }
    }

    inputString = reorderModifiers(inputString);

    JavaInput javaInput = new JavaInput(fileName, inputString);
    final RangeSet<Integer> tokens = tokenRanges(javaInput);

    final JavaOutput javaOutput = new JavaOutput(javaInput, new JavaCommentsHelper(options));
    List<FormatterDiagnostic> errors = new ArrayList<>();
    Formatter.format(javaInput, javaOutput, options, errors);
    if (!errors.isEmpty()) {
      throw new FormatterException(errors);
    }
    return javaOutput.writeMerged(tokens);
  }

  private String reorderModifiers(String inputString) throws FormatterException {
    JavaInput javaInput = new JavaInput(fileName, inputString);
    return ModifierOrderer.reorderModifiers(javaInput, tokenRanges(javaInput));
  }

  private RangeSet<Integer> tokenRanges(JavaInput javaInput) throws FormatterException {
    final RangeSet<Integer> tokens = TreeRangeSet.create();
    for (Range<Integer> lineRange : lineRanges.asRanges()) {
      tokens.add(javaInput.lineRangeToTokenRange(lineRange));
    }
    for (int i = 0; i < offsets.size(); i++) {
      tokens.add(javaInput.characterRangeToTokenRange(offsets.get(i), lengths.get(i)));
    }
    if (tokens.isEmpty()) {
      if (lineRanges.asRanges().isEmpty() && offsets.isEmpty()) {
        tokens.add(Range.<Integer>all());
      }
    }
    return tokens;
  }
}
