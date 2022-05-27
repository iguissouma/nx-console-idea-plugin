package com.github.iguissouma.nxconsole

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object NxIcons {

    val NRWL_ICON = load("/icons/nrwl.svg")
    val NX_PLUGINS_ICON = load("/icons/nx_plugins.svg")
    val NX_APP_FOLDER = load("/icons/appFolder.svg")
    val NX_APP_FOLDER_EXCLUDED = load("/icons/appFolderExcluded.svg")
    val NX_APPS_FOLDER = load("/icons/appsFolder.svg")
    val NX_LIB_FOLDER = load("/icons/libForlder.svg")
    val NX_LIB_FOLDER_EXCLUDED = load("/icons/libForlderExcluded.svg")
    val NX_LIBS_FOLDER = load("/icons/libsForlder.svg")
    val NX_CONSOLE_ICON = load("/icons/nx_console.svg")
    val TREE_VIEW_ICON = load("/icons/tree_view.svg")
    val ANGULAR = load("/icons/angular2.svg")

    private fun load(path: String): Icon {
        return IconLoader.getIcon(path, NxIcons::class.java.classLoader)
    }
}
