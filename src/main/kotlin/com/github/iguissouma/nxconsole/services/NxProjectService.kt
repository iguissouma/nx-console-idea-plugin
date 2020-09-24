package com.github.iguissouma.nxconsole.services

import com.github.iguissouma.nxconsole.NxBundle
import com.intellij.openapi.project.Project

class NxProjectService(project: Project) {

    init {
        println(NxBundle.message("projectService", project.name))
    }
}
