package com.github.iguissouma.nxconsole.execution

import com.github.iguissouma.nxconsole.NxBundle
import com.github.iguissouma.nxconsole.NxIcons
import com.github.iguissouma.nxconsole.schematics.Option
import com.github.iguissouma.nxconsole.schematics.Schematic
import com.intellij.diff.util.FileEditorBase
import com.intellij.icons.AllIcons
import com.intellij.ide.FileIconProvider
import com.intellij.ide.actions.SplitAction
import com.intellij.javascript.nodejs.CompletionModuleInfo
import com.intellij.javascript.nodejs.NodeModuleSearchUtil
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.lang.javascript.boilerplate.NpmPackageProjectGenerator
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.SideBorder
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.LayoutBuilder
import com.intellij.ui.layout.Row
import com.intellij.ui.layout.panel
import com.intellij.vcs.log.impl.VcsLogContentUtil
import com.intellij.vcs.log.impl.VcsLogTabsManager
import org.angular2.cli.AngularCliFilter
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.lang.Integer.min
import javax.swing.DefaultComboBoxModel
import javax.swing.Icon
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.LayoutFocusTraversalPolicy
import javax.swing.border.EmptyBorder
import javax.swing.event.DocumentEvent

class NxUiIconProvider : FileIconProvider {
    override fun getIcon(file: VirtualFile, flags: Int, project: Project?): Icon? = (file as? NxUiFile)?.fileType?.icon
}

class NxUiFileType : FileType {
    override fun getName(): String = "NxUi"
    override fun getDescription(): String = ""
    override fun getDefaultExtension(): String = ".nx"
    override fun getIcon(): Icon? = NxIcons.NRWL_ICON
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

class NxUiPanel(project: Project, schematic: Schematic, args: MutableList<String>) : JPanel(BorderLayout()) {

    var hasFocus = false

    var centerPanel: DialogPanel? = null

    var modelUI = (schematic.arguments + schematic.options)
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
        val filter = AngularCliFilter(project, project.baseDir.path)

        centerPanel = panel {
            /*titledRow("Arguments") {
                schematic.arguments.forEachIndexed { index, option ->
                    addRow(option)
                }
            }
            titledRow("Options") {
                schematic.options.filter { it.name !in ignoredOptions() }.forEach { option ->
                    addRow(option)
                }
            }*/
            schematic.options.filter { it.name !in ignoredOptions() }.forEach { option ->
                addRow(option)
            }
        }.apply {
            border = EmptyBorder(10, 10, 4, 15)
            // requestFocusInWindow(true)
        }

        val actionGroup = DefaultActionGroup()
        val run: AnAction = object : AnAction("Run", "", AllIcons.Actions.Run_anything) {
            init {
                // shortcutSet = CustomShortcutSet(*KeymapManager.getInstance().activeKeymap.getShortcuts("Refresh"))
            }

            override fun actionPerformed(e: AnActionEvent) {
                NpmPackageProjectGenerator.generate(
                    interpreter!!, NodePackage(module?.virtualFile?.path!!),
                    { pkg -> pkg.findBinFile("nx", null)?.absolutePath },
                    cli, VfsUtilCore.virtualToIoFile(workingDir ?: cli), project,
                    null, arrayOf(filter), "generate", schematic.name,
                    *computeArgsFromModelUi()
                        .toTypedArray()
                )
            }
        }

        val dryRun: AnAction = object : AnAction("Dry Run", "", AllIcons.RunConfigurations.RemoteDebug) {
            init {
                // shortcutSet = CustomShortcutSet(*KeymapManager.getInstance().activeKeymap.getShortcuts("Run"))
            }

            override fun actionPerformed(e: AnActionEvent) {
                NpmPackageProjectGenerator.generate(
                    interpreter!!, NodePackage(module?.virtualFile?.path!!),
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
        actionGroup.add(TextLabelAction("  nx generate ${schematic.name}"))
        actionGroup.addAction(run)
        actionGroup.addAction(dryRun)

        val actionToolbar =
            ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, actionGroup, true)
        actionToolbar.setMinimumButtonSize(ActionToolbar.NAVBAR_MINIMUM_BUTTON_SIZE)
        actionToolbar.layoutPolicy = ActionToolbar.AUTO_LAYOUT_POLICY
        actionToolbar.component.border = IdeBorderFactory.createBorder(SideBorder.TOP + SideBorder.BOTTOM)
        actionToolbar.setMinimumButtonSize(Dimension(22, 22))
        actionToolbar.component.isOpaque = true

        val mainPanel = JPanel(BorderLayout())
        mainPanel.add(actionToolbar.component, BorderLayout.NORTH)
        val jbScrollPane = JBScrollPane(centerPanel)
        mainPanel.add(jbScrollPane, BorderLayout.CENTER)
        actionToolbar.setTargetComponent(jbScrollPane)

        isFocusCycleRoot = true
        focusTraversalPolicy = LayoutFocusTraversalPolicy()
        // isFocusable = true
        // isRequestFocusEnabled = true

        add(mainPanel)
        // panel.isFocusable = true

        /*UIUtil.invokeLaterIfNeeded {
            //panel.requestFocusInWindow()
            //panel.preferredFocusedComponent?.requestFocus()
            //panel.preferredFocusedComponent?.grabFocus()
            //panel.preferredFocusedComponent?.let { IdeFocusManager.getInstance(project).requestFocus(it, true) }
            centerPanel.preferredFocusedComponent?.let {
                IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown {
                    IdeFocusManager.getGlobalInstance().requestFocus(it, true)
                }
            }


        }*/
    }

    private fun ignoredOptions() = listOf("dryRun", "linter", "strict", "force", "importantPath")

    private fun LayoutBuilder.addRow(option: Option) {
        row(option.takeIf { it.type == "string" }?.let { "${it.name}:" }) {
            buildComponentForOption<JComponent>(option)
        }
    }

    private inline fun <T : JComponent> Row.buildComponentForOption(option: Option) {
        when {
            option.type?.toLowerCase() == "string" && option.enum.isNullOrEmpty() -> buildTextField(option)
            option.type?.toLowerCase() == "string" && option.enum.isNotEmpty() -> buildSelectField(option)
            option.type?.toLowerCase() == "boolean" -> buildCheckboxField(option)
            else -> buildTextField(option)
        }
    }

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
        comboBox.selectedItem = modelUI[option.name] ?: option.enum.first()
        comboBox.addActionListener {
            modelUI[option.name ?: ""] = (comboBox.selectedItem as? String) ?: ""
        }
        comboBox()
    }

    private inline fun Row.buildTextField(option: Option) {
        val jTextField = JBTextField()
        // jTextField.emptyText.text = option.description ?: ""
        option.default?.let {
            // jTextField.text = it as? String ?: ""
        }
        jTextField.text = modelUI[option.name] as? String ?: ""
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
