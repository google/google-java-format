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

/** An error that prevented formatting from succeeding. */
public class FormatterDiagnostic {
  private final int lineNumber;
  private final String message;
  private final int column;

  public static FormatterDiagnostic create(String message) {
    return new FormatterDiagnostic(-1, -1, message);
  }

  public static FormatterDiagnostic create(int lineNumber, int column, String message) {
    checkArgument(lineNumber >= 0);
    checkArgument(column >= 0);
    checkNotNull(message);
    return new FormatterDiagnostic(lineNumber, column, message);
  }

  private FormatterDiagnostic(int lineNumber, int column, String message) {
    this.lineNumber = lineNumber;
    this.column = column;
    this.message = message;
  }

  /**
   * Returns the line number on which the error occurred, or {@code -1} if the error does not have a
   * line number.
   */
  public int line() {
    return lineNumber;
  }

  /**
   * Returns the 0-indexed column number on which the error occurred, or {@code -1} if the error
   * does not have a column.
   */
  public int column() {
    return column;
  }

  /** Returns a description of the problem that prevented formatting from succeeding. */
  public String message() {
    return message;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (lineNumber >= 0) {
      sb.append(lineNumber).append(':');
    }
    if (column >= 0) {
      // internal column numbers are 0-based, but diagnostics use 1-based indexing by convention
      sb.append(column + 1).append(':');
    }
    if (lineNumber >= 0 || column >= 0) {
      sb.append(' ');
    }
    sb.append("error: ").append(message);
    return sb.toString();
  }
}
