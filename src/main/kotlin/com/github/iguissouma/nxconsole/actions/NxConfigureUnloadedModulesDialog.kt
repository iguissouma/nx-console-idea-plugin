package com.github.iguissouma.nxconsole.actions

import com.github.iguissouma.nxconsole.NxBundle
import com.github.iguissouma.nxconsole.cli.NxCliProjectGenerator
import com.github.iguissouma.nxconsole.cli.config.NxConfigProvider
import com.github.iguissouma.nxconsole.util.NxExecutionUtil
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.intellij.CommonBundle
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil.execAndGetOutput
import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.actions.MarkRootActionBase.findContentEntry
import com.intellij.ide.projectView.impl.ProjectRootsUtil
import com.intellij.javascript.nodejs.interpreter.NodeCommandLineConfigurator
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.javascript.nodejs.packageJson.NodeInstalledPackageFinder
import com.intellij.lang.javascript.service.JSLanguageServiceUtil
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.ModuleDescription
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.openapi.util.text.NaturalComparator
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.DoubleClickListener
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.graph.CachingSemiGraph
import com.intellij.util.graph.Graph
import com.intellij.util.graph.GraphAlgorithms
import com.intellij.util.graph.GraphGenerator
import com.intellij.util.graph.InboundSemiGraph
import com.intellij.util.ui.GridBag
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.tree.TreeUtil
import com.intellij.xml.util.XmlStringUtil
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreePath

class NxModuleDescription(val myName: String, val myDependencyModuleNames: MutableList<String> = mutableListOf()) :
    ModuleDescription {
    override fun getName(): String {
        return this.myName
    }

    override fun getDependencyModuleNames(): MutableList<String> {
        return this.myDependencyModuleNames
    }
}

