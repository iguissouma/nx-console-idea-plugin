package com.github.iguissouma.nxconsole.plugins

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.text.HtmlChunk
import org.jetbrains.annotations.Nls
import java.awt.Component

class NxValidationInfo(val component: Component?, errorHtmlDescriptionTemplate: @Nls String, linkText: @Nls String) {

    private val myErrorHtmlDescription: @Nls String?
    val linkText: @Nls String?

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
        val linkHtml = HtmlChunk.link(linkText, linkText).toString()
        myErrorHtmlDescription = errorHtmlDescriptionTemplate.replace("{{LINK}}", linkHtml)
        this.linkText = linkText
    }
}
