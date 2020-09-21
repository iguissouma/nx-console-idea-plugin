package com.github.iguissouma.nxconsole.buildTools

import com.github.iguissouma.nxconsole.NxIcons
import com.github.iguissouma.nxconsole.buildTools.NxJsonUtil.findChildNxJsonFile
import com.github.iguissouma.nxconsole.buildTools.NxJsonUtil.isNxJsonFile
import com.github.iguissouma.nxconsole.buildTools.NxJsonUtil.listTasks
import com.github.iguissouma.nxconsole.buildTools.rc.NxConfigurationType
import com.github.iguissouma.nxconsole.buildTools.rc.NxRunConfiguration
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.lang.javascript.buildTools.base.JsbtApplicationService
import com.intellij.lang.javascript.buildTools.base.JsbtFileManager
import com.intellij.lang.javascript.buildTools.base.JsbtFileStructure
import com.intellij.lang.javascript.buildTools.base.JsbtService
import com.intellij.lang.javascript.buildTools.base.JsbtTaskFetchException
import com.intellij.lang.javascript.buildTools.base.JsbtTaskSet
import com.intellij.lang.javascript.buildTools.base.JsbtTaskTreeView
import com.intellij.lang.javascript.buildTools.base.JsbtToolWindowManager
import com.intellij.lang.javascript.buildTools.base.JsbtUtil
import com.intellij.lang.javascript.library.JSLibraryUtil
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.SmartList
import com.intellij.util.containers.ContainerUtil
import javax.swing.Icon

@Service
class NxService(val project: Project) : JsbtService(project) {

    companion object {

        val LOG = Logger.getInstance(NxService::class.java)

        val APPLICATION_SERVICE = NxApplicationService()

        fun getInstance(project: Project): NxService {
            return APPLICATION_SERVICE.getProjectService(project)
        }

        val STRUCTURE_KEY = Key.create<CachedValue<NxFileStructure>>(NxService::class.java.name)

        fun doComputeAvailability(project: Project, psiFile: PsiFile, nxJson: VirtualFile) =
            CachedValuesManager.getCachedValue(psiFile, STRUCTURE_KEY) {
                val value: NxFileStructure = try {
                    listTasks(project, nxJson)
                } catch (var5: JsbtTaskFetchException) {
                    NxFileStructure(nxJson)
                }
                CachedValueProvider.Result.create(value, psiFile)
            }
    }

    override fun getApplicationService(): JsbtApplicationService {
        return APPLICATION_SERVICE
    }

    override fun getFileManager(): JsbtFileManager {
        return NxFileManager.getInstance(project)
    }

    override fun createToolWindowManager(): JsbtToolWindowManager {
        return JsbtToolWindowManager(project, "Nx ", NxIcons.NRWL_ICON, "reference.tool.window.nx", this)
    }

    override fun createTaskTreeView(layoutPlace: String?): JsbtTaskTreeView {
        return NxTaskTreeView(this, project, layoutPlace)
    }

    override fun detectAllBuildfiles(): MutableList<VirtualFile> {
        return if (DumbService.isDumb(myProject)) {
            this.detectAllBuildfilesInContentRoots(false, false)
        } else {
            ReadAction.compute<MutableList<VirtualFile>, RuntimeException> {
                if (myProject.isDisposed) {
                    return@compute mutableListOf<VirtualFile>()
                } else {
                    val scope = JSLibraryUtil.getContentScopeWithoutLibraries(myProject)
                    val files = FilenameIndex.getVirtualFilesByName(myProject, "nx.json", scope)
                    return@compute files.toMutableList()
                }
            }
        }
    }

