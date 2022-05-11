package com.github.iguissouma.nxconsole.execution.ui

import com.github.iguissouma.nxconsole.NxBundle
import com.github.iguissouma.nxconsole.cli.NxCliFilter
import com.github.iguissouma.nxconsole.cli.config.NxConfigProvider
import com.github.iguissouma.nxconsole.execution.NxGenerator
import com.github.iguissouma.nxconsole.execution.NxUiPanel
import com.github.iguissouma.nxconsole.execution.SchematicProjectOptionsTextField
import com.github.iguissouma.nxconsole.execution.parseArguments
import com.github.iguissouma.nxconsole.execution.parseOptions
import com.github.iguissouma.nxconsole.schematics.NxCliSchematicsRegistryService
import com.github.iguissouma.nxconsole.schematics.Option
import com.github.iguissouma.nxconsole.schematics.Schematic
import com.intellij.icons.AllIcons
import com.intellij.ide.ui.search.SearchUtil
import com.intellij.javascript.nodejs.CompletionModuleInfo
import com.intellij.javascript.nodejs.NodeModuleSearchUtil
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
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
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.ColorUtil
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.GuiUtils
import com.intellij.ui.IdeBorderFactory
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
import javax.swing.JTextField
import javax.swing.LayoutFocusTraversalPolicy
import javax.swing.SwingConstants
import javax.swing.border.EmptyBorder
import javax.swing.event.DocumentEvent

