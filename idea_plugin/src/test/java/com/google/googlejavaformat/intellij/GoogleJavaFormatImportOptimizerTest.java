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

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.googlejavaformat.intellij.GoogleJavaFormatSettings.State;
import com.intellij.codeInsight.actions.OptimizeImportsProcessor;
import com.intellij.formatting.service.FormattingService;
import com.intellij.lang.ImportOptimizer;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.ExtensionTestUtil;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture;
import com.intellij.testFramework.fixtures.JavaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import java.io.IOException;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GoogleJavaFormatImportOptimizerTest {
  private JavaCodeInsightTestFixture fixture;
  private DelegatingFormatter delegatingFormatter;

  @Before
  public void setUp() throws Exception {
    TestFixtureBuilder<IdeaProjectTestFixture> projectBuilder =
        IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(getClass().getName());
    fixture =
        JavaTestFixtureFactory.getFixtureFactory()
            .createCodeInsightFixture(projectBuilder.getFixture());
    fixture.setUp();

    delegatingFormatter = new DelegatingFormatter();
    ExtensionTestUtil.maskExtensions(
        FormattingService.EP_NAME,
        ImmutableList.of(delegatingFormatter),
        fixture.getProjectDisposable());

    var settings = GoogleJavaFormatSettings.getInstance(fixture.getProject());
    State resetState = new State();
    resetState.setEnabled("true");
    settings.loadState(resetState);
  }

  @After
  public void tearDown() throws Exception {
    fixture.tearDown();
  }

  @Test
  public void removesUnusedImports() throws Exception {
    PsiFile file =
        createPsiFile(
            "com/foo/ImportTest.java",
            "package com.foo;",
            "import java.util.List;",
            "import java.util.ArrayList;",
            "import java.util.Map;",
            "public class ImportTest {",
            "static final Map map;",
            "}");
    OptimizeImportsProcessor processor = new OptimizeImportsProcessor(file.getProject(), file);
    WriteCommandAction.runWriteCommandAction(
        file.getProject(),
        () -> {
          processor.run();
          PsiDocumentManager.getInstance(file.getProject()).commitAllDocuments();
        });

    assertThat(file.getText()).doesNotContain("List");
    assertThat(file.getText()).contains("import java.util.Map;");
    assertThat(delegatingFormatter.wasInvoked()).isTrue();
  }

  @Test
  public void reordersImports() throws Exception {
    PsiFile file =
        createPsiFile(
            "com/foo/ImportTest.java",
            "package com.foo;",
            "import java.util.List;",
            "import java.util.ArrayList;",
            "import java.util.Map;",
            "public class ImportTest {",
            "static final ArrayList arrayList;",
            "static final List list;",
            "static final Map map;",
            "}");
    OptimizeImportsProcessor processor = new OptimizeImportsProcessor(file.getProject(), file);
    WriteCommandAction.runWriteCommandAction(
        file.getProject(),
        () -> {
          processor.run();
          PsiDocumentManager.getInstance(file.getProject()).commitAllDocuments();
        });

    assertThat(file.getText())
        .contains(
            "import java.util.ArrayList;\n"
                + "import java.util.List;\n"
                + "import java.util.Map;\n");
    assertThat(delegatingFormatter.wasInvoked()).isTrue();
  }

  private PsiFile createPsiFile(String path, String... contents) throws IOException {
    VirtualFile virtualFile =
        fixture.getTempDirFixture().createFile(path, String.join("\n", contents));
    fixture.configureFromExistingVirtualFile(virtualFile);
    PsiFile psiFile = fixture.getFile();
    assertThat(psiFile).isNotNull();
    return psiFile;
  }

  private static final class DelegatingFormatter extends GoogleJavaFormatFormattingService {

    private boolean invoked = false;

    private boolean wasInvoked() {
      return invoked;
    }

    @Override
    public @NotNull Set<ImportOptimizer> getImportOptimizers(@NotNull PsiFile file) {
      return super.getImportOptimizers(file).stream().map(this::wrap).collect(toImmutableSet());
    }

    private ImportOptimizer wrap(ImportOptimizer delegate) {
      return new ImportOptimizer() {
        @Override
        public boolean supports(@NotNull PsiFile file) {
          return delegate.supports(file);
        }

        @Override
        public @NotNull Runnable processFile(@NotNull PsiFile file) {
          return () -> {
            invoked = true;
            delegate.processFile(file).run();
          };
        }
      };
    }
  }
}
