package com.github.iguissouma.nxconsole.listeners

import com.github.iguissouma.nxconsole.services.NxProjectService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener

internal class NxProjectManagerListener : ProjectManagerListener {

    override fun projectOpened(project: Project) {
        project.getService(NxProjectService::class.java)
    }
}
