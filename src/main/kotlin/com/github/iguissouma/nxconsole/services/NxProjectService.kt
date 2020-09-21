package com.github.iguissouma.nxconsole.services

import com.github.iguissouma.nxconsole.MyBundle
import com.intellij.openapi.project.Project

class NxProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
