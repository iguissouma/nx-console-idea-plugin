package com.github.iguissouma.nxconsole.actions

import com.github.iguissouma.nxconsole.NxIcons
import com.github.iguissouma.nxconsole.cli.config.NxConfigProvider
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.javascript.nodejs.CompletionModuleInfo
import com.intellij.javascript.nodejs.NodeModuleSearchUtil
import com.intellij.javascript.nodejs.interpreter.NodeCommandLineConfigurator
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.javascript.nodejs.npm.registry.NpmRegistryService
import com.intellij.javascript.nodejs.packageJson.NodeInstalledPackageFinder
import com.intellij.lang.javascript.buildTools.base.JsbtUtil
import com.intellij.lang.javascript.buildTools.npm.PackageJsonUtil.findChildPackageJsonFile
import com.intellij.notification.Notification
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import javax.swing.event.HyperlinkEvent


class NxMigrateWorkspaceAction : DumbAwareAction({ "Nx Migrate Workspace" }, NxIcons.NRWL_ICON) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val nxConfig = NxConfigProvider.getNxConfig(project, project.baseDir) ?: return
        val interpreter = NodeJsInterpreterManager.getInstance(project).interpreter
        var configurator: NodeCommandLineConfigurator? = null
        try {
            configurator = NodeCommandLineConfigurator.find(interpreter!!)
        } catch (e: Exception) {
            return
        }

        val modules: MutableList<CompletionModuleInfo> = mutableListOf()
        NodeModuleSearchUtil.findModulesWithName(
            modules,
            "@nrwl/cli",
            project.baseDir, // TODO change deprecation
            null
        )
        val module = modules.firstOrNull() ?: return
        val moduleExe =
            "${module.virtualFile!!.path}${File.separator}bin${File.separator}nx"
        val commandLine =
            GeneralCommandLine("", moduleExe, "migrate", "@nrwl/workspace")
        commandLine.withWorkDirectory(project.basePath)
        configurator.configure(commandLine)

        ProgressManager.getInstance()
            .run(object : Backgroundable(null, "Nx migrate Workspace", false, ALWAYS_BACKGROUND) {
                override fun run(indicator: ProgressIndicator) {
                    val handler = CapturingProcessHandler(commandLine)
                    //val indicator = ProgressManager.getInstance().progressIndicator ?: EmptyProgressIndicator()
                    var startNotified = false;
                    var success = false;
                    var packageJsonHasBeenUpdated = false;
                    var migrationsJsonHasBeenGenerated = false;
                    handler.addProcessListener(object : ProcessListener {
                        override fun startNotified(event: ProcessEvent) {
                            //TODO("Not yet implemented")
                            println("start notified...")
                            startNotified = true
                        }

                        override fun processTerminated(event: ProcessEvent) {
                            //TODO("Not yet implemented")
                            println("process terminated...")
                            val notificationGroup = NotificationGroup(
                                "nx.notifications.balloon",
                                NotificationDisplayType.BALLOON,
                                true
                            )
                            if (success) {
                                val packageJson = findChildPackageJsonFile(project.baseDir)
                                val content = "NX The migrate command has run successfully." +
                                        "<br>" +
                                        "- " +
                                        (if (packageJson != null) createFileLink(
                                            project,
                                            packageJson
                                        ) else "package.json") +
                                        " has been updated" +
                                        (if (migrationsJsonHasBeenGenerated) {
                                            val migrationsJson = findChildMigrationsJsonFile(project.baseDir)
                                            "<br>" +
                                                    "- " +
                                                    (if (migrationsJson != null) createFileLink(
                                                        project,
                                                        migrationsJson
                                                    ) else "migrations.json") +
                                                    " has been generated"
                                        } else "")

                                val msg = notificationGroup.createNotification(
                                    "Nx Workspace Migration",
                                    content,
                                    NotificationType.INFORMATION,
                                    MyNotificationListener(project)
                                )
                                if (migrationsJsonHasBeenGenerated) {
                                    msg.addAction(NxRunMigrationsAction())
                                }
                                msg.notify(project)
                            } else {
                                val content = "NX The migrate command failed."
                                val msg = notificationGroup.createNotification(
                                    "Nx Workspace Migration",
                                    content,
                                    NotificationType.ERROR
                                )
                                msg.notify(project)
                            }


                        }

                        override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                            //TODO("Not yet implemented")
                            if (startNotified) {
                                val text = event.text
                                if (text.startsWith(">  NX  The migrate command has run successfully.")) {
                                    success = true
                                }
                                if (text.startsWith("- package.json has been updated")) {
                                    packageJsonHasBeenUpdated = true
                                }
                                if (text.startsWith("- migrations.json has been generated")) {
                                    migrationsJsonHasBeenGenerated = true
                                }

                                indicator.text = text
                                println(text)
                                println("--->")
                            }
                        }
                    })
                    handler.runProcess()
                }

                override fun isHeadless(): Boolean {
                    // Necessary, otherwise runs synchronously in unit tests.
                    return false
                }
            })


        /*ProgressManager.getInstance().runProcessWithProgressSynchronously({
          runReadAction {

          }
        }, "Nx migrate workspace", false, project) */

    }

    override fun update(e: AnActionEvent) {
        val project = e.project ?: return
        val presentation = e.presentation
        val nxConfig = NxConfigProvider.getNxConfig(project, project.baseDir)
        presentation.isEnabled = nxConfig != null

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

    fun findChildMigrationsJsonFile(dir: VirtualFile?): VirtualFile? {
        if (dir != null && dir.isValid) {
            val migrationsJson = dir.findChild("migrations.json")
            if (migrationsJson != null && migrationsJson.isValid && !migrationsJson.isDirectory) {
                return migrationsJson
            }
        }
        return null
    }

    class MyStartupActivity : StartupActivity.Background {
        override fun runActivity(project: Project) {

            val nodeInstalledPackageFinder = NodeInstalledPackageFinder(project, project.baseDir)
            val pkg = "@nrwl/workspace"
            val fromVersion = nodeInstalledPackageFinder.findInstalledPackage(pkg)?.version ?: return
            val service = service<NpmRegistryService>()
            val cachedPackageVersions = service.getCachedOrFetchPackageVersions(EmptyProgressIndicator(), pkg).versions
            val toVersion = cachedPackageVersions.first {
                it.preRelease == null && it.major != 9999 && it.major != 999
            }

            if (toVersion > fromVersion) {
                val notificationGroup = NotificationGroup(
                    "nx.notifications.balloon",
                    NotificationDisplayType.BALLOON,
                    true
                )

                val msg = notificationGroup.createNotification(
                    "Nx Workspace Migration",
                    "A new @nrwl/nx version is available " + createLink(
                        "https://github.com/nrwl/nx/releases/tag/$toVersion",
                        toVersion.parsedVersion
                    ),
                    NotificationType.INFORMATION,
                    MyNotificationListener(project)
                )
                msg.addAction(NxMigrateWorkspaceAction())
                msg.notify(project)

            }

        }
    }

}

private fun createFileLink(projet: Project, file: VirtualFile): String {
    return createLink(file.path, JsbtUtil.getRelativePath(projet, file, false))
}

private fun createLink(href: String, linkText: String): String {
    return "<a href='" + StringUtil.escapeXmlEntities(href) + "'>" + StringUtil.escapeXmlEntities(linkText) + "</a>"
}
