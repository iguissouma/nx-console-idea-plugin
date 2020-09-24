package com.github.iguissouma.nxconsole.graph

import com.github.iguissouma.nxconsole.graph.model.AppNode
import com.github.iguissouma.nxconsole.graph.model.BasicNxEdge
import com.github.iguissouma.nxconsole.graph.model.BasicNxNode
import com.github.iguissouma.nxconsole.graph.model.LibNode
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.javascript.nodejs.CompletionModuleInfo
import com.intellij.javascript.nodejs.NodeModuleSearchUtil
import com.intellij.javascript.nodejs.interpreter.NodeCommandLineConfigurator
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.graph.builder.GraphDataModel
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiFile
import java.io.File

private val LOG: Logger = Logger.getInstance("#com.github.iguissouma.nxconsole.graph.NxDepGraphDataModel")

class NxDepGraphDataModel(val nxJsonFile: PsiFile) : GraphDataModel<BasicNxNode, BasicNxEdge>() {

    val LOG = Logger.getInstance(NxDepGraphDataModel::class.java)

    private val myNodes: MutableSet<BasicNxNode> = mutableSetOf()
    private val myEdges: MutableSet<BasicNxEdge> = mutableSetOf()
    private val myProject = nxJsonFile.project

    override fun dispose() {
    }

    override fun getNodes(): MutableCollection<BasicNxNode> {
        refreshDataModel()
        return myNodes.toMutableSet()
    }

    override fun getEdges(): MutableCollection<BasicNxEdge> {
        return myEdges.toMutableSet()
    }

    override fun getSourceNode(edge: BasicNxEdge): BasicNxNode {
        return edge.source
    }

    override fun getTargetNode(edge: BasicNxEdge): BasicNxNode {
        return edge.target
    }

    override fun getNodeName(node: BasicNxNode): String {
        return node.name
    }

    override fun getEdgeName(node: BasicNxEdge): String {
        return node.name
    }

    override fun createEdge(from: BasicNxNode, to: BasicNxNode): BasicNxEdge? {
        return null
    }

    private fun refreshDataModel() {
        myNodes.clear()
        myEdges.clear()
        updateDataModel()
    }

    private fun updateDataModel() {

        val nodeJsInterpreter = NodeJsInterpreterManager.getInstance(nxJsonFile.project).interpreter ?: return
        val configurator: NodeCommandLineConfigurator
        try {
            configurator = NodeCommandLineConfigurator.find(nodeJsInterpreter)
        } catch (e: Exception) {
            LOG.error("Cannot load schematics", e)
            return
        }

        val modules: MutableList<CompletionModuleInfo> = mutableListOf()
        NodeModuleSearchUtil.findModulesWithName(modules, "@nrwl/cli", this.nxJsonFile.virtualFile, null)
        val module = modules.firstOrNull() ?: return
        val moduleExe = "${module.virtualFile!!.path}${File.separator}bin${File.separator}nx"
        // TODO check if json can be out of monorepo
        // val createTempFile = createTempFile("tmp", ".json", File(nxJsonFile.parent!!.virtualFile.path))
        val depGraph = File(File(nxJsonFile.parent!!.virtualFile.path), ".nxdeps.json")
        val commandLine = GeneralCommandLine("", moduleExe, "dep-graph", "--file=.nxdeps.json")
        configurator.configure(commandLine)

        val grabCommandOutput = grabCommandOutput(commandLine, nxJsonFile.parent!!.virtualFile.path)

        println(grabCommandOutput)
        // TODO temp file
        // val depGraphJsonFile = createTempFile.readText()
        // createTempFile.deleteOnExit()
        val depGraphJsonFile = depGraph.readText()
        val listPersonType = object : TypeToken<Map<String, Any>>() {}.type
        val graph: Map<String, Any> = Gson().fromJson(depGraphJsonFile, listPersonType)
        val graphProperty = graph["graph"] as Map<*, *>
        (graphProperty["nodes"] as Map<*, *>).forEach {
            if ((it.value as Map<*, *>)["type"] as String in listOf("e2e", "app")) {
                addNode(AppNode(it.key as String))
            } else {
                addNode(LibNode(it.key as String))
            }
        }
        (graphProperty["dependencies"] as Map<*, *>).forEach { entry ->
            val deps = entry.value as List<Map<*, *>>
            deps.forEach { x ->
                val source = myNodes.firstOrNull { it.name == x["source"] }
                val target = myNodes.firstOrNull { it.name == x["target"] }
                if (source != null && target != null) {
                    addEdge(BasicNxEdge(source, target, if (x["type"] as String == "implicit") "implicit" else ""))
                }
            }
        }
    }

    private fun addNode(node: BasicNxNode) {
        myNodes.add(node)
    }

    private fun addEdge(edge: BasicNxEdge) {
        myEdges.add(edge)
    }
}

private var myLogErrors: ThreadLocal<Boolean> = ThreadLocal.withInitial { true }

private fun grabCommandOutput(commandLine: GeneralCommandLine, workingDir: String?): String {
    if (workingDir != null) {
        commandLine.withWorkDirectory(workingDir)
    }
    val handler = CapturingProcessHandler(commandLine)
    val output = handler.runProcess()

    if (output.exitCode == 0) {
        if (output.stderr.trim().isNotEmpty()) {
            if (myLogErrors.get()) {
                LOG.error(
                    "Error while loading schematics info.\n" +
                        shortenOutput(output.stderr),
                    Attachment("err-output", output.stderr)
                )
            } else {
                LOG.info(
                    "Error while loading schematics info.\n" +
                        shortenOutput(output.stderr)
                )
            }
        }
        return output.stdout
    } else if (myLogErrors.get()) {
        LOG.error(
            "Failed to load schematics info.\n" +
                shortenOutput(output.stderr),
            Attachment("err-output", output.stderr),
            Attachment("std-output", output.stdout)
        )
    } else {
        LOG.info(
            "Error while loading schematics info.\n" +
                shortenOutput(output.stderr)
        )
    }
    return ""
}

private fun shortenOutput(output: String): String {
    return StringUtil.shortenTextWithEllipsis(
        output.replace('\\', '/')
            .replace("(/[^()/:]+)+(/[^()/:]+)(/[^()/:]+)".toRegex(), "/...$1$2$3"),
        750,
        0
    )
}
