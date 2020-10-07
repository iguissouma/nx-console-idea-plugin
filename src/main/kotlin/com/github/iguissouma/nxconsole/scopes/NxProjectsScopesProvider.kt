package com.github.iguissouma.nxconsole.scopes

import com.intellij.psi.search.scope.packageSet.CustomScopesProviderEx
import com.intellij.psi.search.scope.packageSet.NamedScope

class NxProjectsScopesProvider : CustomScopesProviderEx() {

    companion object {
        val NxProjectFilesScope = NxProjectFilesScope()
        val NxAppsFilesScope = NxAppsFilesScope()
        val NxLibsScope = NxLibsFilesScope()
    }

    override fun getCustomScopes(): List<NamedScope> {
        return listOf(NxProjectFilesScope, NxAppsFilesScope, NxLibsScope)
    }
}
