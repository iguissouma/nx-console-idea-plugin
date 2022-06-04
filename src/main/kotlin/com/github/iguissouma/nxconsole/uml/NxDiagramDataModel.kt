package com.github.iguissouma.nxconsole.uml

import com.github.iguissouma.nxconsole.cli.config.NxConfigProvider
import com.github.iguissouma.nxconsole.util.NxExecutionUtil
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.intellij.diagram.DiagramDataModel
import com.intellij.diagram.DiagramEdge
import com.intellij.diagram.DiagramNode
import com.intellij.diagram.util.DiagramUpdateService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiManager
import com.intellij.util.ui.UIUtil

class NxDiagramDataModel(project: Project, val provider: NxDiagramProvider) :
    DiagramDataModel<NxDiagramObject>(project, provider) {

    private var myNodes: MutableSet<NxGraphNode>? = null
    private var myEdges: MutableSet<NxGraphEdge>? = null

    init {
        NxExecutionUtil(project).executeAndGetOutputAsync("print-affected", arrayOf()) {
            UIUtil.invokeAndWaitIfNeeded(Runnable {
                val result = it?.stdout?.extractJson() ?: "{}"
                val depGraphType = object : TypeToken<Map<String, Any>>() {}.type
                val depGraph: Map<String, Any> = Gson().fromJson(result, depGraphType)
                val projectGraph: Map<String, Any> = depGraph["projectGraph"] as Map<String, Any>
                myNodes = mutableSetOf()
                myEdges = mutableSetOf()
                val nxConfig = NxConfigProvider.getNxConfig(project, project.projectFile ?: return@Runnable)
                (projectGraph["nodes"] as List<String>).forEach { node ->
                    myNodes?.add(
                        NxGraphNode(
                            NxDiagramObject(
                                node,
                                nxConfig?.projects?.firstOrNull { it.name == node }?.type?.name ?: "library"
                            ), provider
                        )
                    )
                }
                (projectGraph["dependencies"] as Map<String, Any>).forEach { entry ->
                    val deps = entry.value as List<Map<*, *>>
                    deps.forEach { x ->
                        val source = myNodes?.firstOrNull { it.diagramObject.name == x["source"] }
                        val target = myNodes?.firstOrNull { it.diagramObject.name == x["target"] }
                        if (source != null && target != null) {
                            myEdges?.add(
                                NxGraphEdge(
                                    NxGraphNode(
                                        NxDiagramObject(source.diagramObject.name, source.diagramObject.type),
                                        provider
                                    ),
                                    NxGraphNode(
                                        NxDiagramObject(target.diagramObject.name, target.diagramObject.type),
                                        provider
                                    )
                                )
                            )
                        }
                    }
                }
                DiagramUpdateService.getInstance().requestDataModelRefreshPreservingLayout(builder).runAsync()
                    .thenAccept {
                        builder.graphBuilder.fitContent(true)
                    }
            })

        }
    }

    override fun dispose() = Unit

    override fun getNodes(): MutableCollection<out DiagramNode<NxDiagramObject>> {
        return myNodes?.toMutableList() ?: mutableListOf()
    }

    override fun getEdges(): MutableCollection<out DiagramEdge<NxDiagramObject>> {
        return myEdges?.toMutableList() ?: mutableListOf()
    }

    override fun getModificationTracker(): ModificationTracker {
        return PsiManager.getInstance(project).modificationTracker
    }

    override fun addElement(p0: NxDiagramObject?): DiagramNode<NxDiagramObject>? {
        return null
    }

    override fun getNodeName(p0: DiagramNode<NxDiagramObject>): String {
        return p0.identifyingElement.name
    }

}

fun String.extractJson() = substring(indexOf("{"), lastIndexOf("}") + 1)
