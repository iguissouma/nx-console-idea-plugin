package com.github.iguissouma.nxconsole

import com.github.iguissouma.nxconsole.cli.config.NxConfigProvider
import com.github.iguissouma.nxconsole.cli.config.NxProject
import com.intellij.ide.IconProvider
import com.intellij.ide.projectView.impl.ProjectRootsUtil
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import javax.swing.Icon

class NxDirectoryIconProvider : IconProvider(), DumbAware {

    override fun getIcon(element: PsiElement, flags: Int): Icon? {
        if (element is PsiDirectory) {
            val project: Project = element.getProject()
            val file: VirtualFile = element.virtualFile
            val nxConfig = NxConfigProvider.getNxConfig(project, file) ?: return null

            // TODO compare with folder virtual file
            if ("apps" == file.name) {
                return NxIcons.NX_APPS_FOLDER
            }

            if ("libs" == file.name) {
                return NxIcons.NX_LIBS_FOLDER
            }

            val nxProject = nxConfig.projects.firstOrNull { it.rootDir == file } ?: return null
            val module = ModuleManager.getInstance(project).modules.firstOrNull() ?: return null
            val isExcluded = ProjectRootsUtil.findExcludeFolder(module, file)?.let { true } ?: false
            return when (nxProject.type) {
                NxProject.AngularProjectType.APPLICATION -> if (isExcluded) NxIcons.NX_APP_FOLDER_EXCLUDED else NxIcons.NX_APP_FOLDER
                NxProject.AngularProjectType.LIBRARY -> if (isExcluded) NxIcons.NX_LIB_FOLDER_EXCLUDED else NxIcons.NX_LIB_FOLDER
                null -> null
            }
        }
        return null
    }
}
