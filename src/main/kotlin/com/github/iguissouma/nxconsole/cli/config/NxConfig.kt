package com.github.iguissouma.nxconsole.cli.config

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.lang.javascript.buildTools.npm.NpmScriptsUtil
import com.intellij.lang.javascript.library.JSLibraryUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.util.text.CharSequenceReader
import one.util.streamex.StreamEx
import java.nio.file.Paths

open class INxConfig(open val angularJsonFile: VirtualFile) {
    var projects: List<NxProject> = emptyList()
    var defaultProject: NxProject? = null
    fun getProject(context: VirtualFile): NxProject? {
        return StreamEx.of(projects)
            .map { Pair(it, it.proximity(context)) }
            .filter { it.second >= 0 }
            .minByInt { it.second }
            .map { it.first }
            .orElse(null)
    }

    override fun toString(): String {
        return """
      | NxConfig {
      |   defaultProject: ${defaultProject?.name}
      |   projects: [
      |     ${projects.joinToString(",\n     ") { it.toString() }}
      |   ]
      | }
    """.trimMargin()
    }
}

class NxConfigFromGlobs(
    project: Project,
    val packageJsonFile: VirtualFile
) : INxConfig(packageJsonFile) {

    init {

        val mapper = ObjectMapper(
            JsonFactory.builder()
                .configure(JsonReadFeature.ALLOW_JAVA_COMMENTS, true)
                .configure(JsonReadFeature.ALLOW_SINGLE_QUOTES, true)
                .configure(JsonReadFeature.ALLOW_MISSING_VALUES, true)
                .configure(JsonReadFeature.ALLOW_TRAILING_COMMA, true)
                .configure(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES, true)
                .build()
        )
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
        val scope = JSLibraryUtil.getContentScopeWithoutLibraries(project)

        val packageJsonFiles = FilenameIndex.getVirtualFilesByName("package.json", scope)
            .filterNot { it == packageJsonFile }


        val projectJsonFiles = FilenameIndex.getVirtualFilesByName("project.json", scope)

        projects = packageJsonFiles.map {
            val relativePath = VfsUtilCore.getRelativePath(it, project.baseDir)
            val psiFile = PsiManager.getInstance(project).findFile(it) as? JsonFile
            val scriptsProperty = NpmScriptsUtil.findScriptsProperty(psiFile)
            NxProjectImpl(
                name = it.parent.nameWithoutExtension,
                projectPath = FileUtil.getRelativePath(packageJsonFile.path, it.parent.path, '/') ,
                ngProject = mapper.readValue("""
                                {
                                  "name": "${it.parent.nameWithoutExtension}",
                                  "projectType": "library",
                                  "root": "$relativePath",
                                  "sourceRoot": "$relativePath",
                                  "targets": {
                                      ${(scriptsProperty?.value as? JsonObject)?.propertyList?.map { it.name }?.joinToString(",") { "\"${it}\": {}" } ?: ""}
                                   }
                                }
                                """.trimIndent()),
                angularCliFolder = packageJsonFile,
                project = project
            )
        } + projectJsonFiles.map {
            val psiFile = PsiManager.getInstance(project).findFile(it) ?: error("cannot PsiFile for file ${it.path}")
            NxProjectImpl(
                name = it.parent.nameWithoutExtension,
                projectPath = FileUtil.getRelativePath(packageJsonFile.path, it.parent.path, '/') ,
                ngProject = mapper.readValue(psiFile.text),
                angularCliFolder = packageJsonFile,
                project = project
            )
        }

    }


    override fun toString(): String {
        return """
      | NxConfigFromGlobs {
      |   defaultProject: ${defaultProject?.name}
      |   projects: [
      |     ${projects.joinToString(",\n     ") { it.toString() }}
      |   ]
      | }
    """.trimMargin()
    }
}

class NxConfig(text: CharSequence, override val angularJsonFile: VirtualFile, project: Project) :
    INxConfig(angularJsonFile) {

    init {
        val angularCliFolder = angularJsonFile.parent
        val mapper = ObjectMapper(
            JsonFactory.builder()
                .configure(JsonReadFeature.ALLOW_JAVA_COMMENTS, true)
                .configure(JsonReadFeature.ALLOW_SINGLE_QUOTES, true)
                .configure(JsonReadFeature.ALLOW_MISSING_VALUES, true)
                .configure(JsonReadFeature.ALLOW_TRAILING_COMMA, true)
                .configure(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES, true)
                .build()
        )
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
        val angularJson = mapper.readValue(CharSequenceReader(text), AngularJson::class.java)
        if (angularJson.projects.isNotEmpty()) {
            projects = angularJson.projects.map { (name, ngProjectJson) ->
                when (ngProjectJson) {
                    is String -> {
                        val projectPath = angularCliFolder.path + "/" + ngProjectJson
                        val path = "$projectPath/project.json"
                        val virtualFile = VirtualFileManager.getInstance().findFileByNioPath(Paths.get(path))
                            ?: error("cannot read project ...")
                        val psiFile =
                            PsiManager.getInstance(project).findFile(virtualFile) ?: error("cannot read project ...")
                        NxProjectImpl(name, ngProjectJson, mapper.readValue(psiFile.text), angularCliFolder, project)
                    }
                    is Map<*, *> -> NxProjectImpl(
                        name,
                        ngProjectJson["root"] as? String,
                        mapper.readValue(mapper.writeValueAsString(ngProjectJson)),
                        angularCliFolder,
                        project
                    )
                    else -> error("cannot read project ...")
                }
            }
            defaultProject = angularJson.defaultProject?.let { defaultProject ->
                projects.find { it.name == defaultProject }
            }
        } else {
            projects = angularJson.legacyApps.map { app ->
                NxLegacyProjectImpl(angularJson, app, angularCliFolder, project)
            }
            defaultProject = projects.firstOrNull()
        }
    }

}
