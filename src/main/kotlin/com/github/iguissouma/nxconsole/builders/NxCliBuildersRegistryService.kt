package com.github.iguissouma.nxconsole.builders

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

interface NxCliBuildersRegistryService {

    fun readBuilderSchema(project: Project, cliFolder: VirtualFile, builderName: String): List<NxBuilderOptions>?

    companion object {
        fun getInstance(): NxCliBuildersRegistryService {
            return ApplicationManager.getApplication().getService(NxCliBuildersRegistryService::class.java)
        }
    }
}