class NxConfigureUnloadedModulesDialog(private val project: Project, projectGraph:Map<*,*>, selectedModuleName: String?) :
    DialogWrapper(project) {
    private val nxConfig = NxConfigProvider.getNxConfig(project, project.baseDir)
    private val loadedModulesTree = NxModuleDescriptionsTree(project)
    private val unloadedModulesTree = NxModuleDescriptionsTree(project)
    private var moduleDescriptions: Map<String, NxModuleDescription> =
        nxConfig?.projects?.associateBy({ it.name }, { NxModuleDescription(it.name) }) ?: emptyMap()
    private val statusLabel = JBLabel()

    //
    private val module = ModuleManager.getInstance(project).modules.first()
    private val loadedModules =
        nxConfig?.projects?.filter { it.rootDir != null }
            ?.filter { ProjectRootsUtil.findExcludeFolder(module!!, it.rootDir!!) == null }
            ?.map { it.name } ?: emptyList()
    private val unloadedModules =
        nxConfig?.projects?.filter { it.rootDir != null }
            ?.filter { ProjectRootsUtil.findExcludeFolder(module!!, it.rootDir!!) != null }
            ?.map { it.name } ?: emptyList()

    /** graph contains an edge a -> b if b depends on a */
    private val dependentsGraph by lazy { buildGraph() }
    private val initiallyFocusedTree: NxModuleDescriptionsTree

    init {
        title = NxBundle.message("nx.module.load.unload.dialog.title")

        val map = projectGraph["dependencies"] as Map<*, *>
        nxConfig?.projects?.forEach {
            val nxModuleDescription = moduleDescriptions[it.name]
            (map[it.name] as List<Map<*, *>>).forEach { x ->
                nxModuleDescription?.dependencyModuleNames?.add(x["target"] as String)
            }
        }

        loadedModulesTree.fillTree(moduleDescriptions.values.filter { it.name in loadedModules })
        unloadedModulesTree.fillTree(moduleDescriptions.values.filter { it.name in unloadedModules })
        if (selectedModuleName != null) {
            initiallyFocusedTree = if (selectedModuleName in unloadedModules) unloadedModulesTree else loadedModulesTree
            initiallyFocusedTree.selectNodes(setOf(selectedModuleName))
        } else {
            initiallyFocusedTree = loadedModulesTree
        }
        init()
    }

    private fun buildGraph(): Graph<NxModuleDescription> {
        return GraphGenerator.generate(
            CachingSemiGraph.cache(
                object : InboundSemiGraph<NxModuleDescription> {
                    override fun getNodes(): Collection<NxModuleDescription> {
                        return moduleDescriptions.values
                    }

                    override fun getIn(node: NxModuleDescription): Iterator<NxModuleDescription> {
                        return node.dependencyModuleNames.asIterable().mapNotNull { moduleDescriptions[it] }.iterator()
                    }
                }
            )
        )
    }

    override fun createCenterPanel(): JComponent? {
        val buttonsPanel = JPanel(VerticalFlowLayout())
        val moveToUnloadedButton = JButton(NxBundle.message("nx.module.unload.button.text"))
        val moveToLoadedButton = JButton(NxBundle.message("nx.module.load.button.text"))
        val moveAllToUnloadedButton = JButton(NxBundle.message("nx.module.unload.all.button.text"))
        val moveAllToLoadedButton = JButton(NxBundle.message("nx.module.load.all.button.text"))
        moveToUnloadedButton.addActionListener {
            moveToUnloaded()
        }
        moveToLoadedButton.addActionListener {
            moveToLoaded()
        }
        moveAllToUnloadedButton.addActionListener {
            moveAllNodes(loadedModulesTree, unloadedModulesTree)
        }
        moveAllToLoadedButton.addActionListener {
            moveAllNodes(unloadedModulesTree, loadedModulesTree)
        }
        buttonsPanel.add(moveToUnloadedButton)
        buttonsPanel.add(moveToLoadedButton)
        buttonsPanel.add(moveAllToUnloadedButton)
        buttonsPanel.add(moveAllToLoadedButton)
        loadedModulesTree.installDoubleClickListener(this::moveToUnloaded)
        unloadedModulesTree.installDoubleClickListener(this::moveToLoaded)

        val mainPanel = JPanel(BorderLayout())
        val gridBag = GridBag().setDefaultWeightX(0, 0.5).setDefaultWeightX(1, 0.0).setDefaultWeightX(2, 0.5)
        val treesPanel = JPanel(GridBagLayout())
        treesPanel.add(
            JBLabel(NxBundle.message("nx.module.loaded.label.text")),
            gridBag.nextLine().next().anchor(GridBagConstraints.WEST)
        )
        treesPanel.add(
            JBLabel(NxBundle.message("nx.module.unloaded.label.text")),
            gridBag.next().next().anchor(GridBagConstraints.WEST)
        )

        treesPanel.add(JBScrollPane(loadedModulesTree.tree), gridBag.nextLine().next().weighty(1.0).fillCell())
        treesPanel.add(buttonsPanel, gridBag.next().anchor(GridBagConstraints.CENTER))
        treesPanel.add(JBScrollPane(unloadedModulesTree.tree), gridBag.next().weighty(1.0).fillCell())
        mainPanel.add(treesPanel, BorderLayout.CENTER)
        statusLabel.text = XmlStringUtil.wrapInHtml(NxBundle.message("nx.module.unloaded.explanation"))
        mainPanel.add(statusLabel, BorderLayout.SOUTH)
        // current label text looks better when it's split on 2.5 lines, so set size of the whole component accordingly
        mainPanel.preferredSize = Dimension(
            Math.max(treesPanel.preferredSize.width, statusLabel.preferredSize.width * 2 / 5),
            treesPanel.preferredSize.height
        )
        return mainPanel
    }

    private fun moveToLoaded() {
        val modulesToMove = includeMissingModules(
            unloadedModulesTree.getSelectedModules(),
            loadedModulesTree.getAllModules(),
            GraphAlgorithms.getInstance().invertEdgeDirections(dependentsGraph),
            NxBundle.message("nx.module.load.dependencies.dialog.title"),
            { selectedSize, additionalSize, additionalFirst ->
                NxBundle.message(
                    "nx.module.load.dependencies.dialog.text",
                    selectedSize,
                    additionalSize,
                    additionalFirst
                )
            },
            NxBundle.message("nx.module.load.with.dependencies.button.text"),
            NxBundle.message("nx.module.load.without.dependencies.button.text")
        )
        moveModules(modulesToMove, unloadedModulesTree, loadedModulesTree)
    }

    private fun moveToUnloaded() {
        val modulesToMove = includeMissingModules(
            loadedModulesTree.getSelectedModules(),
            unloadedModulesTree.getAllModules(),
            dependentsGraph,
            NxBundle.message("nx.module.unload.dependents.dialog.title"),
            { selectedSize, additionalSize, additionalFirst ->
                NxBundle.message(
                    "nx.module.unload.dependents.dialog.text",
                    selectedSize,
                    additionalSize,
                    additionalFirst
                )
            },
            NxBundle.message("nx.module.unload.with.dependents.button.text"),
            NxBundle.message("nx.module.unload.without.dependents.button.text")
        )
        moveModules(modulesToMove, loadedModulesTree, unloadedModulesTree)
    }

    private fun includeMissingModules(
        selected: List<NxModuleDescription>,
        availableTargetModules: List<NxModuleDescription>,
        dependenciesGraph: Graph<NxModuleDescription>,
        dialogTitle: String,
        dialogMessage: (Int, Int, String) -> String,
        yesButtonText: String,
        noButtonText: String
    ): Collection<NxModuleDescription> {
        val additional = computeDependenciesToMove(selected, availableTargetModules, dependenciesGraph)
        if (additional.isNotEmpty()) {
            val answer = Messages.showYesNoCancelDialog(
                project,
                dialogMessage(selected.size, additional.size, additional.first().name),
                dialogTitle,
                yesButtonText,
                noButtonText,
                CommonBundle.getCancelButtonText(),
                null
            )
            if (answer == Messages.YES) {
                return selected + additional
            }
            if (answer == Messages.CANCEL) {
                return emptyList()
            }
        }
        return selected
    }

    private fun computeDependenciesToMove(
        modulesToMove: Collection<NxModuleDescription>,
        availableModules: Collection<NxModuleDescription>,
        graph: Graph<NxModuleDescription>
    ): Set<NxModuleDescription> {
        val result = LinkedHashSet<NxModuleDescription>()
        for (module in modulesToMove) {
            GraphAlgorithms.getInstance().collectOutsRecursively(graph, module, result)
        }
        result.removeAll(modulesToMove)
        result.removeAll(availableModules)
        return result
    }

    private fun moveAllNodes(from: NxModuleDescriptionsTree, to: NxModuleDescriptionsTree) {
        from.removeAllNodes()
        to.fillTree(moduleDescriptions.values)
        IdeFocusManager.getInstance(project).requestFocus(to.tree, true).doWhenDone {
            to.tree.selectionPath = to.tree.getPathForRow(0)
        }
    }

    private fun moveModules(
        modulesToMove: Collection<NxModuleDescription>,
        from: NxModuleDescriptionsTree,
        to: NxModuleDescriptionsTree
    ) {
        if (modulesToMove.isEmpty()) return
        val oldSelectedRow = from.tree.selectionModel.leadSelectionRow
        from.removeModules(modulesToMove)
        val modules = to.addModules(modulesToMove)
        modules.firstOrNull()?.let { TreeUtil.selectNode(to.tree, it) }
        IdeFocusManager.getInstance(project).requestFocus(from.tree, true).doWhenDone {
            from.tree.selectionModel.selectionPath =
                from.tree.getPathForRow(oldSelectedRow.coerceAtMost(from.tree.rowCount - 1))
        }
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return initiallyFocusedTree?.tree
    }

    override fun doOKAction() {
        // ModuleManager.getInstance(project).setUnloadedModules(unloadedModulesTree.getAllModules().map { it.name })
        ModuleManager.getInstance(project).modules.firstOrNull()?.run {
            // val module: Module? = ModuleUtilCore.findModuleForFile(virtualFile, project)
            /* ModuleRootModificationUtil.updateExcludedFolders(
                 this,
                 project.baseDir,
                 nxConfig?.projects?.filter { nxProject ->
                     nxProject.name in loadedModulesTree.getAllModules().map { it.name }
                 }?.mapNotNull { it.rootDir?.path }?.toMutableList() ?: mutableListOf(),
                 nxConfig?.projects?.filter { nxProject ->
                     nxProject.name in unloadedModulesTree.getAllModules().map { it.name }
                 }?.mapNotNull { it.rootDir?.path }?.toMutableList() ?: mutableListOf()
             )*/

            ModuleRootModificationUtil.updateModel(this) { model ->
                runReadAction {
                    nxConfig?.projects?.filter { nxProject ->
                        nxProject.name in unloadedModulesTree.getAllModules().map { it.name }
                    }
                        ?.forEach {
                            it.rootDir?.run { findContentEntry(model, this)?.addExcludeFolder(this) }
                        }

                    nxConfig?.projects?.filter { nxProject ->
                        nxProject.name in loadedModulesTree.getAllModules().map { it.name }
                    }
                        ?.forEach {
                            val findExcludeFolder = ProjectRootsUtil.findExcludeFolder(this, it.rootDir!!)
                            if (findExcludeFolder != null) {
                                it.rootDir?.run {
                                    findContentEntry(model, this)?.removeExcludeFolder(findExcludeFolder)
                                }
                            }
                        }
                }
            }
        }
        /* ModuleRootModificationUtil.updateModel(module) { model ->
             runReadAction {
                 nxConfig?.projects?.filter { nxProject ->
                     nxProject.name in unloadedModulesTree.getAllModules().map { it.name }
                 }
                     ?.forEach {
                         it.rootDir?.run { findContentEntry(model, this)?.addExcludeFolder(this) }
                     }
             }
         }*/
        super.doOKAction()
    }
}

