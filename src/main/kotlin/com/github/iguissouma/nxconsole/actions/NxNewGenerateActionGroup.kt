package com.github.iguissouma.nxconsole.actions

import com.github.iguissouma.nxconsole.NxIcons
import com.github.iguissouma.nxconsole.schematics.NxCliSchematicsRegistryService
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.options.advanced.AdvancedSettings

class NxNewGenerateActionGroup : ActionGroup(
    "Nx Generate (Ui)...",
    "Nx generate (ui)",
    NxIcons.NRWL_ICON
) {

    override fun getChildren(event: AnActionEvent?): Array<AnAction> {
        val virtualFile = event?.getData(LangDataKeys.VIRTUAL_FILE) ?: return emptyArray()
        val project = event.project ?: return emptyArray()
        val nxExcludeGenerators: List<Regex> = AdvancedSettings.getString("nx.exclude.generators")
            .takeIf { it.isNotBlank() }?.split(";")?.map { wildcardToRegex(it) }
            ?: emptyList()
        return NxCliSchematicsRegistryService.getInstance().getSchematics(project, project.baseDir)
            .filterNot { s ->
                nxExcludeGenerators.any { it.matches(s.name!!) }
            }
            .map { NxNewGenerateAction(it, virtualFile, it.name, it.description, NxIcons.NRWL_ICON) }
            .toTypedArray()
    }

}

fun wildcardToRegex(wildcardString: String): Regex {
    // The 12 is arbitrary, you may adjust it to fit your needs depending
    // on how many special characters you expect in a single pattern.
    val sb = StringBuilder(wildcardString.length + 12)
    sb.append('^')
    for (i in 0 until wildcardString.length) {
        val c = wildcardString[i]
        if (c == '*') {
            sb.append(".*")
        } else if (c == '?') {
            sb.append('.')
        } else if ("\\.[]{}()+-^$|".indexOf(c) >= 0) {
            sb.append('\\')
            sb.append(c)
        } else {
            sb.append(c)
        }
    }
    sb.append('$')
    return sb.toString().toRegex()
}
