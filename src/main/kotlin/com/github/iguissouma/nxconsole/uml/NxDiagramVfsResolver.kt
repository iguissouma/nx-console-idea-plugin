package com.github.iguissouma.nxconsole.uml

import com.intellij.diagram.DiagramVfsResolver
import com.intellij.openapi.project.Project

class NxDiagramVfsResolver : DiagramVfsResolver<NxDiagramObject> {
    override fun getQualifiedName(p0: NxDiagramObject?): String? {
        return p0?.name
    }

    override fun resolveElementByFQN(p0: String, p1: Project): NxDiagramObject? {
        return NxDiagramObject(name = p0, "library")
    }

}
