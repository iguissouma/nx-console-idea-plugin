package com.github.iguissouma.nxconsole.execution.ui

import com.github.iguissouma.nxconsole.NxBundle
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
import com.intellij.ide.ui.search.SearchUtil
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
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.ColorUtil
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.GuiUtils
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.SearchTextField
import com.intellij.ui.SideBorder
import com.intellij.ui.TextFieldWithHistoryWithBrowseButton
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.layout.CCFlags
import com.intellij.ui.layout.Cell
import com.intellij.ui.layout.CellBuilder
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.InnerCell
import com.intellij.ui.layout.LayoutBuilder
import com.intellij.ui.layout.PropertyBinding
import com.intellij.ui.layout.Row
import com.intellij.ui.layout.RowBuilder
import com.intellij.ui.layout.enteredTextSatisfies
import com.intellij.ui.layout.panel
import com.intellij.ui.layout.selected
import com.intellij.ui.layout.selectedValueIs
import com.intellij.util.Alarm
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.SwingHelper
import com.intellij.util.ui.UIUtil
import com.intellij.webcore.ui.PathShortener
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import javax.swing.AbstractButton
import javax.swing.ButtonGroup
import javax.swing.DefaultComboBoxModel
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.JTextField
import javax.swing.LayoutFocusTraversalPolicy
import javax.swing.SwingConstants
import javax.swing.border.EmptyBorder
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import javax.swing.event.DocumentEvent

class SpinnerPredicate(private val spinner: JBIntSpinner, private val predicate: (Int?) -> Boolean) : ComponentPredicate() {
    override fun addListener(listener: (Boolean) -> Unit) {
        spinner.addChangeListener { listener(invoke()) }
    }

    override fun invoke(): Boolean = predicate(spinner.value as Int)
}

