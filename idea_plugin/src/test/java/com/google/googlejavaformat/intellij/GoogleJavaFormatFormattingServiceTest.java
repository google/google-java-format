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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.googlejavaformat.intellij.GoogleJavaFormatSettings.State;
import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.JavaFormatterOptions;
import com.google.googlejavaformat.java.JavaFormatterOptions.Style;
import com.intellij.formatting.service.AsyncFormattingRequest;
import com.intellij.formatting.service.FormattingService;
import com.intellij.formatting.service.FormattingServiceUtil;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.testFramework.ExtensionTestUtil;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture;
import com.intellij.testFramework.fixtures.JavaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GoogleJavaFormatFormattingServiceTest {
  private JavaCodeInsightTestFixture fixture;
  private GoogleJavaFormatSettings settings;
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

    settings = GoogleJavaFormatSettings.getInstance(fixture.getProject());
    State resetState = new State();
    resetState.setEnabled("true");
    settings.loadState(resetState);
  }

  @After
  public void tearDown() throws Exception {
    fixture.tearDown();
  }

  @Test
  public void defaultFormatSettings() throws Exception {
    PsiFile file =
        createPsiFile(
            "com/foo/FormatTest.java",
            "package com.foo;",
            "public class FormatTest {",
            "static final String CONST_STR = \"Hello\";",
            "}");
    String origText = file.getText();
    CodeStyleManager manager = CodeStyleManager.getInstance(file.getProject());
    WriteCommandAction.runWriteCommandAction(
        file.getProject(), () -> manager.reformatText(file, 0, file.getTextLength()));

    assertThat(file.getText()).isEqualTo(new Formatter().formatSource(origText));
    assertThat(delegatingFormatter.wasInvoked()).isTrue();
  }

  @Test
  public void aospStyle() throws Exception {
    settings.setStyle(Style.AOSP);
    PsiFile file =
        createPsiFile(
            "com/foo/FormatTest.java",
            "package com.foo;",
            "public class FormatTest {",
            "static final String CONST_STR = \"Hello\";",
            "}");
    String origText = file.getText();
    CodeStyleManager manager = CodeStyleManager.getInstance(file.getProject());
    WriteCommandAction.runWriteCommandAction(
        file.getProject(), () -> manager.reformatText(file, 0, file.getTextLength()));

    assertThat(file.getText())
        .isEqualTo(
            new Formatter(JavaFormatterOptions.builder().style(Style.AOSP).build())
                .formatSource(origText));
    assertThat(delegatingFormatter.wasInvoked()).isTrue();
  }

  @Test
  public void canChangeWhitespaceOnlyDoesNotReorderModifiers() throws Exception {
    settings.setStyle(Style.GOOGLE);
    PsiFile file =
        createPsiFile(
            "com/foo/FormatTest.java",
            "package com.foo;",
            "public class FormatTest {",
            "final static String CONST_STR = \"Hello\";",
            "}");
    CodeStyleManager manager = CodeStyleManager.getInstance(file.getProject());
    var offset = file.getText().indexOf("final static");
    WriteCommandAction.<PsiElement>runWriteCommandAction(
        file.getProject(),
        () ->
            FormattingServiceUtil.formatElement(
                file.findElementAt(offset), /* canChangeWhitespaceOnly= */ true));

    // In non-whitespace mode, this would flip the order of these modifiers. (Also check for leading
    // spaces to make sure the formatter actually ran.
    assertThat(file.getText()).containsMatch("  final static");
    assertThat(delegatingFormatter.wasInvoked()).isTrue();
  }

  @Test
  public void canChangeWhitespaceOnlyDoesNotReformatJavadoc() throws Exception {
    settings.setStyle(Style.GOOGLE);
    PsiFile file =
        createPsiFile(
            "com/foo/FormatTest.java",
            "package com.foo;",
            "public class FormatTest {",
            "/**",
            " * hello",
            " */",
            "static final String CONST_STR = \"Hello\";",
            "}");
    CodeStyleManager manager = CodeStyleManager.getInstance(file.getProject());
    var offset = file.getText().indexOf("hello");
    WriteCommandAction.<PsiElement>runWriteCommandAction(
        file.getProject(),
        () ->
            FormattingServiceUtil.formatElement(
                file.findElementAt(offset), /* canChangeWhitespaceOnly= */ true));

    // In non-whitespace mode, this would join the Javadoc into a single line.
    assertThat(file.getText()).containsMatch(" \\* hello");
    // Also check for leading spaces to make sure the formatter actually ran. (Technically, this is
    // outside the range that we asked to be formatted, but gjf will still format it.)
    assertThat(file.getText()).containsMatch("  static final");
    assertThat(delegatingFormatter.wasInvoked()).isTrue();
  }

  @Test
  public void canChangeNonWhitespaceReordersModifiers() throws Exception {
    settings.setStyle(Style.GOOGLE);
    PsiFile file =
        createPsiFile(
            "com/foo/FormatTest.java",
            "package com.foo;",
            "public class FormatTest {",
            "final static String CONST_STR = \"Hello\";",
            "}");
    CodeStyleManager manager = CodeStyleManager.getInstance(file.getProject());
    var offset = file.getText().indexOf("final static");
    WriteCommandAction.<PsiElement>runWriteCommandAction(
        file.getProject(),
        () ->
            FormattingServiceUtil.formatElement(
                file.findElementAt(offset), /* canChangeWhitespaceOnly= */ false));

    assertThat(file.getText()).containsMatch("static final");
    assertThat(delegatingFormatter.wasInvoked()).isTrue();
  }

  @Test
  public void canChangeNonWhitespaceReformatsJavadoc() throws Exception {
    settings.setStyle(Style.GOOGLE);
    PsiFile file =
        createPsiFile(
            "com/foo/FormatTest.java",
            "package com.foo;",
            "public class FormatTest {",
            "/**",
            " * hello",
            " */",
            "static final String CONST_STR = \"Hello\";",
            "}");
    CodeStyleManager manager = CodeStyleManager.getInstance(file.getProject());
    var offset = file.getText().indexOf("hello");
    WriteCommandAction.<PsiElement>runWriteCommandAction(
        file.getProject(),
        () ->
            FormattingServiceUtil.formatElement(
                file.findElementAt(offset), /* canChangeWhitespaceOnly= */ false));

    assertThat(file.getText()).containsMatch("/\\*\\* hello \\*/");
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
    protected FormattingTask createFormattingTask(AsyncFormattingRequest request) {
      FormattingTask delegateTask = super.createFormattingTask(request);
      return new FormattingTask() {
        @Override
        public boolean cancel() {
          return delegateTask.cancel();
        }

        @Override
        public void run() {
          invoked = true;
          delegateTask.run();
        }
      };
    }
  }
}
