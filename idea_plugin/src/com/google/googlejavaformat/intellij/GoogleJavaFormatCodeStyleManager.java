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
import com.google.common.collect.Range;
import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import com.google.googlejavaformat.java.Replacement;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.impl.CheckUtil;
import com.intellij.util.IncorrectOperationException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * A {@link CodeStyleManager} implementation which formats .java files with google-java-format.
 * Formatting of all other types of files is delegated to IJ's default implementation.
 *
 * @author bcsf@google.com (Brian Chang)
 */
public abstract class GoogleJavaFormatCodeStyleManager extends CodeStyleManagerDecorator {

  public GoogleJavaFormatCodeStyleManager(@NotNull CodeStyleManager original) {
    super(original);
  }

  @Override
  public void reformatText(@NotNull PsiFile file, int startOffset, int endOffset)
      throws IncorrectOperationException {
    Optional<Formatter> formatter = getFormatterForFile(file);
    if (formatter.isPresent() && StdFileTypes.JAVA.equals(file.getFileType())) {
      formatInternal(
          formatter.get(), file, ImmutableList.of(Range.closedOpen(startOffset, endOffset)));
    } else {
      super.reformatText(file, startOffset, endOffset);
    }
  }

  @Override
  public void reformatText(@NotNull PsiFile file, @NotNull Collection<TextRange> ranges)
      throws IncorrectOperationException {
    Optional<Formatter> formatter = getFormatterForFile(file);
    if (formatter.isPresent() && StdFileTypes.JAVA.equals(file.getFileType())) {
      formatInternal(formatter.get(), file, convertToRanges(ranges));
    } else {
      super.reformatText(file, ranges);
    }
  }

  @Override
  public void reformatTextWithContext(@NotNull PsiFile file, @NotNull Collection<TextRange> ranges)
      throws IncorrectOperationException {
    Optional<Formatter> formatter = getFormatterForFile(file);
    if (formatter.isPresent() && StdFileTypes.JAVA.equals(file.getFileType())) {
      formatInternal(formatter.get(), file, convertToRanges(ranges));
    } else {
      super.reformatTextWithContext(file, ranges);
    }
  }

  /**
   * Get the {@link Formatter} to be used with the given file, or absent to use the built-in
   * IntelliJ formatter.
   */
  protected abstract Optional<Formatter> getFormatterForFile(PsiFile file);

  private void formatInternal(Formatter formatter, PsiFile file, List<Range<Integer>> ranges)
      throws IncorrectOperationException {
    ApplicationManager.getApplication().assertWriteAccessAllowed();
    PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
    CheckUtil.checkWritable(file);

    Document document = PsiDocumentManager.getInstance(getProject()).getDocument(file);
    if (document != null) {
      try {
        List<Replacement> replacements =
            formatter
                .getFormatReplacements(document.getText(), ranges)
                .stream()
                .sorted(
                    comparing((Replacement r) -> r.getReplaceRange().lowerEndpoint()).reversed())
                .collect(Collectors.toList());
        performReplacements(document, replacements);
      } catch (FormatterException e) {
        // Do not format on errors
      }
    }
  }

  private void performReplacements(
      final Document document, final List<Replacement> reverseSortedReplacements) {
    WriteCommandAction.runWriteCommandAction(
        getProject(),
        () -> {
          for (Replacement replacement : reverseSortedReplacements) {
            Range<Integer> range = replacement.getReplaceRange();
            document.replaceString(
                range.lowerEndpoint(), range.upperEndpoint(), replacement.getReplacementString());
          }
          PsiDocumentManager.getInstance(getProject()).commitDocument(document);
        });
  }

  private static List<Range<Integer>> convertToRanges(Collection<TextRange> textRanges) {
    ImmutableList.Builder<Range<Integer>> ranges = ImmutableList.builder();
    for (TextRange textRange : textRanges) {
      ranges.add(Range.closedOpen(textRange.getStartOffset(), textRange.getEndOffset()));
    }
    return ranges.build();
  }
}
