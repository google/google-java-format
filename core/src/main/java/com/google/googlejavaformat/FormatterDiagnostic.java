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

package com.google.googlejavaformat;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An error that prevented formatting from succeeding.
 *
 * @param line the line number on which the error occurred, or {@code -1} if the error does not have
 *     a line number.
 * @param column the 1-indexed column number on which the error occurred, or {@code -1} if the error
 *     does not have a column.
 * @param message a description of the problem that prevented formatting from succeeding.
 */
public record FormatterDiagnostic(int line, int column, String message) {
  public FormatterDiagnostic {
    checkArgument(line >= -1);
    checkArgument(column >= -1);
    checkNotNull(message);
  }

  public FormatterDiagnostic(String message) {
    this(-1, -1, message);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (line >= 0) {
      sb.append(line).append(':');
    }
    if (column >= 0) {
      sb.append(column).append(':');
    }
    if (line >= 0 || column >= 0) {
      sb.append(' ');
    }
    sb.append("error: ").append(message);
    return sb.toString();
  }
}
