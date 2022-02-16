package com.github.iguissouma.nxconsole.graph.fileEditor

import com.github.iguissouma.nxconsole.graph.NxDepGraphDataModel
import com.github.iguissouma.nxconsole.graph.NxDepGraphPresentationModel
import com.github.iguissouma.nxconsole.graph.grabCommandOutput
import com.github.iguissouma.nxconsole.graph.model.BasicNxEdge
import com.github.iguissouma.nxconsole.graph.model.BasicNxNode
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.javascript.nodejs.CompletionModuleInfo
import com.intellij.javascript.nodejs.NodeModuleSearchUtil
import com.intellij.javascript.nodejs.interpreter.NodeCommandLineConfigurator
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.graph.GraphManager
import com.intellij.openapi.graph.builder.GraphBuilder
import com.intellij.openapi.graph.builder.GraphBuilderFactory
import com.intellij.openapi.graph.builder.util.GraphViewUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.psi.PsiFile
import java.awt.BorderLayout
import java.io.File
import javax.swing.JPanel

class NxDepGraphComponent(val nxJsonFile: PsiFile) : JPanel(), DataProvider, Disposable {

    lateinit var myBuilder: GraphBuilder<BasicNxNode, BasicNxEdge>

    init {

        val progress = ProgressManager.getInstance().progressIndicator
        if (progress != null) {

            progress.text = "Initializing..."
            val project = nxJsonFile.project
            val graph = GraphManager.getGraphManager().createGraph2D()
            val view = GraphManager.getGraphManager().createGraph2DView()
            // view.isGridVisible = false
            // view.gridMode = false

            progress.text = "Building model..."

            val myDataModel = NxDepGraphDataModel(nxJsonFile)
            val presentationModel = NxDepGraphPresentationModel(project, graph)

            myBuilder = GraphBuilderFactory.getInstance(project).createGraphBuilder(
                graph,
                view,
                myDataModel,
                presentationModel
            )
            Disposer.register(this, myBuilder)

            val graphComponent = myBuilder.view.jComponent
            layout = BorderLayout()

            val commonToolbarActions = GraphViewUtil.getCommonToolbarActions()
            val group = DefaultActionGroup()
            group.addAll(commonToolbarActions)
            group.addSeparator()
            group.add(ActionManager.getInstance().getAction("Nx.GRAPH.ShowAffectedAction"))
            val toolbar = ActionManager.getInstance().createActionToolbar(
                ActionPlaces.TOOLBAR,
                group,
                true
            )
            toolbar.targetComponent = graphComponent

            add(toolbar.component, BorderLayout.NORTH)
            add(graphComponent, BorderLayout.CENTER)

            val nxDepGraphTask = object : Task.Backgroundable(nxJsonFile.project, "Loading nx depgraph...", false) {
                override fun run(indicator: ProgressIndicator) {
                    // ApplicationManager.getApplication().runReadAction{
                    ApplicationManager.getApplication().invokeLater {
                        val nodeJsInterpreter = NodeJsInterpreterManager.getInstance(nxJsonFile.project).interpreter
                        if (nodeJsInterpreter != null) {
                            val configurator: NodeCommandLineConfigurator
                            try {
                                configurator = NodeCommandLineConfigurator.find(nodeJsInterpreter)
                                val modules: MutableList<CompletionModuleInfo> = mutableListOf()
                                NodeModuleSearchUtil.findModulesWithName(
                                    modules,
                                    "@nrwl/cli",
                                    nxJsonFile.virtualFile,
                                    null
                                )
                                val module = modules.firstOrNull()
                                if (module != null) {
                                    val moduleExe =
                                        "${module.virtualFile!!.path}${File.separator}bin${File.separator}nx"
                                    // TODO check if json can be out of monorepo
                                    // val createTempFile = createTempFile("tmp", ".json", File(nxJsonFile.parent!!.virtualFile.path))
                                    val commandLine =
                                        GeneralCommandLine("", moduleExe, "affected:dep-graph", "--file=.nxdeps.json")
                                    configurator.configure(commandLine)
                                    grabCommandOutput(
                                        nxJsonFile.project,
                                        commandLine,
                                        nxJsonFile.parent!!.virtualFile.path
                                    )
                                    myBuilder.initialize()
                                }
                            } catch (e: Exception) {
                                // LOG.error("Cannot load schematics", e)
                                // return
                            }
                        }
                    }
                }
            }
            nxDepGraphTask.queue()

            // listen to nx.json changes
            // TODO check if this optimal, it's better to use PSI instead
            project.messageBus.connect().subscribe(
                VirtualFileManager.VFS_CHANGES,
                object : BulkFileListener {
                    override fun after(events: MutableList<out VFileEvent>) {
                        // handle the events
                        events.forEach { event ->
                            if (event is VFileContentChangeEvent && event.file.name == "nx.json" && isShowing) {
                                nxDepGraphTask.queue()
                            }
                        }
                    }
                }
            )

            /*PsiManager.getInstance(project).addPsiTreeChangeListener(
                object : PsiTreeChangeAdapter() {
                    override fun childrenChanged(event: PsiTreeChangeEvent) {
                        val file = event.file
                        val virtualFile: VirtualFile? = PsiUtilCore.getVirtualFile(file)
                        if (virtualFile != null && virtualFile.name == "nx.json" && isShowing) {
                            myBuilder.queueUpdate()
                        }
                    }

                    override fun propertyChanged(event: PsiTreeChangeEvent) {
                        childrenChanged(event)
                    }
                },
                this
            )*/
        }
    }

    override fun getData(dataId: String): Any? {
        if (dataId == NX_DESIGNER_COMPONENT) {
            return this
        }
        return null
    }

    override fun dispose() {
    }

    companion object {
        private const val NX_DESIGNER_COMPONENT: String = "NX_DESIGNER_COMPONENT"
    }
}
