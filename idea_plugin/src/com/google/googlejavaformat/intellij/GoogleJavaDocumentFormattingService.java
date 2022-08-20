/*
 * Copyright 2022 Google Inc. All Rights Reserved.
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

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.JavaFormatterOptions;
import com.intellij.formatting.FormattingContext;
import com.intellij.formatting.service.AbstractDocumentFormattingService;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.jetbrains.annotations.NotNull;

class GoogleJavaDocumentFormattingService extends AbstractDocumentFormattingService {

  @Override
  public @NotNull Set<Feature> getFeatures() {
    return Set.of(Feature.AD_HOC_FORMATTING, Feature.FORMAT_FRAGMENTS);
  }

  @Override
  public boolean canFormat(@NotNull PsiFile file) {
    return JavaFileType.INSTANCE.equals(file.getFileType())
        && GoogleJavaFormatSettings.getInstance(file.getProject()).isEnabled();
  }

  @Override
  public void formatDocument(
      @NotNull Document document,
      @NotNull List<TextRange> formattingRanges,
      @NotNull FormattingContext formattingContext,
      boolean canChangeWhiteSpaceOnly,
      boolean quickFormat) {

    Project project = formattingContext.getProject();
    JavaFormatterOptions.Style style = GoogleJavaFormatSettings.getInstance(project).getStyle();
    Formatter formatter = new Formatter(JavaFormatterOptions.builder().style(style).build());
    performReplacements(
        project,
        document,
        FormatterUtil.getReplacements(formatter, document.getText(), formattingRanges));
  }

  private void performReplacements(
      final Project project, final Document document, final Map<TextRange, String> replacements) {

    if (replacements.isEmpty()) {
      return;
    }

    TreeMap<TextRange, String> sorted = new TreeMap<>(comparing(TextRange::getStartOffset));
    sorted.putAll(replacements);
    WriteCommandAction.runWriteCommandAction(
        project,
        () -> {
          for (Map.Entry<TextRange, String> entry : sorted.descendingMap().entrySet()) {
            document.replaceString(
                entry.getKey().getStartOffset(), entry.getKey().getEndOffset(), entry.getValue());
          }
          PsiDocumentManager.getInstance(project).commitDocument(document);
        });
  }
}
