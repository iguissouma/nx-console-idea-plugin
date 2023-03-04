package com.github.iguissouma.nxconsole.buildTools

import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import com.intellij.lang.javascript.buildTools.base.JsbtFileStructure
import com.intellij.lang.javascript.buildTools.base.JsbtTree
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.tree.DefaultMutableTreeNode

class NxFileStructure(val nxJson: VirtualFile) : JsbtFileStructure(nxJson) {

    var myScripts: List<NxTask> = mutableListOf()
    var myNxGenerateAndRunTargetTask: List<NxTask> =
        mutableListOf("Generate", "Run", "Build", "Serve", "Serve", "Test", "E2e", "Lint", "Change workspace").map {
            NxTask(this, it)
        }
    var myNxProjectsTask: Map<String, List<NxTask>> = mutableMapOf()
    var myNxCommonCommandTask: List<NxTask> = mutableListOf(
        "dep-graph",
        "run-many",
        "affected",
        "affected:apps",
        "affected:build",
        "affected:dep-graph",
        "affected:e2e",
        "affected:libs",
        "affected:lint",
        "affected:test",
        "list",
        "migrate"
    ).map { NxTask(this, it) }

    private var myTaskNames: List<String> = mutableListOf()


    fun setScripts(scripts: List<NxTask>) {
        myScripts = ImmutableList.copyOf(scripts)
        myTaskNames = ImmutableList.copyOf(Lists.transform(myScripts) { input: NxTask? -> input!!.name })
    }

    companion object {
        fun getUserObject(node: DefaultMutableTreeNode?): NxFileStructure? {
            return JsbtTree.getUserObject(node, NxFileStructure::class.java)
        }
    }

    override val taskNames: List<String> = myNxProjectsTask.flatMap { it.value.map { t -> "${it.key}:${t.name}" } }
}
