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

import com.intellij.openapi.project.Project;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.picocontainer.MutablePicoContainer;

/**
 * A utility class to replace (and revert) the default IntelliJ {@link CodeStyleManager} with one
 * that formats via google-java-format.
 */
public final class GoogleJavaFormatInstaller {

  private static final String CODE_STYLE_MANAGER_KEY = CodeStyleManager.class.getName();

  private GoogleJavaFormatInstaller() {}

  public static void installFormatter(Project project, boolean useGoogleFormatter) {
    CodeStyleManager currentManager = CodeStyleManager.getInstance(project);
    CodeStyleManager newManager = null;

    if (useGoogleFormatter) {
      if (!(currentManager instanceof GoogleJavaFormatCodeStyleManager)) {
        newManager = new GoogleJavaFormatCodeStyleManager(currentManager);
      }
    } else {
      if (currentManager instanceof GoogleJavaFormatCodeStyleManager) {
        newManager = ((GoogleJavaFormatCodeStyleManager) currentManager).getDelegate();
      }
    }

    if (newManager != null) {
      MutablePicoContainer container = (MutablePicoContainer) project.getPicoContainer();
      container.unregisterComponent(CODE_STYLE_MANAGER_KEY);
      container.registerComponentInstance(CODE_STYLE_MANAGER_KEY, newManager);
    }
  }
}
