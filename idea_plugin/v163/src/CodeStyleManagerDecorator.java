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

import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.ChangedRangesInfo;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.Indent;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ThrowableRunnable;
import java.util.Collection;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

/**
 * Decorates the {@link CodeStyleManager} abstract class by delegating to a concrete implementation
 * instance (likely IJ's default instance).
 *
 * @author bcsf@google.com (Brian Chang)
 */
@SuppressWarnings("deprecation")
class CodeStyleManagerDecorator extends CodeStyleManager {
  @NotNull private final CodeStyleManager delegate;

  CodeStyleManagerDecorator(@NotNull CodeStyleManager delegate) {
    this.delegate = delegate;
  }

  @NotNull
  CodeStyleManager getDelegate() {
    return delegate;
  }

  @Override
  @NotNull
  public Project getProject() {
    return delegate.getProject();
  }

  @Override
  @NotNull
  public PsiElement reformat(@NotNull PsiElement element) throws IncorrectOperationException {
    return delegate.reformat(element);
  }

  @Override
  @NotNull
  public PsiElement reformat(@NotNull PsiElement element, boolean canChangeWhiteSpacesOnly)
      throws IncorrectOperationException {
    return delegate.reformat(element, canChangeWhiteSpacesOnly);
  }

  @Override
  public PsiElement reformatRange(@NotNull PsiElement element, int startOffset, int endOffset)
      throws IncorrectOperationException {
    return delegate.reformatRange(element, startOffset, endOffset);
  }

  @Override
  public PsiElement reformatRange(
      @NotNull PsiElement element, int startOffset, int endOffset, boolean canChangeWhiteSpacesOnly)
      throws IncorrectOperationException {
    return delegate.reformatRange(element, startOffset, endOffset, canChangeWhiteSpacesOnly);
  }

  @Override
  public void reformatText(@NotNull PsiFile file, int startOffset, int endOffset)
      throws IncorrectOperationException {
    delegate.reformatText(file, startOffset, endOffset);
  }

  @Override
  public void reformatText(@NotNull PsiFile file, @NotNull Collection<TextRange> ranges)
      throws IncorrectOperationException {
    delegate.reformatText(file, ranges);
  }

  @Override
  public void reformatTextWithContext(
      @NotNull PsiFile psiFile, @NotNull ChangedRangesInfo changedRangesInfo)
      throws IncorrectOperationException {
    delegate.reformatTextWithContext(psiFile, changedRangesInfo);
  }

  @Override
  public void adjustLineIndent(@NotNull PsiFile file, TextRange rangeToAdjust)
      throws IncorrectOperationException {
    delegate.adjustLineIndent(file, rangeToAdjust);
  }

  @Override
  public int adjustLineIndent(@NotNull PsiFile file, int offset)
      throws IncorrectOperationException {
    return delegate.adjustLineIndent(file, offset);
  }

  @Override
  public int adjustLineIndent(@NotNull Document document, int offset) {
    return delegate.adjustLineIndent(document, offset);
  }

  @Override
  public boolean isLineToBeIndented(@NotNull PsiFile file, int offset) {
    return delegate.isLineToBeIndented(file, offset);
  }

  @Override
  @Nullable
  public String getLineIndent(@NotNull PsiFile file, int offset) {
    return delegate.getLineIndent(file, offset);
  }

  @Override
  @Nullable
  public String getLineIndent(@NotNull Document document, int offset) {
    return delegate.getLineIndent(document, offset);
  }

  @Override
  public Indent getIndent(String text, FileType fileType) {
    return delegate.getIndent(text, fileType);
  }

  @Override
  public String fillIndent(Indent indent, FileType fileType) {
    return delegate.fillIndent(indent, fileType);
  }

  @Override
  public Indent zeroIndent() {
    return delegate.zeroIndent();
  }

  @Override
  public void reformatNewlyAddedElement(@NotNull ASTNode block, @NotNull ASTNode addedElement)
      throws IncorrectOperationException {
    delegate.reformatNewlyAddedElement(block, addedElement);
  }

  @Override
  public boolean isSequentialProcessingAllowed() {
    return delegate.isSequentialProcessingAllowed();
  }

  @Override
  public void performActionWithFormatterDisabled(Runnable r) {
    delegate.performActionWithFormatterDisabled(r);
  }

  @Override
  public <T extends Throwable> void performActionWithFormatterDisabled(ThrowableRunnable<T> r)
      throws T {
    delegate.performActionWithFormatterDisabled(r);
  }

  @Override
  public <T> T performActionWithFormatterDisabled(Computable<T> r) {
    return delegate.performActionWithFormatterDisabled(r);
  }
}
