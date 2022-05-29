package com.github.iguissouma.nxconsole.actions
import com.github.iguissouma.nxconsole.NxIcons
import com.github.iguissouma.nxconsole.cli.config.NxConfigProvider
import com.github.iguissouma.nxconsole.uml.extractJson
import com.github.iguissouma.nxconsole.util.NxExecutionUtil
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.util.ui.UIUtil

class NxLoadUnloadModulesAction : DumbAwareAction({ "Nx Load/Unload App or Libs..." }, NxIcons.NRWL_ICON) {

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
        NxExecutionUtil(project).executeAndGetOutputAsync("print-affected", arrayOf()) {
            val result = it?.stdout?.extractJson() ?: return@executeAndGetOutputAsync
            if (it.exitCode == 0 && result.isNotEmpty()) {
                val depGraphType = object : TypeToken<Map<String, Any>>() {}.type
                val depGraph: Map<String, Any> = Gson().fromJson(result, depGraphType)
                val projectGraph = depGraph["projectGraph"] as Map<*, *>
                invokeLater {
                    NxConfigureUnloadedModulesDialog(project, projectGraph, getSelectedProjectName(e)).show()
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
