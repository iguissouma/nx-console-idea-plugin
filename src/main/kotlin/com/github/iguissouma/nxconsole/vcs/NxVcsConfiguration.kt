package com.github.iguissouma.nxconsole.vcs

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "NxVcsManagerConfiguration", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class NxVcsConfiguration(val project: Project) : PersistentStateComponent<NxVcsConfiguration> {

    var NX_REFORMAT_BEFORE_PROJECT_COMMIT = false

    override fun getState(): NxVcsConfiguration {
        return NxVcsConfiguration.getInstance(project)
    }

    override fun loadState(state: NxVcsConfiguration) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(project: Project): NxVcsConfiguration {
            return project.getService(NxVcsConfiguration::class.java)
        }
    }
}
