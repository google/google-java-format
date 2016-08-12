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

import com.intellij.lifecycle.PeriodicalTasksCloser;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

@State(
  name = "GoogleJavaFormatSettings",
  storages = {@Storage("google-java-format.xml")}
)
class GoogleJavaFormatSettings extends AbstractProjectComponent
    implements PersistentStateComponent<GoogleJavaFormatSettings.State> {

  private boolean enabled = false;

  protected GoogleJavaFormatSettings(Project project) {
    super(project);
  }

  public static GoogleJavaFormatSettings getInstance(Project project) {
    return PeriodicalTasksCloser.getInstance()
        .safeGetComponent(project, GoogleJavaFormatSettings.class);
  }

  @Nullable
  @Override
  public State getState() {
    State state = new State();
    state.setEnabled(enabled);
    return state;
  }

  @Override
  public void loadState(State state) {
    setEnabled(state.isEnabled());
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
    GoogleJavaFormatInstaller.installFormatter(myProject, enabled);
  }

  static class State {
    private boolean enabled = false;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }
}
