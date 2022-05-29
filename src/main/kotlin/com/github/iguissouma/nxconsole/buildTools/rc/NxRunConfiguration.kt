package com.github.iguissouma.nxconsole.buildTools.rc

import com.github.iguissouma.nxconsole.buildTools.NxRunSettings
import com.github.iguissouma.nxconsole.buildTools.NxService
import com.github.iguissouma.nxconsole.cli.config.NxConfigProvider
import com.github.iguissouma.nxconsole.cli.config.WorkspaceType
import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.impl.BeforeRunTaskAwareConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.javascript.JSRunProfileWithCompileBeforeLaunchOption
import com.intellij.javascript.nodejs.debug.NodeDebugRunConfiguration
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreter
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import org.jdom.Element

class NxRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String
) : LocatableConfigurationBase<Any>(project, factory, name),
    NodeDebugRunConfiguration,
    JSRunProfileWithCompileBeforeLaunchOption,
    BeforeRunTaskAwareConfiguration {

    var runSettings: NxRunSettings = NxRunSettings()

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
        val nxPackage = getNxPackage(this.project, this.runSettings)
        return nxPackage?.let { NxRunProfileState(environment, this.runSettings, it) }
    }

    private fun getNxPackage(project: Project, runSettings: NxRunSettings): NodePackage? {
        val p = if (NxConfigProvider.getNxWorkspaceType(project, project.baseDir) == WorkspaceType.ANGULAR) "@angular/cli" else "@nrwl/cli"
        return NodePackage.findDefaultPackage(project, p, NodeJsInterpreterRef.createProjectRef().resolve(project))
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return NxRunConfigurationEditor(this.project)
    }

    override fun onNewConfigurationCreated() {
        val runSettings: NxRunSettings = this.runSettings
        if (StringUtil.isEmptyOrSpaces(runSettings.nxFilePath)) {
            val nxfile = NxService.getInstance(this.project).detectFirstBuildfileInContentRoots(false)
            if (nxfile != null) {
                this.runSettings.apply {
                    nxFilePath = nxfile.path
                }
            }
        }
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        val readFromXml = runSettings.readFromXml(element)
        if (readFromXml != null) {
            this.runSettings.apply {
                nxFilePath = readFromXml.nxFileSystemIndependentPath
                tasks = readFromXml.tasks
                arguments = readFromXml.arguments
                envData = readFromXml.envData
            }
        }
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        runSettings.writeToXml(element)
    }
    override fun getInterpreter(): NodeJsInterpreter? {
        return this.runSettings.interpreterRef.resolve(this.project)
    }

    override fun useRunExecutor(): Boolean {
        return true
    }
}
