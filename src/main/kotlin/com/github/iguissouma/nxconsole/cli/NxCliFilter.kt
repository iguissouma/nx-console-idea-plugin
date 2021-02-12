package com.github.iguissouma.nxconsole.cli

import com.intellij.execution.filters.AbstractFileHyperlinkFilter
import com.intellij.execution.filters.FileHyperlinkRawData
import com.github.iguissouma.nxconsole.cli.NxCliFilter
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.annotations.NonNls

class NxCliFilter(project: Project?, baseDir: String?) : AbstractFileHyperlinkFilter(
    project!!, baseDir
), DumbAware {
    override fun parse(line: String): List<FileHyperlinkRawData> {
        val create = parse(line, CREATE)
        return if (!create.isEmpty()) create else parse(line, UPDATE)
    }

    fun parse(line: String, prefix: String): List<FileHyperlinkRawData> {
        val index = StringUtil.indexOfIgnoreCase(line, prefix, 0)
        if (index >= 0) {
            val start = index + prefix.length
            var end = line.indexOf(" (", start)
            if (end == -1) end = line.length
            val fileName = line.substring(start, end).trim { it <= ' ' }
            return listOf(FileHyperlinkRawData(fileName, -1, -1, start, start + fileName.length))
        }
        return emptyList()
    }

    override fun supportVfsRefresh(): Boolean {
        return true
    }

    companion object {
        @NonNls
        private val CREATE = "create "

        @NonNls
        private val UPDATE = "update "
    }
}
