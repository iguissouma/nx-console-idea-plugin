package com.github.iguissouma.nxconsole.cli

import com.github.iguissouma.nxconsole.NxBundle
import com.github.iguissouma.nxconsole.NxIcons
import com.intellij.execution.filters.Filter
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.ide.util.projectWizard.SettingsStep
import com.intellij.javascript.CreateRunConfigurationUtil
import com.intellij.javascript.nodejs.packages.NodePackageUtil
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.lang.javascript.boilerplate.NpmPackageProjectGenerator
import com.intellij.lang.javascript.boilerplate.NpxPackageDescriptor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ProjectGeneratorPeer
import com.intellij.util.PathUtil
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.io.File
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField

class NxPluginsCliProjectGenerator : NpmPackageProjectGenerator() {
    private val LOG = Logger.getInstance(NxPluginsCliProjectGenerator::class.java)

    private val PACKAGE_NAME = "create-nx-plugin"
    private val COMMAND = "create-nx-plugin"
    private val NX_PLUGIN_NAME_KEY = Key.create<String>("nx.project.generator.plugin.name")

    override fun getName(): String {
        return NxBundle.message("nx.plugins.project.generator.name")
    }

    override fun getDescription(): String {
        return NxBundle.message("nx.plugins.project.generator.description")
    }

    override fun getIcon(): Icon {
        return NxIcons.NX_PLUGINS_ICON
    }

    override fun customizeModule(baseDir: VirtualFile, entry: ContentEntry) {}

    override fun generatorArgs(project: Project, baseDir: VirtualFile, settings: Settings?): Array<String> {
        val pluginName = settings?.getUserData(NX_PLUGIN_NAME_KEY) ?: "my-plugin"
        return arrayOf(baseDir.name, "--pluginName", pluginName)
    }

    override fun workingDir(settings: Settings?, baseDir: VirtualFile): File {
        return VfsUtilCore.virtualToIoFile(baseDir).parentFile
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
        return NxBundle.message("nx.plugins.project.generator.presentable.package.name")
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
                    // nothing to customise for now
                }
            }
        )
    }

    override fun createPeer(): ProjectGeneratorPeer<Settings> {
        val pluginNameTextField = JTextField()

        return object : NpmPackageGeneratorPeer() {
            override fun createPanel(): JPanel {
                val panel = super.createPanel()
                val component = LabeledComponent.create(
                    pluginNameTextField,
                    NxBundle.message("nx.plugins.project.generator.plugin.name")
                )
                component.anchor = panel.getComponent(0) as JComponent
                component.labelLocation = BorderLayout.WEST
                panel.add(component)
                return panel
            }

            override fun buildUI(settingsStep: SettingsStep) {
                super.buildUI(settingsStep)
                settingsStep.addSettingsField(
                    UIUtil.replaceMnemonicAmpersand(
                        NxBundle.message("nx.plugins.project.generator.plugin.name")
                    ),
                    pluginNameTextField
                )
            }

            override fun getSettings(): Settings {
                val settings = super.getSettings()
                settings.putUserData(NX_PLUGIN_NAME_KEY, pluginNameTextField.text)
                return settings
            }
        }
    }

    override fun onGettingSmartAfterProjectGeneration(project: Project, baseDir: VirtualFile) {
        super.onGettingSmartAfterProjectGeneration(project, baseDir)
        CreateRunConfigurationUtil.npmConfiguration(project, "start")
    }
}
