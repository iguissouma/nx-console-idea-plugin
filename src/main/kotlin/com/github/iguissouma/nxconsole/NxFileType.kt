package com.github.iguissouma.nxconsole

import com.intellij.json.JsonFileType
import com.intellij.json.JsonLanguage
import javax.swing.Icon

class NxFileType : JsonFileType(JsonLanguage.INSTANCE, true) {

    companion object {
        val INSTANCE = NxFileType()
    }

    override fun getName(): String = "nx"

    override fun getIcon(): Icon = NxIcons.NRWL_ICON

    override fun getDescription(): String = "Nx config"

    override fun getDefaultExtension(): String = "json"
}
