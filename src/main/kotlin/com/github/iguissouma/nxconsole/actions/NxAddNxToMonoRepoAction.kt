package com.github.iguissouma.nxconsole.actions

import com.github.iguissouma.nxconsole.NxIcons
import com.github.iguissouma.nxconsole.cli.NxCliFilter
import com.intellij.execution.ExecutionException
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.execution.ui.RunnerLayoutUi
import com.intellij.ide.file.BatchFileChangeListener
import com.intellij.javascript.debugger.CommandLineDebugConfigurator
import com.intellij.javascript.nodejs.NodeCommandLineUtil
import com.intellij.javascript.nodejs.execution.NodeTargetRun
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.javascript.nodejs.npm.NpmManager
import com.intellij.javascript.nodejs.npm.NpmNodePackage
import com.intellij.javascript.nodejs.npm.NpmPackageDescriptor
import com.intellij.javascript.nodejs.npm.NpmUtil
import com.intellij.javascript.nodejs.packageJson.PackageJsonDependenciesExternalUpdateManager
import com.intellij.json.psi.JsonFile
import com.intellij.lang.javascript.JavaScriptBundle
import com.intellij.lang.javascript.buildTools.npm.PackageJsonUtil.findChildPackageJsonFile
import com.intellij.lang.javascript.buildTools.npm.PackageJsonUtil.findDependencyByName
import com.intellij.lang.javascript.buildTools.npm.PackageJsonUtil.isPackageJsonWithTopLevelProperty
import com.intellij.lang.javascript.modules.ConsoleProgress
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.progress.util.BackgroundTaskUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.psi.PsiManager
import com.intellij.util.ThreeState
import java.util.List
import javax.swing.Icon

class NxAddNxToMonoRepoAction : DumbAwareAction({ "Add Nx to MonoRepo" }, NxIcons.NRWL_ICON) {

    companion object {
        private val GENERATING = Key.create<kotlin.Boolean>(
            NxAddNxToMonoRepoAction::class.java.simpleName + ".generating"
        )
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val done = PackageJsonDependenciesExternalUpdateManager.getInstance(project)
            .externalUpdateStarted(null, null)
        GENERATING[project] = java.lang.Boolean.TRUE
        val useConsoleViewImpl = java.lang.Boolean.getBoolean("npm.project.generators.ConsoleViewImpl")
        val node = NodeJsInterpreterManager.getInstance(project).interpreter ?: return
        val targetRun = NodeTargetRun(
            node,
            project,
            null as CommandLineDebugConfigurator?,
            NodeTargetRun.createOptions(if (useConsoleViewImpl) ThreeState.NO else ThreeState.YES, List.of())
        )
        val commandLine = targetRun.commandLineBuilder
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
        NpmPackageDescriptor.findBinaryFilePackage(node, "npx")?.configureNpmPackage(targetRun)
        commandLine.addParameter("add-nx-to-monorepo")
        val workingDir = project.baseDir
        commandLine.setWorkingDirectory(targetRun.path(workingDir.path))
        NodeCommandLineUtil.prependNodeDirToPATH(targetRun)

        val handler = targetRun.startProcess()
        val console = NodeCommandLineUtil.createConsole(handler, project, false)
        val baseDir = project.baseDir
        console.addMessageFilter(NxCliFilter(project, baseDir.path))
        console.attachToProcess(handler)
        ConsoleProgress.install(console, handler)
        BackgroundTaskUtil.syncPublisher(BatchFileChangeListener.TOPIC)
            .batchChangeStarted(project, JavaScriptBundle.message("project.generation", *arrayOfNulls(0)))
        handler.addProcessListener(object : ProcessAdapter() {
            override fun processTerminated(event: ProcessEvent) {
                baseDir.refresh(false, true)
                baseDir.children
                handler.notifyTextAvailable("Done\n", ProcessOutputTypes.SYSTEM)
                done.run()
                GENERATING.set(project, null)
                BackgroundTaskUtil.syncPublisher(BatchFileChangeListener.TOPIC).batchChangeCompleted(project)
            }
        })
        val defaultExecutor = DefaultRunExecutor.getRunExecutorInstance()
        val title = "Add Nx to MonoRepo"
        val ui = RunnerLayoutUi.Factory.getInstance(project).create("none", title, title, project)
        val consoleContent =
            ui.createContent("none", console.component, title, null as Icon?, console.preferredFocusableComponent)
        ui.addContent(consoleContent)
        val descriptor = RunContentDescriptor(console, handler, console.component, title)
        descriptor.isAutoFocusContent = true
        RunContentManager.getInstance(project).showRunContent(defaultExecutor, descriptor)
        handler.startNotify()
    }


    class MyStartupActivity : StartupActivity.Background {

        override fun runActivity(project: Project) {
            val packageJsonFile = findChildPackageJsonFile(project.baseDir)
            if (packageJsonFile != null) {
                invokeLater {
                    val psiFile = PsiManager.getInstance(project).findFile(packageJsonFile) as? JsonFile ?: return@invokeLater
                    val isWorkspace = isPackageJsonWithTopLevelProperty(packageJsonFile, "workspaces")
                    if (isWorkspace && hasNx(psiFile).not()) {
                        val notificationGroup = NotificationGroup(
                            "nx.notifications.balloon",
                            NotificationDisplayType.BALLOON,
                            true
                        )
                        //Nx is not installed in your MonoRepo workspace. Do you want to add it?
                        val msg = notificationGroup.createNotification(
                            "Add Nx to monorepo",
                            "You can use Nx easily together with your current Lerna/Yarn/PNPM/NPM monorepo setup. " +
                                    "Why? To speed up your tasks by leveraging Nx's powerful scheduling and caching capabilities." + createLink(
                                "https://nx.dev/migration/adding-to-monorepo",
                                " Learn more"
                            ),
                            NotificationType.INFORMATION,
                        )
                        msg.addAction(NxAddNxToMonoRepoAction())
                        msg.notify(project)
                    }
                }

            }
        }

        private fun hasNx(psiFile: JsonFile): Boolean {
            return findDependencyByName(psiFile, "nx") != null
                    || findDependencyByName(psiFile, "@nrwl/tao") != null
        }

    }
}