class NxGenerateUiPanel(project: Project, var schematic: Schematic, args: MutableList<String>) :
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

    private val settingsRows = mutableListOf<SettingsRow>()
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

    private fun createModelUI() = (schematic.arguments + schematic.options)
        .filterNot { it.name == null }.associate { it.name!! to it.default }.toMutableMap()

    init {

        val parsedArgs: List<String> = parseArguments(args.toTypedArray())
            .filter { it != schematic.name }

        (0 until Integer.min(parsedArgs.size, schematic.arguments.size)).forEach {
            modelUI[schematic.arguments[it].name ?: ""] = parsedArgs[it]
        }

        parseOptions(args.toTypedArray())
            .filterKeys { modelUI.containsKey(it) }
            .forEach { (t: String, u: List<String>) ->
                val get = modelUI[t]
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

        fun apply() {
            (centerPanel as DialogPanel).apply()
        }

        val actionGroup = DefaultActionGroup()
        val run: AnAction = object : AnAction("Run", "", AllIcons.Actions.Execute) {
            init {
                // shortcutSet = CustomShortcutSet.fromString(if (SystemInfo.isMac) "meta ENTER" else "control ENTER")
                registerCustomShortcutSet(
                    CustomShortcutSet.fromString(if (SystemInfo.isMac) "meta ENTER" else "control ENTER"),
                    this@NxGenerateUiPanel
                )
            }

            override fun actionPerformed(e: AnActionEvent) {
                apply()
                NxGenerator().generate(
                    interpreter!!,
                    // NodePackage(module?.virtualFile?.path!!),
                    // { pkg -> pkg!!.findBinFile("nx", null)?.absolutePath },
                    cli,
                    VfsUtilCore.virtualToIoFile(workingDir ?: cli),
                    project,
                    null,
                    "Generating",
                    arrayOf(filter),
                    *computeGenerateRunCommand(schemaName = schematic.name).toTypedArray(),
                    *computeArgsFromModelUi()
                        .toTypedArray()
                )
            }
        }

        val dryRun: AnAction = object : AnAction("Dry Run", "", AllIcons.Actions.StartDebugger) {
            init {
                // shortcutSet = CustomShortcutSet.fromString("shift ENTER")
                registerCustomShortcutSet(CustomShortcutSet.fromString("shift ENTER"), this@NxGenerateUiPanel)
            }

            override fun actionPerformed(e: AnActionEvent) {
                apply()
                NxGenerator().generate(
                    interpreter!!,
                    // NodePackage(module?.virtualFile?.path!!),
                    // { pkg -> pkg!!.findBinFile("nx", null)?.absolutePath },
                    cli,
                    VfsUtilCore.virtualToIoFile(workingDir ?: cli),
                    project,
                    null,
                    "Generating",
                    arrayOf(filter),
                    *computeGenerateRunCommand(schemaName = schematic.name).toTypedArray(),
                    *computeArgsFromModelUi()
                        .toTypedArray(),
                    "--dry-run",
                    "--no-interactive"
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
                apply()
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
                presentation.text = this@NxGenerateUiPanel.schematic.name
                templatePresentation.text = this@NxGenerateUiPanel.schematic.name
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
                                centerPanel = this@NxGenerateUiPanel.createCenterPanel()

                                this@NxGenerateUiPanel.mainPanel?.remove(1)
                                this@NxGenerateUiPanel.mainPanel?.add(JBScrollPane(centerPanel), BorderLayout.CENTER)

                                (button as ComboBoxButton).text = it.name
                                this@NxGenerateUiPanel.modelUI = createModelUI()
                                this@NxGenerateUiPanel.validate()
                                this@NxGenerateUiPanel.repaint()
                            }
                        }
                    )
                }
                return group
            }

            private fun selectSchematic(schematic: Schematic) {
                this@NxGenerateUiPanel.schematic = schematic
            }
        }
        // actionGroup.add(ActionManager.getInstance().getAction(VcsLogActionPlaces.TEXT_FILTER_SETTINGS_ACTION_GROUP))
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

    private fun Option.type(): OptionSettingType =
        if (this.type == "boolean") OptionSettingType.Bool
        else if (this.name == "path" || this.name == "directory") OptionSettingType.Directory
        else if (this.name == "project" || this.name == "projectName") OptionSettingType.Project
        else if (this.type == "string" && this.enum.isEmpty()) OptionSettingType.String
        else if (this.type == "string" && this.enum.isNotEmpty()) OptionSettingType.Enum
        else if (this.type == "number") OptionSettingType.Int
        else OptionSettingType.String

    private fun RowBuilder.createComponentRow() {
        for (option in schematic.options) {
            val settingsRowsInGroup = mutableListOf<SettingsRow>()

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

                    val row = SettingsRow(
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

    private fun InnerCell.control(option: Option): OptionSettingControl? {
        return when (option.type()) {
            OptionSettingType.Bool -> {
                val cb = checkBox(
                    option.name!!,
                    { modelUI[option.name!!] as? Boolean ?: false },
                    { modelUI[option.name!!] = it }
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
                val comboBoxModel =
                    CollectionComboBoxModel(if ((option.default as? String) == null) listOf("") + option.enum else option.enum)
                val cb = comboBox(
                    comboBoxModel,
                    { modelUI[option.name!!] as? String ?: "" },
                    { modelUI[option.name!!] = it }
                )
                OptionSettingControl(
                    cb,
                    cb.component.selectedValueIs(option.default as? String ?: ""),
                    { cb.component.selectedItem = option.default as? String ?: "" }
                )
            }

            OptionSettingType.Int -> {
                val textField = intTextField(
                    { modelUI[option.name!!] as? Int ?: 0 },
                    { modelUI[option.name!!] = it },
                    columns = 10
                )
                OptionSettingControl(
                    textField,
                    textField.component.enteredTextSatisfies { it == option.default.toString() },
                    { textField.component.text = option.default.toString() }
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

    private fun createCenterPanel(): DialogPanel {
        return panel {
            createComponentRow()
        }.apply {
            border = EmptyBorder(10, 15, 4, 15)
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
                // default value here is Any it can be string boolean ...
                schematic.options.first { o -> o.name == it.key }.default == it.value // filter value are equal to default.
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
            // templatePresentation.isEnabled = false
        }
    }

    override fun dispose() {
    }
}
