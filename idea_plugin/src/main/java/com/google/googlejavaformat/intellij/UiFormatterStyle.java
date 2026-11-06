/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import com.google.googlejavaformat.java.JavaFormatterOptions;
import com.google.googlejavaformat.java.JavaFormatterOptions.Style;
import java.util.Arrays;
import java.util.Objects;

/** Configuration options for the formatting style. */
enum UiFormatterStyle {
  GOOGLE("Default Google Java style", Style.GOOGLE),
  AOSP("Android Open Source Project (AOSP) style", Style.AOSP);

  private final String description;
  private final JavaFormatterOptions.Style style;

  UiFormatterStyle(String description, JavaFormatterOptions.Style style) {
    this.description = description;
    this.style = style;
  }

  @Override
  public String toString() {
    return description;
  }

  public JavaFormatterOptions.Style convert() {
    return style;
  }

  static UiFormatterStyle convert(JavaFormatterOptions.Style style) {
    return Arrays.stream(UiFormatterStyle.values())
        .filter(value -> Objects.equals(value.style, style))
        .findFirst()
        .get();
  }
}
