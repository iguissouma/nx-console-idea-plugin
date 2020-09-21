package com.github.iguissouma.nxconsole.buildTools

import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef
import com.intellij.openapi.util.io.FileUtil

class NxRunSettings(
    val interpreterRef: NodeJsInterpreterRef = NodeJsInterpreterRef.createProjectRef(),
    var nxFilePath: String? = null,
    var tasks: List<String> = emptyList()
) {
    val nxFileSystemIndependentPath: String? = nxFilePath?.let { FileUtil.toSystemIndependentName(it) }
}
