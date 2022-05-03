package com.github.iguissouma.nxconsole.buildTools.rc

import com.github.iguissouma.nxconsole.buildTools.NxRunSettings
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.ConsoleView
import com.intellij.javascript.debugger.CommandLineDebugConfigurator
import com.intellij.javascript.nodejs.NodeCommandLineUtil
import com.intellij.javascript.nodejs.NodeConsoleAdditionalFilter
import com.intellij.javascript.nodejs.NodeStackTraceFilter
import com.intellij.javascript.nodejs.debug.NodeCommandLineOwner
import com.intellij.javascript.nodejs.execution.NodeBaseRunProfileState
import com.intellij.javascript.nodejs.execution.NodeTargetRun
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreter
import com.intellij.javascript.nodejs.npm.NpmManager
import com.intellij.javascript.nodejs.npm.NpmManager.InvalidNpmPackageException
import com.intellij.javascript.nodejs.npm.NpmNodePackage
import com.intellij.javascript.nodejs.npm.NpmPackageDescriptor
import com.intellij.javascript.nodejs.npm.NpmUtil
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.javascript.nodejs.util.NodePackageRef
import com.intellij.lang.javascript.JavaScriptBundle
import com.intellij.lang.javascript.buildTools.TypeScriptErrorConsoleFilter
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.util.ThreeState
import com.intellij.util.execution.ParametersListUtil
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Path

class NxRunProfileState(
    val environment: ExecutionEnvironment,
    val runSettings: NxRunSettings,
    val nxPackage: NodePackage
) : NodeBaseRunProfileState, NodeCommandLineOwner {

    override fun startProcess(configurator: CommandLineDebugConfigurator?): ProcessHandler {
        val nodeInterpreter: NodeJsInterpreter =
            this.runSettings.interpreterRef.resolveNotNull(this.environment.project)
        val project = this.environment.project
        val targetRun = NodeTargetRun(
            nodeInterpreter,
            project,
            configurator,
            NodeTargetRun.createOptions(
                ThreeState.UNSURE,
                listOf(),
                true,
                null,
                this.environment.runProfile as NxRunConfiguration
            )
        )
        configureNodeTargetRun(targetRun, this.runSettings)
        return targetRun.startProcess()
    }

    private fun configureNodeTargetRun(targetRun: NodeTargetRun, runSettings: NxRunSettings) {
        val envData = this.runSettings.envData
        val npmPackageRef = this.runSettings.packageManagerPackageRef
        configureCommandLine(targetRun, npmPackageRef, envData)
    }

    override fun createExecutionResult(processHandler: ProcessHandler): ExecutionResult {

        ProcessTerminatedListener.attach(processHandler)
        val console = createConsole(
            // processHandler, File(this.runSettings.getPackageJsonSystemDependentPath())
            processHandler,
            File(this.runSettings.nxFileSystemIndependentPath!!)
                .parentFile
        )
        console.attachToProcess(processHandler)
        foldCommandLine(console, processHandler)
        return DefaultExecutionResult(console, processHandler)
    }

    private fun configureCommandLine(
        targetRun: NodeTargetRun,
        npmPackageRef: NodePackageRef,
        envData: EnvironmentVariablesData
    ) {
        targetRun.enableWrappingWithYarnPnpNode = false
        val commandLine = targetRun.commandLineBuilder
        commandLine.setCharset(StandardCharsets.UTF_8)
        val workingDirectory: File = File(this.runSettings.nxFilePath).parentFile
        commandLine.setWorkingDirectory(targetRun.path(workingDirectory.absolutePath))
        targetRun.enableWrappingWithYarnPnpNode = false
        targetRun.configureEnvironment(envData)
        // commandLine.addParameters(ParametersListUtil.parse(nodeOptions.trim { it <= ' ' }))

        // configureNpmCommand
        val npmPkg = NpmUtil.resolveRef(npmPackageRef, targetRun.project, targetRun.interpreter)
        if (npmPkg == null) {
            if (NpmUtil.isProjectPackageManagerPackageRef(npmPackageRef)) {
                val message = JavaScriptBundle.message(
                    "npm.dialog.message.cannot.resolve.package.manager",
                    NpmManager.getInstance(targetRun.project).packageRef.identifier
                )
                throw InvalidNpmPackageException(
                    targetRun.project,
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
        } else {
            configureNxCommand(targetRun, npmPkg, workingDirectory.toPath())
        }

        val yarn = NpmUtil.isYarnAlikePackage(npmPkg)
        if (NpmUtil.isPnpmPackage(npmPkg)) {
            if (npmPkg.version != null && npmPkg.version!!.major >= 6 && npmPkg.version!!.minor >= 13) {
                // useExec like vscode extension
                commandLine.addParameter("exec")
            } else {
                NpmPackageDescriptor.findBinaryFilePackage(targetRun.interpreter, "pnpx")?.configureNpmPackage(targetRun)
            }
        } else if (yarn.not()) {
            NpmPackageDescriptor.findBinaryFilePackage(targetRun.interpreter, "npx")?.configureNpmPackage(targetRun)
        }
        commandLine.addParameter("nx")
        val tasks = this.runSettings.tasks
        if (tasks.size > 1) {
            commandLine.addParameters("run-many")
            val target = tasks.first().substringAfter(":")
            commandLine.addParameter("--target=$target")
            commandLine.addParameters("--projects=" + tasks.joinToString(",") { it.substringBefore(":") })
        } else {
            commandLine.addParameters("run")
            commandLine.addParameters(tasks.firstOrNull() ?: "")
        }

        commandLine.addParameters(this.runSettings.arguments?.let { ParametersListUtil.parse(it) } ?: emptyList())
        // val nodeModuleBinPath =
        //    workingDirectory.path + File.separator + "node_modules" + File.separator + ".bin"
        // val shellPath = EnvironmentUtil.getValue("PATH")
        // val separator = if (SystemInfo.isWindows) ";" else ":"
        // commandLine.addEnvironmentVariable("PATH", listOfNotNull(WslPath.parseWindowsUncPath(nodeModuleBinPath)?.linuxPath).joinToString(separator = separator))
    }

    fun configureNxCommand(
        targetRun: NodeTargetRun,
        npmPackage: NodePackage,
        workingDirectory: Path?,
    ) {

        val commandLineBuilder = targetRun.commandLineBuilder
        if (workingDirectory != null) {
            commandLineBuilder.setWorkingDirectory(targetRun.path(workingDirectory.toString()))
        }
        targetRun.enableWrappingWithYarnPnpNode = false
        NpmNodePackage.configureNpmPackage(targetRun, npmPackage, *arrayOfNulls(0))
        NodeCommandLineUtil.prependNodeDirToPATH(targetRun)
    }

    private fun createConsole(processHandler: ProcessHandler, cwd: File?): ConsoleView {
        val project: Project = this.environment.project
        val consoleView = NodeCommandLineUtil.createConsole(processHandler, project, false)
        consoleView.addMessageFilter(NodeStackTraceFilter(project, cwd))
        consoleView.addMessageFilter(NodeConsoleAdditionalFilter(project, cwd))
        consoleView.addMessageFilter(TypeScriptErrorConsoleFilter(project, cwd))
        return consoleView
    }
}
