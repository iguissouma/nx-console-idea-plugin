package com.github.iguissouma.nxconsole.actions

import com.github.iguissouma.nxconsole.NxIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup

class NxNewMenuGenerateAction : AnAction({ "Nx Generate (Ui)..." }, NxIcons.NRWL_ICON) {

    override fun actionPerformed(event: AnActionEvent) {
        val popup = JBPopupFactory.getInstance().createActionGroupPopup(
            "Nx Generate (Ui)",
            ActionManager.getInstance().getAction("Nx.NxNewGenerateActionGroup") as ActionGroup,
            event.dataContext,
            JBPopupFactory.ActionSelectionAid.MNEMONICS,
            true,
            null,
            10,
            null,
            ActionPlaces.ACTION_PLACE_QUICK_LIST_POPUP_ACTION
        )
        showPopup(event, popup)

    }

    private fun showPopup(e: AnActionEvent, popup: ListPopup) {
        val project = e.project
        if (project != null) {
            popup.showCenteredInCurrentWindow(project)
        } else {
            popup.showInBestPositionFor(e.dataContext)
        }
    }
}