private class NxModuleDescriptionsTree(project: Project) {
    private val root = RootNode()
    private val model = DefaultTreeModel(root)

    // private val helper = createModuleDescriptionHelper(project)
    internal val tree = Tree(model)

    init {
        tree.isRootVisible = false
        tree.showsRootHandles = true
        TreeSpeedSearch(
            tree,
            { treePath -> (treePath.lastPathComponent as? NxModuleDescriptionTreeNode)?.text ?: "" },
            true
        )
        tree.cellRenderer = ModuleDescriptionTreeRenderer()
    }

    fun getSelectedModules(): List<NxModuleDescription> =
        tree.selectionPaths
            ?.mapNotNull { it.lastPathComponent }
            ?.filterIsInstance<NxModuleDescriptionTreeNode>()
            ?.flatMap { getAllModulesUnder(it) }
            ?: emptyList<NxModuleDescription>()

    fun getAllModules() = getAllModulesUnder(root)

    fun installDoubleClickListener(action: () -> Unit) {
        object : DoubleClickListener() {
            override fun onDoubleClick(event: MouseEvent): Boolean {
                if (tree.selectionPaths?.all { (it?.lastPathComponent as? NxModuleDescriptionTreeNode)?.isLeaf == true }
                    ?: false
                ) {
                    action()
                    return true
                }
                return false
            }
        }.installOn(tree)
    }

