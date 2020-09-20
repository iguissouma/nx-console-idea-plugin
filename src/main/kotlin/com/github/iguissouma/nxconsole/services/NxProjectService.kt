package com.github.iguissouma.nxconsole.services

import com.intellij.openapi.project.Project
import com.github.iguissouma.nxconsole.MyBundle

class NxProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
