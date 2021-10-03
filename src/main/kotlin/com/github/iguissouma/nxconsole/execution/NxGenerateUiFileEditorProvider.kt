package com.github.iguissouma.nxconsole.execution

import com.github.iguissouma.nxconsole.NxBundle
import com.github.iguissouma.nxconsole.NxIcons
import com.github.iguissouma.nxconsole.buildTools.NxRunSettings
import com.github.iguissouma.nxconsole.buildTools.NxService
import com.github.iguissouma.nxconsole.buildTools.rc.NxConfigurationType
import com.github.iguissouma.nxconsole.buildTools.rc.NxRunConfiguration
import com.github.iguissouma.nxconsole.builders.NxBuilderOptions
import com.github.iguissouma.nxconsole.builders.NxCliBuildersRegistryService
import com.github.iguissouma.nxconsole.cli.NxCliFilter
import com.github.iguissouma.nxconsole.cli.config.NxConfigProvider
import com.github.iguissouma.nxconsole.cli.config.NxProject
import com.github.iguissouma.nxconsole.schematics.NxCliSchematicsRegistryService
import com.github.iguissouma.nxconsole.schematics.Option
import com.github.iguissouma.nxconsole.schematics.Schematic
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.diff.util.FileEditorBase
import com.intellij.execution.Executor
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.icons.AllIcons
import com.intellij.ide.FileIconProvider
import com.intellij.ide.actions.SplitAction
import com.intellij.javascript.nodejs.CompletionModuleInfo
import com.intellij.javascript.nodejs.NodeModuleSearchUtil
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.lang.javascript.boilerplate.NpmPackageProjectGenerator
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.SideBorder
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.ui.TextFieldWithAutoCompletionListProvider
import com.intellij.ui.TextFieldWithHistoryWithBrowseButton
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.CCFlags
import com.intellij.ui.layout.LayoutBuilder
import com.intellij.ui.layout.Row
import com.intellij.ui.layout.panel
import com.intellij.util.ThrowableRunnable
import com.intellij.util.ui.SwingHelper
import com.intellij.vcs.log.impl.VcsLogContentUtil
import com.intellij.vcs.log.impl.VcsLogTabsManager
import com.intellij.webcore.ui.PathShortener
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import java.lang.Integer.min
import java.lang.reflect.InvocationTargetException
import javax.swing.DefaultComboBoxModel
import javax.swing.Icon
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.JTextField
import javax.swing.LayoutFocusTraversalPolicy
import javax.swing.SwingUtilities
import javax.swing.border.EmptyBorder
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import javax.swing.event.DocumentEvent

class NxUiIconProvider : FileIconProvider {
    override fun getIcon(file: VirtualFile, flags: Int, project: Project?): Icon? = (file as? NxUiFile)?.fileType?.icon
}

class NxUiFileType : FileType {
    override fun getName(): String = "NxUi"
    override fun getDescription(): String = ""
    override fun getDefaultExtension(): String = ".nx"
    override fun getIcon(): Icon = NxIcons.NRWL_ICON
    override fun isBinary(): Boolean = true
    override fun isReadOnly(): Boolean = true
    override fun getCharset(file: VirtualFile, content: ByteArray): String? = null

    companion object {
        val INSTANCE = NxUiFileType()
    }
}

abstract class NxUiFile(name: String) : LightVirtualFile(name, NxUiFileType.INSTANCE, "") {
    init {
        isWritable = false
    }

    abstract fun createMainComponent(project: Project): JComponent
}

internal class DefaultNxUiFile(val task: String, panel: NxUiPanel) : NxUiFile(task) {
    private var nxUiPanel: NxUiPanel? = null

    init {
        nxUiPanel = panel
        // Disposer.register(panel.getUi(), Disposable { nxUiPanel = null })

        putUserData(SplitAction.FORBID_TAB_SPLIT, true)
    }

    override fun createMainComponent(project: Project): JComponent {
        return nxUiPanel ?: JBPanelWithEmptyText().withEmptyText(NxBundle.message("nx.ui.tab.closed.status"))
    }

    fun getDisplayName(): String? {
        return nxUiPanel?.let {
            val logUi = VcsLogContentUtil.getLogUi(it) ?: return null
            return VcsLogTabsManager.generateDisplayName(logUi)
        }
    }

    override fun isValid(): Boolean = nxUiPanel != null
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DefaultNxUiFile

        if (task != other.task) return false

        return true
    }

    override fun hashCode(): Int {
        return task.hashCode()
    }
}

open class NxUiPanel(val project: Project, args: MutableList<String>) :
    JPanel(BorderLayout()),
    Disposable {

    var centerPanel: DialogPanel? = null
    override fun dispose() {
    }
}

