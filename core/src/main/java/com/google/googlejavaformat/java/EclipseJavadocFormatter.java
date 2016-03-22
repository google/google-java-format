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

  static String formatJavadoc(String input, int indent, JavaFormatterOptions options) {

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
