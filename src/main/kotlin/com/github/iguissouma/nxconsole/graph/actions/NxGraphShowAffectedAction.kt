package com.github.iguissouma.nxconsole.graph.actions

import com.github.iguissouma.nxconsole.graph.NxGraphConfiguration
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.graph.builder.actions.AbstractGraphToggleAction
import com.intellij.openapi.graph.view.Graph2D
import com.intellij.openapi.project.Project

class NxGraphShowAffectedAction : AbstractGraphToggleAction() {

    override fun isSelected(graph: Graph2D, project: Project, event: AnActionEvent): Boolean {
        println("NxGraphShowAffectedAction::isSelected called...${NxGraphConfiguration.getInstance(project).NX_SHOW_AFFECTED}")
        return NxGraphConfiguration.getInstance(project).NX_SHOW_AFFECTED
    }

    override fun setSelected(graph: Graph2D, state: Boolean, project: Project, event: AnActionEvent) {
        NxGraphConfiguration.getInstance(project).NX_SHOW_AFFECTED = state
        // (this.getBuilder(event)?.graphDataModel as? NxDepGraphDataModel)?.nodes
        this.getBuilder(event)?.queueUpdate()
        // graph.updateViews()
    }
}
