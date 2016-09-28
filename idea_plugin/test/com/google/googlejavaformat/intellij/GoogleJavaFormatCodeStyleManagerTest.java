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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.googlejavaformat.java.JavaFormatterOptions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Tests for {@link GoogleJavaFormatCodeStyleManager}.
 *
 * @author bcsf@google.com (Brian Chang)
 */
public class GoogleJavaFormatCodeStyleManagerTest extends LightCodeInsightFixtureTestCase {

  @Override
  protected void setUp() throws Exception {
    if (getClass().getClassLoader() instanceof URLClassLoader) {
      for (URL url : ((URLClassLoader) getClass().getClassLoader()).getURLs()) {
        if (url.getPath().matches(".*/ecj-\\d(\\.\\d)*\\.jar")) {
          System.err.println(
              "If you see a SecurityException while running this test from within IJ,\n"
                  + "unfortunately you'll need to somehow remove ecj.jar from your classpath,\n"
                  + "as it will cause a cert conflict with org.eclipse.jdt.core.jar.\n"
                  + "e.g. if "
                  + url.getPath()
                  + " is in IDEA_HOME/lib, temporarily relocate it");
        }
      }
    }
    super.setUp();
  }

  public void testFormatFile() {
    CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(getProject());
    final GoogleJavaFormatCodeStyleManager googleJavaFormat =
        new BasicGoogleJavaFormatCodeStyleManager(
            codeStyleManager, JavaFormatterOptions.Style.GOOGLE);
    myFixture.configureByText(
        StdFileTypes.JAVA,
        "public class Test {public static void main(String[]args){System.out.println();}}");
    final int startOffset = 0;
    final int endOffset = myFixture.getFile().getTextLength();
    ApplicationManager.getApplication()
        .runWriteAction(
            () -> googleJavaFormat.reformatText(myFixture.getFile(), startOffset, endOffset));
    myFixture.checkResult(
        join(
            "public class Test {",
            "  public static void main(String[] args) {",
            "    System.out.println();",
            "  }",
            "}",
            ""));
  }

  public void testDontFormatNonJavaFile() {
    CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(getProject());
    final GoogleJavaFormatCodeStyleManager googleJavaFormat =
        new BasicGoogleJavaFormatCodeStyleManager(
            codeStyleManager, JavaFormatterOptions.Style.GOOGLE);
    myFixture.configureByText(
        StdFileTypes.PLAIN_TEXT,
        "public class Test {public static void main(String[]args){System.out.println();}}");
    final int startOffset = 0;
    final int endOffset = myFixture.getFile().getTextLength();
    ApplicationManager.getApplication()
        .runWriteAction(
            () -> googleJavaFormat.reformatText(myFixture.getFile(), startOffset, endOffset));
    myFixture.checkResult(
        "public class Test {public static void main(String[]args){System.out.println();}}");
  }

  public void testFormatRanges() {
    CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(getProject());
    final GoogleJavaFormatCodeStyleManager googleJavaFormat =
        new BasicGoogleJavaFormatCodeStyleManager(
            codeStyleManager, JavaFormatterOptions.Style.GOOGLE);
    String content =
        join(
            "public class Test {", //
            "  int a=0;",
            "  int b=1;",
            "  int c=2;",
            "}");
    myFixture.configureByText(StdFileTypes.JAVA, content);
    int aLineStart = content.indexOf("int a");
    int aLineEnd = content.indexOf("\n", aLineStart);
    int cLineStart = content.indexOf("int c");
    int cLineEnd = content.indexOf("\n", cLineStart);
    final ImmutableList<TextRange> ranges =
        ImmutableList.of(new TextRange(aLineStart, aLineEnd), new TextRange(cLineStart, cLineEnd));
    ApplicationManager.getApplication()
        .runWriteAction(() -> googleJavaFormat.reformatText(myFixture.getFile(), ranges));
    myFixture.checkResult(
        join(
            "public class Test {", //
            "  int a = 0;",
            "  int b=1;",
            "  int c = 2;",
            "}"));
  }

  private static String join(String... lines) {
    return Joiner.on("\n").join(lines);
  }
}
