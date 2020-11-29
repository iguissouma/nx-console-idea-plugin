package com.github.iguissouma.nxconsole.actions

import com.github.iguissouma.nxconsole.NxIcons
import com.github.iguissouma.nxconsole.buildTools.NxJsonUtil
import com.github.iguissouma.nxconsole.schematics.NxCliSchematicsRegistryService
import com.github.iguissouma.nxconsole.schematics.Schematic
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.application.ApplicationManager

class NxNewGenerateActionGroup : ActionGroup(
    "Nx Generate (Ui)...",
    "Nx generate (ui)",
    NxIcons.NRWL_ICON
) {

    var schematics: MutableCollection<Schematic> = mutableListOf()

    override fun getChildren(event: AnActionEvent?): Array<AnAction> {
        // val psiFile = event?.getData(LangDataKeys.PSI_FILE) ?: return emptyArray()
        val virtualFile = event?.getData(LangDataKeys.VIRTUAL_FILE) ?: return emptyArray()
        val project = event.project ?: return emptyArray()
        val nxJson = NxJsonUtil.findChildNxJsonFile(project.baseDir) ?: return emptyArray()
        if (schematics.isEmpty()) {
            ApplicationManager.getApplication().executeOnPooledThread {
                val schematicsInWorkspace =
                    NxCliSchematicsRegistryService.getInstance().getSchematics(project, project.baseDir)
                ApplicationManager.getApplication().invokeLater {
                    schematics.clear()
                    schematicsInWorkspace.forEach { schematics.add(it) }
                }
            }
        }
        return schematics.map { NxNewGenerateAction(it, virtualFile, it.name, it.description, NxIcons.NRWL_ICON) }
            .toTypedArray()
    }
}
