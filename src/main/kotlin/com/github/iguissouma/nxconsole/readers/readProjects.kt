package com.github.iguissouma.nxconsole.readers

import java.io.File

private val <K, V> Map<K, V>.json: Map<K, V>
    get() {
        return this.get<Any?, V>(key = "json") as Map<K, V>
    }

fun readBuilderSchema(baseDir: String, builder: String, projectDeafaults: Map<String, Any>?): List<Option> {
    val (packageName, builderName) = builder.split(':').let {
        it.first() to it.last()
    }

    val packagePath = workspaceDependencyPath(baseDir, packageName);

    if (packagePath == null) {
        return emptyList<Option>();
    }

    val packageJson =  readAndCacheJsonFile(
        File(packagePath, "package.json").normalize().path
    );

    val b = packageJson.json["builders"] as? String ?: packageJson.json["executors"] as? String
    val buildersPath = if(b?.startsWith('.') == true)  b else "./${b}"

    val buildersJson = readAndCacheJsonFile(
        buildersPath,
        // get directory from file path
        File(packageJson.get("path") as String).normalize().parentFile.path
    )

    val builderDef: Map<String, Any?> = mapOf<String, Any?>()
        .plus(buildersJson?.json.get("builders") as Map<String, Any?>? ?: emptyMap())
        .plus(buildersJson?.json.get("executors") as Map<String, Any?>? ?: emptyMap())[builderName] as Map<String, Any?>


    val builderSchema = readAndCacheJsonFile(
        builderDef["schema"] as String,
        File(buildersJson.get("path") as String).normalize().parentFile.path
    );

    // TODO projectDefaults
    return  normalizeSchema(builderSchema["json"] as Map<String, Any?>, null);


}
