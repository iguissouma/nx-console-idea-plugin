package com.github.iguissouma.nxconsole.schematics

import com.github.iguissouma.nxconsole.buildTools.NxJsonUtil
import com.intellij.javascript.nodejs.library.NodeModulesDirectoryManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderEx
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.CachedValueProvider
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.annotations.NonNls
import java.util.*
import java.util.function.Supplier

class NxCliSchematicsRegistryServiceImpl : NxCliSchematicsRegistryService {

    companion object {
        @NonNls
        private val SCHEMATICS_PUBLIC =
            Key<CachedSchematics>("nx.cli.schematics.public")

        @NonNls
        private val SCHEMATICS_ALL =
            Key<CachedSchematics>("nx.cli.schematics.all")

        private val SCHEMATICS_CACHE_TRACKER = SimpleModificationTracker()
    }

    override fun getSchematics(
        project: Project,
        cliFolder: VirtualFile,
        includeHidden: Boolean,
        logErrors: Boolean
    ): List<Schematic> {
        return listOfNotNull(NxCliUtil.findCliJson(cliFolder), NxJsonUtil.findChildNxJsonFile(cliFolder)).firstOrNull()
            ?.let { ReadAction.compute<PsiFile, Throwable> { PsiManager.getInstance(project).findFile(it) } }
            ?.let { angularJson ->
                getCachedSchematics(
                    angularJson,
                    if (includeHidden) SCHEMATICS_ALL else SCHEMATICS_PUBLIC
                ).getUpToDateOrCompute {
                    CachedValueProvider.Result.create(
                        doLoadGenerators(angularJson.project),
                        NodeModulesDirectoryManager.getInstance(angularJson.project).getNodeModulesDirChangeTracker(),
                        SCHEMATICS_CACHE_TRACKER,
                        angularJson
                    )
                }
            }

            ?: emptyList()
    }

    override fun clearProjectSchematicsCache() {
        SCHEMATICS_CACHE_TRACKER.incModificationCount()
    }

    private fun getCachedSchematics(dataHolder: UserDataHolder, key: Key<CachedSchematics>): CachedSchematics {
        var result = dataHolder.getUserData(key)
        if (result != null) {
            return result
        }
        if (dataHolder is UserDataHolderEx) {
            return dataHolder.putUserDataIfAbsent(key, CachedSchematics())
        }
        result = CachedSchematics()
        dataHolder.putUserData(key, result)
        return result
    }

    private class CachedSchematics {
        private var mySchematics: MutableList<Schematic>? = null
        private var myTrackers: List<Pair<Any, Long>>? = null

        @Synchronized
        fun getUpToDateOrCompute(provider: Supplier<CachedValueProvider.Result<List<Schematic?>?>>): List<Schematic>? {
            if (mySchematics != null && myTrackers != null && ContainerUtil.all(
                    myTrackers!!
                ) { pair: Pair<Any, Long> ->
                    pair.second >= 0 && getTimestamp(
                        pair.first
                    ) == pair.second
                }
            ) {
                return mySchematics
            }
            val schematics = provider.get()
            mySchematics = Collections.unmodifiableList(schematics.value)
            myTrackers = ContainerUtil.map(
                schematics.dependencyItems
            ) { obj: Any ->
                Pair.pair(
                    obj,
                    getTimestamp(obj)
                )
            }
            return mySchematics
        }

        companion object {
            private fun getTimestamp(dependency: Any): Long {
                if (dependency is ModificationTracker) {
                    return dependency.modificationCount
                }
                if (dependency is PsiElement) {
                    val element = dependency
                    if (!element.isValid) return -1
                    val containingFile = element.containingFile
                    return containingFile?.virtualFile?.modificationStamp ?: -1
                }
                throw UnsupportedOperationException(dependency.javaClass.toString())
            }
        }
    }
}
