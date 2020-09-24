package com.github.iguissouma.nxconsole.cli

import com.github.iguissouma.nxconsole.NxBundle
import com.github.iguissouma.nxconsole.NxIcons
import com.intellij.execution.filters.Filter
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.javascript.CreateRunConfigurationUtil
import com.intellij.javascript.nodejs.packages.NodePackageUtil
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.lang.javascript.boilerplate.NpmPackageProjectGenerator
import com.intellij.lang.javascript.boilerplate.NpxPackageDescriptor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.PathUtil
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import javax.swing.Icon

class NxCliProjectGenerator : NpmPackageProjectGenerator() {
    private val LOG = Logger.getInstance(NxCliProjectGenerator::class.java)

    private val PACKAGE_NAME = "create-nx-workspace"
    private val COMMAND = "create-nx-workspace"

    override fun getName(): String {
        return NxBundle.message("nx.project.generator.name")
    }

    override fun getDescription(): String {
        return NxBundle.message("nx.project.generator.description")
    }

    override fun getIcon(): Icon {
        return NxIcons.NRWL_ICON
    }

    override fun customizeModule(baseDir: VirtualFile, entry: ContentEntry) {}

    override fun generatorArgs(project: Project, baseDir: VirtualFile): Array<String> {
        return arrayOf(baseDir.name)
    }

    override fun generateInTemp(): Boolean {
        return true
    }

    override fun filters(project: Project, baseDir: VirtualFile): Array<Filter> {
        return Filter.EMPTY_ARRAY
    }

    override fun executable(pkg: NodePackage): String {
        return pkg.systemDependentPath + File.separator + "bin" + File.separator + "index.js"
    }

    override fun packageName(): String {
        return PACKAGE_NAME
    }

    override fun presentablePackageName(): String {
        return NxBundle.message("nx.project.generator.presentable.package.name")
    }

    override fun getNpxCommands(): List<NpxPackageDescriptor.NpxCommand> {
        return listOf(NpxPackageDescriptor.NpxCommand(PACKAGE_NAME, COMMAND))
    }

    override fun validateProjectPath(path: String): String? {
        val error = NodePackageUtil.validateNpmPackageName(PathUtil.getFileName(path))
        return error ?: super.validateProjectPath(path)
    }

    override fun onProcessHandlerCreated(processHandler: ProcessHandler) {
        processHandler.addProcessListener(
            object : ProcessAdapter() {
                override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
// https://github.com/vuejs/vue-cli/blob/dev/packages/%40vue/cli/lib/create.js#L43
                    if (event.text.contains("Generate project in current directory?")) {
                        event.processHandler.removeProcessListener(this)
                        val processInput = event.processHandler.processInput
                        if (processInput != null) {
                            try {
                                processInput.write("yes\n".toByteArray(StandardCharsets.UTF_8))
                                processInput.flush()
                            } catch (e: IOException) {
                                LOG.warn("Failed to write 'yes' to the Vue CLI console.", e)
                            }
                        }
                    }
                }
            }
        )
    }

    override fun onGettingSmartAfterProjectGeneration(project: Project, baseDir: VirtualFile) {
        super.onGettingSmartAfterProjectGeneration(project, baseDir)
        CreateRunConfigurationUtil.npmConfiguration(project, "start")
    }
}
