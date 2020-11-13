package com.github.iguissouma.nxconsole.actions

import com.github.iguissouma.nxconsole.NxIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import org.angular2.cli.AngularCliSchematicsRegistryService
import org.angular2.cli.Schematic

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
        if (schematics.isEmpty()) {
            schematics = AngularCliSchematicsRegistryService.getInstance().getSchematics(project, project.baseDir)
        }
        return schematics.map { NxNewGenerateAction(it, virtualFile, it.name, it.description, NxIcons.NRWL_ICON) }
            .toTypedArray()
    }
}
