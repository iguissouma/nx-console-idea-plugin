package com.github.iguissouma.nxconsole.execution

import com.github.iguissouma.nxconsole.NxIcons
import com.github.iguissouma.nxconsole.cli.NxCliFilter
import com.github.iguissouma.nxconsole.cli.config.NxConfigProvider
import com.github.iguissouma.nxconsole.cli.config.exe
import com.github.iguissouma.nxconsole.execution.ui.NxGenerateUiPanel
import com.github.iguissouma.nxconsole.schematics.NxCliSchematicsRegistryService
import com.github.iguissouma.nxconsole.schematics.Schematic
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.ide.actions.runAnything.RunAnythingAction
import com.intellij.ide.actions.runAnything.RunAnythingAction.EXECUTOR_KEY
import com.intellij.ide.actions.runAnything.RunAnythingContext
import com.intellij.ide.actions.runAnything.RunAnythingUtil
import com.intellij.ide.actions.runAnything.activity.RunAnythingCommandLineProvider
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import javax.swing.Icon

class NxGenerateRunAnythingProvider : RunAnythingCommandLineProvider() {

    companion object {

        // TODO check how to initialize without Synchronous execution under ReadAction error
        var schematics: MutableList<Schematic> = mutableListOf()

        const val HELP_COMMAND = "nx generate"
    }

    override fun getIcon(value: String): Icon = NxIcons.NRWL_ICON

    override fun getHelpGroupTitle() = "Nx"

    override fun getCompletionGroupTitle(): String {
        return "Nx tasks"
    }

    override fun getHelpCommandPlaceholder(): String {
        return "nx generate <schematic...> <--option-name...>"
    }

    override fun getHelpCommand(): String {
        return HELP_COMMAND
    }

    override fun getHelpCommandAliases(): List<String> {
        return listOf("nx g")
    }

    override fun getHelpIcon(): Icon = NxIcons.NRWL_ICON

    override fun getMainListItem(dataContext: DataContext, value: String) =
        RunAnythingNxItem(
            getCommand(value),
            getIcon(value)
        )

    override fun run(dataContext: DataContext, commandLine: CommandLine): Boolean {
        val project = RunAnythingUtil.fetchProject(dataContext)
        val interpreter = NodeJsInterpreterManager.getInstance(project).interpreter ?: return false
        val executor = dataContext.getData(EXECUTOR_KEY)
        val executionContext = dataContext.getData(EXECUTING_CONTEXT) ?: RunAnythingContext.ProjectContext(project)
        val context = createContext(project, executionContext, dataContext)

        // TODO check current directory
        val cli = project.baseDir
        val workingDir = project.baseDir

        val filter = NxCliFilter(project, project.baseDir.path)

        val args = mutableListOf(*commandLine.parameters.toTypedArray())
        if (executor is DefaultDebugExecutor && "--dryRun" !in args) {
            args.add("--dryRun")
        }

        if (!isUI(args)) {
            NxGenerator().generate(
                node = interpreter,
                nxExe = NxConfigProvider.getNxWorkspaceType(project, workingDir).exe(),
                baseDir = cli,
                workingDir = VfsUtilCore.virtualToIoFile(workingDir ?: cli),
                project = project,
                callback = null,
                title = "Generate",
                filters = arrayOf(filter),
                "generate",
                *args.toTypedArray()
            )
        } else {
            val schematic = hasSchematic(context, commandLine)
            if (schematic != null) {
                val vFile = DefaultNxUiFile("Generate.nx", NxGenerateUiPanel(project, schematic, args))
                // close file if isOpened to display another schematic
                val fem = FileEditorManager.getInstance(project)
                if (fem.isFileOpen(vFile)) {
                    fem.closeFile(vFile)
                }
                fem.openFile(vFile, true)
            }
        }

        return true
    }

    private fun isUI(args: MutableList<String>): Boolean {
        return "--ui" in args
    }

    override fun suggestCompletionVariants(dataContext: DataContext, commandLine: CommandLine): Sequence<String> {
        val project = RunAnythingUtil.fetchProject(dataContext)
        val executionContext = dataContext.getData(EXECUTING_CONTEXT) ?: RunAnythingContext.ProjectContext(project)
        val context = createContext(project, executionContext, dataContext)

        val basicPhasesVariants = completeBasicPhases(commandLine).sorted()
        val customGoalsVariants = completeCustomGoals(dataContext, commandLine).sorted()
        val longOptionsVariants = completeOptions(context, commandLine, isLongOpt = true).sorted()

        return when {
            commandLine.toComplete.startsWith("--") ->
                longOptionsVariants + basicPhasesVariants + customGoalsVariants
            commandLine.toComplete.startsWith("-") ->
                longOptionsVariants + basicPhasesVariants + customGoalsVariants
            else ->
                basicPhasesVariants + customGoalsVariants + longOptionsVariants
        }
    }

    private fun completeOptions(context: Context, commandLine: CommandLine, isLongOpt: Boolean): Sequence<String> {
        val schematic = hasSchematic(context, commandLine) ?: return emptySequence()
        return schematic.options.map { "--${it.name}" }.plus("--ui").filter { it !in commandLine }.asSequence()
    }

    private fun hasSchematic(context: Context, commandLine: CommandLine): Schematic? {
        return getSchematics(project = context.project)
            .firstOrNull { schematic: Schematic -> commandLine.completedParameters.contains(schematic.name) }
    }

    private fun completeCustomGoals(dataContext: DataContext, commandLine: CommandLine): Sequence<String> {
        val project = RunAnythingUtil.fetchProject(dataContext)
        val schematics = getSchematics(project)
            .map { "${it.name}" }
            .toList()
        return if (schematics.any { it in commandLine }) emptySequence() else schematics.asSequence()
    }

    private fun getSchematics(project: Project?): MutableList<Schematic> {
        if (project == null) {
            return mutableListOf()
        }

        if (schematics.isEmpty()) {
            // TODO java.lang.Throwable: Synchronous execution on EDT
            ApplicationManager.getApplication().executeOnPooledThread {
                ApplicationManager.getApplication().invokeLater {
                    val mySchematics = runCatching {
                        NxCliSchematicsRegistryService.getInstance().getSchematics(project, project.baseDir)
                    }.getOrNull() ?: emptyList()
                    schematics.clear()
                    schematics.addAll(mySchematics)
                }
            }
        }
        return schematics
    }

    private fun completeBasicPhases(commandLine: CommandLine): Sequence<String> {
        return listOf("").asSequence().filter { it !in commandLine }
    }

    private fun createContext(project: Project, context: RunAnythingContext, dataContext: DataContext): Context {
        val tasks = schematics
            .filterNot { it.name.isNullOrEmpty() }.map { it.name!! to it }.toMap()
        val executor = RunAnythingAction.EXECUTOR_KEY.getData(dataContext)
        return Context(context, project, tasks)
    }

    data class Context(
        val context: RunAnythingContext,
        val project: Project,
        val tasks: Map<String, Schematic>
    )
}
