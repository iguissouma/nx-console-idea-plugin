package com.github.iguissouma.nxconsole.plugins

import com.github.iguissouma.nxconsole.NxIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAware

class NxShowNxDevToolsSettingsAction : AnAction(NxIcons.NRWL_ICON), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ShowSettingsUtil.getInstance().editConfigurable(
            e.getData(PlatformDataKeys.CONTEXT_COMPONENT),
            NxDevToolsConfigurable(project)
        )
    }
}