    override fun detectAllBuildfilesInContentRoots(
        webModulesOnly: Boolean,
        filterOutEmptyBuildfiles: Boolean
    ): MutableList<VirtualFile> {
        val buildfiles: MutableList<VirtualFile> = SmartList()
        JsbtUtil.iterateOverContentRoots(myProject, webModulesOnly) { contentRoot: VirtualFile ->
            val packageJson = findChildNxJsonFile(contentRoot)
            LOG.info("Found nx.json in " + contentRoot.path + ": " + packageJson)
            if (packageJson != null && (!filterOutEmptyBuildfiles || hasScripts(myProject, packageJson))) {
                buildfiles.add(packageJson)
            }
        }
        return buildfiles
    }

    private fun hasScripts(myProject: Project, packageJson: VirtualFile): Boolean {
        return try {
            val structure = listTasks(myProject, packageJson)
            !structure.taskNames.isEmpty()
        } catch (var3: JsbtTaskFetchException) {
            false
        }
    }

    override fun isBuildfile(file: VirtualFile): Boolean {
        return isNxJsonFile(file)
    }

    override fun createEmptyFileStructure(buildFile: VirtualFile): JsbtFileStructure {
        return NxFileStructure(buildFile)
    }

    override fun fetchBuildfileStructure(nxJson: VirtualFile): JsbtFileStructure {
        return ReadAction.compute<NxFileStructure, JsbtTaskFetchException> {
            if (myProject.isDisposed) {
                throw JsbtTaskFetchException.newGenericException(nxJson, "myProject is disposed already")
            } else if (!nxJson.isValid()) {
                throw JsbtTaskFetchException.newBuildfileSyntaxError(nxJson)
            } else {
                val psiFile = PsiManager.getInstance(myProject).findFile(nxJson)
                if (psiFile == null) {
                    throw JsbtTaskFetchException.newGenericException(nxJson, "Cannot find PSI file")
                } else {
                    doComputeAvailability(project, psiFile, nxJson)
                }
            }
        }
    }

    override fun getConfigurationFactory(): ConfigurationFactory {
        return NxConfigurationType.getFactory()
    }

    override fun isConfigurationMatched(runConfiguration: RunConfiguration, patternObject: Any): Boolean {
        if (runConfiguration is NxRunConfiguration) {
            val runSettings = runConfiguration.runSettings
            if (patternObject is NxRunSettings) {
                val patternRunSettings = patternObject
                return JsbtUtil.equalsOrderless(
                    patternRunSettings.tasks,
                    runSettings.tasks
                ) && patternRunSettings.nxFileSystemIndependentPath == runSettings.nxFileSystemIndependentPath
            }
            if (patternObject is JsbtTaskSet) {
                val patternTaskSet = patternObject
                return JsbtUtil.equalsOrderless(
                    patternTaskSet.taskNames,
                    runSettings.tasks
                ) && patternTaskSet.structure.buildfile.path == runSettings.nxFileSystemIndependentPath
            }
        }
        return false
    }

    override fun setupRunConfiguration(runConfiguration: RunConfiguration, taskSet: JsbtTaskSet) {
        val structure = taskSet.structure as NxFileStructure
        val nxRunConfiguration = runConfiguration as NxRunConfiguration
        val merged = NxRunSettings(
            nxFilePath = structure.nxJson.path,
            tasks = taskSet.taskNames
        )
        NxRunConfigurationProducer.setupConfigurationFromSettings(nxRunConfiguration, merged)
    }

    override fun showTaskListingSettingsDialog(contextNxfile: VirtualFile?): Boolean {
        editConfigurations()
        return true
    }

    fun detectFirstBuildfileInContentRoots(webModulesOnly: Boolean): VirtualFile? {
        val buildfiles = this.detectAllBuildfilesInContentRoots(webModulesOnly)
        return ContainerUtil.getFirstItem(buildfiles) as VirtualFile
    }

    class NxApplicationService : JsbtApplicationService() {

        override fun getProjectService(project: Project): NxService {
            return ServiceManager.getService(project, NxService::class.java) as NxService
        }

        override fun getName(): String {
            return "Nx"
        }

        override fun getIcon(): Icon? {
            return NxIcons.NRWL_ICON
        }

        override fun getBuildfileCommonName(): String {
            return "nx.json"
        }
    }
}
