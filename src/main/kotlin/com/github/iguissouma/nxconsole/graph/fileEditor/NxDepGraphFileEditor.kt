package com.github.iguissouma.nxconsole.graph.fileEditor

import com.intellij.json.psi.JsonFile
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.xml.DomElement
import com.intellij.util.xml.ui.PerspectiveFileEditor
import javax.swing.JComponent


class NxDepGraphFileEditor(project: Project, file: VirtualFile) : PerspectiveFileEditor(project, file) {

    private var myComponent: NxDepGraphComponent? = null
    private val myNxJsonFile: JsonFile = psiFile as JsonFile

    override fun getPreferredFocusedComponent(): JComponent? {
        return NxDepGraphComponent(myNxJsonFile)
    }

    override fun getName(): String {
        return "Graph"
    }

    override fun commit() {
    }

    override fun reset() {
    }

    override fun getSelectedDomElement(): DomElement? {
        return null
    }

    override fun setSelectedDomElement(domElement: DomElement?) {

    }

    override fun createCustomComponent(): JComponent {
        return getNxGraphComponent()!!
    }

    private fun getNxGraphComponent(): NxDepGraphComponent? {
        if (myComponent == null) {
            myComponent = createGraphComponent()
            myComponent?.let { Disposer.register(this, it) }
        }
        return myComponent
    }

    /**
     * Creates graph component while showing modal wait dialog.
     *
     * @return new instance.
     */
    private fun createGraphComponent(): NxDepGraphComponent? {
        val graphComponent: Array<NxDepGraphComponent?> = arrayOf(null)
        ProgressManager.getInstance().runProcessWithProgressSynchronously(
            {
                graphComponent[0] =
                    ReadAction.compute<NxDepGraphComponent, Throwable> { NxDepGraphComponent(myNxJsonFile) }
            },
            "Generating Graph",
            false,
            myNxJsonFile.project
        )
        return graphComponent[0]
    }

}
