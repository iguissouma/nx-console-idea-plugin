package com.github.iguissouma.nxconsole.execution

import com.github.iguissouma.nxconsole.schematics.Schematic
import com.intellij.ide.actions.runAnything.items.RunAnythingItemBase
import com.intellij.openapi.util.text.StringUtil.notNullize
import com.intellij.openapi.util.text.StringUtil.shortenTextWithEllipsis
import com.intellij.openapi.util.text.StringUtil.substringAfterLast
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.Icon
import javax.swing.JPanel

class RunAnythingNxItem(command: String, icon: Icon?) : RunAnythingItemBase(command, icon) {

    override fun createComponent(pattern: String?, isSelected: Boolean, hasFocus: Boolean): Component {
        val command = command
        val component: JPanel = super.createComponent(pattern, isSelected, hasFocus) as JPanel

        val toComplete: String = notNullize(substringAfterLast(command, " "))
        if (toComplete.startsWith("-")) {
            val option = NxGenerateRunAnythingProvider.schematics
                .firstOrNull { schematic: Schematic -> command.split(" ").contains(schematic.name) }
                ?.options
                ?.firstOrNull { it.name?.let { it1 -> "--$it1" }?.contains(toComplete) ?: false }

            if (option != null) {
                val description: String? = option.description
                if (description != null) {
                    val descriptionComponent = SimpleColoredComponent()
                    descriptionComponent.append(
                        " " + shortenTextWithEllipsis(description, 200, 0),
                        SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES
                    )
                    component.add(descriptionComponent, BorderLayout.EAST)
                }
            }
        }

        return component
    }
}
