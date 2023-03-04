package com.github.iguissouma.nxconsole.execution

import com.github.iguissouma.nxconsole.cli.config.exe
import com.github.iguissouma.nxconsole.util.replaceNpmCliJsFilePathToNpx
import com.github.iguissouma.nxconsole.util.replaceNpmToNpx
import com.github.iguissouma.nxconsole.util.replacePnpmToPnpx
import com.intellij.execution.ExecutionException
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.filters.Filter
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.execution.ui.RunnerLayoutUi
import com.intellij.ide.file.BatchFileChangeListener
import com.intellij.javascript.debugger.CommandLineDebugConfigurator
import com.intellij.javascript.nodejs.NodeCommandLineUtil
import com.intellij.javascript.nodejs.execution.NodeTargetRun
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreter
import com.intellij.javascript.nodejs.interpreter.remote.NodeJsRemoteInterpreter
import com.intellij.javascript.nodejs.npm.NpmManager
import com.intellij.javascript.nodejs.npm.NpmNodePackage
import com.intellij.javascript.nodejs.npm.NpmPackageDescriptor
import com.intellij.javascript.nodejs.npm.NpmUtil
import com.intellij.javascript.nodejs.npm.WorkingDirectoryDependentNpmPackageVersionManager
import com.intellij.javascript.nodejs.packageJson.PackageJsonDependenciesExternalUpdateManager
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.lang.javascript.JavaScriptBundle
import com.intellij.lang.javascript.buildTools.npm.rc.NpmCommand
import com.intellij.lang.javascript.modules.ConsoleProgress
import com.intellij.openapi.progress.util.BackgroundTaskUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ThreeState
import com.intellij.util.text.SemVer
import org.jetbrains.annotations.Nls
import java.io.File
import java.lang.Boolean
import java.util.List
import javax.swing.Icon
import kotlin.Array
import kotlin.String
import kotlin.arrayOfNulls
import kotlin.let

class NxGenerator {

    companion object {
        private val GENERATING = Key.create<kotlin.Boolean>(
            NxGenerator::class.java.simpleName + ".generating"
        )
    }

    fun generate(
        node: NodeJsInterpreter,
        nxExe: String = "nx",
        baseDir: VirtualFile,
        workingDir: File,
        project: Project,
        callback: Runnable?,
        title: @Nls String,
        filters: Array<Filter>?,
        vararg args: String?
    ): ProcessHandler {

        val done = PackageJsonDependenciesExternalUpdateManager.getInstance(project)
            .externalUpdateStarted(null, null)
        GENERATING[project] = Boolean.TRUE
        val useConsoleViewImpl = Boolean.getBoolean("npm.project.generators.ConsoleViewImpl")
        val targetRun = NodeTargetRun(
            node,
            project,
            null as CommandLineDebugConfigurator?,
            NodeTargetRun.createOptions(if (useConsoleViewImpl) ThreeState.NO else ThreeState.YES, List.of())
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
        NodeCommandLineUtil.prependNodeDirToPATH(targetRun)

        if (targetRun.interpreter !is NodeJsRemoteInterpreter) {

            NpmNodePackage.configureNpmPackage(targetRun, npmPkg, *arrayOfNulls(0))
            val yarn = NpmUtil.isYarnAlikePackage(npmPkg)
            if (NpmUtil.isPnpmPackage(npmPkg)) {
                var version: SemVer? = null
                WorkingDirectoryDependentNpmPackageVersionManager.getInstance(project)
                    .fetchVersion(node, npmPkg, workingDir) {
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
                val findBinaryFilePackage = NpmPackageDescriptor.findBinaryFilePackage(targetRun.interpreter, "npx")
                if (findBinaryFilePackage != null) {
                    findBinaryFilePackage.configureNpmPackage(targetRun)
                } else {
                    val validNpmCliJsFilePath = NpmUtil.getValidNpmCliJsFilePath(npmPkg, targetRun.interpreter)
                    NpmNodePackage(replaceNpmCliJsFilePathToNpx(validNpmCliJsFilePath))
                        .let {
                            if (it.isValid(targetRun.project, targetRun.interpreter)) {
                                it.configureNpmPackage(targetRun)
                            }
                        }
                }
            }
        } else {
            NpmUtil.configureNpmCommand(
                targetRun,
                npmPackageRef,
                workingDir.toPath(),
                NpmCommand.RUN_SCRIPT,
                emptyList(),
                {}
            )
        }

        commandLine.addParameter(nxExe)
        if (targetRun.interpreter is NodeJsRemoteInterpreter) {
            commandLine.addParameter("--")
        }
        commandLine.addParameters(arguments.filterNotNull())
        commandLine.addParameters(args.filterNotNull().toList())
        commandLine.setWorkingDirectory(targetRun.path(workingDir.path))
        NodeCommandLineUtil.prependNodeDirToPATH(targetRun)

        val handler = targetRun.startProcess()
        val console = NodeCommandLineUtil.createConsole(handler, project, false)
        filters?.forEach {
            console.addMessageFilter(it)
        }
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
                callback?.run()
                BackgroundTaskUtil.syncPublisher(BatchFileChangeListener.TOPIC).batchChangeCompleted(project)
            }
        })
        val defaultExecutor = DefaultRunExecutor.getRunExecutorInstance()
        val ui = RunnerLayoutUi.Factory.getInstance(project).create("none", title, title, project)
        val consoleContent =
            ui.createContent("none", console.component, title, null as Icon?, console.preferredFocusableComponent)
        ui.addContent(consoleContent)
        val descriptor = RunContentDescriptor(console, handler, console.component, title)
        descriptor.isAutoFocusContent = true
        RunContentManager.getInstance(project).showRunContent(defaultExecutor, descriptor)
        handler.startNotify()
        return handler
    }

    fun getNxBinFile(nxPackage: NodePackage): File {
        return File(nxPackage.systemDependentPath, "bin" + File.separator + "nx.js")
    }
}
