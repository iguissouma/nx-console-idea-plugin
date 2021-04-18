package com.github.iguissouma.nxconsole.actions

import com.github.iguissouma.nxconsole.NxIcons
import com.github.iguissouma.nxconsole.schematics.NxCliSchematicsRegistryService
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys

class NxNewGenerateActionGroup : ActionGroup(
    "Nx Generate (Ui)...",
    "Nx generate (ui)",
    NxIcons.NRWL_ICON
) {

    override fun getChildren(event: AnActionEvent?): Array<AnAction> {
        val virtualFile = event?.getData(LangDataKeys.VIRTUAL_FILE) ?: return emptyArray()
        val project = event.project ?: return emptyArray()
        return NxCliSchematicsRegistryService.getInstance().getSchematics(project, project.baseDir)
            .map { NxNewGenerateAction(it, virtualFile, it.name, it.description, NxIcons.NRWL_ICON) }
            .toTypedArray()
    }
}
