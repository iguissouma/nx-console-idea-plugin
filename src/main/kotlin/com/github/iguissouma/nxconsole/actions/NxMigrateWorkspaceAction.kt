package com.github.iguissouma.nxconsole.actions

import com.github.iguissouma.nxconsole.NxIcons
import com.github.iguissouma.nxconsole.cli.config.NxConfigProvider
import com.github.iguissouma.nxconsole.util.replacePnpmToPnpx
import com.intellij.execution.ExecutionException
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.javascript.debugger.CommandLineDebugConfigurator
import com.intellij.javascript.nodejs.NodeCommandLineUtil
import com.intellij.javascript.nodejs.execution.NodeTargetRun
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.javascript.nodejs.npm.NpmManager
import com.intellij.javascript.nodejs.npm.NpmNodePackage
import com.intellij.javascript.nodejs.npm.NpmPackageDescriptor
import com.intellij.javascript.nodejs.npm.NpmUtil
import com.intellij.javascript.nodejs.npm.WorkingDirectoryDependentNpmPackageVersionManager
import com.intellij.javascript.nodejs.npm.registry.NpmRegistryService
import com.intellij.javascript.nodejs.packageJson.NodeInstalledPackageFinder
import com.intellij.lang.javascript.JavaScriptBundle
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
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ThreeState
import com.intellij.util.text.SemVer
import java.io.File
import java.util.List
import javax.swing.event.HyperlinkEvent

class NxMigrateWorkspaceAction : DumbAwareAction({ "Nx Migrate Workspace" }, NxIcons.NRWL_ICON) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        // val nxConfig = NxConfigProvider.getNxConfig(project, project.baseDir) ?: return
        val node = NodeJsInterpreterManager.getInstance(project).interpreter ?: return
        val targetRun = NodeTargetRun(
            node,
            project,
            null as CommandLineDebugConfigurator?,
            NodeTargetRun.createOptions(ThreeState.UNSURE, List.of())
        )
        val commandLine = targetRun.commandLineBuilder
        val arguments: MutableList<String?> = mutableListOf()
        val npmPackageRef = NpmUtil.createProjectPackageManagerPackageRef()
        val npmPkg = NpmUtil.resolveRef(npmPackageRef, targetRun.project, targetRun.interpreter)
        if (npmPkg == null) {
            if (NpmUtil.isProjectPackageManagerPackageRef(npmPackageRef)) {
                val message = JavaScriptBundle.message(
                    "npm.dialog.message.cannot.resolve.package.manager",
                    NpmManager.getInstance(project).packageRef.identifier
                )
                throw NpmManager.InvalidNpmPackageException(
                    project,
                    HtmlBuilder().append(message).append(HtmlChunk.p())
                        .toString() + JavaScriptBundle.message("please.specify.package.manager", *arrayOfNulls(0))
                ) {} // onNpmPackageRefResolved
            } else {
                throw ExecutionException(
                    JavaScriptBundle.message(
                        "npm.dialog.message.cannot.resolve.package.manager",
                        npmPackageRef.identifier
                    )
                )
            }
        }
        targetRun.enableWrappingWithYarnPnpNode = false
        NpmNodePackage.configureNpmPackage(targetRun, npmPkg, *arrayOfNulls(0))
        NodeCommandLineUtil.prependNodeDirToPATH(targetRun)
        val yarn = NpmUtil.isYarnAlikePackage(npmPkg)
        if (NpmUtil.isPnpmPackage(npmPkg)) {
            var version: SemVer? = null
            WorkingDirectoryDependentNpmPackageVersionManager.getInstance(project)
                .fetchVersion(node, npmPkg, File(project.basePath!!)) {
                    version = it
                }

            // version is null first time use exec
            if (version == null || (version!!.major >= 6 && version!!.minor >= 13)) {
                // useExec like vscode extension
                commandLine.addParameter("exec")
            } else {
                NpmNodePackage(replacePnpmToPnpx(npmPkg.systemIndependentPath))
                    .let {
                        if (it.isValid(targetRun.project, targetRun.interpreter)) {
                            it.configureNpmPackage(targetRun)
                        }
                    }
            }
        } else if (yarn.not()) {
            NpmPackageDescriptor.findBinaryFilePackage(node, "npx")?.configureNpmPackage(targetRun)
        }

        commandLine.setWorkingDirectory(project.basePath!!)
        commandLine.addParameter("nx")
        commandLine.addParameters("migrate", "@nrwl/workspace")

        ProgressManager.getInstance()
            .run(object : Backgroundable(null, "Nx migrate Workspace", false, ALWAYS_BACKGROUND) {
                override fun run(indicator: ProgressIndicator) {
                    val handler = targetRun.startProcess()
                    var startNotified = false
                    var success = false
                    var packageJsonHasBeenUpdated = false
                    var migrationsJsonHasBeenGenerated = false
                    handler.addProcessListener(object : ProcessListener {
                        override fun startNotified(event: ProcessEvent) {
                            // TODO("Not yet implemented")
                            println("start notified...")
                            startNotified = true
                        }

                        override fun processTerminated(event: ProcessEvent) {
                            // TODO("Not yet implemented")
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
                                    (
                                        if (packageJson != null) createFileLink(
                                            project,
                                            packageJson
                                        ) else "package.json"
                                        ) +
                                    " has been updated" +
                                    (
                                        if (migrationsJsonHasBeenGenerated) {
                                            val migrationsJson = findChildMigrationsJsonFile(project.baseDir)
                                            "<br>" +
                                                "- " +
                                                (
                                                    if (migrationsJson != null) createFileLink(
                                                        project,
                                                        migrationsJson
                                                    ) else "migrations.json"
                                                    ) +
                                                " has been generated"
                                        } else ""
                                        )

                                val msg = notificationGroup.createNotification(
                                    "Nx workspace migration",
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
                                    "Nx workspace migration",
                                    content,
                                    NotificationType.ERROR
                                )
                                msg.notify(project)
                            }
                        }

                        override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                            // TODO("Not yet implemented")
                            if (startNotified) {
                                val text = event.text
                                if (!success && text.contains("The migrate command has run successfully.")) {
                                    success = true
                                }
                                if (!packageJsonHasBeenUpdated && text.contains("package.json has been updated")) {
                                    packageJsonHasBeenUpdated = true
                                }
                                if (!migrationsJsonHasBeenGenerated && text.contains("migrations.json has been generated")) {
                                    migrationsJsonHasBeenGenerated = true
                                }

                                indicator.text = text
                                println(text)
                                println("--->")
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
            val toVersion = cachedPackageVersions.firstOrNull {
                it.preRelease == null && it.major != 9999 && it.major != 999
            } ?: return

            if (toVersion > fromVersion) {
                val notificationGroup = NotificationGroup(
                    "nx.notifications.balloon",
                    NotificationDisplayType.BALLOON,
                    true
                )

                val msg = notificationGroup.createNotification(
                    "Nx workspace migration",
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
