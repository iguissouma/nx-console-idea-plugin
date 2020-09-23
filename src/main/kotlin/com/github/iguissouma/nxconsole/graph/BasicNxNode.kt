package com.github.iguissouma.nxconsole.graph

import com.intellij.icons.AllIcons
import javax.swing.Icon

abstract class BasicNxNode(open val name: String) {

    abstract fun getIcon(): Icon

}


class AppNode(override val name: String) : BasicNxNode(name) {
    override fun getIcon(): Icon {
        // return AllIcons.Nodes.PpWeb
        return AllIcons.RunConfigurations.Web_app
    }
}


class LibNode(override val name: String) : BasicNxNode(name) {
    override fun getIcon(): Icon {
        return AllIcons.Nodes.PpLibFolder
    }
}
