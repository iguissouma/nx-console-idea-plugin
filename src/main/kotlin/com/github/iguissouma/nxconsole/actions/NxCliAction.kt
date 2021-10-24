package com.github.iguissouma.nxconsole.actions

import com.github.iguissouma.nxconsole.cli.config.NxProject
import com.github.iguissouma.nxconsole.execution.DefaultNxUiFile
import com.github.iguissouma.nxconsole.execution.ui.NxBuildersUiPanel
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

class NxCliAction(
    val command: String,
    val target: String,
    val architect: NxProject.Architect,
    val virtualFile: VirtualFile,
    text: String?,
    description: String?,
    icon: Icon?
) : AnAction(text, description, icon) {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val vFile = DefaultNxUiFile(
            "${command.capitalize()}.nx",
            NxBuildersUiPanel(project, command, target, architect, mutableListOf())
        )
        val fem = FileEditorManager.getInstance(project)
        // close file if isOpened to display another schematic
        if (fem.isFileOpen(vFile)) {
            fem.closeFile(vFile)
        }
        fem.openFile(vFile, true)
    }
}
