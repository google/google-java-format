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

import com.google.googlejavaformat.java.JavaFormatterOptions;
import com.intellij.lifecycle.PeriodicalTasksCloser;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import javax.annotation.Nullable;

@State(
  name = "GoogleJavaFormatSettings",
  storages = {@Storage("google-java-format.xml")}
)
class GoogleJavaFormatSettings extends AbstractProjectComponent
    implements PersistentStateComponent<GoogleJavaFormatSettings.State> {

  private State state = new State();

  protected GoogleJavaFormatSettings(Project project) {
    super(project);
  }

  static GoogleJavaFormatSettings getInstance(Project project) {
    return PeriodicalTasksCloser.getInstance()
        .safeGetComponent(project, GoogleJavaFormatSettings.class);
  }

  @Nullable
  @Override
  public State getState() {
    return state;
  }

  @Override
  public void loadState(State state) {
    this.state = state;
  }

  boolean isEnabled() {
    return state.enabled;
  }

  void setEnabled(boolean enabled) {
    state.enabled = enabled;
  }

  JavaFormatterOptions.Style getStyle() {
    return state.style;
  }

  void setStyle(JavaFormatterOptions.Style style) {
    state.style = style;
    updateFormatterState();
  }

  private void updateFormatterState() {
    if (state.enabled) {
      GoogleJavaFormatInstaller.installFormatter(
          myProject,
          (delegate) -> new BasicGoogleJavaFormatCodeStyleManager(delegate, state.style));
    } else {
      GoogleJavaFormatInstaller.removeFormatter(myProject);
    }
  }

  static class State {
    public boolean enabled = false;
    public JavaFormatterOptions.Style style = JavaFormatterOptions.Style.GOOGLE;
  }
}