class NxBuildersUiPanel(
    project: Project,
    var command: String,
    var target: String,
    var architect: NxProject.Architect,
    args: MutableList<String>
) :
    NxUiPanel(project, args) {
    private var mainPanel: JPanel? = null
    var hasFocus = false
    var focusedComponent = null

    var modelUI = createModelUI()

    private fun createModelUI() = emptyMap<String, Any>().toMutableMap()

    var builderOptions = listOf<NxBuilderOptions>()

    val nxConfig = NxConfigProvider.getNxConfig(project, project.baseDir)

    init {

        centerPanel = createCenterPanel()

        builderOptions = runInEdtAndGet {
            NxCliBuildersRegistryService.getInstance()
                .readBuilderSchema(
                    project,
                    nxConfig?.angularJsonFile!!,
                    architect.builder!!
                ) ?: emptyList()
        }

        centerPanel = createCenterPanel()

        val actionGroup = DefaultActionGroup()
        val run: AnAction = object : AnAction("Run", "", AllIcons.Actions.Execute) {
            init {
                // shortcutSet = CustomShortcutSet.fromString(if (SystemInfo.isMac) "meta ENTER" else "control ENTER")
                registerCustomShortcutSet(
                    CustomShortcutSet.fromString(if (SystemInfo.isMac) "meta ENTER" else "control ENTER"),
                    this@NxBuildersUiPanel
                )
            }

            override fun actionPerformed(e: AnActionEvent) {
                val runManager = RunManager.getInstance(project)
                val runnerAndConfigurationSettings: RunnerAndConfigurationSettings = runnerAndConfigurationSettings()
                runManager.selectedConfiguration = runnerAndConfigurationSettings
                val executor: Executor = DefaultRunExecutor.getRunExecutorInstance()
                ExecutionUtil.runConfiguration(runnerAndConfigurationSettings, executor)
            }
        }

        val dryRun: AnAction = object : AnAction("Dry Run", "", AllIcons.Actions.StartDebugger) {
            init {
                // shortcutSet = CustomShortcutSet.fromString("shift ENTER")
                registerCustomShortcutSet(CustomShortcutSet.fromString("shift ENTER"), this@NxBuildersUiPanel)
            }

            override fun actionPerformed(e: AnActionEvent) {
                val runManager = RunManager.getInstance(project)
                val runnerAndConfigurationSettings: RunnerAndConfigurationSettings = runnerAndConfigurationSettings()
                runManager.selectedConfiguration = runnerAndConfigurationSettings
                val executor: Executor = DefaultDebugExecutor.getDebugExecutorInstance()
                ExecutionUtil.runConfiguration(runnerAndConfigurationSettings, executor)
            }
        }

        val copyRun: AnAction = object : AnAction("Copy To Clipboard", "", AllIcons.General.CopyHovered) {
            private fun copyInfoToClipboard(text: String) {
                try {
                    CopyPasteManager.getInstance().setContents(StringSelection(text))
                } catch (ignore: Exception) {
                }
            }

            init {
                // shortcutSet = CustomShortcutSet.fromString("shift ENTER")
                // registerCustomShortcutSet(CustomShortcutSet.fromString("meta C", "control C"), this@NxBuildersUiPanel)
            }

            override fun actionPerformed(e: AnActionEvent) {
                copyInfoToClipboard("nx run $target:$command ${computeArgsFromModelUi().joinToString(separator = " ")}")
            }
        }

        val chooseTarget: AnAction = object : ComboBoxAction() {

            override fun update(e: AnActionEvent) {
                val project = e.project ?: return
                nxConfig ?: return
                val presentation = e.presentation
                presentation.isEnabled = isEnabled
                presentation.text = this@NxBuildersUiPanel.target
                templatePresentation.text = this@NxBuildersUiPanel.target
                val t = nxConfig.projects.first { it.name == target }
                presentation.icon =
                    if (t.type == NxProject.AngularProjectType.APPLICATION) NxIcons.NX_APP_FOLDER
                    else NxIcons.NX_LIB_FOLDER
                super.update(e)
            }

            override fun createPopupActionGroup(button: JComponent?): DefaultActionGroup {
                val group = DefaultActionGroup()
                if (nxConfig == null) {
                    return group
                }
                nxConfig.projects.filter { it.architect.containsKey(command) }.forEach {
                    group.add(
                        object : DumbAwareAction(
                            it.name,
                            it.name,
                            if (it.type == NxProject.AngularProjectType.APPLICATION) NxIcons.NX_APP_FOLDER
                            else NxIcons.NX_LIB_FOLDER
                        ) {

                            override fun actionPerformed(e: AnActionEvent) {
                                selectTarget(it.name)
                                centerPanel = this@NxBuildersUiPanel.createCenterPanel()

                                this@NxBuildersUiPanel.mainPanel?.remove(1)
                                this@NxBuildersUiPanel.mainPanel?.add(JBScrollPane(centerPanel), BorderLayout.CENTER)

                                (button as ComboBoxButton).text = it.name
                                // this@NxBuildersUiPanel.modelUI = createModelUI()
                                this@NxBuildersUiPanel.validate()
                                this@NxBuildersUiPanel.repaint()
                            }
                        }
                    )
                }
                return group
            }

            private fun selectTarget(target: String) {
                this@NxBuildersUiPanel.target = target
            }
        }
        // Add an empty action and disable it permanently for displaying file name.
        actionGroup.add(NxGeneratorsUiPanel.TextLabelAction("  nx $command "))
        actionGroup.addAction(chooseTarget)
        actionGroup.addAction(run)
        actionGroup.addAction(dryRun)
        actionGroup.addAction(copyRun)

        val actionToolbar =
            ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, actionGroup, true)
        actionToolbar.setMinimumButtonSize(ActionToolbar.NAVBAR_MINIMUM_BUTTON_SIZE)
        actionToolbar.layoutPolicy = ActionToolbar.AUTO_LAYOUT_POLICY
        actionToolbar.component.border = IdeBorderFactory.createBorder(SideBorder.TOP + SideBorder.BOTTOM)
        actionToolbar.setMinimumButtonSize(Dimension(22, 22))
        actionToolbar.component.isOpaque = true

        mainPanel = JPanel(BorderLayout())
        mainPanel!!.add(actionToolbar.component, BorderLayout.NORTH)
        val jbScrollPane = JBScrollPane(centerPanel)
        mainPanel!!.add(jbScrollPane, BorderLayout.CENTER)

        isFocusCycleRoot = true
        focusTraversalPolicy = LayoutFocusTraversalPolicy()

        add(mainPanel)
    }

    private fun runnerAndConfigurationSettings(): RunnerAndConfigurationSettings {
        val runManager = RunManager.getInstance(project)
        val configurationSettings: List<RunnerAndConfigurationSettings> =
            runManager.getConfigurationSettingsList(NxConfigurationType.getInstance())
        var runnerAndConfigurationSettings: RunnerAndConfigurationSettings? = null
        val name = "$target:$command"
        for (cfg in configurationSettings) {
            val nxRunConfiguration: NxRunConfiguration = cfg.configuration as NxRunConfiguration
            if (nxRunConfiguration.runSettings.tasks.contains(name)) {
                nxRunConfiguration.runSettings.apply {
                    arguments = computeArgsFromModelUi().joinToString(separator = " ")
                }
                runnerAndConfigurationSettings = cfg
            }
        }
        if (runnerAndConfigurationSettings == null) {
            runnerAndConfigurationSettings =
                runManager.createConfiguration(
                    name,
                    NxConfigurationType::class.java
                )
            val configuration: NxRunConfiguration =
                runnerAndConfigurationSettings.configuration as NxRunConfiguration
            configuration.runSettings = NxRunSettings().apply {
                nxFilePath = NxService.getInstance(project).detectFirstBuildfileInContentRoots(false)?.path
                tasks = listOf(name)
                arguments = computeArgsFromModelUi().joinToString(separator = " ")
            }
            runManager.addConfiguration(runnerAndConfigurationSettings)
        }
        return runnerAndConfigurationSettings
    }

    private fun createCenterPanel(): DialogPanel {
        return panel {
            builderOptions.filter { it.name !in ignoredOptions() }.forEach { option ->
                addRow(option)
            }
        }.apply {
            border = EmptyBorder(10, 10, 4, 15)
        }
    }

    private fun computeArgsFromModelUi(): List<String> {
        return modelUI
            .filterKeys { it !in ignoredOptions() }
            .filterNot {
                builderOptions.first { o -> o.name == it.key }.default == it.value.toString() // filter value are equal to default.
            }
            .filterValues { (it is Number) or (it is Boolean) or (it is String && it.isNotBlank()) }
            .map {
                if (it.value is Boolean) {
                    if (it.value == false) "--no-${it.key}" else "--${it.key}"
                } else {
                    "--${it.key}=${it.value}"
                }
            }
    }

    private fun ignoredOptions() = emptyList<String>()

    private fun LayoutBuilder.addRow(option: NxBuilderOptions) {
        row(option.takeIf { it.type == "string" || it.type == "number" || it.type == "array" }?.let { "${it.name}:" }) {
            buildComponentForOption<JComponent>(option)
        }
    }

    private inline fun <T : JComponent> Row.buildComponentForOption(option: NxBuilderOptions) {
        when {
            option.type?.toLowerCase() == "string" && option.enum.isNullOrEmpty() && (
                "project".equals(
                    option.name,
                    ignoreCase = true
                ) || "projectName".equals(option.name, ignoreCase = true)
                ) -> buildProjectTextField(option)
            option.type?.toLowerCase() == "string" && option.enum.isNullOrEmpty() && (
                "path".equals(
                    option.name,
                    ignoreCase = true
                ) || "directory".equals(option.name, ignoreCase = true)
                ) -> buildDirectoryTextField(option)
            option.type?.toLowerCase() == "string" && option.enum.isNullOrEmpty() -> buildTextField(option)
            (option.type?.toLowerCase() == "string" || option.type?.toLowerCase() == "enum") && option.enum.isNotEmpty() -> buildSelectField(
                option
            )
            option.type?.toLowerCase() == "boolean" -> buildCheckboxField(option)
            option.type?.toLowerCase() == "number" -> buildNumberField(option)
            else -> buildTextField(option)
        }
    }

    private inline fun Row.buildDirectoryTextField(option: NxBuilderOptions) {

        val directoryTextField = TextFieldWithHistoryWithBrowseButton()
        SwingHelper.installFileCompletionAndBrowseDialog(
            project,
            directoryTextField,
            "Select Test File",
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
        )
        val textField = directoryTextField.childComponent.textEditor
        PathShortener.enablePathShortening(textField, JTextField(project.basePath))
        textField.text = modelUI[option.name] as? String ?: ""
        val docListener: javax.swing.event.DocumentListener = object : DocumentAdapter() {
            private fun updateValue() {
                modelUI[option.name ?: ""] = textField.text
            }

            override fun textChanged(e: DocumentEvent) {
                updateValue()
            }
        }
        textField.document.addDocumentListener(docListener)
        directoryTextField(comment = option.description)
    }

    private inline fun Row.buildCheckboxField(option: NxBuilderOptions) {
        // return checkBox(option.name?:"", option.default as? Boolean ?: false, option.description ?: "")
        val key = option.name ?: ""
        checkBox(
            text = option.name ?: "",
            comment = option.description ?: "",
            isSelected = modelUI[key] as? Boolean ?: option.default.toBoolean(),
            // getter = { modelUI[key] as? Boolean ?: false },
            // setter = { modelUI[key] = it },
            actionListener = { e: ActionEvent, cb: JCheckBox -> modelUI[key] = !(modelUI[key] as? Boolean ?: false) }
        )
    }

    private inline fun Row.buildNumberField(option: NxBuilderOptions) {
        val spinner = JSpinner().apply {
            editor = JSpinner.NumberEditor(this, "#")
        }
        if (option.default.isNotBlank()) {
            option.default.toIntOrNull()?.run {
                spinner.value = this
            }
        }
        spinner.addChangeListener(
            object : ChangeListener {
                override fun stateChanged(e: ChangeEvent?) {
                    modelUI[option.name] = spinner.value ?: ""
                }
            }
        )
        spinner(comment = option.description)
    }

    private inline fun Row.buildSelectField(option: NxBuilderOptions) {
        val model: DefaultComboBoxModel<String> = DefaultComboBoxModel(option.enum.toTypedArray())
        val comboBox = ComboBox(model)
        comboBox.selectedItem = modelUI[option.name] ?: option.enum.first()
        comboBox.addActionListener {
            modelUI[option.name ?: ""] = (comboBox.selectedItem as? String) ?: ""
        }
        comboBox(comment = option.description)
    }

    private inline fun Row.buildTextField(option: NxBuilderOptions) {
        val jTextField = JBTextField()
        // jTextField.emptyText.text = option.description ?: ""
        // option.default?.let {
        // jTextField.text = it as? String ?: ""
        // }
        jTextField.text = modelUI[option.name] as? String ?: option.default
        val docListener: javax.swing.event.DocumentListener = object : DocumentAdapter() {
            private fun updateValue() {
                modelUI[option.name ?: ""] = jTextField.text
            }

            override fun textChanged(e: DocumentEvent) {
                updateValue()
            }
        }
        jTextField.document.addDocumentListener(docListener)

        // add focus on first input text field
        if (!hasFocus) {
            jTextField(comment = option.description).focused()
            hasFocus = true
        } else {
            jTextField(comment = option.description)
        }
    }

    private inline fun Row.buildProjectTextField(option: NxBuilderOptions) {
        val textField = SchematicProjectOptionsTextField(
            project = project,
            NxConfigProvider.getNxConfig(project, project.baseDir)?.projects ?: emptyList()
        )
        textField.text = modelUI[option.name] as? String ?: ""
        textField.document.addDocumentListener(
            object : DocumentListener {
                private fun updateValue() {
                    modelUI[option.name ?: ""] = textField.text
                }

                override fun documentChanged(event: com.intellij.openapi.editor.event.DocumentEvent) {
                    updateValue()
                }
            }
        )

        textField(comment = option.description, constraints = arrayOf(CCFlags.growX))
    }
}