    private fun getAllModulesUnder(node: NxModuleDescriptionTreeNode): List<NxModuleDescription> {
        val modules = ArrayList<NxModuleDescription>()
        TreeUtil.traverse(
            node,
            { node ->
                if (node is NxModuleDescriptionNode) {
                    modules.add(node.moduleDescription)
                }
                return@traverse true
            }
        )
        return modules
    }

    fun fillTree(modules: Collection<NxModuleDescription>) {
        removeAllNodes()
        // helper.createModuleNodes(modules, root, model)
        modules.forEach {
            createModuleNode(it)
        }
        expandRoot()
    }

    private fun createModuleNode(it: NxModuleDescription): NxModuleDescriptionNode {
        val nxModuleDescriptionNode = NxModuleDescriptionNode(it)
        TreeUtil.insertNode(nxModuleDescriptionNode, root, model, nodeComparator)
        return nxModuleDescriptionNode
    }

    private fun expandRoot() {
        tree.expandPath(TreePath(root))
    }

    fun addModules(modules: Collection<NxModuleDescription>): List<NxModuleDescriptionTreeNode> {
        return modules.map { createModuleNode(it) }
    }

    fun removeModules(modules: Collection<NxModuleDescription>) {
        val names = modules.mapTo(HashSet<String>()) { it.name }
        val toRemove = findNodes { it.moduleDescription.name in names }
        for (node in toRemove) {
            // helper.removeNode(node, root, model)
            removeNode(node, root, model)
        }
        expandRoot()
    }

    private fun removeNode(node: NxModuleDescriptionNode, root: RootNode, model: DefaultTreeModel) {
        model.removeNodeFromParent(node)
    }

