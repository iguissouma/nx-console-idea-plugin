package com.github.iguissouma.nxconsole.readers

import com.github.iguissouma.nxconsole.readers.CollectionType.*
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import java.io.File
import java.nio.file.Paths


//----workspce-json-project-----------------------------------------------------
data class WorkspaceJsonConfiguration(
    val version: Int,
    /**
     * Projects' projects
     * projects: {
     *      [projectName: string]: ProjectConfiguration;
     *  };
     */
    val projects: List<Any>,
)

/**
 * Type of project supported
 */
enum class ProjectType { library, application }

/**
 * Project configuration
 */
data class ProjectConfiguration(
    /**
     * Project's name. Optional if specified in workspace.json
     */
    //name?: string;
    val name: String?,

    /**
     * Project's targets
     */
    //targets?: { [targetName: string]: TargetConfiguration };
    val targets: List<Any>,

    /**
     * Project's location relative to the root of the workspace
     */
    //root: string;
    val root: String,

    /**
     * The location of project's sources relative to the root of the workspace
     */
    val sourceRoot: String?,

    /**
     * Project type
     */
    val projectType: ProjectType?,

    /**
     * List of default values used by generators.
     *
     * These defaults are project specific.
     *
     * Example:
     *
     * ```
     * {
     *   "@nrwl/react": {
     *     "library": {
     *       "style": "scss"
     *     }
     *   }
     * }
     * ```
     */
    //generators?: { [collectionName: string]: { [generatorName: string]: any }
    val generators: Any?,

    /**
     * List of projects which are added as a dependency
     */
// implicitDependencies?: string[];
    val implicitDependencies: List<String>?,

    /**
     * List of tags used by nx-enforce-module-boundaries / project graph
     */
//  tags?: string[];
    val tags: List<String>?,
)

//----schema----------------------------------------------------------------------------------------------------------------------
enum class CollectionType {
    executor, generator;
}

enum class OptionType(val type: String) {
    AnyType("any"),
    ArrayType("array"),
    BooleanType("boolean"),
    NumberType("number"),
    StringType("string"),
}

enum class GeneratorType(val type: String) {
    Application("application"),
    Library("library"),
    Other("other"),
}

data class CollectionInfo(
    val name: String,
    val path: String,
    val type: CollectionType,
    val data: Generator? = null
)

/**
 *  export type OptionPropertyDescription = Schema['properties'][number];
 *
 *  export type CliOption = {
 *      name: string;
 *      positional?: number;
 *      alias?: string;
 *      hidden?: boolean;
 *      deprecated?: boolean | string;
 *  } & OptionPropertyDescription;
 *
 *  export interface Option extends CliOption {
 *      tooltip?: string;
 *      itemTooltips?: ItemTooltips;
 *      items?: string[] | ItemsWithEnum;
 *      aliases: string[];
 *      isRequired: boolean;
 *      'x-dropdown'?: 'projects';
 *  }
 */
data class Option(
    val type: String? = null,
    val description: String? = null,
    val enum: List<String> = emptyList(),
    val format: String? = null,
    val `x-prompt`: Any? = null,
    val `$default`: String? = null,

    // CliOption
    val name: String,
    val positional: Int? = null,
    val alias: String? = null,
    val hidden: Boolean? = null,
    val deprecated: Any? = null,

    // Option
    val tooltip: String? = null,
    val itemTooltips: Any? = null, //ItemTooltips
    val items: Any? = null, //string[] | ItemsWithEnum
    val aliases: List<String> = emptyList(),
    val isRequired: Boolean = false,
    val `x-dropdown`: String? = null,
)

data class Generator(
    val collection: String,
    val name: String,
    val description: String,
    val options: List<Option> = emptyList(),
    val type: GeneratorType,
)

// export type WorkspaceProjects = WorkspaceJsonConfiguration['projects'];
typealias WorkspaceProjects = List<Any>?

data class ReadCollectionsOptions(val projects: WorkspaceProjects, val clearPackageJsonCache: Boolean?)

fun readCollections(workspacePath: String, options: ReadCollectionsOptions?): List<CollectionInfo> {
    //TODO
    if (options?.clearPackageJsonCache == true) {
        clearJsonCache("package.json", workspacePath);
    }
    val packages = workspaceDependencies(workspacePath, options?.projects);

    val collections = packages.map { packageDetails(it) }

    val allCollections = collections.flatMap { readCollection(workspacePath, it) ?: emptyList() }

    /**
     * Since we gather all collections, and collections listed in `extends`, we need to dedupe collections here if workspaces have that extended collection in their own package.json
     */
    val dedupedCollections = mutableMapOf<String, CollectionInfo>();
    for (singleCollection in allCollections) {
        if (singleCollection == null) {
            continue;
        }

        if (
            !dedupedCollections.containsKey(
                collectionNameWithType(singleCollection.name, singleCollection.type)
            )
        ) {
            dedupedCollections[collectionNameWithType(singleCollection.name, singleCollection.type)] = singleCollection;
        }
    }

    return dedupedCollections.values.toList();
}

