package com.github.iguissouma.nxconsole.actions

import com.github.iguissouma.nxconsole.NxIcons
import com.github.iguissouma.nxconsole.cli.config.NxConfigProvider
import com.github.iguissouma.nxconsole.util.NxExecutionUtil
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.intellij.ide.projectView.actions.MarkRootActionBase
import com.intellij.ide.projectView.impl.ProjectRootsUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.util.graph.CachingSemiGraph
import com.intellij.util.graph.Graph
import com.intellij.util.graph.GraphAlgorithms
import com.intellij.util.graph.GraphGenerator
import com.intellij.util.graph.InboundSemiGraph

class NxFocusOnAppOrLibAction : DumbAwareAction({ "Nx Focus on App or Lib" }, NxIcons.NRWL_ICON) {

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
    }

    private fun isEnabled(e: AnActionEvent): Boolean {
        val project = e.project ?: return false
        val file = e.getData(LangDataKeys.VIRTUAL_FILE) ?: return false
        return NxConfigProvider.getNxConfig(project, file) != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val nxConfig = NxConfigProvider.getNxConfig(project, project.baseDir) ?: return
        val selectedProjectName = getSelectedProjectName(e) ?: return

        NxExecutionUtil(project).executeAndGetOutputAsync("print-affected", arrayOf()) {
            val result = it?.stdout ?: return@executeAndGetOutputAsync
            if (it.exitCode == 0 && result.isNotEmpty()) {
                invokeLater {
                    val depGraphType = object : TypeToken<Map<String, Any>>() {}.type
                    val depGraph: Map<String, Any> = Gson().fromJson(result, depGraphType)
                    val projectGraph = depGraph["projectGraph"] as Map<*, *>
                    val map = projectGraph["dependencies"] as Map<*, *>
                    val moduleDescriptions: Map<String, NxModuleDescription> =
                        nxConfig.projects.associateBy({ it.name }, { NxModuleDescription(it.name) }) ?: emptyMap()

                    nxConfig.projects.forEach {
                        val nxModuleDescription = moduleDescriptions[it.name]
                        (map[it.name] as List<Map<*, *>>).forEach { x ->
                            nxModuleDescription?.dependencyModuleNames?.add(x["target"] as String)
                        }
                    }
                    // val modulesToLoad: List<String> = mutableListOf(selectedProjectName).plus(moduleDescriptions[selectedProjectName]?.dependencyModuleNames ?: emptyList())
                    fun buildGraph(): Graph<NxModuleDescription> {
                        return GraphGenerator.generate(
                            CachingSemiGraph.cache(
                                object : InboundSemiGraph<NxModuleDescription> {
                                    override fun getNodes(): Collection<NxModuleDescription> {
                                        return moduleDescriptions.values
                                    }

                                    override fun getIn(node: NxModuleDescription): Iterator<NxModuleDescription> {
                                        return node.dependencyModuleNames.asIterable()
                                            .mapNotNull { moduleDescriptions[it] }.iterator()
                                    }
                                }
                            )
                        )
                    }

                    val graph = buildGraph()
                    val invertEdgeDirections = GraphAlgorithms.getInstance().invertEdgeDirections(graph)
                    val result = LinkedHashSet<NxModuleDescription>()
                    GraphAlgorithms.getInstance().collectOutsRecursively(
                        invertEdgeDirections,
                        moduleDescriptions.get(selectedProjectName),
                        result
                    )

                    val modulesToLoad = result.map { it.name }.toList()
                    ModuleManager.getInstance(project).modules.firstOrNull()?.run {
                        ModuleRootModificationUtil.updateModel(this) { model ->
                            runReadAction {
                                // for loaded modules unload not deps of selected project
                                nxConfig.projects.filter { nxProject ->
                                    nxProject.name !in modulesToLoad
                                }
                                    .forEach {
                                        it.rootDir?.run {
                                            MarkRootActionBase.findContentEntry(model, this)?.addExcludeFolder(this)
                                        }
                                    }

                                // for unloaded modules load deps of selected project
                                nxConfig.projects.filter { nxProject ->
                                    nxProject.name in modulesToLoad
                                }
                                    .forEach {
                                        val findExcludeFolder = ProjectRootsUtil.findExcludeFolder(this, it.rootDir!!)
                                        if (findExcludeFolder != null) {
                                            it.rootDir?.run {
                                                MarkRootActionBase.findContentEntry(model, this)
                                                    ?.removeExcludeFolder(findExcludeFolder)
                                            }
                                        }
                                    }
                            }
                        }
                    }
                }
            }
        }

    }

    private fun getSelectedProjectName(e: AnActionEvent): String? {
        val project = e.project ?: return null
        val file = e.getData(LangDataKeys.VIRTUAL_FILE) ?: return null
        val nxProject = NxConfigProvider.getNxProject(project, file)
        if (nxProject != null) {
            return nxProject.name
        }
        return NxConfigProvider.getNxConfig(project, file)?.projects?.firstOrNull { it.rootDir == file }?.name
    }
}
