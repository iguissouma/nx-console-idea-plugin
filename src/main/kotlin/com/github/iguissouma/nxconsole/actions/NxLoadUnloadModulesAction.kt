package com.github.iguissouma.nxconsole.actions
import com.github.iguissouma.nxconsole.NxIcons
import com.github.iguissouma.nxconsole.cli.config.NxConfigProvider
import com.intellij.ide.actions.OpenModuleSettingsAction
import com.intellij.ide.projectView.impl.ProjectRootsUtil
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.roots.ui.configuration.ConfigureUnloadedModulesDialog

class NxLoadUnloadModulesAction : DumbAwareAction({ "Nx Load/Unload App or Libs..." }, NxIcons.NRWL_ICON) {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = isEnabled(e)
  }

  private fun isEnabled(e: AnActionEvent): Boolean {
    val project = e.project ?: return false
    val moduleManager = ModuleManager.getInstance(project)
    //if (moduleManager.modules.size <= 1 && moduleManager.unloadedModuleDescriptions.isEmpty()) return false

    val file = e.getData(LangDataKeys.VIRTUAL_FILE) ?: return false
    /*return !ActionPlaces.isPopupPlace(e.place) || OpenModuleSettingsAction.isModuleInContext(e)
           || file != null && ProjectRootsUtil.findUnloadedModuleByContentRoot(file, project) != null*/
    return NxConfigProvider.getNxConfig(project, file) != null
  }

  override fun actionPerformed(e: AnActionEvent) {
    val selectedModuleName = e.getData(LangDataKeys.MODULE_CONTEXT)?.name ?: getSelectedUnloadedModuleName(e)
                             ?: e.getData(LangDataKeys.MODULE)?.name
    NxConfigureUnloadedModulesDialog(e.project!!, selectedModuleName).show()
  }

  private fun getSelectedUnloadedModuleName(e: AnActionEvent): String? {
    val project = e.project ?: return null
    val file = e.getData(LangDataKeys.VIRTUAL_FILE) ?: return null
    return ProjectRootsUtil.findUnloadedModuleByFile(file, project)
  }
}
