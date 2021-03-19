package com.github.iguissouma.nxconsole.schematics

import com.github.iguissouma.nxconsole.cli.NxCliProjectGenerator
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.javascript.nodejs.interpreter.NodeCommandLineConfigurator
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.lang.javascript.service.JSLanguageServiceUtil
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

private var myLogErrors: ThreadLocal<Boolean> = ThreadLocal.withInitial { true }
private val LOG: Logger = Logger.getInstance("#NxSchematicsLoader.kt")

fun doLoad(project: Project, cli: VirtualFile, includeHidden: Boolean, logErrors: Boolean): List<Schematic> {
    myLogErrors.set(logErrors)
    val interpreter = NodeJsInterpreterManager.getInstance(project).interpreter ?: return emptyList()
    val configurator: NodeCommandLineConfigurator
    try {
        configurator = NodeCommandLineConfigurator.find(interpreter)
    } catch (e: Exception) {
        LOG.error("Cannot load schematics", e)
        return emptyList()
    }

    var parse: Collection<Schematic> = emptyList()

    val schematicsInfoJson = loadSchematicsInfoJson(configurator, cli, includeHidden)
    if (schematicsInfoJson.isNotEmpty() && !schematicsInfoJson.startsWith("No schematics")) {
        try {
            parse = NxSchematicsJsonParser.parse(schematicsInfoJson).toSchematic()
        } catch (e: Exception) {
            LOG.error("Failed to parse schematics: " + e.message, e, Attachment("output", schematicsInfoJson))
        }
    }

    return parse.sortedBy { it.name }
}

private fun loadSchematicsInfoJson(
    configurator: NodeCommandLineConfigurator,
    cli: VirtualFile,
    includeHidden: Boolean
): String {
    val directory = JSLanguageServiceUtil.getPluginDirectory(NxCliProjectGenerator::class.java, "nxCli")
    val utilityExe = "${directory}${File.separator}runner.js"
    val commandLine = GeneralCommandLine("", utilityExe, cli.path, "./schematicsInfoProvider.js")
    if (includeHidden)
        commandLine.addParameter("--includeHidden")
    configurator.configure(commandLine)
    return grabCommandOutput(commandLine, directory.path)
}

private fun grabCommandOutput(commandLine: GeneralCommandLine, workingDir: String?): String {
    if (workingDir != null) {
        commandLine.withWorkDirectory(workingDir)
    }
    val handler = CapturingProcessHandler(commandLine)
    val output = handler.runProcess()

    if (output.exitCode == 0) {
        if (output.stderr.trim().isNotEmpty()) {
            if (myLogErrors.get()) {
                LOG.error(
                    "Error while loading schematics info.\n" +
                        shortenOutput(output.stderr),
                    Attachment("err-output", output.stderr)
                )
            } else {
                LOG.info(
                    "Error while loading schematics info.\n" +
                        shortenOutput(output.stderr)
                )
            }
        }
        return output.stdout
    } else if (myLogErrors.get()) {
        LOG.error(
            "Failed to load schematics info.\n" +
                shortenOutput(output.stderr),
            Attachment("err-output", output.stderr),
            Attachment("std-output", output.stdout)
        )
    } else {
        LOG.info(
            "Error while loading schematics info.\n" +
                shortenOutput(output.stderr)
        )
    }
    return ""
}

private fun shortenOutput(output: String): String {
    return StringUtil.shortenTextWithEllipsis(
        output.replace('\\', '/')
            .replace("(/[^()/:]+)+(/[^()/:]+)(/[^()/:]+)".toRegex(), "/...$1$2$3"),
        750,
        0
    )
}
