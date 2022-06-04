package com.github.iguissouma.nxconsole.jest

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.intellij.lang.javascript.linter.JSLinterConfigFileUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ObjectUtils
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.MultiMap
import com.intellij.webcore.util.JsonUtil
import org.jetbrains.annotations.Contract
import java.io.File
import java.io.IOException
import java.util.*

class NxCliConfig(private val myConfig: VirtualFile) {

    fun getProjectContainingFileOrDefault(file: VirtualFile?): String? {
        return try {
            val value = doGetProjectContainingFile(file, true)
            if (value == null) {
                LOG.info("No project found in " + myConfig.path)
            }
            value
        } catch (var3: Exception) {
            LOG.info("Failed to find project in " + myConfig.path, var3)
            null
        }
    }

    fun getProjectContainingFile(file: VirtualFile?): String? {
        return try {
            val value = doGetProjectContainingFile(file, false)
            if (value == null) {
                LOG.info("No project found in " + myConfig.path)
            }
            value
        } catch (var3: Exception) {
            LOG.info("Failed to find project in " + myConfig.path, var3)
            null
        }
    }

    @Throws(IOException::class)
    private fun doGetProjectContainingFile(file: VirtualFile?, returnDefaultProjectOnFallback: Boolean): String? {
        return if (!myConfig.isValid) {
            null
        } else {
            val text = JSLinterConfigFileUtil.loadActualText(myConfig)
            val rootObj = JsonParser().parse(text).asJsonObject
            val defaultProject = JsonUtil.getChildAsString(rootObj, "defaultProject")
            val projectsObj = JsonUtil.getChildAsObject(rootObj, "projects")
            if (projectsObj != null && file != null) {
                val projectRootToNameMap = getProjectRootToNameMap(projectsObj)
                val nearestRoot = findNearestRoot(file, projectRootToNameMap.keySet())
                if (nearestRoot != null) {
                    val projects = projectRootToNameMap[nearestRoot]
                    if (projects.contains(defaultProject) && isSuitableProject(defaultProject)) {
                        return defaultProject
                    }
                    return ObjectUtils.notNull(
                        projects.stream().filter { projectName: String? -> isSuitableProject(projectName) }
                            .findFirst().orElse(null),
                        Objects.requireNonNull(ContainerUtil.getFirstItem(projects) as String) as String
                    )
                }
            }
            if (returnDefaultProjectOnFallback) {
                LOG.info("Cannot find project containing file, fallback to default project")
                defaultProject
                    ?: if (projectsObj != null) ContainerUtil.getFirstItem(projectsObj.keySet()) as String else null
            } else {
                null
            }
        }
    }

    private fun getProjectRootToNameMap(projectsObj: JsonObject): MultiMap<VirtualFile, String?> {
        val projectRootToNameMap = MultiMap.create<VirtualFile, String?>()
        val var3: Iterator<*> = projectsObj.entrySet().iterator()
        while (var3.hasNext()) {
            val entry: java.util.Map.Entry<String, JsonElement> =
                var3.next() as java.util.Map.Entry<String, JsonElement>
            val projectObj = JsonUtil.getAsObject(entry.value as JsonElement)
            if (projectObj == null) {
                val root = (entry.value as? JsonPrimitive)?.let { getProjectRoot(it) }
                if (root != null) {
                    projectRootToNameMap.putValue(root, entry.key as String)
                }
            } else {
                val root = getProjectRoot(projectObj)
                if (root != null) {
                    projectRootToNameMap.putValue(root, entry.key as String)
                }
            }
        }
        return projectRootToNameMap
    }

    private fun getProjectRoot(projectObj: JsonObject): VirtualFile? {
        val rootStr = StringUtil.notNullize(JsonUtil.getChildAsString(projectObj, "root"))
        return myConfig.parent.findFileByRelativePath(rootStr)
    }

    private fun getProjectRoot(projectPrimitive: JsonPrimitive): VirtualFile? {
        val rootStr = StringUtil.notNullize(projectPrimitive.asString)
        return myConfig.parent.findFileByRelativePath(rootStr)
    }

    companion object {
        private val LOG = Logger.getInstance(
            NxCliConfig::class.java
        )
        private const val DEFAULT_PROJECT = "defaultProject"
        private fun findNearestRoot(file: VirtualFile, roots: Set<VirtualFile>): VirtualFile? {
            var parent: VirtualFile? = file
            while (parent != null) {
                if (roots.contains(parent)) {
                    return parent
                }
                parent = parent.parent
            }
            return null
        }

        @Contract("null -> false")
        private fun isSuitableProject(projectName: String?): Boolean {
            return projectName != null && !projectName.endsWith("-e2e")
        }

        fun findProjectConfig(workingDirectory: File): NxCliConfig? {
            val root = LocalFileSystem.getInstance().findFileByIoFile(workingDirectory)
            return if (root == null) {
                null
            } else {
                val config = JSLinterConfigFileUtil.findFileUpToFileSystemRoot(
                    root,
                    arrayOf(
                        "workspace.json",
                        "angular.json",
                        ".angular.json",
                        "angular-cli.json",
                        ".angular-cli.json"
                    )
                )
                if (config != null) NxCliConfig(config) else null
            }
        }
    }
}
