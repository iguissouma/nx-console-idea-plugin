package com.github.iguissouma.nxconsole.buildTools

import com.github.iguissouma.nxconsole.buildTools.NxJsonUtil.isNxJsonFile
import com.intellij.lang.javascript.buildTools.base.JsbtFileManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.SmartList
import com.intellij.util.xmlb.annotations.XCollection

@State(
    name = "JsBuildToolNxFileManager",
    storages = [
        Storage("\$CACHE_FILE$"),
        Storage(value = "\$WORKSPACE_FILE$", deprecated = true)
    ]
)
class NxFileManager(val project: Project) :
    JsbtFileManager(project, NxService.getInstance(project)),
    PersistentStateComponent<NxFileManager.State> {

    val LOCK = Object()
    var myNxJsonFiles: Set<VirtualFile> = setOf()

    companion object {
        fun getInstance(project: Project): NxFileManager {
            return ServiceManager.getService(project, NxFileManager::class.java) as NxFileManager
        }
    }

    override fun getState(): State {
        return State(this.getValidNxJsonFiles().map { it.path }.sorted().toList())
    }

    fun getValidNxJsonFiles(): Set<VirtualFile> {
        var nxJsonFiles: Set<VirtualFile>?
        synchronized(this.LOCK) { nxJsonFiles = this.myNxJsonFiles }
        return this.myNxJsonFiles.toSet()
        // return this.filter(packageJsonFiles, false)
    }

    override fun loadState(state: State) {

        val files: MutableList<VirtualFile> = SmartList()
        val var3: Iterator<*> = state.myPaths.iterator()

        while (var3.hasNext()) {
            val path = var3.next() as String
            val file = LocalFileSystem.getInstance().findFileByPath(path)
            if (file != null && file.isValid && isNxJsonFile(file)) {
                files.add(file)
            }
        }

        synchronized(LOCK) { this.myNxJsonFiles = files.toSet() }
    }

    class State {
        @XCollection(propertyElementName = "nxJsonPaths", elementName = "path")
        var myPaths: MutableList<String?>

        constructor() {
            myPaths = SmartList<String?>()
        }

        constructor(paths: List<String?>) {
            myPaths = SmartList<String?>()
            myPaths.addAll(paths)
        }
    }
}