class NxGeneratorsUiPanel(project: Project, var schematic: Schematic, args: MutableList<String>) :
    NxUiPanel(project, args) {

    private var mainPanel: JPanel? = null
    var hasFocus = false
    var focusedComponent = null

    var modelUI = createModelUI()

    private fun createModelUI() = (schematic.arguments + schematic.options)
        .filterNot { it.name == null }
        .map { it.name!! to it.default }.toMap().toMutableMap()

    init {

        val parsedArgs: List<String> = parseArguments(args.toTypedArray())
            .filter { it != schematic.name }

        (0 until min(parsedArgs.size, schematic.arguments.size)).forEach {
            modelUI[schematic.arguments[it].name ?: ""] = parsedArgs[it]
        }

        parseOptions(args.toTypedArray())
            .filterKeys { modelUI.containsKey(it) }
            .forEach { (t: String, u: List<String>) ->
                val get = modelUI.get(t)
                if (get is Boolean) {
                    val b = u.firstOrNull()
                    if (b == null) {
                        modelUI[t] = true
                    } else {
                        modelUI[t] = b.toBoolean()
                    }
                    // u.firstOrNull()?.let { it.toBoolean() }?.let { modelUI[t] = it }
                } else {
                    u.firstOrNull()?.let { modelUI[t] = it }
                }
            }

        val modules: MutableList<CompletionModuleInfo> = mutableListOf()
        NodeModuleSearchUtil.findModulesWithName(modules, "@nrwl/cli", project.baseDir, null)
        val interpreter = NodeJsInterpreterManager.getInstance(project).interpreter
        // TODO check current directory
        val cli = project.baseDir
        val workingDir = project.baseDir

        val module = modules.firstOrNull()
        val filter = NxCliFilter(project, project.baseDir.path)

        centerPanel = createCenterPanel()

        val actionGroup = DefaultActionGroup()
        val run: AnAction = object : AnAction("Run", "", AllIcons.Actions.Execute) {
            init {
                // shortcutSet = CustomShortcutSet.fromString(if (SystemInfo.isMac) "meta ENTER" else "control ENTER")
                registerCustomShortcutSet(
                    CustomShortcutSet.fromString(if (SystemInfo.isMac) "meta ENTER" else "control ENTER"),
                    this@NxGeneratorsUiPanel
                )
            }

            override fun actionPerformed(e: AnActionEvent) {
                NpmPackageProjectGenerator.generate(
                    interpreter!!, NodePackage(module?.virtualFile?.path!!),
                    { pkg -> pkg.findBinFile("nx", null)?.absolutePath },
                    cli, VfsUtilCore.virtualToIoFile(workingDir ?: cli), project,
                    null, arrayOf(filter), *computeGenerateRunCommand(schemaName = schematic.name).toTypedArray(),
                    *computeArgsFromModelUi()
                        .toTypedArray()
                )
            }
        }

        val dryRun: AnAction = object : AnAction("Dry Run", "", AllIcons.Actions.StartDebugger) {
            init {
                // shortcutSet = CustomShortcutSet.fromString("shift ENTER")
                registerCustomShortcutSet(CustomShortcutSet.fromString("shift ENTER"), this@NxGeneratorsUiPanel)
            }

            override fun actionPerformed(e: AnActionEvent) {
                NpmPackageProjectGenerator.generate(
                    interpreter!!, NodePackage(module?.virtualFile?.path!!),
                    { pkg -> pkg.findBinFile("nx", null)?.absolutePath },
                    cli, VfsUtilCore.virtualToIoFile(workingDir ?: cli), project,
                    null, arrayOf(filter),
                    *computeGenerateRunCommand(schemaName = schematic.name).toTypedArray(),
                    *computeArgsFromModelUi()
                        .toTypedArray(),
                    "--dry-run", "--no-interactive"
                )
            }
        }

        val copyRun: AnAction = object : AnAction("Copy To Clipboard", "", AllIcons.General.CopyHovered) {
            private fun copyInfoToClipboard(text: String) {
                try {
                    CopyPasteManager.getInstance().setContents(StringSelection(text))
                } catch (ignore: Exception) {
                }
            }

            init {
                // shortcutSet = CustomShortcutSet.fromString("shift ENTER")
                // registerCustomShortcutSet(CustomShortcutSet.fromString("meta C", "control C"), this@NxBuildersUiPanel)
            }

            override fun actionPerformed(e: AnActionEvent) {
                copyInfoToClipboard(
                    "nx  ${
                    (computeGenerateRunCommand(schematic.name) + computeArgsFromModelUi()).joinToString(
                        separator = " "
                    )
                    }"
                )
            }
        }

        val chooseSchematic: AnAction = object : ComboBoxAction() {

            override fun update(e: AnActionEvent) {
                val project = e.project ?: return
                val presentation = e.presentation
                presentation.isEnabled = isEnabled
                presentation.text = this@NxGeneratorsUiPanel.schematic.name
                templatePresentation.text = this@NxGeneratorsUiPanel.schematic.name
                super.update(e)
            }

            private inner class ChangeSchematicAction constructor(val schematic: Schematic) : DumbAwareAction() {

                override fun actionPerformed(e: AnActionEvent) {
                }

                init {
                    templatePresentation.setText(schematic.name)
                }
            }

            inner class OptionAction<T>(val value: T, name: String, val set: (T) -> Unit) : DumbAwareAction(name) {
                override fun actionPerformed(e: AnActionEvent) {
                    set(value)
                }
            }

            override fun createPopupActionGroup(button: JComponent?): DefaultActionGroup {
                val group = DefaultActionGroup()
                val schematicsInWorkspace =
                    NxCliSchematicsRegistryService.getInstance().getSchematics(project, project.baseDir)
                schematicsInWorkspace.forEach {
                    group.add(
                        object : DumbAwareAction(it.name) {

                            override fun actionPerformed(e: AnActionEvent) {
                                println(it.name)
                                selectSchematic(it)
                                centerPanel = this@NxGeneratorsUiPanel.createCenterPanel()

                                this@NxGeneratorsUiPanel.mainPanel?.remove(1)
                                this@NxGeneratorsUiPanel.mainPanel?.add(JBScrollPane(centerPanel), BorderLayout.CENTER)

                                (button as ComboBoxButton).text = it.name
                                this@NxGeneratorsUiPanel.modelUI = createModelUI()
                                this@NxGeneratorsUiPanel.validate()
                                this@NxGeneratorsUiPanel.repaint()
                            }
                        }
                    )
                }
                return group
            }

            private fun selectSchematic(schematic: Schematic) {
                this@NxGeneratorsUiPanel.schematic = schematic
            }
        }
        // Add an empty action and disable it permanently for displaying file name.
        actionGroup.add(TextLabelAction("  nx generate "))
        actionGroup.addAction(chooseSchematic)
        actionGroup.addAction(run)
        actionGroup.addAction(dryRun)
        actionGroup.addAction(copyRun)

        val actionToolbar =
            ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, actionGroup, true)
        actionToolbar.setMinimumButtonSize(ActionToolbar.NAVBAR_MINIMUM_BUTTON_SIZE)
        actionToolbar.layoutPolicy = ActionToolbar.AUTO_LAYOUT_POLICY
        actionToolbar.component.border = IdeBorderFactory.createBorder(SideBorder.TOP + SideBorder.BOTTOM)
        actionToolbar.setMinimumButtonSize(Dimension(22, 22))
        actionToolbar.component.isOpaque = true

        mainPanel = JPanel(BorderLayout())
        mainPanel!!.add(actionToolbar.component, BorderLayout.NORTH)
        val jbScrollPane = JBScrollPane(centerPanel)
        mainPanel!!.add(jbScrollPane, BorderLayout.CENTER)

        isFocusCycleRoot = true
        focusTraversalPolicy = LayoutFocusTraversalPolicy()

        add(mainPanel)
    }

    private fun createCenterPanel(): DialogPanel {
        return panel {
            schematic.options.filter { it.name !in ignoredOptions() }.forEach { option ->
                addRow(option)
            }
        }.apply {
            border = EmptyBorder(10, 10, 4, 15)
        }
    }

    private fun ignoredOptions() = emptyList<String>()

    private fun LayoutBuilder.addRow(option: Option) {
        row(option.takeIf { it.type == "string" || it.type == "number" }?.let { "${it.name}:" }) {
            buildComponentForOption<JComponent>(option)
        }
    }

    private inline fun <T : JComponent> Row.buildComponentForOption(option: Option) {
        when {
            option.type?.toLowerCase() == "string" && option.enum.isNullOrEmpty() && (
                "project".equals(
                    option.name,
                    ignoreCase = true
                ) || "projectName".equals(option.name, ignoreCase = true)
                ) -> buildProjectTextField(option)
            option.type?.toLowerCase() == "string" && option.enum.isNullOrEmpty() && (
                "path".equals(
                    option.name,
                    ignoreCase = true
                ) || "directory".equals(option.name, ignoreCase = true)
                ) -> buildDirectoryTextField(option)
            option.type?.toLowerCase() == "string" && option.enum.isNullOrEmpty() -> buildTextField(option)
            option.type?.toLowerCase() == "string" && option.enum.isNotEmpty() -> buildSelectField(option)
            option.type?.toLowerCase() == "boolean" -> buildCheckboxField(option)
            else -> buildTextField(option)
        }
    }

    private inline fun Row.buildDirectoryTextField(option: Option) {

        val directoryTextField = TextFieldWithHistoryWithBrowseButton()
        SwingHelper.installFileCompletionAndBrowseDialog(project, directoryTextField, "Select Test File", FileChooserDescriptorFactory.createSingleFolderDescriptor())
        val textField = directoryTextField.childComponent.textEditor
        PathShortener.enablePathShortening(textField, JTextField(project.basePath))
        textField.text = modelUI[option.name] as? String ?: ""
        val docListener: javax.swing.event.DocumentListener = object : DocumentAdapter() {
            private fun updateValue() {
                modelUI[option.name ?: ""] = textField.text
            }

            override fun textChanged(e: DocumentEvent) {
                updateValue()
            }
        }
        textField.document.addDocumentListener(docListener)
        directoryTextField(comment = option.description)
    }

    private inline fun Row.buildCheckboxField(option: Option) {
        // return checkBox(option.name?:"", option.default as? Boolean ?: false, option.description ?: "")
        val key = option.name ?: ""
        checkBox(
            text = option.name ?: "",
            comment = option.description ?: "",
            isSelected = modelUI[key] as? Boolean ?: option.default.toString().toBoolean(),
            // getter = { modelUI[key] as? Boolean ?: false },
            // setter = { modelUI[key] = it },
            actionListener = { e: ActionEvent, cb: JCheckBox -> modelUI[key] = !(modelUI[key] as? Boolean ?: false) }
        )
    }

    private inline fun Row.buildSelectField(option: Option) {
        val model: DefaultComboBoxModel<String> = DefaultComboBoxModel(option.enum.toTypedArray())
        val comboBox = ComboBox(model)
        comboBox.selectedItem = modelUI[option.name] ?: option.enum.first()
        comboBox.addActionListener {
            modelUI[option.name ?: ""] = (comboBox.selectedItem as? String) ?: ""
        }
        comboBox(comment = option.description)
    }

    private inline fun Row.buildTextField(option: Option) {
        val jTextField = JBTextField()
        // jTextField.emptyText.text = option.description ?: ""
        // option.default?.let {
        //    jTextField.text = it as? String ?: ""
        // }
        jTextField.text = modelUI[option.name] as? String ?: (option.default as String? ?: "")
        val docListener: javax.swing.event.DocumentListener = object : DocumentAdapter() {
            private fun updateValue() {
                modelUI[option.name ?: ""] = jTextField.text
            }

            override fun textChanged(e: DocumentEvent) {
                updateValue()
            }
        }
        jTextField.document.addDocumentListener(docListener)

        // add focus on first input text field
        if (!hasFocus) {
            jTextField(comment = option.description).focused()
            hasFocus = true
        } else {
            jTextField(comment = option.description)
        }
    }

    private inline fun Row.buildProjectTextField(option: Option) {
        val textField = SchematicProjectOptionsTextField(
            project = project,
            NxConfigProvider.getNxConfig(project, project.baseDir)?.projects ?: emptyList()
        )
        textField.text = modelUI[option.name] as? String ?: ""
        textField.document.addDocumentListener(
            object : DocumentListener {
                private fun updateValue() {
                    modelUI[option.name ?: ""] = textField.text
                }

                override fun documentChanged(event: com.intellij.openapi.editor.event.DocumentEvent) {
                    updateValue()
                }
            }
        )

        textField(comment = option.description, constraints = arrayOf(CCFlags.growX))
    }

    private fun computeGenerateRunCommand(schemaName: String?): List<String> {
        if (schemaName.isNullOrBlank()) return emptyList()
        return if (schemaName.startsWith("workspace-schematic:") or schemaName.startsWith("workspace-generator:")) {
            val split = schemaName.split(":")
            listOf(split[0], split[1])
        } else listOf("generate", schemaName)
    }

    private fun computeArgsFromModelUi(): List<String> {
        return modelUI
            .filterKeys { it !in ignoredOptions() }
            .filterNot {
                schematic.options.first { o -> o.name == it.key }.default == it.value.toString() // filter value are equal to default.
            }
            .filterValues { (it is Number) or (it is Boolean) or (it is String && it.isNotBlank()) }
            .map {
                if (it.value is Boolean) {
                    if (it.value == false) "--no-${it.key}" else "--${it.key}"
                } else {
                    "--${it.key}=${it.value}"
                }
            }
    }

    /**
     * An disabled action for displaying text in action toolbar.
     */
    class TextLabelAction internal constructor(text: String) : AnAction(null as String?) {
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

    override fun dispose() {
    }
}

