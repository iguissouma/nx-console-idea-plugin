package com.github.iguissouma.nxconsole.plugins

import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.javascript.nodejs.util.NodePackageDescriptor
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.JDOMExternalizerUtil
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jdom.Element

@State(name = "NxDevToolsSettings", storages = [Storage("\$WORKSPACE_FILE$")])
class NxDevToolsSettingsManager(val myProject: Project) : PersistentStateComponent<Element> {

    var mySettings: NxDevToolsSettings? = createSettings(null, null, null)

    override fun getState(): Element? {
        val settings: NxDevToolsSettings? = mySettings
        return if (settings == null) {
            null
        } else {
            val root = Element("nx-dev-tools-settings")
            JDOMExternalizerUtil.writeCustomField(root, TAG_NODE_INTERPRETER, settings.myInterpreterRef?.referenceName)
            JDOMExternalizerUtil.writeCustomField(root, TAG_NX_PACKAGE, settings.myNxPackage?.systemIndependentPath)
            JDOMExternalizerUtil.writeCustomField(root, TAG_NX_JSON, settings.myNxJsonPath)
            root
        }
    }

    override fun loadState(state: Element) {
        mySettings = createSettings(
            JDOMExternalizerUtil.readCustomField(state, TAG_NODE_INTERPRETER),
            JDOMExternalizerUtil.readCustomField(state, TAG_NX_PACKAGE),
            JDOMExternalizerUtil.readCustomField(state, TAG_NX_JSON)
        )
    }


    private fun createSettings(
        nodeInterpreterRefName: String?,
        nxPkgPath: String?,
        nxJsonPath: String?
    ): NxDevToolsSettings {
        val interpreterRef = NodeJsInterpreterRef.create(nodeInterpreterRefName)
        val nxPkg: NodePackage
        if (nxPkgPath != null) {
            nxPkg = NodePackage(nxPkgPath)
        } else {
            nxPkg = guessNxPackage(interpreterRef)
        }
        return NxDevToolsSettings(myProject).apply {
            this.myInterpreterRef = interpreterRef
            this.myNxJsonPath = nxJsonPath ?: guessNxConfig()
            this.myNxPackage = nxPkg
        }
    }


    private fun guessNxConfig(): String {
        val baseDir = myProject.baseDir
        if (baseDir != null && baseDir.isValid) {
            val nxJson = baseDir.findChild("nx.json")
            if (nxJson != null && nxJson.isValid && !nxJson.isDirectory) {
                return FileUtil.toSystemDependentName(nxJson.path)
            }
        }
        return ""
    }


    private fun guessNxPackage(interpreterRef: NodeJsInterpreterRef): NodePackage {
        val interpreter = interpreterRef.resolve(myProject)
        return PKG_DESCRIPTOR.findFirstDirectDependencyPackage(
            myProject,
            interpreter,
            null as VirtualFile?
        )
    }

    companion object {
        const val NX_PACKAGE_NAME = "nx"

        val PKG_DESCRIPTOR = NodePackageDescriptor("nx")
        const val TAG_NODE_INTERPRETER = "node-interpreter"
        const val TAG_NX_PACKAGE = "nx-package"
        const val TAG_NX_JSON = "nx.json"

        fun getInstance(project: Project): NxDevToolsSettingsManager {
            return ServiceManager.getService(
                project,
                NxDevToolsSettingsManager::class.java
            )
        }
    }
}
