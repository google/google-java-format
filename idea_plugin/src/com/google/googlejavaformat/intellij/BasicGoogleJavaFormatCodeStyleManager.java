/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.googlejavaformat.intellij;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.JavaFormatterOptions;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

final class BasicGoogleJavaFormatCodeStyleManager extends GoogleJavaFormatCodeStyleManager {

  private final Formatter formatter;

  public BasicGoogleJavaFormatCodeStyleManager(
      @NotNull CodeStyleManager original, JavaFormatterOptions.Style style) {
    super(original);
    this.formatter = new Formatter(JavaFormatterOptions.builder().style(style).build());
  }

  @Override
  protected Optional<Formatter> getFormatterForFile(@NotNull PsiFile file) {
    return Optional.of(formatter);
  }
}
