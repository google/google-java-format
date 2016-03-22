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

/**
 * Options for a google-java-format invocation.
 *
 * <p>
 * Like gofmt, the google-java-format CLI exposes <em>no</em> configuration
 * options (aside from {@code --aosp}).
 *
 * <p>
 * The goal of google-java-format is to provide consistent formatting, and to
 * free developers from arguments over style choices. It is an explicit non-goal
 * to support developers' individual preferences, and in fact it would work
 * directly against our primary goals.
 *
 * <p>
 * This class exists primarily to distinguish between Google Java Style mode and
 * {@code --aosp} mode. It also provides a configuration that enables the
 * Eclipse Javadoc formatter until
 * <a href="https://github.com/google/google-java-format/issues/7">google-java-
 * format#7</a> is resolved.
 */
@Immutable
public class JavaFormatterOptions {

  static final int DEFAULT_MAX_LINE_LENGTH = 100;

  public enum SortImports {
    NO,
    ONLY,
    ALSO
  }

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

  public enum JavadocFormatter {
    NONE {
      @Override
      public String format(JavaFormatterOptions options, String text, int column0) {
        return text;
      }
    },
    ECLIPSE {
      @Override
      public String format(JavaFormatterOptions options, String text, int column0) {
        return EclipseJavadocFormatter.formatJavadoc(text, column0, options);
      }
    };

    public abstract String format(JavaFormatterOptions options, String text, int column0);
  }

  private final JavadocFormatter javadocFormatter;
  private final Style style;
  private final SortImports sortImports;

  public JavaFormatterOptions(
      JavadocFormatter javadocFormatter, Style style, SortImports sortImports) {
    this.javadocFormatter = javadocFormatter;
    this.style = style;
    this.sortImports = sortImports;
  }

  /** Returns the Javadoc formatter. */
  public JavadocFormatter javadocFormatter() {
    return javadocFormatter;
  }

  /** Returns the maximum formatted width */
  public int maxLineLength() {
    return DEFAULT_MAX_LINE_LENGTH;
  }

  /** Returns the multiplier for the unit of indent */
  public int indentationMultiplier() {
    return style.indentationMultiplier();
  }

  public SortImports sortImports() {
    return sortImports;
  }
}
