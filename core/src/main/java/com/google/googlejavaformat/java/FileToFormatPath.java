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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * A {@link FileToFormat} that comes from a {@link Path}.
 */
class FileToFormatPath extends FileToFormat {

  private final Path path;

  public FileToFormatPath(
      Path path,
      RangeSet<Integer> lineRanges,
      List<Integer> offsetFlags,
      List<Integer> lengthFlags) {
    super(lineRanges, offsetFlags, lengthFlags);
    this.path = path;
  }

  @Override
  public String fileName() {
    return path.toString();
  }

  @Override
  public InputStream inputStream() throws IOException {
    return Files.newInputStream(path);
  }
}
