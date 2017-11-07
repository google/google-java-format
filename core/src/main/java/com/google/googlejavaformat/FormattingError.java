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

package com.google.googlejavaformat;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

/** An unchecked formatting error. */
public class FormattingError extends Error {

  private final ImmutableList<FormatterDiagnostic> diagnostics;

  public FormattingError(FormatterDiagnostic diagnostic) {
    this(ImmutableList.of(diagnostic));
  }

  public FormattingError(Iterable<FormatterDiagnostic> diagnostics) {
    super(Joiner.on("\n").join(diagnostics) + "\n");
    this.diagnostics = ImmutableList.copyOf(diagnostics);
  }

  public ImmutableList<FormatterDiagnostic> diagnostics() {
    return diagnostics;
  }
}
