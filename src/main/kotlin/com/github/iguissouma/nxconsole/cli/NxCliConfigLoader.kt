@file:JvmName("NxCliConfigLoader")

package com.github.iguissouma.nxconsole.cli

import com.github.iguissouma.nxconsole.cli.config.INxConfig
import com.github.iguissouma.nxconsole.cli.config.NxConfig
import com.github.iguissouma.nxconsole.cli.config.NxConfigProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

@Suppress("DeprecatedCallableAddReplaceWith", "DEPRECATION")
@Deprecated("Use AngularConfigProvider instead")
fun load(project: Project, context: VirtualFile): NxCliConfig {
    return NxConfigProvider.getNxConfig(project, context)
        ?.let { NxCliJsonFileConfig(it) }
        ?: NxCliEmptyConfig()
}

@Deprecated("Use AngularConfigProvider and AngularConfig instead")
interface NxCliConfig {

    /**
     * @return root folders according to apps -> root in .angular-cli.json; usually it is a single 'src' folder.
     */
    fun getRootDirs(): Collection<VirtualFile>

    fun exists(): Boolean
}

@Suppress("DEPRECATION")
private class NxCliEmptyConfig : NxCliConfig {

    override fun getRootDirs(): Collection<VirtualFile> = emptyList()

    override fun exists(): Boolean = false
}

@Suppress("DEPRECATION")
private class NxCliJsonFileConfig(private val config: INxConfig) : NxCliConfig {

    override fun getRootDirs(): Collection<VirtualFile> {
        return config.projects.mapNotNull { it.rootDir }
    }

    override fun exists(): Boolean = true
}
