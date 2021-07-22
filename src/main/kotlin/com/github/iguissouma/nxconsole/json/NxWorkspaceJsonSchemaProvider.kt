package com.github.iguissouma.nxconsole.json

import com.github.iguissouma.nxconsole.builders.grabCommandOutput
import com.github.iguissouma.nxconsole.cli.NxCliProjectGenerator
import com.github.iguissouma.nxconsole.cli.config.NxConfigProvider
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.javascript.nodejs.interpreter.NodeCommandLineConfigurator
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.lang.javascript.service.JSLanguageServiceUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightVirtualFile
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.SchemaType
import java.io.File


class NxWorkspaceJsonSchemaV2Provider(val project: Project) : JsonSchemaFileProvider {

    private val schemaFileLazy: VirtualFile? by lazy {
        val interpreter = NodeJsInterpreterManager.getInstance(project).interpreter ?: return@lazy null
        val configurator: NodeCommandLineConfigurator
        try {
            configurator = NodeCommandLineConfigurator.find(interpreter)
            val angularJsonFile =
                NxConfigProvider.getNxConfig(project, project.baseDir)?.angularJsonFile ?: return@lazy null
            val workspaceJsonSchemaInfo = loadWorkspaceJsonSchemaInfoJson(configurator, angularJsonFile, "2")
            val file = File(project.basePath + "/.idea/nx-workspace-schema-v2.json")
            file.writeText(workspaceJsonSchemaInfo)
            VirtualFileManager.getInstance().findFileByNioPath(file.toPath())
        } catch (e: Exception) {
            // LOG.error("Cannot load schematics", e)
            null
        }
    }

    override fun isAvailable(file: VirtualFile): Boolean {
        if (file.name !in listOf("workspace.json", "angular.json")) return false
        val psiFile = PsiManager.getInstance(project).findFile(file) as? JsonFile ?: return false
        return (psiFile.topLevelValue as JsonObject).findProperty("version")?.value?.text == "2"

    }

    override fun getName(): String {
        return "Nx Workspace 2"
    }

    override fun getSchemaFile(): VirtualFile? {
        return schemaFileLazy
    }

    override fun getSchemaType(): SchemaType {
        return SchemaType.embeddedSchema
    }
}

class NxWorkspaceJsonSchemaV1Provider(val project: Project) : JsonSchemaFileProvider {

    private val schemaFileLazy: VirtualFile? by lazy {
        val interpreter = NodeJsInterpreterManager.getInstance(project).interpreter ?: return@lazy null
        val configurator: NodeCommandLineConfigurator
        try {
            configurator = NodeCommandLineConfigurator.find(interpreter)
            val angularJsonFile =
                NxConfigProvider.getNxConfig(project, project.baseDir)?.angularJsonFile ?: return@lazy null
            val workspaceJsonSchemaInfo = loadWorkspaceJsonSchemaInfoJson(configurator, angularJsonFile, "1")
            //LightVirtualFile("nx-workspace-schema-v1.json", workspaceJsonSchemaInfo)
            val file = File(project.basePath + "/.idea/nx-workspace-schema-v1.json")
            file.writeText(workspaceJsonSchemaInfo)
            VirtualFileManager.getInstance().findFileByNioPath(file.toPath())
        } catch (e: Exception) {
            // LOG.error("Cannot load schematics", e)
            null
        }
    }

    override fun isAvailable(file: VirtualFile): Boolean {
        if (file.name !in listOf("workspace.json", "angular.json")) return false
        val psiFile = PsiManager.getInstance(project).findFile(file) as? JsonFile ?: return false
        return (psiFile.topLevelValue as JsonObject).findProperty("version")?.value?.text == "1"

    }

    override fun getName(): String {
        return "Nx Workspace 1"
    }

    override fun getSchemaFile(): VirtualFile? {
        return schemaFileLazy
    }

    override fun getSchemaType(): SchemaType {
        return SchemaType.embeddedSchema
    }
}

private fun loadWorkspaceJsonSchemaInfoJson(
    configurator: NodeCommandLineConfigurator,
    cli: VirtualFile,
    schemaVersion: String,
): String {
    val directory = JSLanguageServiceUtil.getPluginDirectory(NxCliProjectGenerator::class.java, "nxCli")
    val utilityExe = "${directory}${File.separator}runner.js"
    val commandLine = GeneralCommandLine(
        "", utilityExe, cli.path, "./workspaceJsonSchemaInfoProvider.js",
        schemaVersion
    )
    // commandLine.addParameter(builderName)
    configurator.configure(commandLine)
    return grabCommandOutput(commandLine, directory.path)
}
