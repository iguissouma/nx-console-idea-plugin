package com.github.iguissouma.nxconsole.schema

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.SchemaType

class NxJsonSchemaProvider(val project: Project) : JsonSchemaFileProvider {
    override fun isAvailable(file: VirtualFile): Boolean {
        return file.name == "nx.json"
    }

    override fun getName(): String = "nx.json"

    override fun getSchemaFile(): VirtualFile? {
        val resource = NxJsonSchemaProvider::class.java.getResource("/schemas/nx-schema.json") ?: return null
        return VirtualFileManager.getInstance().findFileByUrl(VfsUtil.convertFromUrl(resource))
    }

    override fun getSchemaType(): SchemaType = SchemaType.embeddedSchema
}
