package com.github.iguissouma.nxconsole.actions

import com.github.iguissouma.nxconsole.cli.config.NxConfig
import com.github.iguissouma.nxconsole.cli.config.NxConfigProvider
import com.github.iguissouma.nxconsole.cli.config.NxProject
import com.github.iguissouma.nxconsole.execution.DefaultNxUiFile
import com.github.iguissouma.nxconsole.execution.ui.NxGenerateUiPanel
import com.github.iguissouma.nxconsole.readers.ReadCollectionsOptions
import com.github.iguissouma.nxconsole.readers.readCollections
import com.github.iguissouma.nxconsole.schematics.Schematic
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

class NxNewGenerateAction(
    val schematic: Schematic,
    val virtualFile: VirtualFile,
    text: String?,
    description: String?,
    icon: Icon?
) : AnAction(
    text,
    description,
    icon
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val args = mutableListOf<String>()
        val nxConfig: NxConfig = NxConfigProvider.getNxConfig(project, virtualFile) ?: return
        val relativePath = VfsUtilCore.getRelativePath(virtualFile, nxConfig.angularJsonFile.parent)
        val pathArgument = relativePath?.let { "--path=$relativePath" }
        if (pathArgument != null) {
            args.add(pathArgument)
        }
        val nxProject: NxProject? = nxConfig.getProject(virtualFile)
        val projectArgument = nxProject?.let { "--project=${it.name}" }
        if (projectArgument != null) {
            args.add(projectArgument)
        }
        val vFile = DefaultNxUiFile("Generate.nx", NxGenerateUiPanel(project, schematic, args))
        val fem = FileEditorManager.getInstance(project)
        // close file if isOpened to display another schematic
        if (fem.isFileOpen(vFile)) {
            fem.closeFile(vFile)
        }
        fem.openFile(vFile, true)
    }
}
