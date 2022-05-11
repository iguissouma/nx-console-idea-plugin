package com.github.iguissouma.nxconsole.builders

import com.github.iguissouma.nxconsole.readers.CollectionInfo
import com.github.iguissouma.nxconsole.readers.getExecutors
import com.github.iguissouma.nxconsole.readers.getGeneratorOptions
import com.intellij.openapi.project.Project

fun doLoadBuilders(project: Project, builderName: String): List<NxBuilderOptions> {
    val executor: CollectionInfo = getExecutors(project.basePath!!, null, false)
        .firstOrNull { "${it.data?.collection}:${it.data?.name}" == builderName } ?: return emptyList()

    val schematic = executor.data ?: return emptyList()
    val (name, path, type, data) = executor
    val options = getGeneratorOptions(project.basePath!!, data?.collection!!, data.name, path)
        .map { nxOption: com.github.iguissouma.nxconsole.readers.Option ->
            val option = NxBuilderOptions()
            option.default = nxOption.`$default` ?: "" // TODO: check if this is correct
            option.description = nxOption.description ?: ""
            option.type = nxOption.type ?: ""
            option.required = nxOption.isRequired
            // option.isVisible = true // TODO: check if this is correct
            option.enum = nxOption.enum.toMutableList()
            // option.positional = nxOption.positional
            // option._default = nxOption.`$default`
            // option.tooltip = nxOption.tooltip
            // option.itemTooltips = nxOption.itemTooltips as? Map<String, Any>?
            option
        }.toMutableList()

    return options
}

class NxBuilderOptions {
    var name: String = ""
    var description: String = ""
    var type: String = ""
    var required: Boolean = false
    var default: String = ""
    var enum: List<String> = emptyList()
    var aliases: List<String> = emptyList()
    var hidden: Boolean = false

    override fun toString(): String {
        return "NxBuilderOptions(name='$name', description='$description', type='$type', required=$required, aliases=$aliases, hidden=$hidden)"
    }
}
