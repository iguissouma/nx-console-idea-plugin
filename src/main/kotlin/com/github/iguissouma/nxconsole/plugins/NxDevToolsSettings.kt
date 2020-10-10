package com.github.iguissouma.nxconsole.plugins

import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.openapi.project.Project

class NxDevToolsSettings(
    val project: Project,
    var myInterpreterRef: NodeJsInterpreterRef? = NodeJsInterpreterRef.createProjectRef(),
    var myNxPackage: NodePackage? = NodePackage(""),
    var myNxJsonPath: String? = ""
)
