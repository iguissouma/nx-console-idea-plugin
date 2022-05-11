package com.github.iguissouma.nxconsole.schematics

import com.github.iguissouma.nxconsole.readers.CollectionInfo
import com.github.iguissouma.nxconsole.readers.getGeneratorOptions
import com.github.iguissouma.nxconsole.readers.getGenerators
import com.intellij.openapi.project.Project

fun doLoadGenerators(project: Project): List<Schematic> {
    val generators = getGenerators(project.basePath!!, null)
    return generators.mapNotNull { collectionInfo: CollectionInfo ->
        val schematic = collectionInfo.data ?: return@mapNotNull null
        val (name, path, type, data) = collectionInfo
        val options = getGeneratorOptions(project.basePath!!, data?.collection!!, data.name, path)
            .map { nxOption: com.github.iguissouma.nxconsole.readers.Option ->
                val option = Option(name = nxOption.name)
                option.default = nxOption.default ?: "" // TODO: check if this is correct
                option.description = nxOption.description
                option.type = nxOption.type
                option.isRequired = nxOption.isRequired
                option.isVisible = true // TODO: check if this is correct
                option.enum = nxOption.enum.toMutableList()
                option.positional = nxOption.positional
                // option._default = nxOption.`$default`
                option.tooltip = nxOption.tooltip
                option.itemTooltips = nxOption.itemTooltips as? Map<String, Any>?
                option
            }.toMutableList()

        Schematic(
            name = "${schematic.collection}:${schematic.name}",
            description = schematic.description,
            options = options,
            arguments = mutableListOf()
        )
    }

}
