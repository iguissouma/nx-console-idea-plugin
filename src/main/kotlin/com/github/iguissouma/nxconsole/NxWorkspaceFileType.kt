package com.github.iguissouma.nxconsole

import com.intellij.icons.AllIcons
import com.intellij.json.JsonFileType
import com.intellij.json.JsonLanguage
import javax.swing.Icon

class NxWorkspaceFileType : JsonFileType(JsonLanguage.INSTANCE, true) {

    companion object {
        val INSTANCE = NxWorkspaceFileType()
    }

    override fun getName(): String = "nx workspace"

    override fun getDisplayName(): String = "Nx Workspace"

    override fun getIcon(): Icon = AllIcons.FileTypes.Json

    override fun getDescription(): String = "Nx Workspace config"

    override fun getDefaultExtension(): String = "json"
}
