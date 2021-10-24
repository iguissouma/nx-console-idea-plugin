package com.github.iguissouma.nxconsole.execution.ui

import com.github.iguissouma.nxconsole.NxIcons
import com.github.iguissouma.nxconsole.buildTools.NxRunSettings
import com.github.iguissouma.nxconsole.buildTools.NxService
import com.github.iguissouma.nxconsole.buildTools.rc.NxConfigurationType
import com.github.iguissouma.nxconsole.buildTools.rc.NxRunConfiguration
import com.github.iguissouma.nxconsole.builders.NxBuilderOptions
import com.github.iguissouma.nxconsole.builders.NxCliBuildersRegistryService
import com.github.iguissouma.nxconsole.cli.config.NxConfigProvider
import com.github.iguissouma.nxconsole.cli.config.NxProject
import com.github.iguissouma.nxconsole.execution.NxUiPanel
import com.github.iguissouma.nxconsole.execution.SchematicProjectOptionsTextField
import com.github.iguissouma.nxconsole.execution.runInEdtAndGet
import com.intellij.execution.Executor
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.SideBorder
import com.intellij.ui.TextFieldWithHistoryWithBrowseButton
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.CCFlags
import com.intellij.ui.layout.LayoutBuilder
import com.intellij.ui.layout.Row
import com.intellij.ui.layout.panel
import com.intellij.util.ui.SwingHelper
import com.intellij.webcore.ui.PathShortener
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import javax.swing.DefaultComboBoxModel
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.JTextField
import javax.swing.LayoutFocusTraversalPolicy
import javax.swing.border.EmptyBorder
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import javax.swing.event.DocumentEvent

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
        actionGroup.add(NxGenerateUiPanel.TextLabelAction("  nx $command "))
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
                // default value here is always string
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
