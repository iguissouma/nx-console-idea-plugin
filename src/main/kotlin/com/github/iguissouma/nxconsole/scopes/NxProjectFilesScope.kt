package com.github.iguissouma.nxconsole.scopes

import com.github.iguissouma.nxconsole.NxIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.scope.packageSet.FilteredPackageSet
import com.intellij.psi.search.scope.packageSet.NamedScope
import com.intellij.ui.IconManager

class NxProjectFilesScope : NamedScope(NAME, NxIcons.NRWL_ICON, object : FilteredPackageSet(NAME) {
    override fun contains(file: VirtualFile, project: Project): Boolean {
        return (file.path.contains("apps") || file.path.contains("libs")) &&
                !file.path.contains("node_modules")
    }

}) {

    companion object {
        val NAME = "Nx Apps&Libs"
    }
}

private class NxProjectPackageSet(val type: String) : FilteredPackageSet("Nx ${type.capitalize()}", 1) {

    override fun contains(file: VirtualFile, project: Project): Boolean {
        return file.path.contains(type) &&
                !file.path.contains("node_modules")
    }
}


class NxAppsFilesScope : NamedScope(
    NAME,
    IconManager.getInstance().createOffsetIcon(NxIcons.NX_APPS_FOLDER), NxProjectPackageSet("apps")
) {

    companion object {
        val NAME = "Nx Apps"
    }
}

class NxLibsFilesScope : NamedScope(
    NAME,
    IconManager.getInstance().createOffsetIcon(NxIcons.NX_LIBS_FOLDER), NxProjectPackageSet("libs")
) {

    companion object {
        val NAME = "Nx Libs"
    }
}
