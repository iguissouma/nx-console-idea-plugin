package com.github.iguissouma.nxconsole.graph.fileEditor

import com.github.iguissouma.nxconsole.graph.NxDepGraphDataModel
import com.github.iguissouma.nxconsole.graph.NxDepGraphPresentationModel
import com.github.iguissouma.nxconsole.graph.model.BasicNxEdge
import com.github.iguissouma.nxconsole.graph.model.BasicNxNode
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.graph.GraphManager
import com.intellij.openapi.graph.builder.GraphBuilder
import com.intellij.openapi.graph.builder.GraphBuilderFactory
import com.intellij.openapi.graph.builder.util.GraphViewUtil
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.psi.PsiFile
import java.awt.BorderLayout
import javax.swing.JComponent
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

            progress.text = "Building model..."

            val myDataModel = NxDepGraphDataModel(nxJsonFile)
            val presentationModel = NxDepGraphPresentationModel(graph)

            myBuilder = GraphBuilderFactory.getInstance(project).createGraphBuilder(
                graph,
                view,
                myDataModel,
                presentationModel
            )
            Disposer.register(this, myBuilder)

            val graphComponent = myBuilder.getView().jComponent
            layout = BorderLayout()

            val toolbar = ActionManager.getInstance().createActionToolbar(
                ActionPlaces.TOOLBAR,
                GraphViewUtil.getCommonToolbarActions(),
                true
            )
            toolbar.setTargetComponent(graphComponent)

            add(toolbar.component, BorderLayout.NORTH)
            add(graphComponent, BorderLayout.CENTER)

            myBuilder.initialize()

            // listen to nx.json changes
            // TODO check if this optimal, it's better to use PSI instead
            project.messageBus.connect().subscribe(
                VirtualFileManager.VFS_CHANGES,
                object : BulkFileListener {
                    override fun after(events: List<VFileEvent?>) {
                        // handle the events
                        events.forEach { event ->
                            if (event is VFileContentChangeEvent && event.file.name == "nx.json" && isShowing) {
                                myBuilder.queueUpdate()
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
        if (Comparing.equal(dataId, NX_DESIGNER_COMPONENT)) {
            return this
        }

        return null
    }

    override fun dispose() {
    }

    companion object {
        private const val NX_DESIGNER_COMPONENT: String = "STRUTS2_DESIGNER_COMPONENT"
    }
}
