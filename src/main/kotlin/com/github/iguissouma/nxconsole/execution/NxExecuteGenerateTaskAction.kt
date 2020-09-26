package com.github.iguissouma.nxconsole.execution

import com.github.iguissouma.nxconsole.NxIcons
import com.intellij.ide.actions.runAnything.RunAnythingManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project

class NxExecuteGenerateTaskAction : AnAction(NxIcons.NRWL_ICON), DumbAware {

    override fun actionPerformed(event: AnActionEvent) {
        val project: Project = event.project ?: return
        val runAnythingManager = RunAnythingManager.getInstance(project)
        runAnythingManager.show("nx generate ", false, event)
    }

}
