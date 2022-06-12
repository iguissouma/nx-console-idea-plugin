package com.github.iguissouma.nxconsole.cli.config

import com.github.iguissouma.nxconsole.buildTools.NxJsonUtil
import com.github.iguissouma.nxconsole.schematics.NxCliUtil
import com.intellij.lang.javascript.buildTools.npm.PackageJsonUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

class NxConfigProvider private constructor() {

    companion object {

        private val NX_CLI_CONFIG_KEY = Key.create<CachedValue<INxConfig>>("NX_CONFIG_KEY")
        private val LOG = Logger.getInstance(NxConfigProvider::class.java)

        @JvmStatic
        fun getNxProject(project: Project, context: VirtualFile): NxProject? {
            return getNxConfig(project, context)?.getProject(context)
        }

        @JvmStatic
        fun getNxConfig(project: Project, context: VirtualFile): INxConfig? {
            // get package.json file
            val packageJsonFile = PackageJsonUtil.findChildPackageJsonFile(project.baseDir) ?: return null
            val angularCliJson = NxCliUtil.findAngularCliFolder(project, context)?.let {
                NxCliUtil.findCliJson(it)
            } ?: return CachedValuesManager.getManager(project).getCachedValue(
                PsiManager.getInstance(project).findFile(packageJsonFile) ?: return null,
                NX_CLI_CONFIG_KEY,
                {
                    val cachedDocument = FileDocumentManager.getInstance().getCachedDocument(packageJsonFile)
                    val config =
                        try {
                            NxConfigFromGlobs(project, packageJsonFile)
                        } catch (e: ProcessCanceledException) {
                            throw e
                        } catch (e: Exception) {
                            LOG.warn("Cannot load " + packageJsonFile.name + ": " + e.message)
                            null
                        }
                    CachedValueProvider.Result.create(config, cachedDocument ?: packageJsonFile)
                },
                false)


            val psiFile = PsiManager.getInstance(project).findFile(angularCliJson) ?: return null
            return CachedValuesManager.getManager(project).getCachedValue(
                psiFile,
                NX_CLI_CONFIG_KEY,
                {
                    val cachedDocument = FileDocumentManager.getInstance().getCachedDocument(angularCliJson)
                    val config =
                        try {
                            NxConfig(
                                cachedDocument?.charsSequence ?: VfsUtilCore.loadText(angularCliJson),
                                angularCliJson,
                                psiFile.project
                            )
                        } catch (e: ProcessCanceledException) {
                            throw e
                        } catch (e: Exception) {
                            LOG.warn("Cannot load " + angularCliJson.name + ": " + e.message)
                            null
                        }
                    CachedValueProvider.Result.create(config, cachedDocument ?: angularCliJson)
                },
                false
            )
        }

        fun getNxWorkspaceType(project: Project, context: VirtualFile): WorkspaceType {
            val findAngularCliFolder = NxCliUtil.findAngularCliFolder(project, context)
            return findAngularCliFolder?.let {
                NxCliUtil.findCliJson(it)
            }?.let {
                if (it.name != "workspace.json") {
                    if (NxJsonUtil.findChildNxJsonFile(findAngularCliFolder) != null) {
                        WorkspaceType.NX_WITH_ANGULAR
                    } else {
                        WorkspaceType.ANGULAR
                    }
                } else {
                    WorkspaceType.NX
                }
            } ?: WorkspaceType.UNKNOWN
        }
    }
}

enum class WorkspaceType {
    NX, ANGULAR, NX_WITH_ANGULAR, UNKNOWN
}

fun WorkspaceType.exe() = when (this) {
    WorkspaceType.NX -> "nx"
    WorkspaceType.ANGULAR -> "ng"
    WorkspaceType.NX_WITH_ANGULAR -> "nx"
    WorkspaceType.UNKNOWN -> "nx"
}
