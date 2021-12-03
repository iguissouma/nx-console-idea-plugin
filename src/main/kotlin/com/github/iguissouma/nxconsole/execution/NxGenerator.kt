package com.github.iguissouma.nxconsole.execution

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
import com.intellij.javascript.nodejs.NodeCommandLineUtil
import com.intellij.javascript.nodejs.interpreter.NodeCommandLineConfigurator
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreter
import com.intellij.javascript.nodejs.interpreter.local.NodeJsLocalInterpreter
import com.intellij.javascript.nodejs.npm.NpmManager
import com.intellij.javascript.nodejs.npm.NpmUtil
import com.intellij.javascript.nodejs.packageJson.PackageJsonDependenciesExternalUpdateManager
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.lang.javascript.JavaScriptBundle
import com.intellij.lang.javascript.modules.ConsoleProgress
import com.intellij.openapi.progress.util.BackgroundTaskUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Function
import com.intellij.util.PathUtil
import org.jetbrains.annotations.Nls
import java.io.File
import java.lang.Boolean
import java.nio.charset.StandardCharsets
import javax.swing.Icon
import kotlin.Array
import kotlin.String
import kotlin.arrayOfNulls

class NxGenerator {

    companion object {
        private val GENERATING = Key.create<kotlin.Boolean>(
            NxGenerator::class.java.simpleName + ".generating"
        )
    }

    fun generate(
        node: NodeJsInterpreter,
        pkg: NodePackage,
        binFilePathProvider: Function<in NodePackage?, String?>,
        baseDir: VirtualFile,
        workingDir: File,
        project: Project,
        callback: Runnable?,
        title: @Nls String,
        filters: Array<Filter>?,
        vararg args: String?
    ): ProcessHandler {

        val done = PackageJsonDependenciesExternalUpdateManager.getInstance(project)
            .externalUpdateStarted(null as VirtualFile?, null as String?)
        NxGenerator.GENERATING[project] = Boolean.TRUE
        val configurator = NodeCommandLineConfigurator.find(node)
        val useConsoleViewImpl = Boolean.getBoolean("npm.project.generators.ConsoleViewImpl")
        val commandLine = NodeCommandLineUtil.createCommandLine(!useConsoleViewImpl)
        val arguments: MutableList<String?> = mutableListOf()

        val nodePackage = NpmManager.getInstance(project).`package` ?: error("cannot find npm package manager")
        if (NpmUtil.isYarnAlikePackage(nodePackage) || NpmUtil.isPnpmPackage(nodePackage)) {
            val npmCliJsFilePath = NpmUtil.getValidNpmCliJsFilePath(nodePackage, node)
            commandLine.addParameter(npmCliJsFilePath)
            commandLine.addParameter("nx")
        } else {
            commandLine.addParameter(getNxBinFile(pkg).absolutePath)
        }

        commandLine.addParameters(arguments)
        commandLine.addParameters(args.toList())
        commandLine.setWorkDirectory(workingDir.path)
        commandLine.charset = StandardCharsets.UTF_8
        NodeCommandLineUtil.configureUsefulEnvironment(commandLine)
        if (node is NodeJsLocalInterpreter) {
            val bin = PathUtil.getParentPath(node.interpreterSystemDependentPath)
            var path = commandLine.parentEnvironment["PATH"]
            path = if (StringUtil.isEmpty(path)) bin else bin + File.pathSeparatorChar + path
            commandLine.environment["PATH"] = path
        }
        configurator.configure(commandLine)
        val handler: ProcessHandler = NodeCommandLineUtil.createProcessHandler(commandLine, false)
        val console = NodeCommandLineUtil.createConsole(handler, project, false)
        val var18 = filters!!.size
        for (var19 in 0 until var18) {
            val filter = filters[var19]
            console.addMessageFilter(filter)
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
