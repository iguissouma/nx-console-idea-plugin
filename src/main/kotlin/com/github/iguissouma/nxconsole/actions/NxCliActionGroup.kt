package com.github.iguissouma.nxconsole.actions

import com.github.iguissouma.nxconsole.NxIcons
import com.github.iguissouma.nxconsole.cli.config.NxConfigProvider
import com.github.iguissouma.nxconsole.cli.config.NxProject
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys

class NxCliActionGroup : ActionGroup(
    "Nx Task (Ui)...",
    "Nx Task (ui)",
    NxIcons.NRWL_ICON
) {

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
                    return nxConfig.projects.filter { it.architect.containsKey(command.toLowerCase()) }.map {
                        NxCliAction(
                            command.toLowerCase(),
                            it.name,
                            it.architect[command.toLowerCase()]!!,
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
