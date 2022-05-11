package com.github.iguissouma.nxconsole.builders

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class NxCliBuildersRegistryServiceImpl : NxCliBuildersRegistryService {
    var cache: Cache<String, List<NxBuilderOptions>> = CacheBuilder.newBuilder()
        .maximumSize(1000) // Taille Max
        .build()

    override fun readBuilderSchema(
        project: Project,
        cliFolder: VirtualFile,
        builderName: String
    ): List<NxBuilderOptions> {
        return cache.getIfPresent(builderName) ?: return (
            RUN_ONE_OPTIONS + doLoadBuilders(
                project,
                builderName,
            )).also { cache.put(builderName, it) }
    }

    companion object {
        val RUN_ONE_OPTIONS = listOf(
            NxBuilderOptions().apply {
                name = "with-deps"
                type = "boolean"
                description = "Include dependencies of specified projects when computing what to run"
                default = false.toString()
            },
            NxBuilderOptions().apply {
                name = "parallel"
                type = "boolean"
                description = "Parallelize the command"
                default = false.toString()
            },
            NxBuilderOptions().apply {
                name = "maxParallel"
                type = "number"
                description = "Max number of parallel processes"
                default = 3.toString()
            },
            NxBuilderOptions().apply {
                name = "only-failed"
                type = "boolean"
                description = "Isolate projects which previously failed"
                default = false.toString()
            },
            NxBuilderOptions().apply {
                name = "skip-nx-cache"
                type = "boolean"
                description = "Rerun the tasks even when the results are available in the cache"
                default = false.toString()
            },
            NxBuilderOptions().apply {
                name = "exclude"
                type = "string"
                description = "Exclude certain projects from being processed"
            }
        )
    }
}
