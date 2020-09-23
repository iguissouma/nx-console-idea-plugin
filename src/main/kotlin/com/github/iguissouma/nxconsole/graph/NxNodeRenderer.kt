package com.github.iguissouma.nxconsole.graph

import com.github.iguissouma.nxconsole.graph.model.BasicNxEdge
import com.github.iguissouma.nxconsole.graph.model.BasicNxNode
import com.intellij.openapi.graph.builder.GraphBuilder
import com.intellij.openapi.graph.builder.renderer.BasicGraphNodeRenderer
import com.intellij.psi.PsiManager
import javax.swing.Icon

class NxNodeRenderer(graphBuilder: GraphBuilder<BasicNxNode, BasicNxEdge>) :
    BasicGraphNodeRenderer<BasicNxNode, BasicNxEdge>(
        graphBuilder,
        PsiManager.getInstance(graphBuilder.project).modificationTracker
    ) {
    override fun getIcon(node: BasicNxNode): Icon {
        return node.getIcon()
    }

    override fun getNodeName(node: BasicNxNode): String {
        return node.name
    }
}
