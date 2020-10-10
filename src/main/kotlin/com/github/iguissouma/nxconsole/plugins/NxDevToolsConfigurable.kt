package com.github.iguissouma.nxconsole.plugins

import com.github.iguissouma.nxconsole.NxBundle
import com.intellij.openapi.options.Configurable.NoScroll
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class NxDevToolsConfigurable(val myProject: Project) :
    SearchableConfigurable, NoScroll {

    private var myView: NxDevToolsView? = null

    companion object {
        const val ID = "nx.dev.tools.settings"
    }

    override fun createComponent(): JComponent? {
        val view = getView()
        return view.myComponent
    }

    override fun isModified(): Boolean {
        val view: NxDevToolsView = this.getView()
        val viewSettings = view.getSettings()
        val storedSettings = NxDevToolsSettingsManager.getInstance(this.myProject).mySettings
        return viewSettings != storedSettings
    }

    override fun reset() {
        val settings = NxDevToolsSettingsManager.getInstance(myProject).mySettings
        val view: NxDevToolsView = getView()
        view.setSettings(settings!!)
    }

    private fun getView(): NxDevToolsView {
        if (myView == null) {
            myView = NxDevToolsView(this.myProject)
        }
        return myView!!
    }

    override fun apply() {
        val view: NxDevToolsView = getView()
        val settings = view.getSettings()
        NxDevToolsSettingsManager.getInstance(myProject).mySettings = settings
    }

    override fun getDisplayName(): String {
        return NxBundle.message("settings.nx.dev.tools.configurable.name")
    }

    override fun getId(): String {
        return ID
    }
}
