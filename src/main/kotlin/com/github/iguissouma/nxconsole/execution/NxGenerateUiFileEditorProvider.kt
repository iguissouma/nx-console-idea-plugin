package com.github.iguissouma.nxconsole.execution

import com.github.iguissouma.nxconsole.NxBundle
import com.github.iguissouma.nxconsole.NxIcons
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

internal class DefaultNxUiFile(name: String, panel: NxUiPanel) : NxUiFile(name) {
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
}

class NxUiPanel(project: Project, schematic: Schematic) : JPanel(BorderLayout()) {
    var modelUI = (schematic.arguments + schematic.options)
        .filterNot { it.name == null }
        .map { it.name!! to it.default }.toMap().toMutableMap()

    init {

        val modules: MutableList<CompletionModuleInfo> = mutableListOf()
        NodeModuleSearchUtil.findModulesWithName(modules, "@nrwl/cli", project.baseDir, null)
        val interpreter = NodeJsInterpreterManager.getInstance(project).interpreter
        // TODO check current directory
        val cli = project.baseDir
        val workingDir = project.baseDir

        val module = modules.firstOrNull()
        val filter = AngularCliFilter(project, project.baseDir.path)

        val panel = panel {

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
            border = EmptyBorder(10, 10, 4, 15)
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
            ActionManager.getInstance().createActionToolbar("top", actionGroup, true)
        actionToolbar.setMinimumButtonSize(ActionToolbar.NAVBAR_MINIMUM_BUTTON_SIZE)
        actionToolbar.layoutPolicy = ActionToolbar.AUTO_LAYOUT_POLICY
        actionToolbar.component.border = IdeBorderFactory.createBorder(SideBorder.TOP + SideBorder.BOTTOM)

        val mainPanel = JPanel(BorderLayout())
        mainPanel.add(actionToolbar.component, BorderLayout.NORTH)
        mainPanel.add(JBScrollPane(panel), BorderLayout.CENTER)
        actionToolbar.setTargetComponent(panel)

        add(mainPanel)
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
        comboBox.addActionListener {
            modelUI[option.name ?: ""] = (comboBox.selectedItem as? String) ?: ""
        }
        comboBox()
    }

    private inline fun Row.buildTextField(option: Option) {
        val jTextField = JBTextField()
        // jTextField.emptyText.text = option.description ?: ""
        option.default?.let {
            jTextField.text = it as? String ?: ""
        }
        val docListener: javax.swing.event.DocumentListener = object : DocumentAdapter() {
            private fun updateValue() {
                modelUI[option.name ?: ""] = jTextField.text
            }

            override fun textChanged(e: DocumentEvent) {
                updateValue()
            }
        }
        jTextField.document.addDocumentListener(docListener)
        jTextField(comment = option.description)
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
    internal val rootComponent: JComponent = JPanel(BorderLayout()).also {
        it.add(nxUiFile.createMainComponent(project), BorderLayout.CENTER)
    }

    override fun getComponent(): JComponent = rootComponent
    override fun getPreferredFocusedComponent(): JComponent? =
        VcsLogContentUtil.getLogUis(component).firstOrNull()?.mainComponent

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
