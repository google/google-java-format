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

import com.google.common.util.concurrent.Runnables;
import com.google.googlejavaformat.java.FormatterException;
import com.google.googlejavaformat.java.ImportOrderer;
import com.google.googlejavaformat.java.JavaFormatterOptions;
import com.google.googlejavaformat.java.RemoveUnusedImports;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.lang.ImportOptimizer;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/** Uses {@code google-java-format} to optimize imports. */
public class GoogleJavaFormatImportOptimizer implements ImportOptimizer {

  @Override
  public boolean supports(@NotNull PsiFile file) {
    return JavaFileType.INSTANCE.equals(file.getFileType())
        && GoogleJavaFormatSettings.getInstance(file.getProject()).isEnabled();
  }

  @Override
  public @NotNull Runnable processFile(@NotNull PsiFile file) {
    Project project = file.getProject();

    if (!JreConfigurationChecker.checkJreConfiguration(file.getProject())) {
      return Runnables.doNothing();
    }

    PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
    Document document = documentManager.getDocument(file);

    if (document == null) {
      return Runnables.doNothing();
    }

    JavaFormatterOptions.Style style = GoogleJavaFormatSettings.getInstance(project).getStyle();

    String text;
    try {
      text =
          ImportOrderer.reorderImports(
              RemoveUnusedImports.removeUnusedImports(document.getText()), style);
    } catch (FormatterException e) {
      Notifications.displayParsingErrorNotification(project, file.getName());
      return Runnables.doNothing();
    }

    return () -> document.setText(text);
  }
}
