package com.github.iguissouma.nxconsole.buildTools

import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.lang.javascript.buildTools.base.JsbtTaskFetchException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.util.ObjectUtils
import org.jetbrains.annotations.Contract

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
