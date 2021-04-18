package com.github.iguissouma.nxconsole.schematics

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

interface NxCliSchematicsRegistryService {
    /**
     * Loads schematics available in a particular location. The results are cached
     * and recalculated on every change of package.json in any node_modules directory.
     */
    abstract fun getSchematics(
        project: Project,
        cliFolder: VirtualFile,
        includeHidden: Boolean,
        logErrors: Boolean
    ): List<Schematic>

    /**
     * Loads schematics available in a particular location. The results are cached
     * and recalculated on every change of package.json in any node_modules directory.
     */
    fun getSchematics(
        project: Project,
        cliFolder: VirtualFile
    ): Collection<Schematic> {
        return getSchematics(project, cliFolder, false, false)
    }

    /**
     * Clears cache for getSchematics method
     */
    fun clearProjectSchematicsCache()

    companion object {
        fun getInstance(): NxCliSchematicsRegistryService {
            return ApplicationManager.getApplication().getService(NxCliSchematicsRegistryService::class.java)
        }
    }
}