    private fun findNodes(condition: (NxModuleDescriptionNode) -> Boolean): List<NxModuleDescriptionNode> {
        val result = ArrayList<NxModuleDescriptionNode>()
        TreeUtil.traverse(
            root,
            { node ->
                if (node is NxModuleDescriptionNode && condition(node)) {
                    result.add(node)
                }
                return@traverse true
            }
        )
        return result
    }

    fun removeAllNodes() {
        removeAllNodes(root, model)
    }

    fun removeAllNodes(root: DefaultMutableTreeNode, model: DefaultTreeModel) {
        root.removeAllChildren()
        model.nodeStructureChanged(root)
    }

    fun selectNodes(moduleNames: Set<String>) {
        val toSelect = findNodes { it.moduleDescription.name in moduleNames }
        val paths = toSelect.map { TreeUtil.getPath(root, it) }
        paths.forEach { tree.expandPath(it) }
        tree.selectionModel.selectionPaths = paths.toTypedArray()
        if (paths.isNotEmpty()) {
            TreeUtil.showRowCentered(tree, tree.getRowForPath(paths.first()), false, true)
        }
    }
}

private class ModuleDescriptionTreeRenderer : ColoredTreeCellRenderer() {
    override fun customizeCellRenderer(
        tree: JTree,
        value: Any?,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ) {
        if (value is NxModuleDescriptionTreeNode) {
            icon = value.icon
            append(value.text)
        }
    }
}

/*private fun createModuleDescriptionHelper(project: Project): ModuleGroupingTreeHelper<NxModuleDescription, NxModuleDescriptionTreeNode> {
    val moduleGrouper = ModuleGrouper.instanceFor(project)
    return ModuleGroupingTreeHelper.forEmptyTree(true, NxModuleDescriptionGrouping(moduleGrouper),
        ::NxModuleGroupNode, {NxModuleDescriptionNode(it, moduleGrouper)}, nodeComparator)
}*/

/*private class NxModuleDescriptionGrouping(private val moduleGrouper: ModuleGrouper) : ModuleGroupingImplementation<NxModuleDescription> {
    override val compactGroupNodes: Boolean
        get() = moduleGrouper.compactGroupNodes

    override fun getGroupPath(m: NxModuleDescription): List<String> {
        return moduleGrouper.getGroupPath(m)
    }

    override fun getModuleAsGroupPath(m: NxModuleDescription): List<String>? {
        return moduleGrouper.getModuleAsGroupPath(m)
    }
}*/

private val nodeComparator = compareBy(NaturalComparator.INSTANCE) { node: NxModuleDescriptionTreeNode -> node.text }

private interface NxModuleDescriptionTreeNode : MutableTreeNode {
    val text: String
    val icon: Icon
    // val group: ModuleGroup?
}

private class NxModuleDescriptionNode(val moduleDescription: NxModuleDescription/*, val moduleGrouper: ModuleGrouper*/) :
    DefaultMutableTreeNode(), NxModuleDescriptionTreeNode {
    override val text: String
        get() = moduleDescription.name

    override val icon: Icon
        get() = AllIcons.Nodes.Module
/*
    override val group: ModuleGroup?
        get() = moduleGrouper.getModuleAsGroupPath(moduleDescription)?.let {ModuleGroup(it)}*/
}

/*private class NxModuleGroupNode(override val group: ModuleGroup) : DefaultMutableTreeNode(), NxModuleDescriptionTreeNode {
    override val text: String
        get() {
            val parentGroupPath = (parent as? NxModuleDescriptionTreeNode)?.group?.groupPathList
            if (parentGroupPath != null && ContainerUtil.startsWith(group.groupPathList, parentGroupPath)) {
                return group.groupPathList.drop(parentGroupPath.size).joinToString(".")
            }
            return group.groupPathList.last()
        }

    override val icon: Icon
        get() = AllIcons.Nodes.ModuleGroup
}*/

private class RootNode : DefaultMutableTreeNode(), NxModuleDescriptionTreeNode {
    override val text: String
        get() = "<root>"

    override val icon: Icon
        get() = AllIcons.Nodes.ModuleGroup

    /*override val group: ModuleGroup?
        get() = null*/
}
