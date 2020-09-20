package com.github.iguissouma.nxconsole.buildTools

import com.github.iguissouma.nxconsole.NxIcons
import com.github.iguissouma.nxconsole.buildTools.rc.NxConfigurationType
import com.github.iguissouma.nxconsole.buildTools.rc.NxRunConfiguration
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.json.psi.JsonObject
import com.intellij.lang.javascript.buildTools.base.*
import com.intellij.lang.javascript.library.JSLibraryUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.ObjectUtils
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
        val NX_NAME_WITHOUT_EXT = "nx"
        val KNOWN_EXTENSIONS = arrayOf("json")
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
        val buildfiles: MutableList<VirtualFile> = SmartList<VirtualFile>()
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

    fun listTasks(project: Project, nxJson: VirtualFile): NxFileStructure {

        val exRef = Ref.create<JsbtTaskFetchException>()
        val structureRef = Ref.create<NxFileStructure>()
        ApplicationManager.getApplication().runReadAction {
            try {
                structureRef.set(doBuildStructure(project, nxJson))
            } catch (var5: JsbtTaskFetchException) {
                exRef.set(var5)
            }
        }
        val structure = structureRef.get() as NxFileStructure
        return if (structure != null) {
            structure
        } else {
            throw (exRef.get() as JsbtTaskFetchException)
        }
    }


    private fun doBuildStructure(project: Project, nxJson: VirtualFile): NxFileStructure {

        return if (!nxJson.isValid) {
            throw invalidFile(nxJson)
        } else {
            val structure = NxFileStructure(nxJson)
            if (false) {
                structure
            } else {
                val angularJsonFile = findChildAngularJsonFile(nxJson.parent)
                val nxProjectsProperty = findProjectsProperty(project, nxJson)
                val angularProjectsProperty = angularJsonFile?.let { findProjectsProperty(project, it) }
                val projectsFromAngular = ObjectUtils.tryCast(angularProjectsProperty?.value, JsonObject::class.java)
                val projectsFromNx = ObjectUtils.tryCast(nxProjectsProperty?.value, JsonObject::class.java)
                if (projectsFromNx != null && projectsFromAngular != null) {
                    val propertyList = projectsFromNx.propertyList
                    val map1 = propertyList.map { it.name }
                        .map { it to projectsFromAngular.findProperty(it) }
                        .map { it.first to (it.second?.value as JsonObject).findProperty("architect") }
                        .map {
                            it.first to (it.second?.value as JsonObject).propertyList.map { property ->
                                NxTask(
                                    structure,
                                    property.name
                                )
                            }.toList()
                        }
                        .toMap()
                    structure.myNxProjectsTask = map1
                }
                val listOf = listOf("Generate & Run Target", "Common Nx Commands", "Projects")
                val scripts = listOf.map { NxTask(structure, it) }.toList()
                structure.setScripts(scripts)
                structure
            }
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
                throw JsbtTaskFetchException.newGenericException(nxJson, myProject.toString() + " is disposed already")
            } else if (!nxJson.isValid()) {
                throw JsbtTaskFetchException.newBuildfileSyntaxError(nxJson)
            } else {
                val psiFile = PsiManager.getInstance(myProject).findFile(nxJson)
                if (psiFile == null) {
                    throw JsbtTaskFetchException.newGenericException(nxJson, "Cannot find PSI file")
                } else {
                    return@compute CachedValuesManager.getCachedValue(psiFile, NxService.STRUCTURE_KEY) {
                        val value: NxFileStructure
                        value = try {
                            // TODO build tasks
                            //NpmScriptsUtil.listTasks(myProject, nxJson)
                            listTasks(project, nxJson)
                            //NxFileStructure(nxJson)
                        } catch (var5: JsbtTaskFetchException) {
                            NxFileStructure(nxJson)
                        }
                        CachedValueProvider.Result.create(value, *arrayOf<Any>(psiFile))
                    }
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
        )//NxRunSettings.Builder(nxRunConfiguration.runSettings).setGulpfilePath(structure.buildfile.path).setTasks(taskSet.taskNames).build()
        NxRunConfigurationProducer.setupConfigurationFromSettings(nxRunConfiguration, merged)
    }

    override fun showTaskListingSettingsDialog(contextNxfile: VirtualFile?): Boolean {
        return false
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
