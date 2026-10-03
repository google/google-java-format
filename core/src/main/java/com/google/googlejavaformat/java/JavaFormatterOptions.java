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

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.google.auto.value.AutoBuilder;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.Immutable;
import com.google.googlejavaformat.Doc;

/**
 * Options for a google-java-format invocation.
 *
 * <p>Like gofmt, the google-java-format CLI exposes <em>no</em> configuration options (aside from
 * {@code --aosp}).
 *
 * <p>The goal of google-java-format is to provide consistent formatting, and to free developers
 * from arguments over style choices. It is an explicit non-goal to support developers' individual
 * preferences, and in fact it would work directly against our primary goals.
 *
 * @param style Returns the code style.
 */
@Immutable
public record JavaFormatterOptions(boolean formatJavadoc, boolean reorderModifiers, Style style) {
  public JavaFormatterOptions {
    requireNonNull(style, "style");
  }

  /** Code style configuration for layout and imports. */
  @Immutable
  public record Style(
      int indentationMultiplier, int maxLineLength, boolean useTabs, ImportOrder importOrder) {
    public Style {
      checkArgument(
          maxLineLength > 0 && maxLineLength <= MAX_LINE_LENGTH_LIMIT,
          "maxLineLength must be between 1 and %s, was: %s",
          MAX_LINE_LENGTH_LIMIT,
          maxLineLength);
      requireNonNull(importOrder, "importOrder");
    }

    /**
     * The largest supported {@link #maxLineLength}. Layout widths saturate at {@link
     * Doc#MAX_LINE_WIDTH}, which is also how forced breaks are represented, so the line length must
     * be smaller than that.
     */
    static final int MAX_LINE_LENGTH_LIMIT = Doc.MAX_LINE_WIDTH - 1;

    /** The default Google Java Style configuration. */
    public static final Style GOOGLE = builder().google().build();

    /** The AOSP-compliant configuration. */
    public static final Style AOSP = builder().aosp().build();

    /**
     * Returns the predefined styles, in the order they were declared when {@code Style} was an
     * enum.
     *
     * <p>Retained for compatibility with callers compiled against the enum version of {@code
     * Style}.
     */
    public static Style[] values() {
      return new Style[] {GOOGLE, AOSP};
    }

    /**
     * Returns the predefined style with the given name ({@code "GOOGLE"} or {@code "AOSP"}).
     *
     * <p>Retained for compatibility with callers compiled against the enum version of {@code
     * Style}.
     *
     * @throws IllegalArgumentException if there is no predefined style with the given name
     * @throws NullPointerException if {@code name} is null
     */
    public static Style valueOf(String name) {
      return switch (requireNonNull(name, "Name is null")) {
        case "GOOGLE" -> GOOGLE;
        case "AOSP" -> AOSP;
        default ->
            throw new IllegalArgumentException(
                "No enum constant " + Style.class.getCanonicalName() + "." + name);
      };
    }

    /**
     * Returns the name of the predefined style this style is based on ({@code "GOOGLE"} or {@code
     * "AOSP"}), such that {@code valueOf(GOOGLE.name()) == GOOGLE}.
     *
     * <p>Retained for compatibility with callers compiled against the enum version of {@code
     * Style}.
     */
    public String name() {
      return importOrder().name();
    }

    /**
     * Returns the visual column width of a tab stop.
     *
     * <p>This matches the standard block indentation width for the style: 2 columns for Google
     * Style and 4 columns for AOSP.
     */
    public int tabWidth() {
      return 2 * indentationMultiplier();
    }

    /** Returns the indentation string for the given visual column width. */
    public String indentString(int indent) {
      if (!useTabs()) {
        return JavaOutput.spaces(indent);
      }
      int tabWidth = tabWidth();
      return "\t".repeat(indent / tabWidth) + " ".repeat(indent % tabWidth);
    }

    /** Returns the visual column width of the given character sequence. */
    public int visualLength(CharSequence input) {
      return visualLength(input, 0, input.length());
    }

    /** Returns the visual column width of the given subsequence. */
    public int visualLength(CharSequence input, int start, int end) {
      if (!useTabs()) {
        return end - start;
      }
      int tabWidth = tabWidth();
      int column = 0;
      for (int i = start; i < end; i++) {
        if (input.charAt(i) == '\t') {
          column += tabWidth - (column % tabWidth);
        } else {
          column++;
        }
      }
      return column;
    }

    public boolean isAosp() {
      return importOrder() == ImportOrder.AOSP;
    }

    public static Builder builder() {
      return new AutoBuilder_JavaFormatterOptions_Style_Builder()
          .maxLineLength(100)
          .useTabs(false)
          .google();
    }

    public Builder toBuilder() {
      return new AutoBuilder_JavaFormatterOptions_Style_Builder()
          .indentationMultiplier(indentationMultiplier())
          .maxLineLength(maxLineLength())
          .useTabs(useTabs())
          .importOrder(importOrder());
    }

    /** A builder for {@link Style}. */
    @AutoBuilder
    public abstract static class Builder {
      public abstract Builder indentationMultiplier(int indentationMultiplier);

      public abstract Builder maxLineLength(int maxLineLength);

      public abstract Builder useTabs(boolean useTabs);

      public abstract Builder importOrder(ImportOrder importOrder);

      @CanIgnoreReturnValue
      public Builder aosp() {
        return indentationMultiplier(2).importOrder(ImportOrder.AOSP);
      }

      @CanIgnoreReturnValue
      public Builder google() {
        return indentationMultiplier(1).importOrder(ImportOrder.GOOGLE);
      }

      public abstract Style build();
    }
  }

  /** The import order to use. */
  public enum ImportOrder {
    GOOGLE,
    AOSP,
  }

  /** Returns the multiplier for the unit of indent. */
  public int indentationMultiplier() {
    return style().indentationMultiplier();
  }

  public int maxLineLength() {
    return style().maxLineLength();
  }

  public boolean useTabs() {
    return style().useTabs();
  }

  /** Returns the indentation string for the given visual column width. */
  public String indentString(int indent) {
    return style().indentString(indent);
  }

  /** Returns the visual column width of the given character sequence. */
  public int visualLength(CharSequence input) {
    return style().visualLength(input);
  }

  /** Returns the visual column width of the given subsequence. */
  public int visualLength(CharSequence input, int start, int end) {
    return style().visualLength(input, start, end);
  }

  /** Returns the default formatting options. */
  public static JavaFormatterOptions defaultOptions() {
    return builder().build();
  }

  /** Returns a builder for {@link JavaFormatterOptions}. */
  public static Builder builder() {
    return new AutoBuilder_JavaFormatterOptions_Builder()
        .style(Style.GOOGLE)
        .formatJavadoc(true)
        .reorderModifiers(true);
  }

  /** A builder for {@link JavaFormatterOptions}. */
  @AutoBuilder
  public abstract static class Builder {

    public abstract Builder style(Style style);

    public abstract Builder formatJavadoc(boolean formatJavadoc);

    public abstract Builder reorderModifiers(boolean reorderModifiers);

    public abstract JavaFormatterOptions build();
  }
}
