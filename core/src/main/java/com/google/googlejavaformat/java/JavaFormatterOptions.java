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

package com.google.googlejavaformat.java;

import com.google.errorprone.annotations.Immutable;
import com.google.googlejavaformat.java.javadoc.JavadocOptions;

/**
 * Options for a google-java-format invocation.
 *
 * <p>Like gofmt, the google-java-format CLI exposes <em>no</em> configuration options (aside from
 * {@code --aosp}).
 *
 * <p>The goal of google-java-format is to provide consistent formatting, and to free developers
 * from arguments over style choices. It is an explicit non-goal to support developers' individual
 * preferences, and in fact it would work directly against our primary goals.
 */
@Immutable
public class JavaFormatterOptions implements JavadocOptions {

  static final int DEFAULT_MAX_LINE_LENGTH = 100;

  public enum Style {

    /** The default Google Java Style configuration. */
    GOOGLE(1),

    /** The AOSP-compliant configuration. */
    AOSP(2);

    private final int indentationMultiplier;

    Style(int indentationMultiplier) {
      this.indentationMultiplier = indentationMultiplier;
    }

    int indentationMultiplier() {
      return indentationMultiplier;
    }
  }

  private final Style style;

  private JavaFormatterOptions(Style style) {
    this.style = style;
  }

  /** Returns the maximum formatted width */
  @Override
  public int maxLineLength() {
    return DEFAULT_MAX_LINE_LENGTH;
  }

  /** Returns the multiplier for the unit of indent */
  public int indentationMultiplier() {
    return style.indentationMultiplier();
  }

  /** Returns the default formatting options. */
  public static JavaFormatterOptions defaultOptions() {
    return builder().build();
  }

  /** Returns a builder for {@link JavaFormatterOptions}. */
  public static Builder builder() {
    return new Builder();
  }

  /** A builder for {@link JavaFormatterOptions}. */
  public static class Builder {
    private Style style = Style.GOOGLE;

    private Builder() {}

    public Builder style(Style style) {
      this.style = style;
      return this;
    }

    public JavaFormatterOptions build() {
      return new JavaFormatterOptions(style);
    }
  }
}
