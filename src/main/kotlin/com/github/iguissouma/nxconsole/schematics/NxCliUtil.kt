package com.github.iguissouma.nxconsole.schematics

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.NonNls

object NxCliUtil {

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

    @NonNls
    private val ANGULAR_JSON_NAMES: List<String> = listOf(
        "angular.json",
        ".angular-cli.json",
        "angular-cli.json",
        "workspace.json"
    )
}
