package com.github.iguissouma.nxconsole.buildTools

import com.github.iguissouma.nxconsole.buildTools.NxJsonUtil.isProjectProperty
import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons.RunConfigurations.TestState
import com.intellij.json.JsonElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement

class NxRunLineMarkerContributor : RunLineMarkerContributor() {

    override fun getInfo(element: PsiElement): Info? {
        return if (element !is LeafPsiElement) {
            null
        } else {
            if (element.elementType !== JsonElementTypes.DOUBLE_QUOTED_STRING) {
                null
            } else {
                val property = NxJsonUtil.findContainingPropertyInsideNxJsonFile(element)
                if (property != null && property.nameElement === element.parent) {
                    if (!isProjectProperty(property)) null else Info(TestState.Run, ExecutorAction.getActions()) {
                            psiElement: PsiElement? -> "Run Tasks"
                    }
                } else {
                    null
                }
            }
        }
    }
}
