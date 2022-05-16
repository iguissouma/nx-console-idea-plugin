package com.github.iguissouma.nxconsole.uml

import com.intellij.diagram.AbstractDiagramElementManager
import com.intellij.diagram.DiagramElementManager
import com.intellij.diagram.DiagramProvider
import com.intellij.openapi.actionSystem.DataContext

class NxDiagramElementManager(val provider: NxDiagramProvider) : AbstractDiagramElementManager<NxDiagramObject>() {

    init {
        setUmlProvider(provider)
    }

    override fun findInDataContext(p0: DataContext): NxDiagramObject? {
        return null
    }

    override fun findElementsInDataContext(p0: DataContext): MutableCollection<NxDiagramObject> {
        return listOf<NxDiagramObject>(NxDiagramObject("", "LIBRARY")).toMutableList()
    }

    override fun isAcceptableAsNode(p0: Any?): Boolean {
        return true
    }

    override fun getNodeTooltip(p0: NxDiagramObject?): String? {
        return null
    }

    override fun getElementTitle(p0: NxDiagramObject?): String? {
        return p0?.name
    }

    override fun isContainerFor(p0: NxDiagramObject?, p1: NxDiagramObject?): Boolean {
        return false
    }

    override fun canCollapse(p0: NxDiagramObject?): Boolean {
        return false
    }

    override fun getNodeItems(p0: NxDiagramObject?): Array<Any> {
        return emptyArray()
    }

    override fun canBeBuiltFrom(element: Any?): Boolean {
        return true
    }

}
