/*
 * Copyright 2023 Google Inc. All Rights Reserved.
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import com.google.googlejavaformat.java.JavaFormatterOptions;
import com.google.googlejavaformat.java.JavaFormatterOptions.Style;
import com.intellij.formatting.service.AsyncDocumentFormattingService;
import com.intellij.formatting.service.AsyncFormattingRequest;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.lang.ImportOptimizer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/** Uses {@code google-java-format} to reformat code. */
public class GoogleJavaFormatFormattingService extends AsyncDocumentFormattingService {

  public static final ImmutableSet<ImportOptimizer> IMPORT_OPTIMIZERS =
      ImmutableSet.of(new GoogleJavaFormatImportOptimizer());

  @Override
  protected FormattingTask createFormattingTask(AsyncFormattingRequest request) {
    Project project = request.getContext().getProject();

    if (!JreConfigurationChecker.checkJreConfiguration(project)) {
      return null;
    }

    Style style = GoogleJavaFormatSettings.getInstance(project).getStyle();
    Formatter formatter = createFormatter(style, request.canChangeWhitespaceOnly());
    return new GoogleJavaFormatFormattingTask(formatter, request);
  }

  @Override
  protected String getNotificationGroupId() {
    return Notifications.PARSING_ERROR_NOTIFICATION_GROUP;
  }

  @Override
  protected String getName() {
    return "google-java-format";
  }

  private static Formatter createFormatter(Style style, boolean canChangeWhiteSpaceOnly) {
    JavaFormatterOptions.Builder optBuilder = JavaFormatterOptions.builder().style(style);
    if (canChangeWhiteSpaceOnly) {
      optBuilder.formatJavadoc(false).reorderModifiers(false);
    }
    return new Formatter(optBuilder.build());
  }

  @Override
  public @NotNull Set<Feature> getFeatures() {
    return Set.of(Feature.FORMAT_FRAGMENTS, Feature.OPTIMIZE_IMPORTS);
  }

  @Override
  public boolean canFormat(@NotNull PsiFile file) {
    return JavaFileType.INSTANCE.equals(file.getFileType())
        && GoogleJavaFormatSettings.getInstance(file.getProject()).isEnabled();
  }

  @Override
  public @NotNull Set<ImportOptimizer> getImportOptimizers(@NotNull PsiFile file) {
    return IMPORT_OPTIMIZERS;
  }

  private static final class GoogleJavaFormatFormattingTask implements FormattingTask {
    private final Formatter formatter;
    private final AsyncFormattingRequest request;

    private GoogleJavaFormatFormattingTask(Formatter formatter, AsyncFormattingRequest request) {
      this.formatter = formatter;
      this.request = request;
    }

    @Override
    public void run() {
      try {
        String formattedText = formatter.formatSource(request.getDocumentText(), toRanges(request));
        request.onTextReady(formattedText);
      } catch (FormatterException e) {
        request.onError(
            Notifications.PARSING_ERROR_TITLE,
            Notifications.parsingErrorMessage(request.getContext().getContainingFile().getName()));
      }
    }

    private static Collection<Range<Integer>> toRanges(AsyncFormattingRequest request) {
      if (isWholeFile(request)) {
        // The IDE sometimes passes invalid ranges when the file is unsaved before invoking the
        // formatter. So this is a workaround for that issue.
        return ImmutableList.of(Range.closedOpen(0, request.getDocumentText().length()));
      }
      return request.getFormattingRanges().stream()
          .map(textRange -> Range.closedOpen(textRange.getStartOffset(), textRange.getEndOffset()))
          .collect(ImmutableList.toImmutableList());
    }

    private static boolean isWholeFile(AsyncFormattingRequest request) {
      List<TextRange> ranges = request.getFormattingRanges();
      return ranges.size() == 1
          && ranges.get(0).getStartOffset() == 0
          // using greater than or equal because ranges are sometimes passed inaccurately
          && ranges.get(0).getEndOffset() >= request.getDocumentText().length();
    }

    @Override
    public boolean isRunUnderProgress() {
      return true;
    }

    @Override
    public boolean cancel() {
      return false;
    }
  }
}
