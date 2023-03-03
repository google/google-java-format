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

import com.google.common.base.Suppliers;
import com.intellij.ide.ui.IdeUiService;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import java.util.function.Supplier;

class JreConfigurationChecker {

  private final Supplier<Boolean> hasAccess = Suppliers.memoize(this::checkJreConfiguration);

  private final Project project;
  private final Logger logger = Logger.getInstance(JreConfigurationChecker.class);

  public JreConfigurationChecker(Project project) {
    this.project = project;
  }

  static boolean checkJreConfiguration(Project project) {
    return project.getService(JreConfigurationChecker.class).hasAccess.get();
  }

  /**
   * Determine whether the JRE is configured to work with the google-java-format plugin. If not,
   * display a notification with instructions and return false.
   */
  private boolean checkJreConfiguration() {
    try {
      boolean hasAccess =
          testClassAccess(
              "com.sun.tools.javac.api.JavacTrees",
              "com.sun.tools.javac.code.Flags",
              "com.sun.tools.javac.file.JavacFileManager",
              "com.sun.tools.javac.parser.JavacParser",
              "com.sun.tools.javac.tree.JCTree",
              "com.sun.tools.javac.util.Log");
      if (!hasAccess) {
        displayConfigurationErrorNotification();
      }
      return hasAccess;
    } catch (ClassNotFoundException e) {
      logger.error("Error checking jre configuration for google-java-format", e);
      return false;
    }
  }

  private boolean testClassAccess(String... classNames) throws ClassNotFoundException {
    for (String className : classNames) {
      if (!testClassAccess(className)) {
        return false;
      }
    }
    return true;
  }

  private boolean testClassAccess(String className) throws ClassNotFoundException {
    Class<?> klass = Class.forName(className);
    return klass
        .getModule()
        // isExported returns true if the package is either open or exported. Either one is
        // sufficient
        // to run the google-java-format code (even though the documentation specifies --add-opens).
        .isExported(klass.getPackageName(), getClass().getClassLoader().getUnnamedModule());
  }

  private void displayConfigurationErrorNotification() {
    Notification notification =
        new Notification(
            "Configure JRE for google-java-format",
            "Configure the JRE for google-java-format",
            "The google-java-format plugin needs additional configuration before it can be used. "
                + "<a href=\"instructions\">Follow the instructions here</a>.",
            NotificationType.INFORMATION);
    notification.setListener(
        (n, e) -> {
          IdeUiService.getInstance()
              .browse(
                  "https://github.com/google/google-java-format/blob/master/README.md#intellij-jre-config");
          n.expire();
        });
    notification.notify(project);
  }
}
