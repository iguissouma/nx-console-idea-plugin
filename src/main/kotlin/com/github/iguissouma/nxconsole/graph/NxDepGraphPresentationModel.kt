package com.github.iguissouma.nxconsole.graph

import com.github.iguissouma.nxconsole.graph.model.BasicNxEdge
import com.github.iguissouma.nxconsole.graph.model.BasicNxNode
import com.intellij.openapi.graph.builder.components.BasicGraphPresentationModel
import com.intellij.openapi.graph.builder.renderer.BasicGraphNodeRenderer
import com.intellij.openapi.graph.builder.util.GraphViewUtil
import com.intellij.openapi.graph.view.Graph2D
import com.intellij.openapi.graph.view.NodeRealizer


class NxDepGraphPresentationModel(graph: Graph2D) : BasicGraphPresentationModel<BasicNxNode, BasicNxEdge>(graph)  {


    private var myRenderer: BasicGraphNodeRenderer<BasicNxNode, BasicNxEdge>? = null


    override fun getNodeRealizer(n: BasicNxNode?): NodeRealizer {
        return GraphViewUtil.createNodeRealizer("NxNodeRenderer", getRenderer())
    }


    private fun getRenderer(): BasicGraphNodeRenderer<BasicNxNode, BasicNxEdge>? {
        if (myRenderer == null) {
            myRenderer = NxNodeRenderer(graphBuilder)
        }
        return myRenderer
    }
}