fun readCollection(workspacePath: String, packageDetails: PackageDetails): List<CollectionInfo>? {
    try {
        val (packagePath, packageName, json) = packageDetails
        val executorCollections =
            readAndCacheJsonFile((json["executors"] as String?) ?: json["builders"] as String?, packagePath)
        val generatorCollections =
            readAndCacheJsonFile((json["generators"] as String?) ?: json["schematics"] as String?, packagePath)

        return getCollectionInfo(
            workspacePath,
            packageName,
            packagePath,
            executorCollections,
            generatorCollections
        )

    } catch (e: Exception) {
        return null;
    }
}

fun getCollectionInfo(
    workspacePath: String,
    collectionName: String?,
    collectionPath: String,
    executorCollection: Map<String, Any?>,
    generatorCollection: Map<String, Any?>
): List<CollectionInfo>? {
    val collectionMap: MutableMap<String, CollectionInfo> = mutableMapOf()
    fun buildCollectionInfo(
        name: String,
        value: Map<String, Any?>,
        type: CollectionType,
        schemaPath: String,
    ): CollectionInfo {
        val path = Paths.get(collectionPath).resolve(Paths.get(schemaPath).parent).resolve(value["schema"] as String).normalize().toFile().path
        // val path = File(File(collectionPath, schemaPath).parent, value["schema"] as String).path
        return CollectionInfo(
            name = "$collectionName/$name",
            type = type,
            path = path
        )
    }

    val executorCollectionJson = executorCollection["json"] as Map<String, Any?>?
    val executors: Map<String, Any?> = mapOf<String, Any?>()
        .plus(executorCollectionJson?.get("executors") as Map<String, Any?>? ?: emptyMap())
        .plus(executorCollectionJson?.get("builders") as Map<String, Any?>? ?: emptyMap())


    for ((key, schema) in executors.entries) {
        if (!canUse(key, schema as Map<String, Any?>)) {
            continue;
        }
        val collectionInfo = buildCollectionInfo(
            key,
            schema,
            executor,
            executorCollection["path"] as String
        )
        if (
            collectionMap.containsKey(collectionNameWithType(collectionInfo.name, executor))
        ) {
            continue;
        }
        collectionMap[collectionNameWithType(collectionInfo.name, executor)] = collectionInfo
    }


    val generatorCollectionJson = generatorCollection["json"] as Map<String, Any?>?
    val generators: Map<String, Any?> = mapOf<String, Any?>()
        .plus(generatorCollectionJson?.get("generators") as Map<String, Any?>? ?: emptyMap())
        .plus(generatorCollectionJson?.get("schematics") as Map<String, Any?>? ?: emptyMap())

    for ((key, schema) in generators.entries) {
        if (!canUse(key, schema as Map<String, Any?>)) {
            continue;
        }
        try {
            val collectionInfo = buildCollectionInfo(
                key,
                schema,
                generator,
                generatorCollection["path"] as String
            ).copy(data = readCollectionGenerator(collectionName ?: "", key, schema))

            if (collectionMap.containsKey(collectionNameWithType(collectionInfo.name, generator))) {
                continue;
            }

            collectionMap[collectionNameWithType(collectionInfo.name, generator)] = collectionInfo
        } catch (e: Exception) {
            // noop - generator is invalid
        }

    }

    return collectionMap.values.toList()
}


fun readCollectionGenerator(
    collectionName: String,
    collectionSchemaName: String,
    collectionJson: Map<String, Any?>,
): Generator? {
    try {
        val generatorType: GeneratorType = when (collectionJson["x-type"]) {
            "application" -> GeneratorType.Application
            "library" -> GeneratorType.Library
            else -> GeneratorType.Other
        }
        return Generator(
            name = collectionSchemaName,
            collection = collectionName,
            description = (collectionJson["description"] ?: "") as String,
            type = generatorType
        )
    } catch (e: Exception) {
        println("Invalid package.json for schematic ${collectionName}:${collectionSchemaName}");
        return null;
    }
}

fun collectionNameWithType(name: String, type: CollectionType): String {
    return "${name}-${type.name}";
}

/**
 * Checks to see if the collection is usable within Nx Console.
 * @param name
 * @param s
 * @returns
 */