class NxBuildersUiPanel(
    project: Project,
    var command: String,
    var target: String,
    var architect: NxProject.Architect,
    args: MutableList<String>
) :
    NxUiPanel(project, args) {

    private val searchAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD)

    private val searchField = SearchTextField().apply {
        textEditor.emptyText.text = NxBundle.message("nx.search.schematics.options")

        addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                searchAlarm.cancelAllRequests()
                searchAlarm.addRequest({ updateSearch() }, 300)
            }
        })
    }

    private class SettingsRow(
        val row: Row,
        val component: JComponent,
        val id: String,
        val text: String,
        val isDefaultPredicate: ComponentPredicate
    ) {
        lateinit var groupPanel: JPanel
    }

    private val settingsRows = mutableListOf<NxBuildersUiPanel.SettingsRow>()
    private val groupPanels = mutableListOf<JPanel>()
    private lateinit var nothingFoundRow: Row

    private fun updateSearch() {
        applyFilter(searchField.text, false)
    }

    private fun applyFilter(searchText: String?, onlyShowModified: Boolean) {
        if (searchText.isNullOrBlank()) {
            for (groupPanel in groupPanels) {
                groupPanel.isVisible = true
            }
            for (settingsRow in settingsRows) {
                settingsRow.row.visible = true
                settingsRow.row.subRowsVisible = true
                updateMatchText(settingsRow.component, settingsRow.text, null)
            }
            nothingFoundRow.visible = false
            return
        }

        // val searchableOptionsRegistrar = SearchableOptionsRegistrar.getInstance()
        // val filterWords = searchText?.let { searchableOptionsRegistrar.getProcessedWords(it) } ?: emptySet()
        // val filterWordsUnstemmed = searchText?.split(' ') ?: emptySet()
        val visibleGroupPanels = mutableSetOf<JPanel>()
        var matchCount = 0
        for (settingsRow in settingsRows) {
            // val textWords = searchableOptionsRegistrar.getProcessedWords(settingsRow.text)
            val idWords = settingsRow.id.split('.')
            val textMatches = searchText == null || settingsRow.text.contains(searchText, ignoreCase = true) // (filterWords.isNotEmpty() && textWords.any { it.contains(searchText, ignoreCase = true) }) //textWords.containsAll(filterWords))
            // val idMatches =
            //    searchText == null || (filterWordsUnstemmed.isNotEmpty() && idWords.containsAll(filterWordsUnstemmed))
            val modifiedMatches = if (onlyShowModified) !settingsRow.isDefaultPredicate() else true
            // val matches = (textMatches || idMatches) && modifiedMatches
            val matches = textMatches && modifiedMatches
            if (matches) matchCount++

            settingsRow.row.visible = matches
            settingsRow.row.subRowsVisible = matches
            if (matches) {
                // settingsRow.groupPanel.isVisible = true
                // visibleGroupPanels.add(settingsRow.groupPanel)
                val idColor = ColorUtil.toHtmlColor(JBUI.CurrentTheme.ContextHelp.FOREGROUND)
                val baseText = /*if (idMatches && !textMatches)
                    """${settingsRow.text}<br><pre><font color="$idColor">${settingsRow.id}"""
                else*/
                    settingsRow.text
                updateMatchText(settingsRow.component, baseText, searchText)
            }
        }
        for (groupPanel in groupPanels) {
            if (groupPanel !in visibleGroupPanels) {
                groupPanel.isVisible = false
            }
        }
        // nothingFoundRow.visible = visibleGroupPanels.isEmpty()
    }

    private fun updateMatchText(component: JComponent, @NlsSafe baseText: String, @NlsSafe searchText: String?) {
        val text = searchText?.takeIf { it.isNotBlank() }?.let {
            @NlsSafe val highlightedText = SearchUtil.markup(
                baseText, it, UIUtil.getLabelFontColor(UIUtil.FontColor.NORMAL),
                UIUtil.getSearchMatchGradientStartColor()
            )
            "<html>$highlightedText"
        } ?: baseText
        when (component) {
            is JLabel -> component.text = text
            is AbstractButton -> component.text = text
        }
    }

    private var mainPanel: JPanel? = null
    var hasFocus = false
    var focusedComponent = null

    var modelUI = createModelUI()

    private fun createModelUI() = emptyMap<String, Any>().toMutableMap()

    var builderOptions = listOf<NxBuilderOptions>()

    val nxConfig = NxConfigProvider.getNxConfig(project, project.baseDir)

    init {

        builderOptions = runInEdtAndGet {
            NxCliBuildersRegistryService.getInstance()
                .readBuilderSchema(
                    project,
                    nxConfig?.angularJsonFile!!,
                    architect.builder!!
                ) ?: emptyList()
        }

        builderOptions.map { it.name to it.default }.forEach { modelUI[it.first] = it.second }

        centerPanel = createCenterPanel()

        fun apply() {
            (centerPanel as DialogPanel).apply()
        }

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
                apply()
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
                apply()
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
                apply()
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
            ActionManager.getInstance().createActionToolbar("NxBuildersUiPanel", actionGroup, true)
        actionToolbar.setMinimumButtonSize(ActionToolbar.NAVBAR_MINIMUM_BUTTON_SIZE)
        actionToolbar.layoutPolicy = ActionToolbar.AUTO_LAYOUT_POLICY
        // actionToolbar.component.border = IdeBorderFactory.createBorder(SideBorder.TOP + SideBorder.BOTTOM)
        actionToolbar.setMinimumButtonSize(Dimension(22, 22))
        actionToolbar.component.isOpaque = true

        val textFilter: Wrapper = Wrapper()
        textFilter.setVerticalSizeReferent(searchField)
        // textFilter.border = IdeBorderFactory.createBorder(SideBorder.TOP + SideBorder.BOTTOM)

        // val panel = JPanel(MigLayout("ins 0, fill", "[left]0[left, fill]push", "center"))
        val panel = JPanel(BorderLayout())
        panel.border = IdeBorderFactory.createBorder(SideBorder.TOP + SideBorder.BOTTOM)

        GuiUtils.installVisibilityReferent(panel, actionToolbar.component)
        panel.add(actionToolbar.component, BorderLayout.LINE_START)
        panel.add(searchField, BorderLayout.CENTER)

        mainPanel = JPanel(BorderLayout())
        mainPanel!!.add(panel, BorderLayout.NORTH)
        val jbScrollPane = JBScrollPane(centerPanel)
        mainPanel!!.add(jbScrollPane, BorderLayout.CENTER)

        isFocusCycleRoot = true
        focusTraversalPolicy = LayoutFocusTraversalPolicy()

        add(mainPanel)
    }

    enum class OptionSettingType { Int, Bool, String, Enum, Project, Directory }

    private fun NxBuilderOptions.type(): OptionSettingType =
        if (this.type == "boolean") OptionSettingType.Bool
        else if (this.name == "path" || this.name == "directory") OptionSettingType.Directory
        else if (this.name == "project" || this.name == "projectName") OptionSettingType.Project
        else if (this.type == "string" && this.enum.isEmpty()) OptionSettingType.String
        else if (this.type == "string" && this.enum.isNotEmpty()) OptionSettingType.Enum
        else if (this.type == "number") OptionSettingType.Int
        else OptionSettingType.String

    private fun RowBuilder.createComponentRow() {
        for (option in builderOptions) {
            val settingsRowsInGroup = mutableListOf<NxBuildersUiPanel.SettingsRow>()

            val label: JLabel? = if (option.type() == OptionSettingType.Bool)
                null
            else
                JLabel(option.name + ":")

            row(label) {

                lateinit var component: CellBuilder<JComponent>
                cell(isFullWidth = true) {
                    val (c, isDefaultPredicate) = control(option) ?: error("cannot create control for $option")
                    component = c

                    val textComponent = label ?: component.component

                    val row = NxBuildersUiPanel.SettingsRow(
                        this@row,
                        textComponent,
                        option.name!!,
                        label?.text ?: option.name!!,
                        isDefaultPredicate
                    )
                    settingsRows.add(row)
                }

                option.description?.let { description -> component.comment(description, 70, true) }
            }
        }

        nothingFoundRow = row {
            label(NxBundle.message("nx.search.flags.nothing.found"))
                .constraints(CCFlags.growX, CCFlags.growY)
                .also {
                    it.component.foreground = UIUtil.getInactiveTextColor()
                    it.component.horizontalAlignment = SwingConstants.CENTER
                }
        }.also {
            it.visible = false
        }
    }

    data class OptionSettingControl(
        val cellBuilder: CellBuilder<JComponent>,
        val isDefault: ComponentPredicate,
        val reset: () -> Unit
    )

    private fun InnerCell.control(option: NxBuilderOptions): OptionSettingControl? {
        return when (option.type()) {
            OptionSettingType.Bool -> {
                val cb = checkBox(
                    option.name,
                    { modelUI[option.name] as? Boolean ?: false },
                    { modelUI[option.name] = it }
                )
                OptionSettingControl(cb, cb.component.selected, {})
            }
            OptionSettingType.String -> {
                val textField = textField(
                    { modelUI[option.name!!] as? String ?: "" },
                    { modelUI[option.name!!] = it },
                )
                OptionSettingControl(
                    textField,
                    textField.component.enteredTextSatisfies { it == option.default },
                    { textField.component.text = option.default as String }
                )
            }
            OptionSettingType.Enum -> {
                val comboBoxModel: CollectionComboBoxModel<String> = CollectionComboBoxModel(option.enum)
                val cb = comboBox(
                    comboBoxModel,
                    { modelUI[option.name!!] as? String ?: "" },
                    { modelUI[option.name!!] = it ?: "" }
                )
                OptionSettingControl(
                    cb,
                    cb.component.selectedValueIs(option.default as String),
                    { cb.component.selectedItem = option.default }
                )
            }

            OptionSettingType.Int -> {
                val textField = spinner(
                    { modelUI[option.name!!].toString().toIntOrNull() ?: 0 },
                    { modelUI[option.name!!] = it },
                    0,
                    Int.MAX_VALUE
                )
                OptionSettingControl(
                    textField,
                    SpinnerPredicate(textField.component, { it == option.default.toIntOrNull() }),
                    { textField.component.value = option.default.toIntOrNull() }
                )
            }

            OptionSettingType.Directory -> {
                val textField = textFieldWithHistoryWithBrowseButton(
                    { modelUI[option.name!!] as? String ?: "" },
                    { modelUI[option.name!!] = it },
                    "Select ${option.name}",
                    project,
                    FileChooserDescriptorFactory.createSingleFolderDescriptor()
                )
                PathShortener.enablePathShortening(
                    textField.component.childComponent.textEditor,
                    JTextField(project.basePath)
                )
                OptionSettingControl(
                    textField,
                    textField.component.childComponent.textEditor.enteredTextSatisfies { it == option.default.toString() },
                    { textField.component.text = option.default.toString() }
                )
            }

            OptionSettingType.Project -> {
                val textField = SchematicProjectOptionsTextFieldCell(project, this).schematicProjectOptionsTextField(
                    { modelUI[option.name!!] as? String ?: "" },
                    { modelUI[option.name!!] = it },
                )

                OptionSettingControl(
                    textField,
                    textField.component.enteredTextSatisfies { it == option.default.toString() },
                    { textField.component.text = option.default as String }
                )
            }
        }
    }

    private fun SchematicProjectOptionsTextField.enteredTextSatisfies(predicate: (String) -> Boolean): ComponentPredicate {
        return TextCompletionComponentPredicate(this, predicate)
    }

    private class TextCompletionComponentPredicate(
        private val component: SchematicProjectOptionsTextField,
        private val predicate: (String) -> Boolean
    ) : ComponentPredicate() {
        override fun invoke(): Boolean = predicate(component.text)

        override fun addListener(listener: (Boolean) -> Unit) {
            component.document.addDocumentListener(object : DocumentListener {
                override fun documentChanged(event: com.intellij.openapi.editor.event.DocumentEvent) {
                    listener(invoke())
                }
            })
        }
    }

    class SchematicProjectOptionsTextFieldCell(val project: Project, val cell: Cell) : Cell() {

        fun schematicProjectOptionsTextField(
            getter: () -> String,
            setter: (String) -> Unit,
        ): CellBuilder<SchematicProjectOptionsTextField> {
            val textFiled = SchematicProjectOptionsTextField(
                project = project,
                NxConfigProvider.getNxConfig(project, project.baseDir)?.projects ?: emptyList()
            )
            val modelBinding = PropertyBinding(getter, setter)
            textFiled.text = modelBinding.get()
            return textFiled()
                .constraints(CCFlags.growX)
                .withBinding(
                    SchematicProjectOptionsTextField::getText,
                    SchematicProjectOptionsTextField::setText,
                    modelBinding
                )
        }

        override fun <T : JComponent> component(component: T): CellBuilder<T> {
            return cell.component(component)
        }

        override fun <T : JComponent> component(component: T, viewComponent: JComponent): CellBuilder<T> {
            return cell.component(component, viewComponent)
        }

        override fun withButtonGroup(title: String?, buttonGroup: ButtonGroup, body: () -> Unit) {
            cell.withButtonGroup(title, buttonGroup, body)
        }
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
            createComponentRow()
        }.apply {
            border = EmptyBorder(10, 15, 4, 15)
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
