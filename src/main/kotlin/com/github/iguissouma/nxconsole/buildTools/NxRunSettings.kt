package com.github.iguissouma.nxconsole.buildTools

import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef
import com.intellij.openapi.util.JDOMExternalizerUtil
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import org.jdom.Element

class NxRunSettings(
    val interpreterRef: NodeJsInterpreterRef = NodeJsInterpreterRef.createProjectRef(),
    var nxFilePath: String? = null,
    var tasks: List<String> = emptyList()
) {

    val nxFileSystemIndependentPath: String?
        get() = nxFilePath?.let { FileUtil.toSystemIndependentName(it) }

    fun writeToXml(parent: Element) {
        JDOMExternalizerUtil.writeCustomField(parent, "node-interpreter", this.interpreterRef.referenceName)
        if (this.nxFilePath?.isNotEmpty() == true) {
            JDOMExternalizerUtil.writeCustomField(
                parent,
                "nxfile",
                FileUtil.toSystemIndependentName(this.nxFilePath!!)
            )
        }
        this.writeTasks(parent)
        /*if (!this.myArguments.isEmpty()) {
            JDOMExternalizerUtil.writeCustomField(parent, "arguments", this.myArguments)
        }*/
    }

    private fun writeTasks(parent: Element) {
        if (this.tasks.isNotEmpty()) {
            val tasksElement = Element("tasks")
            JDOMExternalizerUtil.addChildrenWithValueAttribute(tasksElement, "task", this.tasks)
            parent.addContent(tasksElement)
        }
    }

    fun readFromXml(parent: Element?): NxRunSettings? {
        if (parent == null) {
            return null
        }
        val interpreterRefName = JDOMExternalizerUtil.readCustomField(parent, "node-interpreter")
        val interpreterRef = createInterpreterRef(interpreterRefName)
        return NxRunSettings(
            interpreterRef = interpreterRef,
            nxFilePath = StringUtil.notNullize(JDOMExternalizerUtil.readCustomField(parent, "nxfile")),
            tasks = readTasks(parent)
        )
    }

    private fun createInterpreterRef(interpreterRefName: String?): NodeJsInterpreterRef {
        return if (interpreterRefName != null) NodeJsInterpreterRef.create(interpreterRefName) else NodeJsInterpreterRef.createProjectRef()
    }

    private fun readTasks(parent: Element): List<String> {
        val tasksElement = parent.getChild("tasks")
        return if (tasksElement != null) {
            JDOMExternalizerUtil.getChildrenValueAttributes(tasksElement, "task")
        } else {
            return emptyList()
        }
    }
}