fun canUse(
    name: String,
    s: Map<String, Any?>?,
// s: { hidden: boolean; private: boolean; schema: string; extends: boolean }
): Boolean {
    return !(s?.get("hidden") as? Boolean ?: false) && !(s?.get("private") as? Boolean
        ?: false) && !(s?.get("extends") as? Boolean ?: false) && name != "ng-add";
}


fun packageDetails(p: String): PackageDetails {
    val json = readAndCacheJsonFile(File(p, "package.json").path)["json"] as Map<String, Any?>
    return PackageDetails(p, json["name"] as String?, json)
}

data class PackageDetails(
    val packagePath: String,
    val packageName: String?,
    val packageJson: Map<String, Any?>,
)

fun readAndCacheJsonFile(filePath: String?, basedir: String = ""): Map<String, Any?> {
    if (filePath == null) {
        return mapOf(
            "path" to "",
            "json" to emptyMap<String, Any>(),
        )
    }
    val fullFilePath = Paths.get(basedir, filePath).normalize().toFile()
    try {
        if (fileContents.contains(fullFilePath.path) || fullFilePath.isFile) {
            if (!fileContents.contains(fullFilePath.path)) {
                fileContents[fullFilePath.path] = readAndParseJson(fullFilePath.path)
            }
            return mapOf(
                "path" to fullFilePath.path,
                "json" to fileContents[fullFilePath.path],
            )

        }
    } catch (e: Exception) {
        println("$fullFilePath does not exist")
    }


    return mapOf(
        "path" to fullFilePath.path,
        "json" to emptyMap<String, Any>(),
    )

}

val type = object : TypeToken<Map<String, Any>>() {}.type

fun readAndParseJson(path: String): Map<String, Any> {
    return File(path).readText().let { Gson().fromJson(it, type) }
}

private operator fun <E> List<E>.get(fullFilePath: E): E {
    return this.firstOrNull { it == fullFilePath } ?: throw Exception("File not found")
}

//----workspace-dependencies-----------------------------------------------------------------------------------------------------
/**
 * Get dependencies for the current workspace.
 * This is needed to continue to support Angular CLI projects.
 *
 * @param workspacePath
 * @returns
 */
fun workspaceDependencies(workspacePath: String, projects: WorkspaceProjects): List<String> {
    val dependencies: MutableList<String> = mutableListOf();
    dependencies.addAll(localDependencies(workspacePath, projects));

    //if (await isWorkspaceInPnp(workspacePath)) {
    //    dependencies.push(...(await pnpDependencies(workspacePath)));
    //}

    dependencies.addAll(npmDependencies(workspacePath));

    return dependencies;
}

/**
 * Get a flat list of all node_modules folders in the workspace.
 * This is needed to continue to support Angular CLI projects.
 *
 * @param workspacePath
 * @returns
 */
fun npmDependencies(workspacePath: String): Collection<String> {
    val nodeModulesDir = File(workspacePath, "node_modules");
    val res: MutableCollection<String> = mutableListOf();
    if (!nodeModulesDir.isDirectory) {
        return res;
    }

    val dirContents = nodeModulesDir.listFiles() ?: return res;
    for (npmPackageOrScope in dirContents) {
        if (npmPackageOrScope.name.startsWith(".")) {
            continue;
        }
        //const packageStats = await stat(join(nodeModulesDir, npmPackageOrScope));
        val packageStats = npmPackageOrScope
        if (!packageStats.isDirectory) {
            continue;
        }

        if (npmPackageOrScope.name.startsWith("@")) {
            npmPackageOrScope.listFiles()?.forEach { p: File ->
                res.add("${nodeModulesDir}/${npmPackageOrScope.name}/${p.name}");
            }
        } else {
            res.add("${nodeModulesDir}/${npmPackageOrScope.name}");
        }

    }

    return res
}

fun localDependencies(workspacePath: String, projects: WorkspaceProjects): Collection<String> {
    if (projects == null) {
        return emptyList();
    }

    // TODO nx version
    // const nxVersion = WorkspaceConfigurationStore.instance.get('nxVersion', null);
    /*if (nxVersion && nxVersion < 13) {
        return [];
    }*/

    val packages = projects.filterIsInstance<ProjectConfiguration>().map {
        "${workspacePath}/${it.root}/package.json"
    }
    val existingPackages: MutableList<String> = mutableListOf();

    for (pkg in packages) {
        try {
            val file = File(pkg)
            if (file.isFile) {
                existingPackages.add(pkg.replace("/package.json", ""))
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    return existingPackages;
}

//****utils----------------------------------------------------------------------------------------------------------------------
val fileContents = mutableMapOf<String, Map<String, Any>>()
fun clearJsonCache(filePath: String, basedir: String = ""): Boolean {
    val fullFilePath = File(basedir, filePath);
    fileContents.remove(fullFilePath.path);
    return true;
}
