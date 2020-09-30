package com.github.iguissouma.nxconsole.graph

import com.github.iguissouma.nxconsole.graph.model.BasicNxEdge
import com.github.iguissouma.nxconsole.graph.model.BasicNxNode
import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.openapi.graph.builder.components.BasicGraphPresentationModel
import com.intellij.openapi.graph.builder.renderer.BasicGraphNodeRenderer
import com.intellij.openapi.graph.builder.util.GraphViewUtil
import com.intellij.openapi.graph.view.Graph2D
import com.intellij.openapi.graph.view.NodeRealizer
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class NxDepGraphPresentationModel(val project: Project, graph: Graph2D) :
    BasicGraphPresentationModel<BasicNxNode, BasicNxEdge>(graph) {

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

    override fun editNode(n: BasicNxNode?): Boolean {
        if (n?.file != null) {
            navigateToFile(project, n.file!!)
            return true
        }
        return super.editNode(n)
    }

    override fun editEdge(e: BasicNxEdge?): Boolean {
        return super.editEdge(e)
    }

    private fun navigateToFile(project: Project, file: VirtualFile) {
        PsiNavigationSupport.getInstance().createNavigatable(project, file, -1).navigate(true)
    }
}
