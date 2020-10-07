package com.github.iguissouma.nxconsole.scopes

import com.intellij.psi.search.scope.packageSet.CustomScopesProviderEx
import com.intellij.psi.search.scope.packageSet.NamedScope

class NxProjectsScopesProvider : CustomScopesProviderEx() {

    companion object {
        val INSTANCE = NxProjectFilesScope()
    }

    override fun getCustomScopes(): List<NamedScope> {
        return listOf(INSTANCE)
    }
}
