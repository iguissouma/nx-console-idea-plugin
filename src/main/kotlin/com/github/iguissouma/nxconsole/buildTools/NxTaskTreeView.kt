package com.github.iguissouma.nxconsole.buildTools

import com.github.iguissouma.nxconsole.NxIcons
import com.intellij.icons.AllIcons
import com.intellij.lang.javascript.buildTools.base.*
import com.intellij.openapi.project.Project
import com.intellij.pom.Navigatable
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ObjectUtils
import com.intellij.util.SmartList
import javax.swing.tree.DefaultMutableTreeNode

class NxTaskTreeView(val nxService: NxService, val project: Project, val layoutPlace: String?) :
    JsbtTaskTreeView(nxService, project, layoutPlace) {

    private val NO_SCRIPTS_FOUND = "No scripts found"

    override fun addBuildfileChildren(buildfileTreeNode: DefaultMutableTreeNode, _structure: JsbtFileStructure) {

        val structure = _structure as NxFileStructure

        val generateAndRunTargetNode = DefaultMutableTreeNode("Generate And Run Target", true)
        for (myScript in structure.myNxGenerateAndRunTargetTask) {
            val node = DefaultMutableTreeNode(myScript, false)
            generateAndRunTargetNode.add(node)
        }
        //buildfileTreeNode.add(generateAndRunTargetNode)

        val nxCommonCommandTaskNode = DefaultMutableTreeNode("Common Nx Commands", true)
        for (myScript in structure.myNxCommonCommandTask) {
            val node = DefaultMutableTreeNode(myScript, false)
            nxCommonCommandTaskNode.add(node)
        }
        //buildfileTreeNode.add(nxCommonCommandTaskNode)

        val nxProjectsTaskNode = DefaultMutableTreeNode("Projects", true)
        for (myScript: Map.Entry<String, List<NxTask>> in structure.myNxProjectsTask.entries) {
            val projectNode = DefaultMutableTreeNode(myScript.key, true)
            myScript.value.forEach {
                val node = DefaultMutableTreeNode(it, false)
                projectNode.add(node)
            }
            nxProjectsTaskNode.add(projectNode)
        }
        buildfileTreeNode.add(nxProjectsTaskNode)

        if (structure.myScripts.isEmpty()) {
            buildfileTreeNode.add(DefaultMutableTreeNode("No scripts found", false))
        }

    }

    override fun hasTaskNodes(nxTreeNode: DefaultMutableTreeNode): Boolean {
        val childCount: Int = nxTreeNode.getChildCount()
        for (i in 0 until childCount) {
            val task =
                NxTask.getUserObject(ObjectUtils.tryCast(nxTreeNode.getChildAt(i), DefaultMutableTreeNode::class.java))
            if (task != null) {
                return true
            }
        }
        return false
    }

    override fun customizeCell(project: Project, renderer: ColoredTreeCellRenderer, node: DefaultMutableTreeNode) {
        renderer.toolTipText = null
        val structure = NxFileStructure.getUserObject(node)
        if (structure != null) {
            renderer.icon = NxIcons.NRWL_ICON
            renderer.isIconOnTheRight = false
            renderer.append(JsbtUtil.getRelativePath(project, structure.buildfile))
        } else if ("No scripts found" == node.userObject) {
            renderer.append("No scripts found", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        } /*else if ("Generate And Run Target" == node.userObject) {
      renderer.append("Generate And Run Target", SimpleTextAttributes.REGULAR_ATTRIBUTES)
    } else if ("Common Nx Commands" == node.userObject) {
      renderer.append("Common Nx Commands", SimpleTextAttributes.REGULAR_ATTRIBUTES)
    } else if ("Projects" == node.userObject) {
      renderer.append("Projects", SimpleTextAttributes.REGULAR_ATTRIBUTES)
    } */ else {
            val script = NxTask.getUserObject(node)
            if (script != null) {
                renderer.icon = AllIcons.Nodes.C_plocal
                renderer.append(script.myName, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            } else {
                renderer.append(node.userObject as String, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            }
        }

    }

    override fun getPersistentId(node: DefaultMutableTreeNode): String? {
        val structure = NxFileStructure.getUserObject(node)
        return if (structure != null) {
            structure.buildfile.path
        } else {
            val script = NxTask.getUserObject(node)
            script?.myName
        }
    }

    override fun getPresentableTaskName(node: DefaultMutableTreeNode): String? {
        val script = NxTask.getUserObject(node)
        return script?.myName
    }

    override fun createTaskSetFromSelectedNodes(): JsbtTaskSet? {
        val nodes = this.selectedNodes
        if (nodes.isEmpty()) {
            return null
        } else {
            var resultStructure: NxFileStructure? = null
            val taskNames: MutableList<String> = SmartList<String>()
            for (node in nodes) {
                val task = NxTask.getUserObject(node) ?: return null
                val structure = task.myStructure

                if (resultStructure != null && resultStructure != structure) {
                    return null
                }
                resultStructure = structure
                val parent = node.parent as DefaultMutableTreeNode
                taskNames.add("${parent.userObject}:"+ task.myName)
            }
            return resultStructure?.let { JsbtTaskSet(it, taskNames) }
        }
    }

    override fun createJumpToSourceDescriptor(p0: Project, p1: DefaultMutableTreeNode): Navigatable? {
        return null;
    }

    override fun compareNodes(node1: DefaultMutableTreeNode, node2: DefaultMutableTreeNode, p2: JsbtSortingMode): Int {
        val script1 = NxTask.getUserObject(node1)
        val script2 = NxTask.getUserObject(node2)
        return if (script1 != null && script2 != null) {
            if (sortingMode == JsbtSortingMode.NAME) script1.myName.compareTo(script2.myName) else getPosition(script1) - getPosition(
                script2
            )
        } else if (script1 == null && script2 == null) {
            0
        } else {
            if (script1 == null) 1 else -1
        }
    }

    private fun getPosition(script: NxTask): Int {
        return script.myStructure.myScripts.indexOf(script)
    }

}
