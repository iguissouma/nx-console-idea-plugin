package com.github.iguissouma.nxconsole.builders

import com.github.iguissouma.nxconsole.cli.NxCliProjectGenerator
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.javascript.nodejs.interpreter.NodeCommandLineConfigurator
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.lang.javascript.service.JSLanguageServiceUtil
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import com.intellij.execution.util.ExecUtil
import com.intellij.execution.ExecutionException;

import com.intellij.execution.process.ProcessOutput

import com.intellij.util.concurrency.AppExecutorUtil

import java.util.concurrent.CompletableFuture




private var myLogErrors: ThreadLocal<Boolean> = ThreadLocal.withInitial { true }
private val LOG: Logger = Logger.getInstance("#NxSchematicsLoader.kt")

fun doLoad(project: Project, cli: VirtualFile, builderName: String, logErrors: Boolean): List<NxBuilderOptions> {
    myLogErrors.set(logErrors)
    val interpreter = NodeJsInterpreterManager.getInstance(project).interpreter ?: return emptyList()
    val configurator: NodeCommandLineConfigurator
    try {
        configurator = NodeCommandLineConfigurator.find(interpreter)
    } catch (e: Exception) {
        LOG.error("Cannot load schematics", e)
        return emptyList()
    }

    var parse: Collection<NxBuilderOptions> = emptyList()

    val buildersInfoJson = loadBuildersInfoJson(configurator, cli, builderName)
    if (buildersInfoJson.isNotEmpty() && !buildersInfoJson.startsWith("No builders")) {
        try {
            parse = NxBuilderOptionsJsonParser.parse(buildersInfoJson)
        } catch (e: Exception) {
            LOG.error("Failed to parse builders: " + e.message, e, Attachment("output", buildersInfoJson))
        }
    }

    return parse.toList()//.sortedBy { it.name }
}

class NxBuilderOptions {
    var name: String = ""
    var description: String = ""
    var type: String = ""
    var required: Boolean = false
    var default: String = ""
    var enum: List<String> = emptyList()
    var aliases: List<String> = emptyList()
    var hidden: Boolean = false

    override fun toString(): String {
        return "NxBuilderOptions(name='$name', description='$description', type='$type', required=$required, aliases=$aliases, hidden=$hidden)"
    }


}

private fun loadBuildersInfoJson(
    configurator: NodeCommandLineConfigurator,
    cli: VirtualFile,
    builderName: String
): String {
    val directory = JSLanguageServiceUtil.getPluginDirectory(NxCliProjectGenerator::class.java, "nxCli")
    val utilityExe = "${directory}${File.separator}runner.js"
    val commandLine = GeneralCommandLine("", utilityExe, cli.path, "./buildersInfoProvider.js", builderName)
    // commandLine.addParameter(builderName)
    configurator.configure(commandLine)
    return grabCommandOutput(commandLine, cli.parent.path)
}

private fun grabCommandOutput(commandLine: GeneralCommandLine, workingDir: String?): String {
    if (workingDir != null) {
        commandLine.withWorkDirectory(workingDir)
    }
    val output = execAndGetOutput(commandLine).get()

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

/**
 * Execute the given command line, and return the process output as one result in a future.
 *
 *
 * This is a non-blocking equivalient to [ExecUtil.execAndGetOutput].
 */
fun execAndGetOutput(cmd: GeneralCommandLine): CompletableFuture<ProcessOutput> {
    val future = CompletableFuture<ProcessOutput>()
    AppExecutorUtil.getAppExecutorService().submit {
        try {
            val output = ExecUtil.execAndGetOutput(cmd)
            future.complete(output)
        } catch (e: ExecutionException) {
            future.completeExceptionally(e)
        }
    }
    return future
}
