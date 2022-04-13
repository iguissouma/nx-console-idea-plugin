package com.github.iguissouma.nxconsole.buildTools.rc

import com.github.iguissouma.nxconsole.NxBundle
import com.github.iguissouma.nxconsole.buildTools.NxRunSettings
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.wsl.WslPath
import com.intellij.javascript.debugger.CommandLineDebugConfigurator
import com.intellij.javascript.nodejs.NodeCommandLineUtil
import com.intellij.javascript.nodejs.NodeConsoleAdditionalFilter
import com.intellij.javascript.nodejs.NodeStackTraceFilter
import com.intellij.javascript.nodejs.debug.NodeCommandLineOwner
import com.intellij.javascript.nodejs.execution.NodeBaseRunProfileState
import com.intellij.javascript.nodejs.execution.NodeTargetRun
import com.intellij.javascript.nodejs.interpreter.NodeCommandLineConfigurator
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreter
import com.intellij.javascript.nodejs.npm.NpmUtil
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.javascript.nodejs.util.NodePackageRef
import com.intellij.lang.javascript.buildTools.TypeScriptErrorConsoleFilter
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.EnvironmentUtil
import com.intellij.util.ThreeState
import com.intellij.util.execution.ParametersListUtil
import com.intellij.webcore.util.CommandLineUtil
import java.io.File
import java.nio.charset.StandardCharsets

class NxRunProfileState(
    val environment: ExecutionEnvironment,
    val runSettings: NxRunSettings,
    val nxPackage: NodePackage
) : NodeBaseRunProfileState, NodeCommandLineOwner {

    override fun startProcess(configurator: CommandLineDebugConfigurator?): ProcessHandler {
        val nodeInterpreter: NodeJsInterpreter =
            this.runSettings.interpreterRef.resolveNotNull(this.environment.project)
        val envData = this.runSettings.envData
        val npmPackageRef = this.runSettings.packageManagerPackageRef
        val project = this.environment.project
        return if (true) {
            val targetRun = NodeTargetRun(
                nodeInterpreter,
                project,
                configurator,
                NodeTargetRun.createOptions(ThreeState.UNSURE, listOf())
            )
            configureCommandLine(targetRun, npmPackageRef, envData)
            targetRun.startProcess()
        } else {
            startProcessOld(configurator)
        }
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

    private fun startProcessOld(configurator: CommandLineDebugConfigurator?): ProcessHandler {
        val nodeInterpreter: NodeJsInterpreter =
            this.runSettings.interpreterRef.resolveNotNull(this.environment.project)
        val envData = this.runSettings.envData
        val npmPackageRef = this.runSettings.packageManagerPackageRef
        val project = this.environment.project
        val commandLine = NodeCommandLineUtil.createCommandLine(true)
        NodeCommandLineUtil.configureCommandLine(
            commandLine, configurator, nodeInterpreter
        ) { debugMode: Boolean? ->
            this.configureCommandLine(
                commandLine, nodeInterpreter, npmPackageRef,
                project, envData
            )
        }
        return NodeCommandLineUtil.createProcessHandler(commandLine, true)
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
        targetRun.configureEnvironment(envData)
        // commandLine.addParameters(ParametersListUtil.parse(nodeOptions.trim { it <= ' ' }))
        val pkg = NpmUtil.resolveRef(npmPackageRef, targetRun.project, targetRun.interpreter)
        if (pkg == null) {
            throw ExecutionException(
                NxBundle.message(
                    "nx.npm.dialog.message.cannot.resolve.package.manager",
                    npmPackageRef.identifier
                )
            )
        } else {
            if (NpmUtil.isYarnAlikePackage(pkg) || NpmUtil.isPnpmPackage(pkg)) {
                commandLine.addParameter(NpmUtil.getValidNpmCliJsFilePath(pkg, targetRun.interpreter))
                commandLine.addParameter("nx")
            } else {
                commandLine.addParameter(
                    WslPath.parseWindowsUncPath(getNxBinFile(nxPackage).absolutePath)?.linuxPath
                        ?: getNxBinFile(nxPackage).absolutePath
                )
            }
        }

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

    private fun configureCommandLine(
        commandLine: GeneralCommandLine,
        nodeInterpreter: NodeJsInterpreter,
        npmPackageRef: NodePackageRef,
        project: Project,
        envData: EnvironmentVariablesData
    ) {

        commandLine.withCharset(StandardCharsets.UTF_8)
        CommandLineUtil.setWorkingDirectory(commandLine, File(this.runSettings.nxFilePath).parentFile, false)

        val pkg = NpmUtil.resolveRef(npmPackageRef, project, nodeInterpreter)
        if (pkg == null) {
            throw ExecutionException(
                NxBundle.message(
                    "nx.npm.dialog.message.cannot.resolve.package.manager",
                    npmPackageRef.identifier
                )
            )
        } else {
            if (NpmUtil.isYarnAlikePackage(pkg) || NpmUtil.isPnpmPackage(pkg)) {
                commandLine.addParameter(NpmUtil.getValidNpmCliJsFilePath(pkg, nodeInterpreter))
                commandLine.addParameter("nx")
            } else {
                commandLine.addParameter(getNxBinFile(nxPackage).absolutePath)
            }
        }

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
        envData.configureCommandLine(commandLine, true)
        NodeCommandLineUtil.configureUsefulEnvironment(commandLine)
        val nodeModuleBinPath =
            commandLine.workDirectory.path + File.separator + "node_modules" + File.separator + ".bin"
        val shellPath = EnvironmentUtil.getValue("PATH")
        val separator = if (SystemInfo.isWindows) ";" else ":"
        commandLine.environment["PATH"] =
            listOfNotNull(shellPath, nodeModuleBinPath).joinToString(separator = separator)
        NodeCommandLineConfigurator.find(nodeInterpreter).configure(commandLine)
    }

    private fun getNxBinFile(nxPackage: NodePackage): File {
        return File(nxPackage.systemDependentPath, "bin" + File.separator + "nx.js")
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
