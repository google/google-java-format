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

import com.intellij.formatting.FormattingRangesInfo;
import com.intellij.formatting.service.AbstractDocumentFormattingService;
import com.intellij.formatting.service.FormattingService;
import com.intellij.lang.ImportOptimizer;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.codeStyle.CoreCodeStyleUtil;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/** A {@link FormattingService} implementation which formats .java files with google-java-format. */
class GoogleJavaFormattingService implements FormattingService {

  private final AbstractDocumentFormattingService documentFormattingService =
      new GoogleJavaDocumentFormattingService();

  @Override
  public @NotNull Set<Feature> getFeatures() {
    return documentFormattingService.getFeatures();
  }

  @Override
  public boolean canFormat(@NotNull PsiFile file) {
    return documentFormattingService.canFormat(file);
  }

  @Override
  public @NotNull PsiElement formatElement(
      @NotNull PsiElement element, boolean canChangeWhiteSpaceOnly) {
    PsiElement formatted =
        documentFormattingService.formatElement(element, canChangeWhiteSpaceOnly);
    if (canChangeWhiteSpaceOnly) {
      return formatted;
    }
    return CoreCodeStyleUtil.postProcessElement(element.getContainingFile(), formatted);
  }

  @Override
  public @NotNull PsiElement formatElement(
      @NotNull PsiElement element, @NotNull TextRange range, boolean canChangeWhiteSpaceOnly) {
    PsiElement formatted =
        documentFormattingService.formatElement(element, range, canChangeWhiteSpaceOnly);
    if (canChangeWhiteSpaceOnly) {
      return formatted;
    }
    return CoreCodeStyleUtil.postProcessElement(element.getContainingFile(), formatted);
  }

  @Override
  public void formatRanges(
      @NotNull PsiFile file,
      FormattingRangesInfo rangesInfo,
      boolean canChangeWhiteSpaceOnly,
      boolean quickFormat) {
    documentFormattingService.formatRanges(file, rangesInfo, canChangeWhiteSpaceOnly, quickFormat);
    if (canChangeWhiteSpaceOnly) {
      return;
    }
    List<CoreCodeStyleUtil.RangeFormatInfo> infos =
        CoreCodeStyleUtil.getRangeFormatInfoList(file, rangesInfo);
    CoreCodeStyleUtil.postProcessRanges(
        file, infos, range -> CoreCodeStyleUtil.postProcessText(file, range));
  }

  @Override
  public @NotNull Set<ImportOptimizer> getImportOptimizers(@NotNull PsiFile file) {
    return documentFormattingService.getImportOptimizers(file);
  }
}
