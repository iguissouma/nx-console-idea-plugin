package com.github.iguissouma.nxconsole.execution

import com.github.iguissouma.nxconsole.NxIcons
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.icons.AllIcons
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
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.LayoutBuilder
import com.intellij.ui.layout.Row
import com.intellij.ui.layout.panel
import org.angular2.cli.AngularCliFilter
import org.angular2.cli.AngularCliSchematicsRegistryService
import org.angular2.cli.Option
import org.angular2.cli.Schematic
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import javax.swing.DefaultComboBoxModel
import javax.swing.Icon
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.border.EmptyBorder
import javax.swing.event.DocumentEvent

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

    var modelUI = mutableMapOf<String, Any?>()

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

        if ("--ui" !in args) {
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

                /*val map = mutableMapOf<String, Any>().apply {
                    put("dryRun", false)
                }*/

                modelUI.clear()

                modelUI.putAll(
                    (schematic.arguments + schematic.options)
                        .filterNot { it.name == null }
                        .map { it.name!! to it.default }.toMap()
                )

                val panel = panel {
                    /*row {
                        label("nx generate ${schematic.name}", bold = true)
                        right {
                            button("Run") {
                                NpmPackageProjectGenerator.generate(
                                    interpreter, NodePackage(module.virtualFile?.path!!),
                                    { pkg -> pkg.findBinFile("nx", null)?.absolutePath },
                                    cli, VfsUtilCore.virtualToIoFile(workingDir ?: cli), project,
                                    null, arrayOf(filter), "generate", schematic.name,
                                    *modelUI
                                        .filterValues { (it is Boolean && it) or (it is String && it.isNotBlank()) }
                                        .map {
                                            if (it.value is String) {
                                                "--${it.key}=${it.value}"
                                            } else {
                                                "--${it.key}"
                                            }
                                        }
                                        .toTypedArray()
                                )
                            }
                        }
                    }*/

                    titledRow("Arguments") {
                        schematic.arguments.forEach { option ->
                            addRow(option)
                        }
                    }
                    titledRow("Options") {
                        schematic.options.filter { it.name !in ignoredOptions() }.forEach { option ->
                            addRow(option)
                        }
                    }
                }.apply {
                    border = EmptyBorder(4, 4, 4, 10)
                }

                val actionGroup = DefaultActionGroup()
                val run: AnAction = object : AnAction(AllIcons.Actions.Run_anything) {
                    init {
                        shortcutSet = CustomShortcutSet(*KeymapManager.getInstance().activeKeymap.getShortcuts("Refresh"))
                    }

                    override fun actionPerformed(e: AnActionEvent) {
                        NpmPackageProjectGenerator.generate(
                            interpreter, NodePackage(module.virtualFile?.path!!),
                            { pkg -> pkg.findBinFile("nx", null)?.absolutePath },
                            cli, VfsUtilCore.virtualToIoFile(workingDir ?: cli), project,
                            null, arrayOf(filter), "generate", schematic.name,
                            *computeArgsFromModelUi()
                                .toTypedArray()
                        )
                    }
                }

                val dryRun: AnAction = object : AnAction(AllIcons.RunConfigurations.RemoteDebug) {
                    init {
                        shortcutSet = CustomShortcutSet(*KeymapManager.getInstance().activeKeymap.getShortcuts("Refresh"))
                    }

                    override fun actionPerformed(e: AnActionEvent) {
                        NpmPackageProjectGenerator.generate(
                            interpreter, NodePackage(module.virtualFile?.path!!),
                            { pkg -> pkg.findBinFile("nx", null)?.absolutePath },
                            cli, VfsUtilCore.virtualToIoFile(workingDir ?: cli), project,
                            null, arrayOf(filter), "generate", schematic.name,
                            *computeArgsFromModelUi()
                                .toTypedArray(),
                            "--dry-run", "--no-interactive"
                        )
                    }
                }
                // Add an empty action and disable it permanently for displaying file name.
                actionGroup.add(TextLabelAction("nx generate ${schematic.name}"))
                actionGroup.addAction(run)
                actionGroup.addAction(dryRun)

                val actionToolbar =
                    ActionManager.getInstance().createActionToolbar("top", actionGroup, true)
                actionToolbar.setMinimumButtonSize(ActionToolbar.NAVBAR_MINIMUM_BUTTON_SIZE)
                actionToolbar.setTargetComponent(panel)

                val jPanel = JPanel(BorderLayout())
                jPanel.add(actionToolbar.component, BorderLayout.NORTH)
                jPanel.add(JBScrollPane(panel), BorderLayout.CENTER)

                // val panel = JPanel(BorderLayout())
                val vFile = DefaultNxUiFile("Generate.nx", NxUiPanel(jPanel))
                FileEditorManager.getInstance(project).openFile(vFile, true)
            }
        }

        return true
    }

    private fun computeArgsFromModelUi(): List<String> {
        return modelUI
            .filterKeys { it !in ignoredOptions() }
            .filterValues { (it is Boolean && it) or (it is String && it.isNotBlank()) }
            .map {
                if (it.value is String) {
                    "--${it.key}=${it.value}"
                } else {
                    "--${it.key}"
                }
            }
    }

    private fun ignoredOptions() = listOf("dryRun", "linter", "strict", "force")

    /**
     * An disabled action for displaying text in action toolbar.
     */
    private class TextLabelAction internal constructor(text: String) : AnAction(null as String?) {
        override fun actionPerformed(e: AnActionEvent) {
            // Do nothing
        }

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = false
        }

        override fun displayTextInToolbar(): Boolean {
            return true
        }

        init {
            templatePresentation.setText(text, false)
            templatePresentation.isEnabled = false
        }
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

    private inline fun <T : JComponent> Row.buildComponentForOption(option: Option) {
        when {
            option.type?.toLowerCase() == "string" && option.enum.isNullOrEmpty() -> buildTextField(option)
            option.type?.toLowerCase() == "string" && option.enum.isNotEmpty() -> buildSelectField(option)
            option.type?.toLowerCase() == "boolean" -> buildCheckboxField(option)
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

    private inline fun Row.buildCheckboxField(option: Option) {
        // return checkBox(option.name?:"", option.default as? Boolean ?: false, option.description ?: "")
        val key = option.name ?: ""
        checkBox(
            text = option.name ?: "",
            comment = option.description ?: "",
            isSelected = modelUI[key] as? Boolean ?: false,
            // getter = { modelUI[key] as? Boolean ?: false },
            // setter = { modelUI[key] = it },
            actionListener = { e: ActionEvent, cb: JCheckBox -> modelUI[key] = !(modelUI[key] as? Boolean ?: false) }
        )
    }

    private inline fun Row.buildSelectField(option: Option) {
        val model: DefaultComboBoxModel<String> = DefaultComboBoxModel(option.enum.toTypedArray())
        val comboBox = ComboBox(model)
        comboBox.addActionListener {
            modelUI[option.name ?: ""] = (comboBox.selectedItem as? String) ?: ""
        }
        comboBox()
    }

    private inline fun Row.buildTextField(option: Option) {
        val jTextField = JBTextField()
        jTextField.emptyText.text = option.description ?: ""
        option.default?.let {
            jTextField.text = it as? String ?: ""
        }
        jTextField.getDocument().addDocumentListener(
            object : javax.swing.event.DocumentListener {
                override fun insertUpdate(e: DocumentEvent?) {
                    updateValue()
                }

                override fun removeUpdate(e: DocumentEvent?) {
                    updateValue()
                }

                override fun changedUpdate(e: DocumentEvent?) {
                    updateValue()
                }

                private fun updateValue() {
                    modelUI[option.name ?: ""] = jTextField.text
                }
            }
        )
        jTextField()
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
