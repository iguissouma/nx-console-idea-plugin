package com.github.iguissouma.nxconsoleideaplugin.services

import com.intellij.openapi.project.Project
import com.github.iguissouma.nxconsoleideaplugin.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
