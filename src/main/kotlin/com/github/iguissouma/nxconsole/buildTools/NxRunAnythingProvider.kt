package com.github.iguissouma.nxconsole.buildTools

import com.intellij.lang.javascript.buildTools.base.JsbtApplicationService
import com.intellij.lang.javascript.buildTools.base.JsbtTaskRunAction
import com.intellij.lang.javascript.buildTools.runAnything.JsbtRunAnythingProvider
import com.intellij.openapi.util.text.StringUtil

class NxRunAnythingProvider : JsbtRunAnythingProvider() {
    override fun getCommand(value: JsbtTaskRunAction): String {
        return "nx run " + StringUtil.join(value.taskSet.taskNames, " ")
    }

    override fun getCompletionGroupTitle(): String? {
        return "Nx"
    }

    override fun getService(): JsbtApplicationService {
        return NxService.APPLICATION_SERVICE
    }

    override fun getCommandPrefix(): String {
        return "nx"
    }

    override fun getHelpCommandPlaceholder(): String? {
        return "nx <task name>"
    }
}
