package com.github.iguissouma.nxconsole.buildTools

import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.json.JsonElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement

class NxAngularRunLineMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        return if (element !is LeafPsiElement) {
            null
        } else {
            if (element.elementType !== JsonElementTypes.DOUBLE_QUOTED_STRING) {
                null
            } else {
                val property = NxJsonUtil.findContainingPropertyInsideAngularJsonFile(element)
                if (property != null && property.nameElement === element.parent) {
                    if (!NxJsonUtil.isArchitectProperty(property)) null else Info(AllIcons.RunConfigurations.TestState.Run, ExecutorAction.getActions()) {
                            psiElement: PsiElement? -> "${psiElement?.text}"
                    }
                } else {
                    null
                }
            }
        }
    }
}
