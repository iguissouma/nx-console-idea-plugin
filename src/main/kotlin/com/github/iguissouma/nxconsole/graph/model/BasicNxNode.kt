package com.github.iguissouma.nxconsole.graph.model

import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

abstract class BasicNxNode(open val name: String, open val affected: Boolean, open val file: VirtualFile?) {

    abstract fun getIcon(): Icon
}
