package com.github.iguissouma.nxconsole.buildTools

import com.github.iguissouma.nxconsole.NxIcons
import com.github.iguissouma.nxconsole.buildTools.NxJsonUtil.findChildAngularJsonFile
import com.github.iguissouma.nxconsole.buildTools.NxJsonUtil.findProjectProperty
import com.github.iguissouma.nxconsole.cli.config.NxConfigProvider
import com.intellij.execution.PsiLocation
import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.impl.ProjectRootsUtil
import com.intellij.json.psi.JsonObject
import com.intellij.lang.javascript.buildTools.base.JsbtFileStructure
import com.intellij.lang.javascript.buildTools.base.JsbtSortingMode
import com.intellij.lang.javascript.buildTools.base.JsbtTaskSet
import com.intellij.lang.javascript.buildTools.base.JsbtTaskTreeView
import com.intellij.lang.javascript.buildTools.base.JsbtUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.pom.Navigatable
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ObjectUtils
import com.intellij.util.SmartList
import javax.swing.tree.DefaultMutableTreeNode

class NxTaskTreeView(val nxService: NxService, val project: Project, val layoutPlace: String?) :
    JsbtTaskTreeView(nxService, project, layoutPlace), Disposable {

    var filterByAffected: List<String> = emptyList()
    var isAffected = false

    companion object {

        private const val NO_TASKS_FOUND = "No tasks found"
    }

    override fun addBuildfileChildren(buildfileTreeNode: DefaultMutableTreeNode, _structure: JsbtFileStructure) {

        val structure = _structure as NxFileStructure

        val generateAndRunTargetNode = DefaultMutableTreeNode("Generate And Run Target", true)
        for (myScript in structure.myNxGenerateAndRunTargetTask) {
            val node = DefaultMutableTreeNode(myScript, false)
            generateAndRunTargetNode.add(node)
        }
        // buildfileTreeNode.add(generateAndRunTargetNode)

        val nxConfig = NxConfigProvider.getNxConfig(project, project.baseDir)
        val module = ModuleManager.getInstance(project).modules.first()
        val unloadedModules =
            nxConfig?.projects?.filter { ProjectRootsUtil.findExcludeFolder(module!!, it.rootDir!!) != null }
                ?.map { it.name } ?: emptyList()

        // nxConfig?.projects?.map { it.name to it.architect.keys }
        val groupByTasks = nxConfig?.projects?.flatMap { p -> p.architect.keys.map { it to p.name } }
            ?.groupBy({ it.first }, { it.second })
            ?.toSortedMap()

        val tasksNode = DefaultMutableTreeNode("Tasks", true)
        groupByTasks?.forEach {
            val node = DefaultMutableTreeNode(it.key, true)
            tasksNode.add(node)
            it.value.filterNot { project -> project in unloadedModules }.forEach { e ->
                node.add(DefaultMutableTreeNode(NxTask(structure, e), false))
            }
        }
        buildfileTreeNode.add(tasksNode)

        val nxProjectsTaskNode = DefaultMutableTreeNode("Projects", true)
        for (myScript: Map.Entry<String, List<NxTask>> in structure.myNxProjectsTask.entries.filterNot { it.key in unloadedModules }) {
            val projectNode = DefaultMutableTreeNode(myScript.key, true)
            myScript.value.forEach {
                val node = DefaultMutableTreeNode(it, false)
                projectNode.add(node)
            }
            if (!isAffected) {
                nxProjectsTaskNode.add(projectNode)
            } else if (filterByAffected.contains(myScript.key)) {
                nxProjectsTaskNode.add(projectNode)
            }
        }
        buildfileTreeNode.add(nxProjectsTaskNode)

        if (structure.myScripts.isEmpty()) {
            buildfileTreeNode.add(DefaultMutableTreeNode(NO_TASKS_FOUND, false))
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
        } else {
            val script = NxTask.getUserObject(node)
            if (script != null) {
                renderer.icon = AllIcons.Nodes.C_plocal
                renderer.append(script.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
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
            script?.name
        }
    }

    override fun getPresentableTaskName(node: DefaultMutableTreeNode): String? {
        val script = NxTask.getUserObject(node)
        return script?.name ?: node.userObject as? String
    }

    override fun createTaskSetFromSelectedNodes(): JsbtTaskSet? {
        val nodes = this.selectedNodes
        if (nodes.isEmpty()) {
            return null
        } else {
            var resultStructure: NxFileStructure? = null
            val taskNames: MutableList<String> = SmartList()
            for (node in nodes) {
                val task = NxTask.getUserObject(node) ?: return null
                val structure = task.structure

                if (resultStructure != null && resultStructure != structure) {
                    return null
                }
                resultStructure = structure
                val parent = node.parent as DefaultMutableTreeNode
                if ((parent.parent as DefaultMutableTreeNode).userObject == "Tasks") {
                    taskNames.add(task.name + ":${parent.userObject}")
                } else {
                    taskNames.add("${parent.userObject}:" + task.name)
                }
            }
            return resultStructure?.let { JsbtTaskSet(it, taskNames) }
        }
    }

    override fun createJumpToSourceDescriptor(project: Project, node: DefaultMutableTreeNode): Navigatable? {
        val script = NxTask.getUserObject(node)
        if (script == null && (node.userObject as String != "Projects")) {
            // TODO check project.baseDir
            val angularJsonFile = findChildAngularJsonFile(project.baseDir) ?: return null
            val property = findProjectProperty(
                project,
                angularJsonFile,
                node.userObject as String
            )
            if (property != null) {
                val location = PsiLocation.fromPsiElement(property)
                if (location != null) {
                    return location.openFileDescriptor
                }
            }
        } else if (script != null) {
            val virtualNxJson = script.structure.buildfile
            val angularJsonFile = findChildAngularJsonFile(virtualNxJson.parent) ?: return null
            val property = findProjectProperty(
                project,
                angularJsonFile,
                (node.parent as DefaultMutableTreeNode).userObject as String
            )
            if (property != null) {
                val jsonObject = property.value as JsonObject
                val architectProperty = jsonObject
                    .findProperty("architect") ?: jsonObject.findProperty("targets")
                val taskProperty = (architectProperty?.value as JsonObject).findProperty(script.name)
                val location = PsiLocation.fromPsiElement(taskProperty)
                if (location != null) {
                    return location.openFileDescriptor
                }
            }
        }
        return null
    }

    override fun compareNodes(node1: DefaultMutableTreeNode, node2: DefaultMutableTreeNode, p2: JsbtSortingMode): Int {
        val script1 = NxTask.getUserObject(node1)
        val script2 = NxTask.getUserObject(node2)
        return if (script1 != null && script2 != null) {
            if (sortingMode == JsbtSortingMode.NAME) script1.name.compareTo(script2.name) else getPosition(script1) - getPosition(
                script2
            )
        } else if (script1 == null && script2 == null) {
            0
        } else {
            if (script1 == null) 1 else -1
        }
    }

    private fun getPosition(script: NxTask): Int {
        return script.structure.myScripts.indexOf(script)
    }
}
