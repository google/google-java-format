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

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;

final class InitialConfigurationComponent implements ProjectComponent {

  private static final String NOTIFICATION_TITLE = "Enable google-java-format";
  private static final NotificationGroup NOTIFICATION_GROUP =
      new NotificationGroup(NOTIFICATION_TITLE, NotificationDisplayType.STICKY_BALLOON, true);

  private final Project project;
  private final GoogleJavaFormatSettings settings;

  public InitialConfigurationComponent(Project project, GoogleJavaFormatSettings settings) {
    this.project = project;
    this.settings = settings;
  }

  @Override
  public void projectOpened() {
    if (settings.isUninitialized()) {
      settings.setEnabled(false);
      displayNewUserNotification();
    }
  }

  private void displayNewUserNotification() {
    Notification notification =
        new Notification(
            NOTIFICATION_GROUP.getDisplayId(),
            NOTIFICATION_TITLE,
            "The google-java-format plugin is disabled by default. "
                + "<a href=\"enable\">Enable for this project</a>.",
            NotificationType.INFORMATION,
            (n, e) -> {
              settings.setEnabled(true);
              n.expire();
            });
    notification.notify(project);
  }
}
