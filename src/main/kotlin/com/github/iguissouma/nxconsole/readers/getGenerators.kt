package com.github.iguissouma.nxconsole.readers

import com.github.iguissouma.nxconsole.cli.config.NxConfigProvider
import com.github.iguissouma.nxconsole.readers.WorkspaceGeneratorType.generators
import com.github.iguissouma.nxconsole.readers.WorkspaceGeneratorType.schematics
import com.google.common.base.CaseFormat
import com.intellij.javascript.nodejs.packageJson.NodeInstalledPackageFinder
import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path


fun getGenerators(
    workspacePath: String,
    projects: WorkspaceProjects
): List<CollectionInfo> {
    val baseDir = workspacePath
    val collections = readCollections(
        workspacePath, ReadCollectionsOptions(
            projects = projects,
            clearPackageJsonCache = false
        )
    )

    val generatorCollections = listOf(
        *collections.filter {
            it.type == CollectionType.generator
        }.toTypedArray(),
        *checkAndReadWorkspaceGenerators(baseDir, schematics).toTypedArray(),
        *checkAndReadWorkspaceGenerators(baseDir, generators).toTypedArray(),
    )
    return generatorCollections.filterNot { it.data == null }

}

fun getGeneratorOptions(
    workspacePath: String,
    collectionName: String,
    generatorName: String,
    generatorPath: String,
    workspaceType: WorkspaceType,
): List<Option> {
    val generatorSchema = readAndCacheJsonFile(generatorPath)
    val workspaceDefaults = readWorkspaceJsonDefaults(workspacePath)

    // TODO
    /*const defaults =
    workspaceDefaults &&
            workspaceDefaults[collectionName] &&
            workspaceDefaults[collectionName][generatorName];*/
    val defaults = if (workspaceDefaults?.isNotEmpty() == true) {
        (workspaceDefaults[collectionName] as? Map<String, Any>)?.get(generatorName) as? Map<String, Any>
            ?: emptyMap<String, Any>()
    } else
        emptyMap<String, Any>()

    return normalizeSchema(generatorSchema["json"] as Map<String, Any?>, workspaceType ,defaults)

}

data class GeneratorDefaults(
    val name: String,
)

val IMPORTANT_FIELD_NAMES = arrayOf(
    "name",
    "project",
    "module",
    "watch",
    "style",
    "directory",
    "port",
)
val IMPORTANT_FIELDS_SET = IMPORTANT_FIELD_NAMES.toSet()

enum class WorkspaceType { nx, ng }

fun normalizeSchema(
    s: Map<String, Any?>,
    workspaceType: WorkspaceType = WorkspaceType.nx,
    projectDefaults: Map<String, Any>? = null
): List<Option> {
    val hyphenate = workspaceType == WorkspaceType.ng && ngVersion() >= 14;
    val options = schemaToOptions(s, SchemaToOptionsConfig(hyphenate))
    val requiredFields = (s["required"] as? List<String> ?: emptyList()).distinct()

    val nxOptions = options.map { option: Option ->
        val xPrompt = option.`x-prompt`
        val workspaceDefault = projectDefaults?.get(option.name)
        val `$default` = option.`$default`
        var nxOption = option.copy(
            isRequired = isFieldRequired(requiredFields, option, xPrompt, `$default`),
            aliases = if (option.alias != null) listOf(option.alias) else emptyList(),

            //  ...($default && { $default }),
            `$default` = `$default`,

            // TODO
            //  ...(option.enum && { items: option.enum.map((item) => item.toString()) }),

            // Strongly suspect items does not belong in the Option schema.
            //  Angular Option doesn't have the items property outside of x-prompt,
            //  but items is used in @schematics/angular - guard
            items = getItems(option),
        )

        // TODO
        //  ...(workspaceDefault !== undefined && { default: workspaceDefault }),
        if (workspaceDefault != null) {
            nxOption = nxOption.copy(default = workspaceDefault.toString())
        }
        if (xPrompt != null) {
            nxOption = nxOption.copy()
        }

        nxOption
    }

    return nxOptions.sortedWith(object : Comparator<Option> {
        override fun compare(a: Option, b: Option): Int {
            if (a.positional != null && b.positional != null) {
                return a.positional.compareTo(b.positional)
            }

            if (a.positional != null) {
                return -1
            } else if (b.positional != null) {
                return 1
            } else if (a.isRequired) {
                if (b.isRequired) {
                    return a.name.compareTo(b.name)
                }
                return -1
            } else if (b.isRequired) {
                return 1
            } else if (IMPORTANT_FIELDS_SET.contains(a.name)) {
                if (IMPORTANT_FIELDS_SET.contains(b.name)) {
                    return (IMPORTANT_FIELD_NAMES.indexOf(a.name) - IMPORTANT_FIELD_NAMES.indexOf(b.name))
                }
                return -1
            } else if (IMPORTANT_FIELDS_SET.contains(b.name)) {
                return 1
            } else {
                return a.name.compareTo(b.name)
            }
        }
    })

}

