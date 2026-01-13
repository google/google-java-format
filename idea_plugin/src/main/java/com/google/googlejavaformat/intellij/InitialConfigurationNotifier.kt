/*
 * Copyright 2025 Google Inc. All Rights Reserved.
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

package com.google.googlejavaformat.intellij

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

private class InitialConfigurationNotifier : ProjectActivity {

    companion object {
        const val NOTIFICATION_TITLE: String = "Enable google-java-format"
    }

    override suspend fun execute(project: Project) {
        val settings = GoogleJavaFormatSettings.getInstance(project)

        if (settings.isUninitialized) {
            settings.isEnabled = false
            displayNewUserNotification(project, settings)
        } else if (settings.isEnabled) {
            JreConfigurationChecker.checkJreConfiguration(project)
        }
    }

    private fun displayNewUserNotification(project: Project?, settings: GoogleJavaFormatSettings) {
        val groupManager = NotificationGroupManager.getInstance()
        val group = groupManager.getNotificationGroup(NOTIFICATION_TITLE)
        val notification =
            Notification(
                group.displayId,
                NOTIFICATION_TITLE,
                "The google-java-format plugin is disabled by default.",
                NotificationType.INFORMATION
            )
        notification.addAction(
            object : NotificationAction("Enable for this project") {
                override fun actionPerformed(
                    anActionEvent: AnActionEvent, notification: Notification
                ) {
                    settings.isEnabled = true
                    notification.expire()
                }
            })
        notification.notify(project)
    }
}
