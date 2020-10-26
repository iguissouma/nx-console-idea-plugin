package com.github.iguissouma.nxconsole.graph.model

import com.github.iguissouma.nxconsole.NxIcons
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

class AppNode(override val name: String, override val affected: Boolean, override val file: VirtualFile? = null) :
    BasicNxNode(name, affected, file) {
    override fun getIcon(): Icon {
        return NxIcons.NX_APP_FOLDER
    }
}
