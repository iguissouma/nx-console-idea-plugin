package com.github.iguissouma.nxconsole.buildTools

import com.github.iguissouma.nxconsole.buildTools.rc.NxConfigurationType
import com.github.iguissouma.nxconsole.buildTools.rc.NxRunConfiguration
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement

class NxRunConfigurationProducer : LazyRunConfigurationProducer<NxRunConfiguration>() {


    companion object {
        fun setupConfigurationFromSettings(configuration: NxRunConfiguration, runSettings: NxRunSettings) {
            configuration.runSettings = runSettings
            configuration.setName(buildName(runSettings.tasks))
        }

        fun buildName(tasks: List<String>): String? {
            return if (tasks.isEmpty()) "default" else StringUtil.join(tasks, ", ")

        }
    }

    override fun getConfigurationFactory(): ConfigurationFactory {
        return ConfigurationTypeUtil.findConfigurationType(NxConfigurationType::class.java)
    }

    override fun isConfigurationFromContext(configuration: NxRunConfiguration, context: ConfigurationContext): Boolean {
        return false
    }

    override fun setupConfigurationFromContext(
      configuration: NxRunConfiguration,
      context: ConfigurationContext,
      sourceElement: Ref<PsiElement>
    ): Boolean {
        return false
    }

}
