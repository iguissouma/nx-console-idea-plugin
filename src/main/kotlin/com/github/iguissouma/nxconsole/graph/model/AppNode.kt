package com.github.iguissouma.nxconsole.graph.model

import com.intellij.icons.AllIcons
import javax.swing.Icon

class AppNode(override val name: String) : BasicNxNode(name) {
    override fun getIcon(): Icon {
        // return AllIcons.Nodes.PpWeb
        return AllIcons.RunConfigurations.Web_app
    }
}
