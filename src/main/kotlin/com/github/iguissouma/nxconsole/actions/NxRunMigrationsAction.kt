package com.github.iguissouma.nxconsole.actions

import com.github.iguissouma.nxconsole.NxIcons
import com.github.iguissouma.nxconsole.cli.config.NxConfigProvider
import com.github.iguissouma.nxconsole.util.NxExecutionUtil
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.javascript.nodejs.CompletionModuleInfo
import com.intellij.javascript.nodejs.NodeModuleSearchUtil
import com.intellij.javascript.nodejs.interpreter.NodeCommandLineConfigurator
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.lang.javascript.buildTools.base.JsbtUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import javax.swing.event.HyperlinkEvent

class NxRunMigrationsAction : DumbAwareAction({ "Nx Run Migration" }, NxIcons.NRWL_ICON) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        ProgressManager.getInstance()
            .run(object : Backgroundable(null, "Nx run migration", false, ALWAYS_BACKGROUND) {
                override fun run(indicator: ProgressIndicator) {
                    val handler = NxExecutionUtil(project)
                        .getProcessHandler("migrate", "--run-migrations=migrations.json") ?: return
                    var startNotified = false
                    var success = false
                    handler.addProcessListener(object : ProcessListener {
                        override fun startNotified(event: ProcessEvent) {
                            startNotified = true
                        }

                        override fun processTerminated(event: ProcessEvent) {
                            val notificationGroup = NotificationGroup(
                                "nx.notifications.balloon",
                                NotificationDisplayType.BALLOON,
                                true
                            )
                            if (success) {
                                val content = "NX Successfully finished running migrations from migrations.json"
                                val msg = notificationGroup.createNotification(
                                    "Nx Run Migration",
                                    content,
                                    NotificationType.INFORMATION,
                                    MyNotificationListener(project)
                                )
                                msg.notify(project)
                            } else {
                                val content = "NX The migrate command failed."
                                val msg = notificationGroup.createNotification(
                                    "Nx Run Migration",
                                    content,
                                    NotificationType.ERROR
                                )
                                msg.notify(project)
                            }
                        }

                        override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                            if (startNotified) {
                                val text = event.text
                                if (text.contains("Successfully finished running migrations from", true)) {
                                    success = true
                                }
                                indicator.text = text
                            }
                        }
                    })
                    handler.startNotify()
                }

                override fun isHeadless(): Boolean {
                    // Necessary, otherwise runs synchronously in unit tests.
                    return false
                }
            })
    }

    override fun update(e: AnActionEvent) {
        val project = e.project ?: return
        val presentation = e.presentation
        val nxConfig = NxConfigProvider.getNxConfig(project, project.baseDir)
        presentation.isEnabled = nxConfig != null
    }

    private fun createFileLink(projet: Project, file: VirtualFile): String {
        return createLink(file.path, JsbtUtil.getRelativePath(projet, file, false))
    }

    private fun createLink(href: String, linkText: String): String {
        return "<a href='" + StringUtil.escapeXmlEntities(href) + "'>" + StringUtil.escapeXmlEntities(linkText) + "</a>"
    }

    private class MyNotificationListener(private val projet: Project) : NotificationListener {
        override fun hyperlinkUpdate(notification: Notification, event: HyperlinkEvent) {
            if (event.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                val file = LocalFileSystem.getInstance().findFileByPath(event.description)
                if (file != null && file.isValid) {
                    FileEditorManager.getInstance(projet).openFile(file, true)
                }
            }
        }
    }
}
