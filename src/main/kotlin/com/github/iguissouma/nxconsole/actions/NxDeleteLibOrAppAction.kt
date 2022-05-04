package com.github.iguissouma.nxconsole.actions

import com.github.iguissouma.nxconsole.NxIcons
import com.github.iguissouma.nxconsole.cli.NxCliFilter
import com.github.iguissouma.nxconsole.cli.config.NxConfigProvider
import com.github.iguissouma.nxconsole.execution.NxGenerator
import com.intellij.CommonBundle
import com.intellij.javascript.nodejs.CompletionModuleInfo
import com.intellij.javascript.nodejs.NodeModuleSearchUtil
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationBundle
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.ui.UIBundle

class NxDeleteLibOrAppAction : AnAction(NxIcons.NRWL_ICON) {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val appOrLibDirectory = event.getData(PlatformDataKeys.VIRTUAL_FILE) ?: return
        if (!appOrLibDirectory.isDirectory) {
            return
        }
        val nxConfig = NxConfigProvider.getNxConfig(project, appOrLibDirectory) ?: return
        val nxProjectToRemove = nxConfig.projects.firstOrNull { it.rootDir == appOrLibDirectory } ?: return
        val title = "${nxProjectToRemove.type?.name?.toLowerCase()?.capitalize()} ''${nxProjectToRemove.name}''"
        val message = "Are you sure you want to remove $title from the workspace?"
        val returnValue = Messages.showOkCancelDialog(
            message,
            UIBundle.message("delete.dialog.title"),
            ApplicationBundle.message("button.delete"),
            CommonBundle.getCancelButtonText(),
            Messages.getQuestionIcon()
        )
        if (returnValue != Messages.OK) return
        val modules: MutableList<CompletionModuleInfo> = mutableListOf()
        NodeModuleSearchUtil.findModulesWithName(modules, "@nrwl/cli", nxConfig.angularJsonFile.parent, null)
        val module = modules.firstOrNull() ?: return
        val filter = NxCliFilter(project, project.baseDir.path)
        val interpreter = NodeJsInterpreterManager.getInstance(project).interpreter ?: return

        val args = arrayOf(
            "@nrwl/workspace:remove",
            "--project",
            nxProjectToRemove.name
        )
        NxGenerator().generate(
            interpreter,
            NodePackage(module.virtualFile?.path!!),
            { pkg -> pkg?.findBinFile("nx", null)?.absolutePath },
            nxConfig.angularJsonFile.parent,
            VfsUtilCore.virtualToIoFile(nxConfig.angularJsonFile.parent ?: nxConfig.angularJsonFile.parent),
            project,
            null,
            "Remove $title",
            arrayOf(filter),
            "generate",
            *args
        )
    }
}
