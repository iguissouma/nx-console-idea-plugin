package com.github.iguissouma.nxconsole.graph

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "NxGraphConfiguration", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class NxGraphConfiguration : PersistentStateComponent<NxGraphConfiguration> {

    var NX_SHOW_AFFECTED = false

    override fun getState(): NxGraphConfiguration = this

    override fun loadState(state: NxGraphConfiguration) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(project: Project): NxGraphConfiguration {
            return project.getService(NxGraphConfiguration::class.java)
        }
    }
}