class NxUIEditor(private val project: Project, private val nxUiFile: NxUiFile) : FileEditorBase() {
    private val createMainComponent: JComponent = nxUiFile.createMainComponent(project)
    private val rootComponent: JComponent = JPanel(BorderLayout()).also {
        it.add(createMainComponent, BorderLayout.CENTER)
    }

    override fun getComponent(): JComponent = rootComponent
    override fun getPreferredFocusedComponent(): JComponent? =
        (createMainComponent as? NxUiPanel)?.centerPanel?.preferredFocusedComponent

    override fun getName(): String = NxBundle.message("nx.ui.editor.name")
    override fun getFile() = nxUiFile
}

class NxUIEditorProvider : FileEditorProvider, DumbAware {
    override fun accept(project: Project, file: VirtualFile): Boolean = file is NxUiFile

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return NxUIEditor(project, file as NxUiFile)
    }

    override fun getEditorTypeId(): String = "NxUIEditor"
    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR

    override fun disposeEditor(editor: FileEditor) {
        if (editor.file?.getUserData(FileEditorManagerImpl.CLOSING_TO_REOPEN) != true) {
            editor.disposeNxUis()
        }

        super.disposeEditor(editor)
    }
}

fun FileEditor.disposeNxUis(): List<String> {
    val logUis = VcsLogContentUtil.getLogUis(component)
    val disposedIds = logUis.map { it.id }
    if (logUis.isNotEmpty()) {
        component.removeAll()
        logUis.forEach(Disposer::dispose)
    }
    return disposedIds
}

