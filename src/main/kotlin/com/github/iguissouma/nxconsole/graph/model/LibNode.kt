package com.github.iguissouma.nxconsole.graph.model

import com.intellij.icons.AllIcons
import javax.swing.Icon

class LibNode(override val name: String) : BasicNxNode(name) {
    override fun getIcon(): Icon {
        return AllIcons.Nodes.PpLibFolder
    }
}
