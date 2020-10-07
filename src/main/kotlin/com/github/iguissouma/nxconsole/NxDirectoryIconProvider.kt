package com.github.iguissouma.nxconsole

import com.github.iguissouma.nxconsole.buildTools.NxJsonUtil
import com.intellij.ide.IconProvider
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import java.io.File
import javax.swing.Icon

class NxDirectoryIconProvider : IconProvider(), DumbAware {

    override fun getIcon(element: PsiElement, flags: Int): Icon? {
        if (element is PsiDirectory) {
            val project: Project = element.getProject()
            val angularJsonFile = NxJsonUtil.findChildAngularJsonFile(project.baseDir) ?: return null
            val projectsProperty = NxJsonUtil.findProjectsProperty(project, angularJsonFile) ?: return null
            val file: VirtualFile = element.virtualFile

            // TODO compare with folder virtual file
            if ("apps" == file.name) {
                return NxIcons.NX_APPS_FOLDER
            }

            if ("libs" == file.name) {
                return NxIcons.NX_LIBS_FOLDER
            }

            val toMap = (projectsProperty.value as JsonObject)
                .propertyList
                .map { Triple(it.name, (it.value as? JsonObject)?.findProperty("root"), (it.value as? JsonObject)?.findProperty("projectType")) }
                .filter { it.second != null }
                .map {
                    LocalFileSystem.getInstance()
                        .findFileByIoFile(File(angularJsonFile.parent.path + "/" + (it.second?.value as? JsonStringLiteral)?.value)) to (it.first to (it.third?.value as? JsonStringLiteral)?.value)
                }.toMap()

            if (toMap.containsKey(file)) {
                val get: Pair<String, String?>? = toMap[file]
                if (get?.second == "application") {
                    return NxIcons.NX_APP_FOLDER
                } else if (get?.second == "library") {
                    return NxIcons.NX_LIB_FOLDER
                }
            }
        }
        return null
    }
}
