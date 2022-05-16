package com.github.iguissouma.nxconsole.uml

import com.github.iguissouma.nxconsole.NxIcons
import com.intellij.diagram.DiagramProvider
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.uml.core.actions.ShowDiagram
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable


class ShowNxGraphNewDiagramAction : ShowDiagram() {

    override fun getForcedProvider(e: AnActionEvent): DiagramProvider<*>? {
        return DiagramProvider.findByID<NxDiagramProvider>(NxDiagramProvider.ID)
    }

    override fun update(@NotNull e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = true // TODO: check if it is a NxGraph
        e.presentation.text = "Show Nx Graph Diagram"
        e.presentation.description = "Show Nx graph diagram"
        e.presentation.icon = NxIcons.NRWL_ICON
    }

}
