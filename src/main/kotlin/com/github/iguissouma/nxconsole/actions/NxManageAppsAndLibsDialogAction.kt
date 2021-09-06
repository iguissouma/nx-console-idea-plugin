package com.github.iguissouma.nxconsole.actions

import com.github.iguissouma.nxconsole.NxBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.util.ui.UIUtil
import org.jetbrains.annotations.NotNull
import java.awt.BorderLayout
import javax.swing.JList
import javax.swing.JPanel

class NxManageAppsAndLibsDialogAction : DumbAwareAction("Nx Manage Apps & Libs") {

    override fun actionPerformed(e: AnActionEvent) {
        val disposable = Disposer.newDisposable()
        val project = e.getRequiredData(CommonDataKeys.PROJECT)
        val panel = object : NxProjectsPanel(disposable) {

        }
        val list = UIUtil.findComponentOfType(
            panel,
            JList::class.java
        ) as JList<*>
        val popup = JBPopupFactory.getInstance().createComponentPopupBuilder(panel, list)
            .setTitle(NxBundle.message("popup.title.nx.focus.projects")).setFocusable(true)
            .setRequestFocus(true).setMayBeParent(true).setMovable(true).setResizable(true).setNormalWindowLevel(true)
            .createPopup()
        Disposer.register(popup, disposable)
        popup.showCenteredInCurrentWindow(project)
    }
}

open class NxProjectsPanel(disposable: @NotNull Disposable) : JPanel(BorderLayout()) {

}
