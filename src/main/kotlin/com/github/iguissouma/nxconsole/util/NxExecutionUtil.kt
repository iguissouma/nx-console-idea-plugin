package com.github.iguissouma.nxconsole.util

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessOutput
import com.intellij.javascript.nodejs.CompletionModuleInfo
import com.intellij.javascript.nodejs.NodeModuleSearchUtil
import com.intellij.javascript.nodejs.interpreter.NodeCommandLineConfigurator
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.openapi.project.Project
import java.io.File

class NxExecutionUtil(val project: Project) {

    fun execute(command: String, vararg args: String): Boolean {
        val output = executeAndGetOutput(command, *args)
        return output?.exitCode == 0
    }

    fun executeAndGetOutput(command: String, vararg args: String): ProcessOutput? {
        val nodeJsInterpreter = NodeJsInterpreterManager.getInstance(project).interpreter ?: return null
        val configurator = NodeCommandLineConfigurator.find(nodeJsInterpreter)
        val modules: MutableList<CompletionModuleInfo> = mutableListOf()
        NodeModuleSearchUtil.findModulesWithName(
            modules,
            "@nrwl/cli",
            project.baseDir, // TODO change deprecation
            null
        )
        val module = modules.firstOrNull() ?: return null
        val moduleExe =
            "${module.virtualFile!!.path}${File.separator}bin${File.separator}nx"
        val commandLine =
            GeneralCommandLine("", moduleExe, command, *args)
        commandLine.withWorkDirectory(project.basePath)
        configurator.configure(commandLine)
        val handler = CapturingProcessHandler(commandLine)
        return handler.runProcess()
    }
}
