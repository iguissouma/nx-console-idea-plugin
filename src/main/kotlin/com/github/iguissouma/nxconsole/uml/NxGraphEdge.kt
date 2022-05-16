package com.github.iguissouma.nxconsole.uml

import com.intellij.diagram.DiagramEdgeBase
import com.intellij.diagram.DiagramRelationshipInfo
import com.intellij.diagram.DiagramRelationshipInfoAdapter
import com.intellij.diagram.presentation.DiagramLineType


class NxGraphEdge(val from: NxGraphNode, val to: NxGraphNode): DiagramEdgeBase<NxDiagramObject>(from, to,
    DiagramRelationshipInfoAdapter.Builder()
        .setName("BUILTIN")
        .setLineType(DiagramLineType.SOLID)
        .setSourceArrow(DiagramRelationshipInfo.NONE)
        .setTargetArrow(DiagramRelationshipInfo.STANDARD)
        .create()) {

    override fun toString(): String {
        return "NxGraphEdge(from=$from, to=$to)"
    }
}
