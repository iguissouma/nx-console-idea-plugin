package com.github.iguissouma.nxconsole.jest

import com.intellij.javascript.jest.JestPackageProvider
import com.intellij.javascript.jest.JestPkgInfo
import com.intellij.javascript.jest.JestRunSettings
import com.intellij.javascript.jest.JestUtil
import com.intellij.openapi.util.io.FileUtil
import java.io.File

class NxJestPackageProvider : JestPackageProvider {

    override fun getPackages(): List<JestPkgInfo> {
        return listOf(
            object : JestPkgInfo("@nrwl/cli", "bin/nx", false, java.util.List.of()) {
                override fun getInitialTestRunnerCliOptions(runSettings: JestRunSettings): List<String> {
                    val contextAngularProjectName = findContextNxProject(runSettings)
                    return if (contextAngularProjectName != null) {
                        listOf("test", contextAngularProjectName)
                    } else {
                        listOf("test")
                    }
                }
            })
    }

    private fun findContextNxProject(runSettings: JestRunSettings): String? {
        val workingDir = runSettings.workingDirSystemDependentPath
        if (FileUtil.isAbsolute(workingDir)) {
            val config = NxCliConfig.findProjectConfig(File(workingDir))
            if (config != null) {
                val contextFile = JestUtil.findContextFile(runSettings)
                if (contextFile != null) {
                    return config.getProjectContainingFile(contextFile)
                }
            }
        }
        return null
    }
}