// a stupid options parser
/**
 * "--directory x --dryRun --dire=y" ->
 */
fun parseOptions(args: Array<String>): MutableMap<String, List<String>> {
    val params = mutableMapOf<String, List<String>>()
    var options: MutableList<String>? = null
    for (i in 0 until args.size) {
        val a = args[i]
        if (a[0] == '-') {
            if (a.length < 2) {
                continue
            }
            options = mutableListOf()
            if (a.contains("=")) {
                val endIndex = a.indexOf("=")
                options.add(a.substring(endIndex + 1))
                if (a[1] == '-') {
                    params[a.substring(2, endIndex)] = options
                } else {
                    params[a.substring(1, endIndex)] = options
                }
            } else {
                if (a[1] == '-') {
                    params[a.substring(2)] = options
                } else {
                    params[a.substring(1)] = options
                }
            }
        } else if (options != null) {
            options.add(a)
        }
    }
    return params
}

// a stupid arg parser
/**
 * "--directory x --dryRun --dire=y" ->
 */
fun parseArguments(args: Array<String>): MutableList<String> {
    val options: MutableList<String> = mutableListOf()
    for (element in args) {
        if (element[0] == '-') {
            break
        } else {
            options.add(element)
        }
    }
    return options
}

class SchematicProjectOptionsTextField(
    project: Project?,
    options: List<NxProject>
) : TextFieldWithAutoCompletion<NxProject>(project, SchematicProjectCompletionProvider(options), false, null)