fun ngVersion(): Int {
    val project = ProjectManager.getInstance().openProjects.firstOrNull() ?: return 0
    val nodeInstalledPackageFinder = NodeInstalledPackageFinder(project, project.baseDir)
    return nodeInstalledPackageFinder.findInstalledPackage("@angular/cli")?.version?.major ?: 0
}

fun getItems(option: Option): Any? {
    when (option.items) {
        is List<*> -> return option.items
        is Map<*, *> -> return option.items["enum"]
        else -> return emptyList<String>()
    }

}

fun isFieldRequired(requiredFields: List<String>, option: Option, xPrompt: Any?, `$default`: Any?): Boolean {
    // checks schema.json requiredFields and xPrompt for required
    return requiredFields.contains(option.name) ||
            // makes xPrompt fields required so nx command can run with --no-interactive
            // - except properties with a default (also falsey, empty, null)
            // - except properties with a $default $source
            // - except boolean properties (should also have default of `true`)
            (xPrompt != null && `$default` != null && option.type != "boolean")
}

data class SchemaToOptionsConfig(val hyphenate: Boolean = false)

fun schemaToOptions(schema: Map<String, Any?>, config: SchemaToOptionsConfig? = null): List<Option> {
    val properties = schema["properties"] as Map<String, Any?>?
    val res = mutableListOf<Option>()
    properties?.entries?.forEach {
        val option = it.key
        val currentProperty = it.value as? Map<String, Any?> ?: return@forEach
        val _default = currentProperty["\$default"] as? Map<String, Any?>?
        val _defaultIndex = if (_default?.get("\$source") == "argv") _default["index"] else null
        val positional: Int? = (_defaultIndex as? Double)?.toInt()
        val visible = isPropertyVisible(option, currentProperty)
        if (!visible) {
            return@forEach
        }
        val name = if (config?.hyphenate == true) CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, option)  else option;
        res.add(
            Option(
                name = name,
                type = currentProperty["type"] as? String?,
                description = currentProperty["description"] as? String?,
                enum = currentProperty["enum"] as? List<String>? ?: emptyList(),
                format = currentProperty["format"] as? String?,
                `x-prompt` = currentProperty["\$x-prompt"] as? String?,
                `$default` = currentProperty["\$default"] as? String?,
                default = currentProperty["default"] as? String?,

                positional = positional,
                alias = currentProperty["alias"] as? String,
                hidden = currentProperty["hidden"] as? Boolean ?: false,
                deprecated = currentProperty["deprecated"] as? Boolean ?: false,
                tooltip = currentProperty["description"] as? String,
                itemTooltips = currentProperty["itemTooltips"],
                items = currentProperty["items"],
                aliases = currentProperty["aliases"] as? List<String> ?: emptyList(),
                isRequired = currentProperty["required"] as? Boolean ?: false,
                `x-dropdown` = currentProperty["x-dropdown"] as? String,
            )
        )

    }
    return res
}

val ALWAYS_VISIBLE_OPTIONS = listOf("path")
fun isPropertyVisible(option: String, property: Map<String, Any?>): Boolean {
    if (ALWAYS_VISIBLE_OPTIONS.contains(option)) {
        return true
    }

    if (property.containsKey("hidden")) {
        return !(property["hidden"] as Boolean)
    }

    return property["visible"] as Boolean? ?: true
}


