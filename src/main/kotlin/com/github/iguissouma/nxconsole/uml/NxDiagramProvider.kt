package com.github.iguissouma.nxconsole.uml

import com.intellij.diagram.BaseDiagramProvider
import com.intellij.diagram.DiagramDataModel
import com.intellij.diagram.DiagramElementManager
import com.intellij.diagram.DiagramPresentationModel
import com.intellij.diagram.DiagramVfsResolver
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile


class NxDiagramProvider : BaseDiagramProvider<NxDiagramObject>() {

    private val diagramVfsResolver: DiagramVfsResolver<NxDiagramObject> = NxDiagramVfsResolver()
    private val diagramElementManager: DiagramElementManager<NxDiagramObject> = NxDiagramElementManager(this)

    companion object {
        val ID = "NxGraphDependencies"
    }

    override fun getID(): String = ID

    override fun getPresentableName(): String = "Nx Graph Dependencies"

    override fun getElementManager(): DiagramElementManager<NxDiagramObject> {
        return diagramElementManager
    }

    override fun getVfsResolver(): DiagramVfsResolver<NxDiagramObject> {
        return diagramVfsResolver
    }

    override fun createDataModel(
        project: Project,
        element: NxDiagramObject?,
        file: VirtualFile?,
        presentationModel: DiagramPresentationModel
    ): DiagramDataModel<NxDiagramObject> {

        return NxDiagramDataModel(project, this)
    }

}
