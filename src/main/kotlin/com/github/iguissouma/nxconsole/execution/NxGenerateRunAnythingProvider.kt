package com.github.iguissouma.nxconsole.execution

import com.github.iguissouma.nxconsole.NxIcons
import com.intellij.ide.actions.runAnything.RunAnythingAction
import com.intellij.ide.actions.runAnything.RunAnythingContext
import com.intellij.ide.actions.runAnything.RunAnythingUtil
import com.intellij.ide.actions.runAnything.activity.RunAnythingCommandLineProvider
import com.intellij.javascript.nodejs.CompletionModuleInfo
import com.intellij.javascript.nodejs.NodeModuleSearchUtil
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.lang.javascript.boilerplate.NpmPackageProjectGenerator
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import org.angular2.cli.AngularCliFilter
import org.angular2.cli.AngularCliSchematicsRegistryService
import org.angular2.cli.Schematic
import javax.swing.Icon

class NxGenerateRunAnythingProvider : RunAnythingCommandLineProvider() {

    companion object {

        // TODO check how to initialize without Synchronous execution under ReadAction error
        var schematics: MutableList<Schematic> = mutableListOf()

        const val HELP_COMMAND = "nx generate"

    }

    private fun loadSchematics(project: Project): Collection<Schematic> =
        AngularCliSchematicsRegistryService.getInstance().getSchematics(project, project.baseDir, true, true)

    override fun getIcon(value: String): Icon? = NxIcons.NRWL_ICON

    override fun getHelpGroupTitle() = "Nx"

    override fun getCompletionGroupTitle(): String? {
        return "Nx tasks"
    }

    override fun getHelpCommandPlaceholder(): String? {
        return "nx generate <schematic...> <--option-name...>"
    }

    override fun getHelpCommand(): String {
        return HELP_COMMAND
    }

    override fun getHelpIcon(): Icon? = NxIcons.NRWL_ICON

    override fun getMainListItem(dataContext: DataContext, value: String) =
        RunAnythingNxItem(
            getCommand(value),
            getIcon(value)
        )

    override fun run(dataContext: DataContext, commandLine: CommandLine): Boolean {
        val project = RunAnythingUtil.fetchProject(dataContext)
        val interpreter = NodeJsInterpreterManager.getInstance(project).interpreter ?: return false

        // TODO check use global or local
        val modules: MutableList<CompletionModuleInfo> = mutableListOf()
        NodeModuleSearchUtil.findModulesWithName(modules, "@nrwl/cli", project.baseDir, null)

        // TODO check current directory
        val cli = project.baseDir
        val workingDir = project.baseDir

        val module = modules.firstOrNull() ?: return false
        val filter = AngularCliFilter(project, project.baseDir.path)

        NpmPackageProjectGenerator.generate(interpreter, NodePackage(module.virtualFile?.path!!),
            { pkg -> pkg.findBinFile("nx", null)?.absolutePath },
            cli, VfsUtilCore.virtualToIoFile(workingDir ?: cli), project,
            null, arrayOf(filter), "generate", *commandLine.completedParameters.toTypedArray())

        return true
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
        return schematic.options.mapNotNull { "--${it.name}" }.filter { it !in commandLine }.asSequence()
    }

    fun hasSchematic(context: Context, commandLine: CommandLine): Schematic? {
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

        // TODO check how to avoid read problems
        if (schematics.isEmpty()) {
            ApplicationManager.getApplication().executeOnPooledThread {
                schematics = loadSchematics(project).toMutableList()
            }
        }
        return schematics
    }

    private fun completeBasicPhases(commandLine: CommandLine): Sequence<String> {
        return listOf("generate").asSequence().filter { it !in commandLine }
    }

    private fun createContext(project: Project, context: RunAnythingContext, dataContext: DataContext): Context {
        val tasks = loadSchematics(project).filterNot { it.name.isNullOrEmpty() }.map { it.name!! to it }.toMap()
        val executor = RunAnythingAction.EXECUTOR_KEY.getData(dataContext)
        return Context(context, project, tasks)
    }

    data class Context(
        val context: RunAnythingContext,
        val project: Project,
        val tasks: Map<String, Schematic>
    )
}
