package com.github.iguissouma.nxconsole.graph.model

import com.intellij.icons.AllIcons
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

class AppNode(override val name: String, override val file: VirtualFile? = null) : BasicNxNode(name, file) {
    override fun getIcon(): Icon {
        // return AllIcons.Nodes.PpWeb
        return AllIcons.RunConfigurations.Web_app
    }
}
