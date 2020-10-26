package com.github.iguissouma.nxconsole.graph

import com.github.iguissouma.nxconsole.graph.model.BasicNxEdge
import com.github.iguissouma.nxconsole.graph.model.BasicNxNode
import com.intellij.openapi.graph.builder.GraphBuilder
import com.intellij.openapi.graph.builder.renderer.BasicGraphNodeRenderer
import com.intellij.openapi.graph.view.NodeRealizer
import com.intellij.psi.PsiManager
import com.intellij.ui.JBColor
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.JPanel

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

    override fun tuneNode(nodeRealizer: NodeRealizer?, wrapper: JPanel?) {
        val node = nodeRealizer!!.node
        val basicNxNode: BasicNxNode? = this.builder.getNodeObject(node)
        if (basicNxNode != null) {
            wrapper!!.add(getIconLabel(basicNxNode), "West")
            wrapper.add(getLabelPanel(nodeRealizer), "Center")
            if (basicNxNode.affected) {
                wrapper.border = BorderFactory.createLineBorder(JBColor.RED, 2)
            }
        }
    }
}
