package com.github.iguissouma.nxconsole.intentions

import com.github.iguissouma.nxconsole.NxBundle
import com.github.iguissouma.nxconsole.buildTools.NxJsonUtil
import com.github.iguissouma.nxconsole.cli.NxCliFilter
import com.github.iguissouma.nxconsole.cli.config.NxConfigProvider
import com.github.iguissouma.nxconsole.execution.NxGenerator
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil.unquoteString
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement

class NxConvertToNxProject : PsiElementBaseIntentionAction() {

    override fun getFamilyName() = text

    override fun getText(): String {
        return NxBundle.message("nx.convert.to.nx.project.intention.text")
    }

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        if (!NxJsonUtil.isAngularJsonFile(element.containingFile)) {
            return false
        }
        if (element is LeafPsiElement && element.parent is JsonStringLiteral) {
            val property = NxJsonUtil.findContainingPropertyInsideAngularJsonFile(element)
            if (property != null && property.nameElement === element.parent) {
                return NxJsonUtil.isProjectProperty(property)
            }
            return false
        }
        return false
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val nxConfig = NxConfigProvider.getNxConfig(project, element.containingFile.virtualFile) ?: return
        val filter = NxCliFilter(project, project.baseDir.path)
        val interpreter = NodeJsInterpreterManager.getInstance(project).interpreter ?: return
        val args = arrayOf(
            "generate",
            "@nrwl/workspace:convert-to-nx-project",
            "--project",
            unquoteString(element.text)
        )
        project.save()
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        FileDocumentManager.getInstance().saveAllDocuments()
        NxGenerator().generate(
            node = interpreter,
            nxExe = "nx",
            baseDir = nxConfig.angularJsonFile.parent,
            workingDir = VfsUtilCore.virtualToIoFile(nxConfig.angularJsonFile.parent ?: nxConfig.angularJsonFile.parent),
            project = project,
            callback = null,
            title = "convert-to-nx-project",
            filters = arrayOf(filter),
            args = args
        )
    }
}
