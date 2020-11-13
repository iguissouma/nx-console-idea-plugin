package com.github.iguissouma.nxconsole.actions

import com.github.iguissouma.nxconsole.buildTools.NxJsonUtil
import com.github.iguissouma.nxconsole.execution.DefaultNxUiFile
import com.github.iguissouma.nxconsole.execution.NxUiPanel
import com.intellij.json.psi.JsonObject
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import org.angular2.cli.Schematic
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
        val pathArgument = "--path=${VfsUtilCore.getRelativePath(virtualFile, project.baseDir)}"
        val nxJson = NxJsonUtil.findChildNxJsonFile(project.baseDir) ?: return
        args.add(pathArgument)
        val nxProjectsProperty = NxJsonUtil.findProjectsProperty(project, nxJson)
        val projectsList = (nxProjectsProperty?.value as? JsonObject)?.propertyList?.map { it.name } ?: emptyList()
        val projectArgument = projectsList.firstOrNull { virtualFile.path.contains("/$it/") }
            ?.let { "--project=$it" }
        if (projectArgument != null) {
            args.add(projectArgument)
        }
        val vFile = DefaultNxUiFile("Generate.nx", NxUiPanel(project, schematic, args))
        FileEditorManager.getInstance(project).openFile(vFile, true)
    }
}
