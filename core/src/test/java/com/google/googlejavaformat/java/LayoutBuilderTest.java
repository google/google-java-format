/*
 * Copyright 2026 Google Inc.
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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.googlejavaformat.Doc.Token.RealOrImaginary;
import com.google.googlejavaformat.FormattingError;
import com.google.googlejavaformat.Indent;
import com.google.googlejavaformat.layout.LayoutBuilder;
import com.sun.tools.javac.util.Context;
import java.util.ArrayList;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/// Tests for [LayoutBuilder].
///
/// These live in the `java` package so they can use [JavaInput], which is package-private.
@RunWith(JUnit4.class)
public class LayoutBuilderTest {

  @Test
  public void sync_afterToken() throws Exception {
    LayoutBuilder builder = newBuilder("class T {}\n");
    builder.sync(0);
    builder.token("class", RealOrImaginary.REAL, Indent.Const.ZERO, Optional.empty());
    builder.sync(6);
  }

  @Test
  public void sync_tokenNotGenerated() throws Exception {
    LayoutBuilder builder = newBuilder("class T {}\n");
    builder.sync(0);
    FormattingError e = assertThrows(FormattingError.class, () -> builder.sync(6));
    assertThat(e).hasMessageThat().isEqualTo("1:7: error: did not generate token \"class\"\n");
  }

  private static LayoutBuilder newBuilder(String text) throws FormatterException {
    JavaInput input = new JavaInput(text);
    // Diagnostics use the compilation unit's line map.
    input.setCompilationUnit(
        Trees.parse(new Context(), new ArrayList<>(), /* allowStringFolding= */ false, text));
    // sync doesn't produce output, so no Output is needed.
    return new LayoutBuilder(input, /* output= */ null);
  }
}
