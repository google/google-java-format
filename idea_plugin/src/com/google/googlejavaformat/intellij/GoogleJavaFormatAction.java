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

import com.intellij.codeInsight.actions.ReformatCodeAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.picocontainer.MutablePicoContainer;

/**
 * An action performing google-java-format in place of IJ's default code style formatting.
 *
 * <p>For now, this substitutes the default CodeStyleManager with an implementation which will
 * perform google-java-format on .java files, and this implementation resides until IJ is restarted.
 *
 * <p>TODO(bcsf): Ideal experience is to let the user configure the CodeStyleManager on a
 * per-project basis.
 *
 * @author bcsf@google.com (Brian Chang)
 */
public class GoogleJavaFormatAction extends ReformatCodeAction {
  private static final String CODE_STYLE_MANAGER_KEY = CodeStyleManager.class.getName();

  public void actionPerformed(AnActionEvent event) {
    if (event.getProject() != null) {
      CodeStyleManager manager = CodeStyleManager.getInstance(event.getProject());
      if (!(manager instanceof GoogleJavaFormatCodeStyleManager)) {
        MutablePicoContainer container =
            (MutablePicoContainer) event.getProject().getPicoContainer();
        container.unregisterComponent(CODE_STYLE_MANAGER_KEY);
        container.registerComponentInstance(
            CODE_STYLE_MANAGER_KEY, new GoogleJavaFormatCodeStyleManager(manager));
      }
    }
    super.actionPerformed(event);
  }
}
