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

import com.google.common.base.CharMatcher;
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
    LineSeparator separator = LineSeparator.detect(input);
    boolean conversionNeeded = separator != LineSeparator.UNIX;
    String in = conversionNeeded ? LineSeparator.UNIX.convert(input, separator) : input;
    if (!parameters.fixImportsOnly()) {
      in = new Formatter(options).formatSource(in, characterRanges(in).asRanges());
    }
    String fixed = fixImports(in);
    return conversionNeeded ? separator.convert(fixed) : fixed;
  }

  private String fixImports(String input) throws FormatterException {
    input =
        RemoveUnusedImports.removeUnusedImports(
            input,
            parameters.removeJavadocOnlyImports()
                ? JavadocOnlyImports.REMOVE
                : JavadocOnlyImports.KEEP);
    input = ImportOrderer.reorderImports(input);
    return input;
  }

  static final CharMatcher CARRIAGE_RETURN = CharMatcher.is('\r');

  private RangeSet<Integer> characterRanges(String input) {
    final RangeSet<Integer> characterRanges = TreeRangeSet.create();

    if (parameters.lines().isEmpty() && parameters.offsets().isEmpty()) {
      characterRanges.add(Range.closedOpen(0, input.length()));
      return characterRanges;
    }

    characterRanges.addAll(Formatter.lineRangesToCharRanges(input, parameters.lines()));

    for (int i = 0; i < parameters.offsets().size(); i++) {
      Integer length = parameters.lengths().get(i);
      if (length == 0) {
        // 0 stands for "format the line under the cursor"
        length = 1;
      }
      // offset and length values are based on the original input
      int offset = parameters.offsets().get(i);
      // TODO Remove following offset and length correction, after CRLF/LF/CR treated the same.
      // See https://github.com/google/google-java-format/pull/79#issuecomment-252153080
      if (this.input.length() != input.length()) {
        assert LineSeparator.detect(this.input) == LineSeparator.WINDOWS;
        assert LineSeparator.detect(input) == LineSeparator.UNIX;
        length -= CARRIAGE_RETURN.countIn(this.input.substring(offset, offset + length));
        offset -= CARRIAGE_RETURN.countIn(this.input.substring(0, offset));
      }
      characterRanges.add(Range.closedOpen(offset, offset + length));
    }

    return characterRanges;
  }
}
