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

import com.google.common.collect.RangeSet;

import java.io.InputStream;
import java.util.List;

/**
 * A {@link FileToFormat} that comes from standard input.
 */
class FileToFormatStdin extends FileToFormat {
  /**
   * A fake filename to return when the file to format comes from stdin.
   */
  private static final String STDIN_FILENAME = "<stdin>";

  public FileToFormatStdin(
      RangeSet<Integer> lineRanges, List<Integer> offsetFlags, List<Integer> lengthFlags) {
    super(lineRanges, offsetFlags, lengthFlags);
  }

  @Override
  public String fileName() {
    return STDIN_FILENAME;
  }

  @Override
  public InputStream inputStream() {
    return System.in;
  }
}
