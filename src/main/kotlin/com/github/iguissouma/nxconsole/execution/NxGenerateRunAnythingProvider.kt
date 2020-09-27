package com.github.iguissouma.nxconsole.execution

import com.github.iguissouma.nxconsole.NxIcons
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.ide.actions.runAnything.RunAnythingAction
import com.intellij.ide.actions.runAnything.RunAnythingAction.EXECUTOR_KEY
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
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.CellBuilder
import com.intellij.ui.layout.LayoutBuilder
import com.intellij.ui.layout.Row
import com.intellij.ui.layout.panel
import org.angular2.cli.AngularCliFilter
import org.angular2.cli.AngularCliSchematicsRegistryService
import org.angular2.cli.Option
import org.angular2.cli.Schematic
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.border.EmptyBorder


class NxGenerateRunAnythingProvider : RunAnythingCommandLineProvider() {

    companion object {

        // TODO check how to initialize without Synchronous execution under ReadAction error
        var schematics: MutableList<Schematic> = mutableListOf()

        const val HELP_COMMAND = "nx generate"
    }

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

    override fun getHelpCommandAliases(): List<String> {
        return listOf("nx g")
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
        val executor = dataContext.getData(EXECUTOR_KEY)
        val executionContext = dataContext.getData(EXECUTING_CONTEXT) ?: RunAnythingContext.ProjectContext(project)
        val context = createContext(project, executionContext, dataContext)


        // TODO check use global or local
        val modules: MutableList<CompletionModuleInfo> = mutableListOf()
        NodeModuleSearchUtil.findModulesWithName(modules, "@nrwl/cli", project.baseDir, null)

        // TODO check current directory
        val cli = project.baseDir
        val workingDir = project.baseDir

        val module = modules.firstOrNull() ?: return false
        val filter = AngularCliFilter(project, project.baseDir.path)

        val args = mutableListOf(*commandLine.parameters.toTypedArray())
        if (executor is DefaultDebugExecutor && "--dryRun" !in args) {
            args.add("--dryRun")
        }

        if (!isUI()) {
            NpmPackageProjectGenerator.generate(
                interpreter, NodePackage(module.virtualFile?.path!!),
                { pkg -> pkg.findBinFile("nx", null)?.absolutePath },
                cli, VfsUtilCore.virtualToIoFile(workingDir ?: cli), project,
                null, arrayOf(filter), "generate", *args.toTypedArray()
            )
        } else {
            val schematic = hasSchematic(context, commandLine)
            if (schematic != null) {

                /*val builder = FormBuilder.createFormBuilder()
                //builder.panel.layout = VerticalFlowLayout()
                val jButton = JButton("Run")
                jButton.preferredSize = Dimension(100, 26)
                builder.addComponentToRightColumn(jButton)
                schematic.options.forEach { option ->
                    builder.addLabeledComponent(option.name.let { "$it:" }, buildComponentForOption(option))
                }*/

                val panel = panel {
                    row{
                        right {
                            JButton("Run")()
                        }
                    }
                    titledRow("Arguments") {
                        schematic.arguments.forEach { option ->
                            addRow(option)
                        }
                    }
                    titledRow("Options") {
                        schematic.options.forEach { option ->
                            addRow(option)
                        }
                    }
                }.apply {
                    border = EmptyBorder(4, 4, 4, 10)
                }
                //val panel = JPanel(BorderLayout())
                val vFile = DefaultNxUiFile("Generate.nx", NxUiPanel(panel))
                FileEditorManager.getInstance(project).openFile(vFile, true)
            }
        }

        return true
    }

    private fun LayoutBuilder.addRow(option: Option) {
        row(option.takeIf { it.type == "string" }?.let { "${it.name}:" }) {
            buildComponentForOption<JComponent>(option)
        }
    }

   /* private fun buildComponentForOption(option: Option): JComponent {
        return when {
            option.type == "string" && option.enum.isNullOrEmpty() ->  buildTextField(option)
            option.type == "string" && option.enum.isNotEmpty() -> buildSelectField(option)
            option.type == "boolean" -> buildCheckboxField(option)
            else -> buildTextField(option)
        }
    }*/

    private fun <T : JComponent> Row.buildComponentForOption(option: Option): CellBuilder<JComponent> {
        return when {
            option.type == "string" && option.enum.isNullOrEmpty() ->  buildTextField(option)
            option.type == "string" && option.enum.isNotEmpty() -> buildSelectField(option)
            option.type == "boolean" -> buildCheckboxField(option)
            else -> buildTextField(option)
        }
    }

    /*private fun buildSelectField(option: Option): JComponent {
        return ComboBox(option.enum.toTypedArray())
    }

    private fun buildCheckboxField(option: Option): JComponent {
        return JBCheckBox(option.name, option.default as? Boolean ?: false)
    }

    private fun buildTextField(option: Option): JBTextField {
        val jTextField = JBTextField()
        jTextField.emptyText.text = option.description ?: ""
        return jTextField
    }*/

    private fun Row.buildCheckboxField(option: Option): CellBuilder<JBCheckBox> {
        return checkBox(option.name?:"", option.default as? Boolean ?: false, option.description ?: "")
    }

    private fun Row.buildSelectField(option: Option): CellBuilder<ComboBox<String>> {
        return ComboBox(option.enum.toTypedArray())()
    }

    private fun Row.buildTextField(option: Option): CellBuilder<JBTextField> {
       val jTextField = JBTextField()
        jTextField.emptyText.text = option.description ?: ""
        return jTextField()
    }

    private fun isUI(): Boolean {
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
                        AngularCliSchematicsRegistryService.getInstance().getSchematics(project, project.baseDir)
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
