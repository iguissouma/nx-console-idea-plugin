package com.github.iguissouma.nxconsole.cli.config

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

abstract class NxProject(internal val angularCliFolder: VirtualFile, internal val project: Project) {
    abstract val name: String

    abstract val rootDir: VirtualFile?

    abstract val sourceDir: VirtualFile?

    abstract val type: AngularProjectType?

    abstract val architect: Map<String, Architect>

    internal open fun resolveFile(filePath: String?): VirtualFile? {
        return filePath?.let { path ->
            rootDir?.takeIf { it.isValid }?.findFileByRelativePath(path)
                ?: angularCliFolder.takeIf { it.isValid }?.findFileByRelativePath(path)
        }
    }

    internal fun proximity(context: VirtualFile): Int {
        val rootDirPath = (rootDir ?: return -1).path + "/"
        val contextPath = context.path
        if (!contextPath.startsWith(rootDirPath)) {
            return -1
        }
        return contextPath.length - rootDirPath.length
    }

    override fun toString(): String {
        return """
      |${javaClass.simpleName} {
      |       name: $name
      |       type: $type
      |       rootDir: $rootDir
      |       sourceDir: $sourceDir
      |     }
    """.trimMargin()
    }

    enum class AngularProjectType {
        @JsonProperty("application")
        APPLICATION,

        @JsonProperty("library")
        LIBRARY
    }

    class Architect {
        val name: String? = null
        val project: String? = null

        @JsonProperty("builder")
        @JsonAlias(value = ["executor"])
        val builder: String? = null
        val description: String? = null

        // val configurations: List<ArchitectConfiguration> = emptyList()
        val configurations: Map<String, Any> = emptyMap()
        val options: Map<String, Any> = emptyMap()
    }

    class ArchitectConfiguration {
        val name: String? = null
        val defaultValues: List<DefaultValue> = emptyList()
    }

    class DefaultValue {
        val name: String? = null
        val defaultValue: String? = null
    }

    class Option {
        val name: String? = null
        val description: String? = null
    }
}

internal class NxProjectImpl(
    override val name: String,
    val projectPath: String?,
    val ngProject: AngularJsonProject,
    angularCliFolder: VirtualFile,
    project: Project
) :
    NxProject(angularCliFolder, project) {

    override val rootDir = ngProject.rootPath?.let { angularCliFolder.findFileByRelativePath(it) } ?: projectPath?.let {
        angularCliFolder.findFileByRelativePath(it)
    }

    override val sourceDir get() = ngProject.sourceRoot?.let { angularCliFolder.findFileByRelativePath(it) } ?: rootDir

    override val type: AngularProjectType?
        get() = ngProject.projectType

    override val architect: Map<String, Architect>
        get() = ngProject.architect
}

internal class NxLegacyProjectImpl(
    private val angularJson: AngularJson,
    private val app: AngularJsonLegacyApp,
    angularCliFolder: VirtualFile,
    project: Project
) :
    NxProject(angularCliFolder, project) {

    override val name: String = app.name ?: angularJson.legacyProject?.name ?: "Angular project"

    override val rootDir: VirtualFile =
        app.appRoot?.let { angularCliFolder.findFileByRelativePath(it) } ?: angularCliFolder

    override val sourceDir: VirtualFile? get() = app.root?.let { rootDir.findFileByRelativePath(it) }

    override val type: AngularProjectType?
        get() = null

    override fun resolveFile(filePath: String?): VirtualFile? {
        return filePath?.let {
            sourceDir?.findFileByRelativePath(it)
                ?: rootDir.findFileByRelativePath(it)
        }
    }

    override val architect: Map<String, Architect>
        get() = emptyMap()
}
