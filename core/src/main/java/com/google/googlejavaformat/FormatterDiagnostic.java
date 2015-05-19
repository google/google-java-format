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

/** An error that prevented formatting from succeeding. */
public class FormatterDiagnostic {
  private final String filename;
  private final int lineNumber;
  private final String message;

  public FormatterDiagnostic(String filename, int lineNumber, String message) {
    this.filename = filename;
    this.lineNumber = lineNumber;
    this.message = message;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (filename != null) {
      sb.append(filename).append(':');
    }
    if (lineNumber > 0) {
      sb.append(lineNumber).append(':');
    }
    if (sb.length() > 0) {
      sb.append(' ');
    }
    sb.append("error: ").append(message);
    return sb.toString();
  }
}
