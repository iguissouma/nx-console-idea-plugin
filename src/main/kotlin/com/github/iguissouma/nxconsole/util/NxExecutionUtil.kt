package com.github.iguissouma.nxconsole.util

import com.github.iguissouma.nxconsole.cli.config.exe
import com.intellij.execution.ExecutionException
import com.intellij.execution.Platform
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.AnsiEscapeDecoder.ColoredTextAcceptor
import com.intellij.execution.process.CapturingProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutput
import com.intellij.javascript.nodejs.NodeCommandLineUtil
import com.intellij.javascript.nodejs.execution.NodeTargetRun
import com.intellij.javascript.nodejs.interpreter.NodeCommandLineConfigurator
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.javascript.nodejs.interpreter.remote.NodeJsRemoteInterpreter
import com.intellij.javascript.nodejs.npm.NpmManager
import com.intellij.javascript.nodejs.npm.NpmNodePackage
import com.intellij.javascript.nodejs.npm.NpmPackageDescriptor
import com.intellij.javascript.nodejs.npm.NpmUtil
import com.intellij.javascript.nodejs.npm.WorkingDirectoryDependentNpmPackageVersionManager
import com.intellij.lang.javascript.JavaScriptBundle
import com.intellij.lang.javascript.buildTools.npm.rc.NpmCommand
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.util.ThreeState
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.text.SemVer
import org.jetbrains.annotations.NotNull
import java.io.File
import java.util.concurrent.CompletableFuture

class NxExecutionUtil(val project: Project) {

    fun execute(command: String, vararg args: String): Boolean {
        val output = executeAndGetOutput(command, *args)
        return output?.exitCode == 0
    }

    fun executeAndGetOutput(command: String, vararg args: String): ProcessOutput? {
        return getOutput(getProcessHandler(command, *args) ?: return null)
    }

    private inline fun runAsync(crossinline task: () -> Unit): CompletableFuture<Void> {
        return CompletableFuture.runAsync(Runnable { task() }, AppExecutorUtil.getAppExecutorService())
    }

    fun executeAndGetOutputAsync(command: String, args: Array<String>, callback: (ProcessOutput?) -> Unit) {
        val result = getProcessHandler(command, *args) ?: return
        val manager = ProgressManager.getInstance()
        ApplicationManager.getApplication()
            .executeOnPooledThread {
                manager.run(
                    object : Task.Backgroundable(project, command, true) {
                        override fun run(@NotNull indicator: ProgressIndicator) {
                            try {
                                getOutput(processHandler = result)?.let { callback(it) }
                            } finally {
                                indicator.cancel()
                            }
                        }
                    })
            }

    }

    fun getProcessHandler(
        command: String,
        vararg args: String
    ): ProcessHandler? {
        val nodeInterpreter = NodeJsInterpreterManager.getInstance(project).interpreter ?: return null
        NodeCommandLineConfigurator.find(nodeInterpreter)
        val npmPackageRef = NpmUtil.createProjectPackageManagerPackageRef()
        val npmPkg = NpmUtil.resolveRef(npmPackageRef, project, nodeInterpreter)
        val targetRun = NodeTargetRun(
            nodeInterpreter,
            project,
            null,
            NodeTargetRun.createOptions(
                ThreeState.UNSURE,
                listOf(),
                true
            )
        )

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
        } else {

            val commandLine = targetRun.commandLineBuilder
            targetRun.enableWrappingWithYarnPnpNode = false
            NodeCommandLineUtil.prependNodeDirToPATH(targetRun)

            if (targetRun.interpreter !is NodeJsRemoteInterpreter) {
                NpmNodePackage.configureNpmPackage(targetRun, npmPkg, *arrayOfNulls(0))
                val yarn = NpmUtil.isYarnAlikePackage(npmPkg)
                if (NpmUtil.isPnpmPackage(npmPkg)) {
                    var version: SemVer? = null
                    WorkingDirectoryDependentNpmPackageVersionManager.getInstance(project)
                        .fetchVersion(targetRun.interpreter, npmPkg, File(project.basePath!!)) {
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
                    project.basePath?.let { File(it).toPath() },
                    NpmCommand.RUN_SCRIPT,
                    emptyList(),
                    {}
                )
            }

            commandLine.setWorkingDirectory(project.basePath!!)
            commandLine.addParameter("nx")
            if (targetRun.interpreter is NodeJsRemoteInterpreter) {
                commandLine.addParameter("--")
            }
            commandLine.addParameter(command)
            commandLine.addParameters(args.toList())
            val handler: ProcessHandler = targetRun.startProcess()
            return handler
        }
    }

    fun getOutput(processHandler: ProcessHandler): ProcessOutput? {
        val adapter = AnsiEscapesAwareAdapter(ProcessOutput())
        processHandler.addProcessListener(adapter)
        processHandler.startNotify()
        processHandler.waitFor()
        return adapter.output
    }
}

// using String's substring method
fun replacePnpmToPnpx(pnpmPath: String): String {
    return pnpmPath.substring(
        0,
        pnpmPath.lastIndexOf("pnpm")
    ) + "pnpx" + pnpmPath.substring(pnpmPath.lastIndexOf("pnpm") + 1)
}

fun replaceNpmToNpx(pnpmPath: String): String {
    return pnpmPath.substring(
        0,
        pnpmPath.lastIndexOf("npm")
    ) + "npx" + pnpmPath.substring(pnpmPath.lastIndexOf("npm") + 3)
}

fun replaceNpmCliJsFilePathToNpx(npmCLiJsFilePath: String): String {
    return npmCLiJsFilePath.replace("npm-cli.js", "npx" + if (Platform.current() == Platform.WINDOWS) ".cmd" else "")
}


class AnsiEscapesAwareAdapter(output: ProcessOutput?) :
    CapturingProcessAdapter(output!!), ColoredTextAcceptor {
    private val myAnsiEscapeDecoder: AnsiEscapeDecoder = object : AnsiEscapeDecoder() {
        override fun getCurrentOutputAttributes(outputType: Key<*>): Key<*> {
            return outputType
        }
    }

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        myAnsiEscapeDecoder.escapeText(event.text, outputType, this)
    }

    override fun coloredTextAvailable(text: String, attributes: Key<*>) {
        addToOutput(text, attributes)
    }
}
