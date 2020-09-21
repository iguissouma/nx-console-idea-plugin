package com.github.iguissouma.nxconsole.buildTools.rc

import com.github.iguissouma.nxconsole.buildTools.NxRunSettings
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.ConsoleView
import com.intellij.javascript.debugger.CommandLineDebugConfigurator
import com.intellij.javascript.nodejs.NodeCommandLineUtil
import com.intellij.javascript.nodejs.NodeConsoleAdditionalFilter
import com.intellij.javascript.nodejs.NodeStackTraceFilter
import com.intellij.javascript.nodejs.debug.NodeLocalDebuggableRunProfileStateSync
import com.intellij.javascript.nodejs.interpreter.NodeCommandLineConfigurator
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreter
import com.intellij.javascript.nodejs.library.yarn.YarnPnpNodePackage
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.lang.javascript.buildTools.TypeScriptErrorConsoleFilter
import com.intellij.openapi.project.Project
import com.intellij.webcore.util.CommandLineUtil
import java.io.File
import java.nio.charset.StandardCharsets

class NxRunProfileState(
    val environment: ExecutionEnvironment,
    val runSettings: NxRunSettings,
    val nxPackage: NodePackage
) : NodeLocalDebuggableRunProfileStateSync() {

    override fun executeSync(configurator: CommandLineDebugConfigurator?): ExecutionResult {
        val interpreter: NodeJsInterpreter = this.runSettings.interpreterRef.resolveNotNull(this.environment.project)
        val commandLine = NodeCommandLineUtil.createCommandLine()
        NodeCommandLineUtil.configureCommandLine(
            commandLine,
            configurator
        ) { debugMode: Boolean? -> this.configureCommandLine(commandLine, interpreter) }
        val processHandler: ProcessHandler = NodeCommandLineUtil.createProcessHandler(commandLine, true)
        ProcessTerminatedListener.attach(processHandler)
        val console: ConsoleView = this.createConsole(processHandler, commandLine.workDirectory)
        console.attachToProcess(processHandler)
        return DefaultExecutionResult(console, processHandler)
    }

    private fun configureCommandLine(commandLine: GeneralCommandLine, interpreter: NodeJsInterpreter) {

        commandLine.withCharset(StandardCharsets.UTF_8)
        CommandLineUtil.setWorkingDirectory(commandLine, File(this.runSettings.nxFilePath).parentFile, false)
        if (this.nxPackage is YarnPnpNodePackage) {
            this.nxPackage.addYarnRunToCommandLine(commandLine, this.environment.project, interpreter, null as String?)
        } else {
            commandLine.addParameter(getNxBinFile(this.nxPackage).absolutePath)
        }

        commandLine.addParameters("run")
        commandLine.addParameters(this.runSettings.tasks)

        NodeCommandLineConfigurator.find(interpreter).configure(commandLine)
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
