package com.github.iguissouma.nxconsole.buildTools.rc

import com.github.iguissouma.nxconsole.NxIcons
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeUtil.findConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.SimpleConfigurationType
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue

class NxConfigurationType :
    SimpleConfigurationType("js.build_tools.nx", "Nx", null, NotNullLazyValue.createValue { NxIcons.NRWL_ICON }),
    DumbAware {
    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return NxRunConfiguration(project, this, "Nx")
    }

    override fun getTag(): String {
        return "nx"
    }

    companion object {
        fun getInstance(): NxConfigurationType {
            return findConfigurationType(NxConfigurationType::class.java)
        }

        fun getFactory(): ConfigurationFactory {
            val type = getInstance()
            return type.configurationFactories[0]
        }
    }


}
