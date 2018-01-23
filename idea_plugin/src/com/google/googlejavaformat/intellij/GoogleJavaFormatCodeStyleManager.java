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

import static java.util.Comparator.comparing;

import com.google.common.collect.ImmutableList;
import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.JavaFormatterOptions;
import com.google.googlejavaformat.java.JavaFormatterOptions.Style;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.impl.CheckUtil;
import com.intellij.util.IncorrectOperationException;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import org.jetbrains.annotations.NotNull;

/**
 * A {@link CodeStyleManager} implementation which formats .java files with google-java-format.
 * Formatting of all other types of files is delegated to IJ's default implementation.
 */
class GoogleJavaFormatCodeStyleManager extends CodeStyleManagerDecorator {

  public GoogleJavaFormatCodeStyleManager(@NotNull CodeStyleManager original) {
    super(original);
  }

  @Override
  public void reformatText(PsiFile file, int startOffset, int endOffset)
      throws IncorrectOperationException {
    if (overrideFormatterForFile(file)) {
      formatInternal(file, ImmutableList.of(new TextRange(startOffset, endOffset)));
    } else {
      super.reformatText(file, startOffset, endOffset);
    }
  }

  @Override
  public void reformatText(PsiFile file, Collection<TextRange> ranges)
      throws IncorrectOperationException {
    if (overrideFormatterForFile(file)) {
      formatInternal(file, ranges);
    } else {
      super.reformatText(file, ranges);
    }
  }

  @Override
  public void reformatTextWithContext(PsiFile file, Collection<TextRange> ranges) {
    if (overrideFormatterForFile(file)) {
      formatInternal(file, ranges);
    } else {
      super.reformatTextWithContext(file, ranges);
    }
  }

  @Override
  public PsiElement reformatRange(
      PsiElement element, int startOffset, int endOffset, boolean canChangeWhiteSpacesOnly) {
    // Only handle elements that are PsiFile for now -- otherwise we need to search for some
    // element within the file at new locations given the original startOffset and endOffsets
    // to serve as the return value.
    PsiFile file = element instanceof PsiFile ? (PsiFile) element : null;
    if (file != null && canChangeWhiteSpacesOnly && overrideFormatterForFile(file)) {
      formatInternal(file, ImmutableList.of(new TextRange(startOffset, endOffset)));
      return file;
    } else {
      return super.reformatRange(element, startOffset, endOffset, canChangeWhiteSpacesOnly);
    }
  }

  /** Return whether or not this formatter can handle formatting the given file. */
  private boolean overrideFormatterForFile(PsiFile file) {
    return StdFileTypes.JAVA.equals(file.getFileType())
        && GoogleJavaFormatSettings.getInstance(getProject()).isEnabled();
  }

  private void formatInternal(PsiFile file, Collection<TextRange> ranges) {
    ApplicationManager.getApplication().assertWriteAccessAllowed();
    PsiDocumentManager documentManager = PsiDocumentManager.getInstance(getProject());
    documentManager.commitAllDocuments();
    CheckUtil.checkWritable(file);

    Document document = documentManager.getDocument(file);

    if (document == null) {
      return;
    }
    // If there are postponed PSI changes (e.g., during a refactoring), just abort.
    // If we apply them now, then the incoming text ranges may no longer be valid.
    if (documentManager.isDocumentBlockedByPsi(document)) {
      return;
    }

    format(document, ranges);
  }

  /**
   * Format the ranges of the given document.
   *
   * <p>Overriding methods will need to modify the document with the result of the external
   * formatter (usually using {@link #performReplacements(Document, Map)}.
   */
  private void format(Document document, Collection<TextRange> ranges) {
    Style style = GoogleJavaFormatSettings.getInstance(getProject()).getStyle();
    Formatter formatter = new Formatter(JavaFormatterOptions.builder().style(style).build());
    performReplacements(
        document, FormatterUtil.getReplacements(formatter, document.getText(), ranges));
  }

  private void performReplacements(
      final Document document, final Map<TextRange, String> replacements) {

    if (replacements.isEmpty()) {
      return;
    }

    TreeMap<TextRange, String> sorted = new TreeMap<>(comparing(TextRange::getStartOffset));
    sorted.putAll(replacements);
    WriteCommandAction.runWriteCommandAction(
        getProject(),
        () -> {
          for (Entry<TextRange, String> entry : sorted.descendingMap().entrySet()) {
            document.replaceString(
                entry.getKey().getStartOffset(), entry.getKey().getEndOffset(), entry.getValue());
          }
          PsiDocumentManager.getInstance(getProject()).commitDocument(document);
        });
  }
}
