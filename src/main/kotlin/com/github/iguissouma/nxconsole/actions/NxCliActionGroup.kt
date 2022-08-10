package com.github.iguissouma.nxconsole.actions

import com.github.iguissouma.nxconsole.NxIcons
import com.github.iguissouma.nxconsole.cli.config.NxConfigProvider
import com.github.iguissouma.nxconsole.cli.config.NxProject
import com.github.iguissouma.nxconsole.cli.config.WorkspaceType
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import java.util.*

class NxCliActionGroup : ActionGroup() {

    override fun update(e: AnActionEvent) {
        val project = e.getData(LangDataKeys.PROJECT) ?: return
        val file = e.getData(LangDataKeys.VIRTUAL_FILE) ?: return
        val nxWorkspaceType = NxConfigProvider.getNxWorkspaceType(project, file)
        if (nxWorkspaceType == WorkspaceType.UNKNOWN) {
            e.presentation.isEnabledAndVisible = false
            return
        }
        val label = nxWorkspaceType.label().lowercase().capitalized()
        e.presentation.text = "$label Task (Ui)..."
        e.presentation.description = "$label Task (Ui)..."
        e.presentation.icon =
            if (nxWorkspaceType == WorkspaceType.ANGULAR) NxIcons.ANGULAR
            else NxIcons.NRWL_ICON
    }

    override fun getChildren(event: AnActionEvent?): Array<AnAction> {
        val project = event?.project ?: return emptyArray()
        val virtualFile = event.getData(LangDataKeys.VIRTUAL_FILE) ?: project.baseDir
        val nxConfig = NxConfigProvider.getNxConfig(project, virtualFile) ?: return emptyArray()
        return listOf(
            // "Generate",
            // "Run",
            "Build",
            "Serve",
            "Test",
            "E2e",
            "Lint"
        ).map { command ->
            object : ActionGroup(command, true) {
                override fun getChildren(e: AnActionEvent?): Array<AnAction> {
                    return nxConfig.projects.filter { it.architect.containsKey(command.lowercase(Locale.getDefault())) }.map {
                        NxCliAction(
                            command.lowercase(Locale.getDefault()),
                            it.name,
                            it.architect[command.lowercase(Locale.getDefault())]!!,
                            virtualFile,
                            it.name,
                            it.name,
                            if (it.type == NxProject.AngularProjectType.APPLICATION) NxIcons.NX_APP_FOLDER
                            else NxIcons.NX_LIB_FOLDER
                        )
                    }.toTypedArray()
                }
            }
        }.toTypedArray()
    }
}

fun WorkspaceType.label(): String {
    return if (this == WorkspaceType.ANGULAR) WorkspaceType.ANGULAR.name else WorkspaceType.NX.name
}
