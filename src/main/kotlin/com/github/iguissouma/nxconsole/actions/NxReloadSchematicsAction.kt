package com.github.iguissouma.nxconsole.actions

import com.github.iguissouma.nxconsole.NxIcons
import com.github.iguissouma.nxconsole.schematics.NxCliSchematicsRegistryService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class NxReloadSchematicsAction : AnAction(NxIcons.NRWL_ICON) {

    override fun actionPerformed(event: AnActionEvent) {
        NxCliSchematicsRegistryService.getInstance().clearProjectSchematicsCache()
    }
}
