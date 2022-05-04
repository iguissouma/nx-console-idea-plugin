package com.github.iguissouma.nxconsole.actions

import com.github.iguissouma.nxconsole.NxIcons
import com.github.iguissouma.nxconsole.buildTools.NxJsonUtil
import com.github.iguissouma.nxconsole.cli.NxCliFilter
import com.github.iguissouma.nxconsole.cli.config.NxConfigProvider
import com.github.iguissouma.nxconsole.execution.NxGenerator
import com.intellij.javascript.nodejs.CompletionModuleInfo
import com.intellij.javascript.nodejs.NodeModuleSearchUtil
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.TextFieldWithHistory
import com.intellij.ui.TextFieldWithHistoryWithBrowseButton
import com.intellij.ui.components.JBLabelDecorator
import com.intellij.ui.layout.panel
import java.awt.Dimension
import java.io.File
import javax.swing.JComponent
import javax.swing.event.DocumentEvent

val TEXT_FIELD_WITH_HISTORY_WHOLE_TEXT: TextComponentAccessor<TextFieldWithHistory> =
    object : TextComponentAccessor<TextFieldWithHistory> {
        override fun getText(textField: TextFieldWithHistory): String {
            return textField.text
        }

        override fun setText(textField: TextFieldWithHistory, text: String) {
            textField.text = text
        }
    }

class NxMoveLibOrAppAction : AnAction(NxIcons.NRWL_ICON) {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        // TODO check if it's monorepo
        val findChildNxJsonFile = NxJsonUtil.findChildNxJsonFile(project.baseDir) ?: return
        val appOrLibDirectory = event.getData(PlatformDataKeys.VIRTUAL_FILE) ?: return
        if (!appOrLibDirectory.isDirectory) {
            return
        }
        val nxConfig = NxConfigProvider.getNxConfig(project, appOrLibDirectory) ?: return
        val files = nxConfig.projects.map { it.rootDir to it }.toMap()
        if (files.containsKey(appOrLibDirectory)) {
            // show dialog
            val nxMoveLibOrAppDialog = NxMoveLibOrAppDialog(project, appOrLibDirectory)
            if (nxMoveLibOrAppDialog.showAndGet()) {
                // TODO check current directory
                val cli = project.baseDir
                val workingDir = project.baseDir
                // TODO check use global or local
                val modules: MutableList<CompletionModuleInfo> = mutableListOf()
                NodeModuleSearchUtil.findModulesWithName(modules, "@nrwl/cli", project.baseDir, null)
                val module = modules.firstOrNull() ?: return
                val filter = NxCliFilter(project, project.baseDir.path)
                val interpreter = NodeJsInterpreterManager.getInstance(project).interpreter ?: return
                val args = arrayOf(
                    "@nrwl/workspace:move",
                    "--project",
                    files[appOrLibDirectory]?.name,
                    nxMoveLibOrAppDialog.myTargetDirectoryField.text.replace(project.baseDir.path, "")
                        .replace("/libs", "")
                        .replace("/apps", "")

                )
                NxGenerator().generate(
                    interpreter,
                    // NodePackage(module.virtualFile?.path!!),
                    // { pkg -> pkg?.findBinFile("nx", null)?.absolutePath },
                    cli,
                    VfsUtilCore.virtualToIoFile(workingDir ?: cli),
                    project,
                    {
                    },
                    "Move",
                    arrayOf(filter),
                    "generate",
                    *args
                )
            }
        }
    }

    internal class NxMoveLibOrAppDialog(val project: Project, val appOrLibDirectory: VirtualFile) : DialogWrapper(
        project
    ) {

        val myLabel = JBLabelDecorator.createJBLabelDecorator().setBold(true)

        val myTargetDirectoryField = TextFieldWithHistoryWithBrowseButton()

        init {
            title = "Move"

            myLabel.text = "Move Nx App or Lib"
            myLabel.preferredSize = Dimension(600, 15)

            init()
        }

        override fun createCenterPanel(): JComponent {

            val initialTargetPath = appOrLibDirectory.presentableUrl
            myTargetDirectoryField.childComponent.text = initialTargetPath
            val lastDirectoryIdx = initialTargetPath.lastIndexOf(File.separator)
            val textLength = initialTargetPath.length
            if (lastDirectoryIdx > 0 && lastDirectoryIdx + 1 < textLength) {
                myTargetDirectoryField.childComponent.textEditor.select(lastDirectoryIdx + 1, textLength)
            }

            val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()

            myTargetDirectoryField.addBrowseFolderListener(
                "Select Target Directory",
                "App or Lib will be moved to this directory",
                project,
                descriptor,
                TEXT_FIELD_WITH_HISTORY_WHOLE_TEXT
            )

            val textField = myTargetDirectoryField.childComponent.textEditor
            FileChooserFactory.getInstance().installFileCompletion(textField, descriptor, true, disposable)
            textField.document.addDocumentListener(
                object : DocumentAdapter() {
                    override fun textChanged(e: DocumentEvent) {
// validateButtons()
                    }
                }
            )

            val shortcutText = KeymapUtil.getFirstKeyboardShortcutText(
                ActionManager.getInstance().getAction(IdeActions.ACTION_CODE_COMPLETION)
            )

            return panel {
                row { myLabel() }
                row("To directory:") {
                    myTargetDirectoryField(comment = "Use $shortcutText for path completion\n")
                }
            }
        }

        override fun getPreferredFocusedComponent(): JComponent {
            return myTargetDirectoryField
        }
    }
}
