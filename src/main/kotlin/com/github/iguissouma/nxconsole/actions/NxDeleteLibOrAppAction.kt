package com.github.iguissouma.nxconsole.actions

import com.github.iguissouma.nxconsole.NxIcons
import com.github.iguissouma.nxconsole.cli.NxCliFilter
import com.github.iguissouma.nxconsole.cli.config.NxConfigProvider
import com.github.iguissouma.nxconsole.execution.NxGenerator
import com.intellij.CommonBundle
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationBundle
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.ui.UIBundle
import java.util.*

class NxDeleteLibOrAppAction : AnAction(NxIcons.NRWL_ICON) {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val appOrLibDirectory = event.getData(PlatformDataKeys.VIRTUAL_FILE) ?: return
        if (!appOrLibDirectory.isDirectory) {
            return
        }
        val nxConfig = NxConfigProvider.getNxConfig(project, appOrLibDirectory) ?: return
        val nxProjectToRemove = nxConfig.projects.firstOrNull { it.rootDir == appOrLibDirectory } ?: return
        val title = "${
            nxProjectToRemove.type?.name?.lowercase(Locale.getDefault())
            ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }} ''${nxProjectToRemove.name}''"
        val message = "Are you sure you want to remove $title from the workspace?"
        val returnValue = Messages.showOkCancelDialog(
            message,
            UIBundle.message("delete.dialog.title"),
            ApplicationBundle.message("button.delete"),
            CommonBundle.getCancelButtonText(),
            Messages.getQuestionIcon()
        )
        if (returnValue != Messages.OK) return
        val filter = NxCliFilter(project, project.baseDir.path)
        val interpreter = NodeJsInterpreterManager.getInstance(project).interpreter ?: return
        val args = arrayOf(
            "generate",
            "@nrwl/workspace:remove",
            "--project",
            nxProjectToRemove.name
        )
        NxGenerator().generate(
            node = interpreter,
            baseDir = nxConfig.angularJsonFile.parent,
            workingDir = VfsUtilCore.virtualToIoFile(
                nxConfig.angularJsonFile.parent ?: nxConfig.angularJsonFile.parent
            ),
            project = project,
            callback = null,
            title = "Remove $title",
            filters = arrayOf(filter),
            args = args
        )
    }
}