fun readWorkspaceJsonDefaults(workspacePath: String): Map<String, Any?>? {
    // get virtual file from path
    val virtualFile = LocalFileSystem.getInstance().findFileByPath(workspacePath) ?: return emptyMap<String, Any>()
    // guess project from virtual file
    val project = ProjectLocator.getInstance().guessProjectForFile(virtualFile) ?: return emptyMap<String, Any>()

    val nxConfig = NxConfigProvider.getNxConfig(project, virtualFile)

    var defaults = emptyMap<String, Any>()

    if (true) {
        defaults = getNxConfig(workspacePath)?.get("generators") as? Map<String, Any> ?: emptyMap()
    }

    val collectionDefaults = mutableMapOf<String, Any>()
    defaults.keys.forEach { key ->
        if (key.contains(":")) {
            val (collectionName, generatorName) = key.split(":").let { it.first() to it.last() }
            if (collectionDefaults[collectionName] == null) {
                collectionDefaults[collectionName] = mutableMapOf<String, Any>()
            }
            (collectionDefaults[collectionName] as MutableMap<String, Any>)[generatorName] = defaults[key] as Any
        } else {
            val collectionName = key
            if (collectionDefaults[collectionName] == null) {
                collectionDefaults[collectionName] = mutableMapOf<String, Any>()
            }
            (defaults.get(collectionName) as Map<String, Any>).keys.forEach { generatorName ->
                (collectionDefaults[collectionName] as MutableMap<String, Any>)[generatorName] =
                    (defaults[key] as Map<String, Any>)[generatorName] as Any
            }

        }
    }
    return collectionDefaults
}


fun getNxConfig(baseDir: String): Map<String, Any>? {

    val file = File(baseDir, "nx.json")
    if (!file.exists()) {
        return emptyMap()
    }

    val cachedNxJson = cacheJson("nx.json", baseDir)["json"] as? Map<String, Any>

    return cachedNxJson
}


enum class WorkspaceGeneratorType {
    generators, schematics
}

fun checkAndReadWorkspaceGenerators(
    baseDir: String,
    workspaceGeneratorType: WorkspaceGeneratorType,
): List<CollectionInfo> {

    val workspaceGeneratorsPath = Path("tools", workspaceGeneratorType.name)
    val workspaceGeneratorsDir = Path(baseDir).resolve(workspaceGeneratorsPath)
    val file = workspaceGeneratorsDir.toFile()
    if (file.isDirectory && file.exists()) {

        val collections = readWorkspaceGeneratorsCollection(
            baseDir,
            workspaceGeneratorsPath,
            workspaceGeneratorType,
        )

        return collections ?: emptyList()
    }
    return emptyList()

}

fun readWorkspaceGeneratorsCollection(
    baseDir: String,
    workspaceGeneratorsPath: Path,
    workspaceGeneratorType: WorkspaceGeneratorType
): List<CollectionInfo>? {
    val collectionDir = Path(baseDir).resolve(workspaceGeneratorsPath).toFile()
    val collectionName = "workspace-${
        if (workspaceGeneratorType === generators) "generator" else "schematic"
    }"
    val collectionPath = File(collectionDir, "collection.json")
    if (collectionPath.exists()) {
        val collection = readAndCacheJsonFile(filePath = "${collectionDir.path}/collection.json")
        return getCollectionInfo(
            baseDir,
            collectionName,
            collectionPath.path,
            mapOf(
                "path" to collectionPath,
                "json" to emptyMap<String, Any>(),
            ),
            collection["json"] as Map<String, Any>,
        ) ?: emptyList()
    } else {
        return collectionDir.walkBottomUp().filter { it.name == "schema.json" }.map {
            val schemaJson = readAndCacheJsonFile(filePath = it.path)
            val json = schemaJson["json"] as Map<String, Any?>
            val name = (json["id"] ?: json["\$id"]) as? String ?: "unknown"

            val type: GeneratorType = when (json["x-type"]) {
                "application" -> GeneratorType.Application
                "library" -> GeneratorType.Library
                else -> GeneratorType.Other
            }

            CollectionInfo(
                name = collectionName,
                type = CollectionType.generator,
                path = it.path,
                data = Generator(
                    name = name,
                    collection = collectionName,
                    options = normalizeSchema(json),
                    description = json["description"] as String? ?: "",
                    type = type,
                )
            )
        }.toList()
    }


}
