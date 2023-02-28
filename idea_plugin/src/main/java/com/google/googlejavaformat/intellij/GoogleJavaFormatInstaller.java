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

import static com.google.common.base.Preconditions.checkState;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.serviceContainer.ComponentManagerImpl;
import org.jetbrains.annotations.NotNull;

/**
 * A component that replaces the default IntelliJ {@link CodeStyleManager} with one that formats via
 * google-java-format.
 */
final class GoogleJavaFormatInstaller implements ProjectManagerListener {

  @Override
  public void projectOpened(@NotNull Project project) {
    installFormatter(project);
  }

  private static void installFormatter(Project project) {
    CodeStyleManager currentManager = CodeStyleManager.getInstance(project);

    if (currentManager instanceof GoogleJavaFormatCodeStyleManager) {
      currentManager = ((GoogleJavaFormatCodeStyleManager) currentManager).getDelegate();
    }

    setManager(project, new GoogleJavaFormatCodeStyleManager(currentManager));
  }

  private static void setManager(Project project, CodeStyleManager newManager) {
    ComponentManagerImpl platformComponentManager = (ComponentManagerImpl) project;
    IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PluginId.getId("google-java-format"));
    checkState(plugin != null, "Couldn't locate our own PluginDescriptor.");
    platformComponentManager.registerServiceInstance(CodeStyleManager.class, newManager, plugin);
  }
}
