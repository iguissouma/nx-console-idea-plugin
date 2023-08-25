package com.github.iguissouma.nxconsole.deprecation

import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.installAndEnable
import com.intellij.openapi.vcs.changes.shelf.ShelveChangesManager.PostStartupActivity

class ShowDeprecationNoticeStartupActivity: ProjectActivity {

    override suspend fun execute(project: Project) {
        val dontShowAgain = PropertiesComponent.getInstance(project).getBoolean("com.github.iguissouma.nxconsole.dontShowDeprecationNoticeAgain")

        if(dontShowAgain) {
            return
        }

        val notificationGroup = NotificationGroupManager.getInstance().getNotificationGroup("Nx Console")
        notificationGroup.createNotification(
                "Please switch to the official Nx Console plugin maintained by the Nx team.",
                NotificationType.WARNING,
        ).setTitle("Nx Console Idea is deprecated").addActions(listOf(
                object : NotificationAction("Install Nx Console") {
                    override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                        installAndEnable(project, setOf(PluginId.getId("dev.nx.console")), true, false, null ) {}
                        notification.expire()
                    }
                },
                object : NotificationAction("Don't show again") {
                    override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                        PropertiesComponent.getInstance(project).setValue("com.github.iguissouma.nxconsole.dontShowDeprecationNoticeAgain", true)
                        notification.expire()
                    }
                }
        ) as Collection<AnAction>).notify(project)
    }
}