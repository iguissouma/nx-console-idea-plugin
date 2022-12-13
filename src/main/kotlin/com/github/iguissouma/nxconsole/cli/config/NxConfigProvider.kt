package com.github.iguissouma.nxconsole.cli.config

import com.github.iguissouma.nxconsole.buildTools.NxJsonUtil
import com.github.iguissouma.nxconsole.schematics.NxCliUtil
import com.intellij.lang.javascript.buildTools.npm.PackageJsonUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.thisLogger
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

private val LOG = logger<NxConfigProvider>()

class NxConfigProvider private constructor() {

    companion object {

        private val NX_CLI_CONFIG_KEY = Key.create<CachedValue<INxConfig>>("NX_CONFIG_KEY")

        @JvmStatic
        fun getNxProject(project: Project, context: VirtualFile): NxProject? {
            return getNxConfig(project, context)?.getProject(context)
        }

        @JvmStatic
        fun getNxConfig(project: Project, context: VirtualFile): INxConfig? {
            LOG.info("getNxConfig called for idea project ${project.name} with context vf path ${context.path}")

            val angularCliJson = NxCliUtil.findAngularCliFolder(project, context)?.let {
                NxCliUtil.findCliJson(it)
            } ?: kotlin.run {

                // get package.json file
                val packageJsonFile = PackageJsonUtil.findChildPackageJsonFile(project.baseDir) ?: return null.also {
                    LOG.info("getNxConfig package.json not found in project.baseDir=${context}")
                }
                LOG.info("root package.json found=${packageJsonFile.path}")

                return CachedValuesManager.getManager(project).getCachedValue(
                    PsiManager.getInstance(project).findFile(packageJsonFile) ?: return null,
                    NX_CLI_CONFIG_KEY,
                    {
                        val cachedDocument = FileDocumentManager.getInstance().getCachedDocument(packageJsonFile)
                        val config =
                            try {
                                NxConfigFromGlobs(project, packageJsonFile)
                            } catch (e: ProcessCanceledException) {
                                LOG.info("NxConfigFromGlobs loading canceled")
                                null
                            } catch (e: Exception) {
                                LOG.info("Cannot load nx config from glob root package json file=" + packageJsonFile.path + ": " + e.message)
                                null
                            }
                        CachedValueProvider.Result.create(config, cachedDocument ?: packageJsonFile)
                    },
                    false
                )

            }

            val psiFile = PsiManager.getInstance(project).findFile(angularCliJson) ?: return null.also {
                LOG.info("cannot find psiFile from vf=${angularCliJson.path}")
            }

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
                            LOG.info("NxConfig loading canceled")
                            null
                        } catch (e: Exception) {
                            LOG.info("Cannot load nx config from " + angularCliJson.path + ": " + e.message)
                            null
                        }
                    CachedValueProvider.Result.create(config, cachedDocument ?: angularCliJson)
                },
                false
            )
        }

        fun getNxWorkspaceType(project: Project, context: VirtualFile): WorkspaceType {
            val findAngularCliFolder = NxCliUtil.findAngularCliFolder(project, context)
            return (findAngularCliFolder?.let {
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
            } ?: NxJsonUtil.findChildNxJsonFile(project.baseDir)?.let { WorkspaceType.NX }
                    ) ?: WorkspaceType.UNKNOWN
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
