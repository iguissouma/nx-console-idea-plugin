package com.github.iguissouma.nxconsole.plugins

import com.intellij.openapi.diagnostic.Logger
import java.awt.Component

class NxValidationInfo(val component: Component?, errorHtmlDescriptionTemplate: String, linkText: String) {

    private val myErrorHtmlDescription: String?
    val linkText: String?

    fun getErrorHtmlDescription(): String {
        val var10000 = myErrorHtmlDescription
        return var10000!!
    }

    companion object {
        const val LINK_TEMPLATE = "{{LINK}}"
        private val LOG = Logger.getInstance(
            NxValidationInfo::class.java
        )
    }

    init {
        if (!errorHtmlDescriptionTemplate.contains("{{LINK}}")) {
            LOG.warn("Cannot find {{LINK}} in $errorHtmlDescriptionTemplate")
        }
        val linkHtml =
            """<a href="$linkText"></a>"""
        myErrorHtmlDescription = errorHtmlDescriptionTemplate.replace("{{LINK}}", linkHtml)
        this.linkText = linkText
    }
}
