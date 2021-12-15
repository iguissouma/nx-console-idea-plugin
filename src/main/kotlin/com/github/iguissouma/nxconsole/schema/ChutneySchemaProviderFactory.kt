package com.github.iguissouma.nxconsole.schema

import com.intellij.openapi.project.Project
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory

class NxSchemaProviderFactory : JsonSchemaProviderFactory {

    override fun getProviders(project: Project): List<JsonSchemaFileProvider> {
        if (project.isDisposed) return emptyList()
        return listOf(NxJsonSchemaProvider(project))
    }
}
