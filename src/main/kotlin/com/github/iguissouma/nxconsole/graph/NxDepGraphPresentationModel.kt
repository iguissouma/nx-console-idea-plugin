package com.github.iguissouma.nxconsole.graph

import com.github.iguissouma.nxconsole.graph.model.AppNode
import com.github.iguissouma.nxconsole.graph.model.BasicNxEdge
import com.github.iguissouma.nxconsole.graph.model.BasicNxNode
import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.openapi.graph.GraphManager
import com.intellij.openapi.graph.builder.components.BasicGraphPresentationModel
import com.intellij.openapi.graph.builder.renderer.BasicGraphNodeRenderer
import com.intellij.openapi.graph.builder.util.GraphViewUtil
import com.intellij.openapi.graph.view.Arrow
import com.intellij.openapi.graph.view.EdgeRealizer
import com.intellij.openapi.graph.view.Graph2D
import com.intellij.openapi.graph.view.LineType
import com.intellij.openapi.graph.view.NodeRealizer
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import java.awt.Color

class NxDepGraphPresentationModel(val project: Project, graph: Graph2D) :
    BasicGraphPresentationModel<BasicNxNode, BasicNxEdge>(graph) {

    private var myRenderer: BasicGraphNodeRenderer<BasicNxNode, BasicNxEdge>? = null

    override fun getNodeRealizer(n: BasicNxNode?): NodeRealizer {
        return GraphViewUtil.createNodeRealizer(
            "NxNodeRenderer",
            getRenderer() ?: error("unable to get node celle renderer")
        )
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

    private fun navigateToFile(project: Project, file: VirtualFile) {
        PsiNavigationSupport.getInstance().createNavigatable(project, file, -1).navigate(true)
    }

    override fun getEdgeRealizer(e: BasicNxEdge?): EdgeRealizer {
        val edgeRealizer: EdgeRealizer = GraphManager.getGraphManager().createPolyLineEdgeRealizer()
        edgeRealizer.lineType = if (e?.type == "dynamic") LineType.DASHED_1 else LineType.LINE_1
        edgeRealizer.lineColor = if (e?.source?.affected == true && e.target.affected) Color.RED else Color.GRAY
        edgeRealizer.arrow = Arrow.STANDARD
        return edgeRealizer
    }

    override fun getNodeTooltip(node: BasicNxNode?): String? {
        if (node == null) {
            return null
        }
        val builder = DocumentationBuilder()
        val type = if (node is AppNode) "APP" else "LIB"
        builder.addLine(type, node.name)
        // builder.addLine("tags", "")
        return builder.getText()
    }
}

/**
 * Builds HTML-table based descriptions for use in documentation, tooltips.
 */
private class DocumentationBuilder {
    private val builder = StringBuilder("<html><table>")

    /**
     * Adds a labeled content line.
     *
     * @param label   Content description.
     * @param content Content text, `null` or empty text will be replaced with '-'.
     * @return this instance.
     */
    fun addLine(label: String, content: String): DocumentationBuilder {
        builder.append("<tr><td><strong>").append(label).append(":</strong></td>")
            .append("<td>").append(if (StringUtil.isNotEmpty(content)) content else "-").append("</td></tr>")
        return this
    }

    fun getText(): String {
        builder.append("</table></html>")
        return builder.toString()
    }
}
