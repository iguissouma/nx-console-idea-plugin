package com.github.iguissouma.nxconsole.graph

import com.github.iguissouma.nxconsole.graph.model.AppNode
import com.github.iguissouma.nxconsole.graph.model.BasicNxEdge
import com.github.iguissouma.nxconsole.graph.model.BasicNxNode
import com.github.iguissouma.nxconsole.graph.model.LibNode
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.graph.builder.GraphDataModel
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiFile
import org.jetbrains.concurrency.AsyncPromise
import java.io.File
import java.util.concurrent.TimeUnit

private val LOG: Logger = Logger.getInstance("#com.github.iguissouma.nxconsole.graph.NxDepGraphDataModel")

class NxDepGraphDataModel(val nxJsonFile: PsiFile) : GraphDataModel<BasicNxNode, BasicNxEdge>() {

    val LOG = Logger.getInstance(NxDepGraphDataModel::class.java)

    private val myNodes: MutableSet<BasicNxNode> = mutableSetOf()
    private val myEdges: MutableSet<BasicNxEdge> = mutableSetOf()
    private val myProject = nxJsonFile.project

    init {
        object : Task.Backgroundable(myProject, "Loading nx-depgraph...", false) {
            override fun run(indicator: ProgressIndicator) {
                // ApplicationManager.getApplication().runReadAction{
                ApplicationManager.getApplication().invokeLater {
                    // refreshDataModel()
                }
            }
        }.queue()
    }

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
        // TODO check how to avoid graph is not fit the content
        /*ApplicationManager.getApplication().executeOnPooledThread{
            ApplicationManager.getApplication().invokeLater {
                updateDataModel()
            }
        }*/

        // this is not working
        /*ProgressManager.getInstance().runProcessWithProgressSynchronously({
            ApplicationManager.getApplication().invokeAndWait {
                updateDataModel()
            }
        }, "loding nx-depgraph...", false, myProject)*/

        updateDataModel()

        /*object : Task.Backgroundable(myProject, "loding nx-depgraph...", false ){
            override fun run(indicator: ProgressIndicator) {
                //ApplicationManager.getApplication().runReadAction{
                ApplicationManager.getApplication().invokeLater {
                    updateDataModel()
                }
            }
        }.queue()*/
    }

    private fun updateDataModel() {
        val depGraph =
            kotlin.runCatching { File(File(nxJsonFile.parent!!.virtualFile.path), ".nxdeps.json") }.getOrNull()
                ?: return
        // println(grabCommandOutput)
        // TODO temp file
        // val depGraphJsonFile = createTempFile.readText()
        // createTempFile.deleteOnExit()
        val depGraphJsonFile = depGraph.readText()
        val listPersonType = object : TypeToken<Map<String, Any>>() {}.type
        val graph: Map<String, Any> = Gson().fromJson(depGraphJsonFile, listPersonType)
        val affectedProjectsProperty = graph["affectedProjects"] as? List<String> ?: return
        val graphProperty = graph["graph"] as? Map<*, *> ?: return
        (graphProperty["nodes"] as Map<*, *>).forEach {
            // val findFileByUrl = VirtualFileManager.getInstance().findFileByUrl(nxJsonFile.parent?.virtualFile?.path + "/apps/api")
            // val findDirectory = PsiManager.getInstance(nxJsonFile.project).findDirectory(findFileByIoFile!!)?.createSmartPointer()
            val map = it.value as Map<*, *>
            val data = map["data"] as Map<*, *>?
            val findFileByIoFile = data?.get("root")?.let { root ->
                LocalFileSystem.getInstance()
                    .findFileByIoFile(File(nxJsonFile.parent?.virtualFile?.path + "/" + root))
            }
            val affected = if (NxGraphConfiguration.getInstance(project = nxJsonFile.project).NX_SHOW_AFFECTED)
                affectedProjectsProperty.contains(it.key)
            else false
            if (map["type"] as String in listOf("e2e", "app")) {
                addNode(AppNode(it.key as String, affected, findFileByIoFile))
            } else {
                addNode(LibNode(it.key as String, affected, findFileByIoFile))
            }
        }
        (graphProperty["dependencies"] as Map<*, *>).forEach { entry ->
            val deps = entry.value as List<Map<*, *>>
            deps.forEach { x ->
                val source = myNodes.firstOrNull { it.name == x["source"] }
                val target = myNodes.firstOrNull { it.name == x["target"] }
                if (source != null && target != null) {
                    addEdge(
                        BasicNxEdge(
                            source,
                            target,
                            if (x["type"] as String? == "implicit") "implicit" else "",
                            x["type"] as String?
                        )
                    )
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

var myLogErrors: ThreadLocal<Boolean> = ThreadLocal.withInitial { true }
fun grabCommandOutput(project: Project, commandLine: GeneralCommandLine, workingDir: String?): String {
    if (workingDir != null) {
        commandLine.withWorkDirectory(workingDir)
    }
    val handler = CapturingProcessHandler(commandLine)
    val promise = AsyncPromise<String>()
    object : Task.Backgroundable(project, "executing nx dep-graph...") {
        override fun run(progress: ProgressIndicator) {
            try {
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
                    return promise.setResult(output.stdout)
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
                promise.setResult("")
            } catch (t: Throwable) {
                promise.setError(t)
            }
        }
    }.queue()

    return promise.blockingGet(5000, TimeUnit.MILLISECONDS)
        ?: error("Failed to fetch list of processes.")
}

private fun shortenOutput(output: String): String {
    return StringUtil.shortenTextWithEllipsis(
        output.replace('\\', '/')
            .replace("(/[^()/:]+)+(/[^()/:]+)(/[^()/:]+)".toRegex(), "/...$1$2$3"),
        750,
        0
    )
}
