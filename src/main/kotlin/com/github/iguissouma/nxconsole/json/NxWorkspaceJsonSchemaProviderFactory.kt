package com.github.iguissouma.nxconsole.json

import com.intellij.openapi.project.Project
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory

class NxWorkspaceJsonSchemaProviderFactory : JsonSchemaProviderFactory {
    override fun getProviders(project: Project): MutableList<JsonSchemaFileProvider> {
        return mutableListOf(NxWorkspaceJsonSchemaV1Provider(project), NxWorkspaceJsonSchemaV2Provider(project))
    }
}
