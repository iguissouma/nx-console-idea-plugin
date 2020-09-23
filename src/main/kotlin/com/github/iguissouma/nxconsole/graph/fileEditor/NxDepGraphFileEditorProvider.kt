package com.github.iguissouma.nxconsole.graph.fileEditor

import com.github.iguissouma.nxconsole.buildTools.NxJsonUtil
import com.github.iguissouma.nxconsole.graph.fileEditor.NxDepGraphFileEditor
import com.intellij.json.psi.JsonFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.util.xml.ui.PerspectiveFileEditor
import com.intellij.util.xml.ui.PerspectiveFileEditorProvider

class NxDepGraphFileEditorProvider : PerspectiveFileEditorProvider() {

    override fun accept(project: Project, file: VirtualFile): Boolean {
        if (!file.isValid) {
            return false;
        }

        val psiFile = PsiManager.getInstance(project).findFile(file);

        if (psiFile !is JsonFile) {
            return false;
        }

        return NxJsonUtil.isNxJsonFile(psiFile)

    }

    override fun createEditor(project: Project, file: VirtualFile): PerspectiveFileEditor {
        return NxDepGraphFileEditor(project, file)
    }


    override fun isDumbAware(): Boolean {
        return false
    }

    override fun getWeight(): Double {
        return 0.toDouble()
    }
}
