package com.github.iguissouma.nxconsole.plugins

import com.github.iguissouma.nxconsole.NxBundle
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.openapi.project.Project
import com.intellij.ui.IdeBorderFactory
import com.intellij.util.ui.JBUI
import com.intellij.webcore.packaging.PackagesNotificationPanel
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class NxDevToolsPackagesView(val myProject: Project) {

    var myPackagesPanel: NxDevToolsInstalledPackagesPanel? = null
    var myPackagesNotificationPanel: PackagesNotificationPanel? = null
    var myComponent: JPanel? = null
    var myCurrentSettings: NxDevToolsSettings? = null

    init{
        val panel = JPanel(BorderLayout(0, 0))
        panel.border =
            IdeBorderFactory.createTitledBorder(
                NxBundle.message(
                    "nx.packages.view.plugins"
                ), false, JBUI.insetsTop(8)
            ).setShowLine(false)
        myPackagesNotificationPanel = PackagesNotificationPanel()
        myPackagesPanel = NxDevToolsInstalledPackagesPanel(myProject, myPackagesNotificationPanel!!)
        panel.add(myPackagesPanel, "Center")
        panel.add(myPackagesNotificationPanel?.component, "South")
        myComponent = panel

    }

    fun onSettingsChanged(settings: NxDevToolsSettings, errors: List<NxValidationInfo>) {
        if (settings != myCurrentSettings) {
            myCurrentSettings = settings
            var service: NxDevToolsPackagingService? = null
            myPackagesNotificationPanel!!.hide()
            myPackagesNotificationPanel!!.removeAllLinkHandlers()
            if (errors.isEmpty()) {
                service = NxDevToolsPackagingService(this.myProject, settings,NodeJsInterpreterManager.getInstance(myProject).interpreter!!)
                //this.checkVersion(settings)
            } else {
                //this.showErrors(errors)
            }
            myPackagesPanel!!.updatePackages(service)
        }
    }


    fun getComponent(): JComponent {
        return myComponent!!
    }

}
