package com.github.iguissouma.nxconsole.buildTools

import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.lang.javascript.buildTools.base.JsbtTaskFetchException
import com.intellij.lang.javascript.buildTools.npm.PackageJsonUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ObjectUtils
import org.jetbrains.annotations.Contract

object NxJsonUtil {

    val LOG = Logger.getInstance(NxService::class.java)

    @Contract("null -> false")
    fun isNxJsonFile(file: VirtualFile?): Boolean {
        return file != null && !file.isDirectory && StringUtil.equals("nx.json", file.nameSequence)
    }

    @Contract("null -> null")
    fun findChildNxJsonFile(dir: VirtualFile?): VirtualFile? {
        if (dir != null && dir.isValid) {
            val nxJson = dir.findChild("nx.json")
            if (nxJson != null && nxJson.isValid && !nxJson.isDirectory) {
                return nxJson
            }
        }
        return null
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
            val angularJsonFile = findChildAngularJsonFile(nxJson.parent)
            val nxProjectsProperty = findProjectsProperty(project, nxJson)
            if (angularJsonFile == null) {
                structure
            } else {
                val angularProjectsProperty = angularJsonFile.let { findProjectsProperty(project, it) }
                val projectsFromAngular = ObjectUtils.tryCast(angularProjectsProperty?.value, JsonObject::class.java)
                val projectsFromNx = ObjectUtils.tryCast(nxProjectsProperty?.value, JsonObject::class.java)
                if (projectsFromNx != null && projectsFromAngular != null) {
                    val propertyList = projectsFromNx.propertyList
                    structure.myNxProjectsTask = propertyList.map { it.name }
                        .map { it to projectsFromAngular.findProperty(it) }
                        .map { it.first to (it.second?.value as? JsonObject)?.findProperty("architect") }
                        .map {
                            it.first to ((it.second?.value as? JsonObject)?.propertyList?.map { property ->
                                NxTask(structure, property.name)
                            }?.toList() ?: listOf())
                        }.toMap()
                }
                val listOf = listOf("Generate & Run Target", "Common Nx Commands", "Projects")
                val scripts = listOf.map { NxTask(structure, it) }.toList()
                structure.setScripts(scripts)
                structure
            }
        }
    }

    fun invalidFile(packageJson: VirtualFile): JsbtTaskFetchException {
        return JsbtTaskFetchException.newBuildfileSyntaxError(packageJson)
    }

    fun findProjectsProperty(project: Project, packageJson: VirtualFile): JsonProperty? {
        val psiManager = PsiManagerEx.getInstanceEx(project)
        val psiFile = psiManager.fileManager.findFile(packageJson)
        return if (psiFile == null) {
            throw invalidFile(packageJson)
        } else {
            findProjectsProperty(ObjectUtils.tryCast(psiFile, JsonFile::class.java))
        }
    }

    fun findProjectsProperty(jsonFile: JsonFile?): JsonProperty? {
        return ObjectUtils.tryCast(jsonFile?.topLevelValue, JsonObject::class.java)?.findProperty("projects")
    }

    fun findProjectProperty(project: Project, packageJson: VirtualFile, scriptName: String): JsonProperty? {
        return if (!project.isDisposed && packageJson.isValid) {
            val scriptsProperty: JsonProperty?
            scriptsProperty = try {
                findProjectsProperty(project, packageJson)
            } catch (var5: JsbtTaskFetchException) {
                LOG.info("Cannot fetch 'projects' from " + packageJson.path)
                return null
            }
            if (scriptsProperty == null) {
                null
            } else {
                ObjectUtils.tryCast(
                    scriptsProperty.value,
                    JsonObject::class.java
                )?.findProperty(scriptName)
            }
        } else {
            null
        }
    }

    @Contract("null -> null")
    fun findChildAngularJsonFile(dir: VirtualFile?): VirtualFile? {
        if (dir != null && dir.isValid) {
            val angularJson = dir.findChild("angular.json")
            if (angularJson != null && angularJson.isValid && !angularJson.isDirectory) {
                return angularJson
            }
        }
        return null
    }

    fun findContainingPropertyInsideNxJsonFile(element: PsiElement): JsonProperty? {
        return if (isInsideNxJsonFile(element)) PsiTreeUtil.getParentOfType(
            element,
            JsonProperty::class.java,
            false
        ) else null
    }

    fun findContainingPropertyInsideAngularJsonFile(element: PsiElement): JsonProperty? {
        return if (isInsideAngularJsonFile(element)) PsiTreeUtil.getParentOfType(
            element,
            JsonProperty::class.java,
            false
        ) else null
    }

    fun isProjectProperty(property: JsonProperty?): Boolean {
        val projectsProperty = PsiTreeUtil.getParentOfType(
            property,
            JsonProperty::class.java,
            true
        )
        return projectsProperty != null && "projects" == projectsProperty.name && PackageJsonUtil.isTopLevelProperty(
            projectsProperty
        )
    }

    fun isArchitectProperty(property: JsonProperty?): Boolean {
        val architectProperty = PsiTreeUtil.getParentOfType(
            property,
            JsonProperty::class.java,
            true
        )
        return architectProperty != null && "architect" == architectProperty.name
    }

    fun isInsideNxJsonFile(element: PsiElement): Boolean {
        return getContainingNxJsonFile(element) != null
    }

    fun isInsideAngularJsonFile(element: PsiElement): Boolean {
        return getContainingAngularJsonFile(element) != null
    }

    fun getContainingNxJsonFile(element: PsiElement): JsonFile? {
        val file = element.containingFile
        return if (isNxJsonFile(file)) file as JsonFile else null
    }

    fun getContainingAngularJsonFile(element: PsiElement): JsonFile? {
        val file = element.containingFile
        return if (isAngularJsonFile(file)) file as JsonFile else null
    }

    @Contract("null -> false")
    fun isNxJsonFile(file: PsiFile?): Boolean {
        return file is JsonFile && "nx.json" == file.getName()
    }

    @Contract("null -> false")
    fun isAngularJsonFile(file: PsiFile?): Boolean {
        return file is JsonFile && "angular.json" == file.getName()
    }

    fun findContainingTopLevelProperty(element: PsiElement?): JsonProperty? {
        return if (element != null && isInsideNxJsonFile(element)) {
            var property: JsonProperty?
            property = PsiTreeUtil.getParentOfType(
                element,
                JsonProperty::class.java,
                false
            )
            while (property != null && !isTopLevelProperty(property)) {
                property = PsiTreeUtil.getParentOfType(
                    property,
                    JsonProperty::class.java,
                    true
                )
            }
            property
        } else {
            null
        }
    }

    fun isTopLevelProperty(property: JsonProperty): Boolean {
        val parent = property.parent
        return parent != null && parent.parent is JsonFile
    }
}
