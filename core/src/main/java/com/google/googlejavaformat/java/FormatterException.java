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

import static java.util.Locale.ENGLISH;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.googlejavaformat.FormatterDiagnostic;
import java.util.List;
import org.openjdk.javax.tools.Diagnostic;
import org.openjdk.javax.tools.JavaFileObject;

/** Checked exception class for formatter errors. */
public final class FormatterException extends Exception {

  private ImmutableList<FormatterDiagnostic> diagnostics;

  public FormatterException(String message) {
    this(FormatterDiagnostic.create(message));
  }

  public FormatterException(FormatterDiagnostic diagnostic) {
    this(ImmutableList.of(diagnostic));
  }

  public FormatterException(Iterable<FormatterDiagnostic> diagnostics) {
    super(diagnostics.iterator().next().toString());
    this.diagnostics = ImmutableList.copyOf(diagnostics);
  }

  public List<FormatterDiagnostic> diagnostics() {
    return diagnostics;
  }

  public static FormatterException fromJavacDiagnostics(
      Iterable<Diagnostic<? extends JavaFileObject>> diagnostics) {
    return new FormatterException(Iterables.transform(diagnostics, d -> toFormatterDiagnostic(d)));
  }

  private static FormatterDiagnostic toFormatterDiagnostic(Diagnostic<?> input) {
    return FormatterDiagnostic.create(
        (int) input.getLineNumber(), (int) input.getColumnNumber(), input.getMessage(ENGLISH));
  }
}
