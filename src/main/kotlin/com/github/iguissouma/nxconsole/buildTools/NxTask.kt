package com.github.iguissouma.nxconsole.buildTools

import com.intellij.lang.javascript.buildTools.base.JsbtTree
import javax.swing.tree.DefaultMutableTreeNode

open class NxTask(open val myStructure: NxFileStructure, open val myName: String) {

    companion object {
        fun getUserObject(node: DefaultMutableTreeNode?): NxTask? {
            return JsbtTree.getUserObject(node, NxTask::class.java)
        }
    }


}

class NxGenerateAndRunTargetTask(
    override val myStructure: NxFileStructure,
    override val myName: String
) : NxTask(myStructure, myName) {

    val myTasks = listOf("generate", "run")

}
