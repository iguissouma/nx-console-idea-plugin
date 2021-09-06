package com.github.iguissouma.nxconsole.actions

import com.github.iguissouma.nxconsole.NxIcons
import com.github.iguissouma.nxconsole.buildTools.NxJsonUtil
import com.github.iguissouma.nxconsole.buildTools.NxService
import com.github.iguissouma.nxconsole.buildTools.NxTaskTreeView
import com.github.iguissouma.nxconsole.graph.grabCommandOutput
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.intellij.CommonBundle
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.icons.AllIcons
import com.intellij.javascript.nodejs.CompletionModuleInfo
import com.intellij.javascript.nodejs.NodeModuleSearchUtil
import com.intellij.javascript.nodejs.interpreter.NodeCommandLineConfigurator
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.IconButton
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Condition
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.ScrollPaneFactory
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.tree.TreeUtil
import com.intellij.vcsUtil.VcsFileUtil
import java.awt.Component
import java.io.File
import javax.swing.BorderFactory
import javax.swing.tree.TreeModel

class NxShowAffectedAction : AnAction(NxIcons.NRWL_ICON) {

    override fun actionPerformed(event: AnActionEvent) {
        val project: Project = event.project ?: return
        if (event.getData<Array<Change>>(VcsDataKeys.CHANGES) != null) {
            showDiscoveredAffectedByChanges(event)
            return
        }

        val virtualFiles: List<VirtualFile> = findFilesInContext(event)
        if (virtualFiles.isNotEmpty()) {
            showDiscoveredAffectedByFiles(event)
            return
        }
    }

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible =
            findFilesInContext(event).isNotEmpty() || event.getData(VcsDataKeys.CHANGES) != null
    }

    private fun findFilesInContext(event: AnActionEvent): List<VirtualFile> {
        var virtualFiles = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        if (virtualFiles == null || virtualFiles.isEmpty()) {
            val file = event.getData(CommonDataKeys.PSI_FILE)
            if (file != null) {
                virtualFiles = arrayOf(file.virtualFile)
            }
        }
        return if (virtualFiles == null) emptyList() else ContainerUtil.filter(
            virtualFiles,
            Condition { v: VirtualFile -> v.isInLocalFileSystem }
        )
    }

    private fun createTitle(files: List<VirtualFile?>): String {
        if (files.isEmpty()) return "Empty Selection"
        val firstName = files[0]!!.name
        if (files.size == 1) return firstName
        return if (files.size == 2) firstName + " and " + files[1]!!.name else "$firstName et al."
    }

    private fun showDiscoveredAffectedByChanges(e: AnActionEvent) {
        val changes = e.getRequiredData(VcsDataKeys.CHANGES).toList()
        val project = e.project!!
        val relativeAffectedPaths = getRelativeAffectedPaths(project, changes)
        showDiscoveredAffected(e, project, relativeAffectedPaths)
    }

    private fun showDiscoveredAffectedByFiles(e: AnActionEvent) {
        val project = e.project!!
        val projectBasePath: VirtualFile = getBasePathAsVirtualFile(project) ?: return
        val files = findFilesInContext(e)
        val paths = files
            .flatMap { f: VirtualFile? -> VfsUtil.collectChildrenRecursively(f!!) }
            .mapNotNull { f: VirtualFile? -> VcsFileUtil.getRelativeFilePath(f, projectBasePath) }
            // .map { p: String -> "/$p" }
            .toList()
        showDiscoveredAffected(e, project, paths)
    }

    private fun showDiscoveredAffected(e: AnActionEvent, project: Project, relativeAffectePaths: List<String>) {
        // val myModel = CollectionListModel<String>()
        // val list = JBList<String>(myModel)
        val nxTaskTreeView = NxTaskTreeView(NxService.getInstance(project), project, null)
        // nxTaskTreeView.init()
        val tree = nxTaskTreeView.component

        val scrollPane = ScrollPaneFactory.createScrollPane(tree)
        scrollPane.border = BorderFactory.createEmptyBorder(0, 0, 0, 0)

        val builder = JBPopupFactory.getInstance().createComponentPopupBuilder(scrollPane, tree)
            .setTitle("Nx Show Affected")
            .setMovable(true)
            .setResizable(true)
            // .setCommandButton(CompositeActiveComponent(pinButton))
            // .setSettingButton(CompositeActiveComponent(runButton).component)
            // .setItemChoosenCallback(Runnable {  })
            /*.registerKeyboardAction(
                 findUsageKeyStroke,
                 ActionListener { __: ActionEvent? -> pinActionListener.run() })*/
            .setMinSize(JBDimension(500, 300))
            // .setDimensionServiceKey(NxShowAffectedAction::class.java.getSimpleName())
            .setMayBeParent(true)
            .setRequestFocus(true)
            .setFocusable(true)
            .setFocusOwners(arrayOf<Component>(tree))
            .setLocateWithinScreenBounds(true)
            .setCancelOnOtherWindowOpen(true)
            .setMovable(true)
            .setResizable(true)
            // .setSettingButtons(toolbarComponent)
            .setCancelOnWindowDeactivation(false)
            .setCancelOnClickOutside(true)
            // .setDimensionServiceKey(project, NxShowAffectedAction::class.java.simpleName, true)
            // .setMinSize(Dimension(JBUI.scale(350), JBUI.scale(300)))
            .setCancelButton(
                IconButton(
                    CommonBundle.message("action.text.close"),
                    AllIcons.Actions.Close,
                    AllIcons.Actions.CloseHovered
                )
            )

        val popup: JBPopup = builder.createPopup()

        val nxJsonFile = NxJsonUtil.findChildNxJsonFile(project.baseDir) ?: return
        ApplicationManager.getApplication().invokeLater {
            val nodeJsInterpreter = NodeJsInterpreterManager.getInstance(project).interpreter
            if (nodeJsInterpreter != null) {
                val configurator: NodeCommandLineConfigurator
                try {
                    configurator = NodeCommandLineConfigurator.find(nodeJsInterpreter)
                    val modules: MutableList<CompletionModuleInfo> = mutableListOf()
                    NodeModuleSearchUtil.findModulesWithName(
                        modules,
                        "@nrwl/cli",
                        nxJsonFile,
                        null
                    )
                    val module = modules.firstOrNull()
                    if (module != null) {
                        val moduleExe =
                            "${module.virtualFile!!.path}${File.separator}bin${File.separator}nx"
                        // TODO check if json can be out of monorepo
                        // val createTempFile = createTempFile("tmp", ".json", File(nxJsonFile.parent!!.virtualFile.path))
                        val commandLine =
                            GeneralCommandLine(
                                "",
                                moduleExe,
                                "print-affected",
                                "--files=${
                                relativeAffectePaths.joinToString(",")
                                }"
                            )
                        configurator.configure(commandLine)
                        val grabCommandOutput = grabCommandOutput(
                            project,
                            commandLine,
                            nxJsonFile.parent.path
                        )
                        val model: TreeModel = tree.getModel()
                        // Disposer.register(popup, tree)

                        val mapType = object : TypeToken<Map<String, Any>>() {}.type
                        val affected: Map<String, Any> = Gson().fromJson(grabCommandOutput, mapType)

                        nxTaskTreeView.isAffected = true
                        nxTaskTreeView.filterByAffected = affected["projects"] as? List<String> ?: emptyList()
                        nxTaskTreeView.init()

                        popup.showInBestPositionFor(e.dataContext)
                        TreeUtil.expandAll(tree)
                    }
                } finally {
                    // nothing
                }
            }
        }
    }

    fun getRelativeAffectedPaths(project: Project, changes: Collection<Change>): List<String> {
        val baseDir = getBasePathAsVirtualFile(project)
        return if (baseDir == null) emptyList<String>() else changes
            .mapNotNull { change: Change -> relativePath(baseDir, change) }
            // .map { s: String -> "/$s" }
            .toList()
    }

    private fun getBasePathAsVirtualFile(project: Project): VirtualFile? {
        val basePath = project.basePath
        return if (basePath == null) null else LocalFileSystem.getInstance().findFileByPath(basePath)
    }

    private fun relativePath(baseDir: VirtualFile, change: Change): String? {
        val file = change.virtualFile
        if (file == null) {
            val before = change.beforeRevision
            if (before != null) {
                return VcsFileUtil.relativePath(baseDir, before.file)
            }
        }
        return if (file == null) null else VfsUtilCore.getRelativePath(file, baseDir)
    }
}