private class SchematicProjectCompletionProvider(options: List<NxProject>) :
    TextFieldWithAutoCompletionListProvider<NxProject>(
        options
    ) {

    override fun getLookupString(item: NxProject): String {
        return item.name
    }

    override fun getTypeText(item: NxProject): String? {
        return item.type?.name?.toLowerCase()
    }

    override fun compare(item1: NxProject, item2: NxProject): Int {
        return StringUtil.compare(item1.name, item2.name, false)
    }

    override fun createLookupBuilder(item: NxProject): LookupElementBuilder {
        return super.createLookupBuilder(item)
            .withTypeIconRightAligned(true)
    }

    override fun getIcon(item: NxProject): Icon {
        return if (item.type == NxProject.AngularProjectType.APPLICATION) NxIcons.NX_APP_FOLDER else NxIcons.NX_LIB_FOLDER
    }
}

class EdtTestUtil {
    companion object {
        @JvmStatic fun <V> runInEdtAndGet(computable: ThrowableComputable<V, Throwable>): V =
            runInEdtAndGet { computable.compute() }

        @JvmStatic fun runInEdtAndWait(runnable: ThrowableRunnable<Throwable>) {
            runInEdtAndWait { runnable.run() }
        }
    }
}

/**
 * Consider using Kotlin coroutines and `com.intellij.openapi.application.AppUIExecutor.onUiThread().coroutineDispatchingContext()`
 * @see com.intellij.openapi.application.AppUIExecutor.onUiThread
 */
fun <V> runInEdtAndGet(compute: () -> V): V {
    var v: V? = null
    runInEdtAndWait { v = compute() }
    return v!!
}

/**
 * Consider using Kotlin coroutines and `com.intellij.openapi.application.AppUIExecutor.onUiThread().coroutineDispatchingContext()`
 * @see com.intellij.openapi.application.AppUIExecutor.onUiThread
 */
fun runInEdtAndWait(runnable: () -> Unit) {
    val app = ApplicationManager.getApplication()
    if (app is Application) {
        if (app.isDispatchThread) {
            // reduce stack trace
            runnable()
        } else {
            var exception: Throwable? = null
            app.invokeAndWait {
                try {
                    runnable()
                } catch (e: Throwable) {
                    exception = e
                }
            }

            exception?.let { throw it }
        }
        return
    }

    if (SwingUtilities.isEventDispatchThread()) {
        runnable()
    } else {
        try {
            SwingUtilities.invokeAndWait { runnable() }
        } catch (e: InvocationTargetException) {
            throw e.cause ?: e
        }
    }
}
