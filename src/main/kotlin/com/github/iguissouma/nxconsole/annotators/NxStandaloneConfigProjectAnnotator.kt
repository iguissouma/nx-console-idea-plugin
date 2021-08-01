package com.github.iguissouma.nxconsole.annotators

import com.github.iguissouma.nxconsole.NxIcons
import com.github.iguissouma.nxconsole.cli.config.NxConfigProvider
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import java.nio.file.Paths

class NxStandaloneConfigProjectAnnotator : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {

        if (element.containingFile.virtualFile.name !in listOf("workspace.json", "angular.json")) return

        val parent = element.parent
        if (element !is JsonStringLiteral) {
            return
        }

        if (parent !is JsonProperty) return

        if ((parent.parent !is JsonObject)) {
            return
        }

        if ((parent.parent.parent !is JsonProperty)) {
            return
        }

        if ((parent.parent.parent as JsonProperty).name != "projects") {
            return
        }

        if (parent.value !is JsonStringLiteral) {
            return
        }
        if (!element.isPropertyName) {
            return
        }

        val ppath = (parent.value as JsonStringLiteral).value
        val project = element.project
        val nxConfig = NxConfigProvider.getNxConfig(project, project.baseDir) ?: return
        val path = nxConfig.angularJsonFile.parent.path + "/" + ppath + "/" + "project.json"
        val virtualFile = VirtualFileManager.getInstance().findFileByNioPath(Paths.get(path)) ?: return

        val findFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return
        val builder: NavigationGutterIconBuilder<PsiElement> = NavigationGutterIconBuilder
            .create(if (ppath.startsWith("libs")) NxIcons.NX_LIB_FOLDER else NxIcons.NX_APP_FOLDER)
            .setTarget(findFile.firstChild)
            .setTooltipText("Navigate to project config file")
        result.add(builder.createLineMarkerInfo(element))
    }
}
