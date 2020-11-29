package com.github.iguissouma.nxconsole.schematics

class NxSchematicCollection {
    var name: String? = null
    var schematics: MutableList<NxSchematic> = ArrayList()
}

class NxSchematic {
    var collection: String? = null
    var name: String? = null
    var description: String? = null
    var options: MutableList<Option> = ArrayList()
}

fun List<NxSchematicCollection>.toSchematic() = this.flatMap { schematicCollection ->
    schematicCollection.schematics.map { schematic ->
        Schematic(
            name = "${schematic.collection}:${schematic.name}",
            description = schematic.description,
            options = schematic.options,
            arguments = ArrayList()
        )
    }
}
