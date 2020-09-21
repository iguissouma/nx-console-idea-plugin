package com.github.iguissouma.nxconsole.buildTools

import com.intellij.lang.javascript.buildTools.base.JsbtTree
import javax.swing.tree.DefaultMutableTreeNode

open class NxTask(open val structure: NxFileStructure, open val name: String) {

    companion object {
        fun getUserObject(node: DefaultMutableTreeNode?): NxTask? {
            return JsbtTree.getUserObject(node, NxTask::class.java)
        }
    }
}

class NxGenerateAndRunTargetTask(
    override val structure: NxFileStructure,
    override val name: String
) : NxTask(structure, name) {

    val myTasks = listOf("generate", "run")
}
