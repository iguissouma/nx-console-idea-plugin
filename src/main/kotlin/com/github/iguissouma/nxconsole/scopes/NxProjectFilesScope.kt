package com.github.iguissouma.nxconsole.scopes

import com.github.iguissouma.nxconsole.NxIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.scope.packageSet.FilteredPackageSet
import com.intellij.psi.search.scope.packageSet.NamedScope

class NxProjectFilesScope : NamedScope(NAME, NxIcons.NRWL_ICON, NxProjectPackageSet()) {

    companion object {
        val NAME = "Nx Apps&Libs"
    }
}

private class NxProjectPackageSet : FilteredPackageSet(NxProjectFilesScope.NAME, 1) {

    override fun contains(file: VirtualFile, project: Project): Boolean {
        return (file.path.contains("apps") || file.path.contains("libs")) &&
                !file.path.contains("node_modules")
    }
}
