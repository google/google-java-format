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

import static java.util.Locale.ENGLISH;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

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

  public static FormattingError fromJavacDiagnostics(
      Iterable<Diagnostic<? extends JavaFileObject>> diagnostics) {
    return new FormattingError(Iterables.transform(diagnostics, TO_FORMATTER_DIAGNOSTIC));
  }

  private static final Function<Diagnostic<?>, FormatterDiagnostic> TO_FORMATTER_DIAGNOSTIC =
      new Function<Diagnostic<?>, FormatterDiagnostic>() {
        @Override
        public FormatterDiagnostic apply(Diagnostic<?> input) {
          return FormatterDiagnostic.create(
              (int) input.getLineNumber(),
              (int) input.getColumnNumber(),
              input.getMessage(ENGLISH));
        }
      };
}
