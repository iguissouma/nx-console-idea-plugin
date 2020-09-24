package com.github.iguissouma.nxconsole.graph.model

import javax.swing.Icon

abstract class BasicNxNode(open val name: String) {

    abstract fun getIcon(): Icon
}
