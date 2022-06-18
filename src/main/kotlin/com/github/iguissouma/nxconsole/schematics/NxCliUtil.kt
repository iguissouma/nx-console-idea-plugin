package com.github.iguissouma.nxconsole.schematics

import com.github.iguissouma.nxconsole.NxBundle
import com.github.iguissouma.nxconsole.actions.NxConsoleNotificationGroup
import com.intellij.javascript.nodejs.CompletionModuleInfo
import com.intellij.javascript.nodejs.NodeModuleSearchUtil
import com.intellij.javascript.nodejs.packageJson.notification.PackageJsonGetDependenciesAction
import com.intellij.lang.javascript.buildTools.npm.PackageJsonUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

object NxCliUtil {

    private val NX_CLI_PACKAGE: String = "@nrwl/cli"

    fun findCliJson(dir: VirtualFile?): VirtualFile? {
        if (dir == null || !dir.isValid) return null
        for (name in ANGULAR_JSON_NAMES) {
            val cliJson = dir.findChild(name)
            if (cliJson != null) {
                return cliJson
            }
        }
        return null
    }

    fun findAngularCliFolder(project: Project, file: VirtualFile?): VirtualFile? {
        var current = file
        while (current != null) {
            if (current.isDirectory && findCliJson(current) != null) return current
            current = current.parent
        }
        return if (findCliJson(project.baseDir) != null) {
            project.baseDir
        } else null
    }

    private val ANGULAR_JSON_NAMES: List<String> = listOf(
        "angular.json",
        ".angular-cli.json",
        "angular-cli.json",
        "workspace.json"
    )

    fun hasNxCLIPackageInstalled(project: Project, cli: VirtualFile): Boolean {
        val modules: MutableList<CompletionModuleInfo> = mutableListOf()
        NodeModuleSearchUtil.findModulesWithName(modules, NX_CLI_PACKAGE, cli, null)
        return modules.isNotEmpty() && modules[0].virtualFile != null
    }

    fun notifyNxCliNotInstalled(
        project: Project,
        cliFolder: VirtualFile,
        message: String
    ) {
        val packageJson = PackageJsonUtil.findChildPackageJsonFile(cliFolder)
        val notification: Notification =
            NxConsoleNotificationGroup.GROUP.createNotification(
                message,
                NxBundle.message("nx.notify.cli.required-package-not-installed"),
                NotificationType.WARNING,
            )
        if (packageJson != null) {
            notification.addAction(PackageJsonGetDependenciesAction(project, packageJson, notification))
        }
        notification.notify(project)
    }
}
