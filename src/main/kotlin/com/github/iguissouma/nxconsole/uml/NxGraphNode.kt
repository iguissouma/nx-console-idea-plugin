package com.github.iguissouma.nxconsole.uml

import com.github.iguissouma.nxconsole.NxIcons
import com.intellij.diagram.DiagramNodeBase
import com.intellij.diagram.DiagramProvider
import javax.swing.Icon

class NxGraphNode(val diagramObject: NxDiagramObject, provider: DiagramProvider<NxDiagramObject>): DiagramNodeBase<NxDiagramObject>(provider) {

    override fun getIcon(): Icon? {
        return if (diagramObject.type == "LIBRARY") NxIcons.NX_LIB_FOLDER else NxIcons.NX_APP_FOLDER
    }

    override fun getIdentifyingElement(): NxDiagramObject {
        return diagramObject
    }

    override fun getTooltip(): String? {
        return diagramObject.name
    }
}
