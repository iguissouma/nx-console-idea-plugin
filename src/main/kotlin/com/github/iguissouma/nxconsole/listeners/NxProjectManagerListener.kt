package com.github.iguissouma.nxconsole.listeners

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.github.iguissouma.nxconsole.services.NxProjectService

internal class NxProjectManagerListener : ProjectManagerListener {

    override fun projectOpened(project: Project) {
        project.getService(NxProjectService::class.java)
    }
}
