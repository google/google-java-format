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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatter;
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatterOptions;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;

/** Format javadoc comments using eclipse's formatter, for now. */
public class EclipseJavadocFormatter {

  /** An ill-formed Unicode 16-bit string of length 3. */
  private static final String PLACEHOLDER =
      Strings.repeat(String.valueOf(Character.MIN_HIGH_SURROGATE), 3);

  private static final String P_TAG = "<p>";

  static String formatJavadoc(String input, int indent, JavaFormatterOptions options) {
    // Eclipse lays out `<p>` tags on separate lines, which we do not want. To hack around this,
    // replace all occurrences of `<p>` in the input with `<p>???`, which will be formatted
    // as:
    //
    // ```
    // paragraph1
    // <p>
    // ???paragraph2
    // ```
    //
    // Finally, delete all `<p>` tags and rewrite `???` back to `<p>`:
    //
    // ```
    // paragraph1
    //
    // <p>paragraph2
    // ```
    //
    // A run of three unpaired surrogate code units is used as the placeholder because it
    // is unlikely to occur naturally in javadoc.

    String preprocessed = input.replace(P_TAG, P_TAG + PLACEHOLDER);
    String output = formatJavadocInternal(preprocessed, indent, options);
    return output.replace(P_TAG, "").replace(PLACEHOLDER, P_TAG);
  }

  private static String formatJavadocInternal(
      String input, int indent, JavaFormatterOptions options) {

    ImmutableMap.Builder<String, String> optionBuilder = ImmutableMap.<String, String>builder();
    optionBuilder.put(
        DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_JAVADOC_COMMENT, "true");
    optionBuilder.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
    optionBuilder.put(
        DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE,
        Integer.toString(options.indentationMultiplier()));
    optionBuilder.put(
        DefaultCodeFormatterConstants.FORMATTER_COMMENT_LINE_LENGTH,
        Integer.toString(options.maxLineLength() - indent));
    optionBuilder.put(
        DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT,
        Integer.toString(options.maxLineLength()));
    optionBuilder.put(
        DefaultCodeFormatterConstants.FORMATTER_COMMENT_INDENT_PARAMETER_DESCRIPTION,
        DefaultCodeFormatterConstants.FALSE);
    optionBuilder.put(
        DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_NEW_LINE_FOR_PARAMETER,
        JavaCore.DO_NOT_INSERT);
    optionBuilder.put(
        DefaultCodeFormatterConstants.FORMATTER_COMMENT_CLEAR_BLANK_LINES_IN_JAVADOC_COMMENT,
        DefaultCodeFormatterConstants.FALSE);
    optionBuilder.put(
        DefaultCodeFormatterConstants.FORMATTER_JOIN_LINES_IN_COMMENTS,
        DefaultCodeFormatterConstants.TRUE);
    optionBuilder.put(
        DefaultCodeFormatterConstants.FORMATTER_JOIN_WRAPPED_LINES,
        DefaultCodeFormatterConstants.TRUE);
    // Disable indenting root tags for now since it indents more than 4 spaces
    optionBuilder.put(
        DefaultCodeFormatterConstants.FORMATTER_COMMENT_INDENT_ROOT_TAGS,
        DefaultCodeFormatterConstants.FALSE);
    optionBuilder.put(
        DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_SOURCE,
        DefaultCodeFormatterConstants.FALSE);
    optionBuilder.put(JavaCore.COMPILER_COMPLIANCE, "1.8");
    optionBuilder.put(JavaCore.COMPILER_SOURCE, "1.8");
    optionBuilder.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, "1.8");
    DefaultCodeFormatter codeFormatter =
        new DefaultCodeFormatter(new DefaultCodeFormatterOptions(optionBuilder.build()));

    TextEdit edit =
        codeFormatter.format(
            CodeFormatter.K_JAVA_DOC,
            input,
            /*offset*/ 0,
            input.length(),
            // eclipse doesn't indent comments reliably, so always request no indent and fix it
            // up later in JavaCommentsHelper
            /*indent*/ 0,
            /*lineSeparator*/ null);
    if (edit == null) {
      throw new RuntimeException("error formatting javadoc");
    }
    Document document = new Document(input);
    try {
      edit.apply(document);
    } catch (BadLocationException e) {
      throw new RuntimeException("error formatting javadoc", e);
    }
    return document.get();
  }
}
