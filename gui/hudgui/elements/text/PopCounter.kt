package dev.wizard.meta.gui.hudgui.elements.text

import dev.wizard.meta.event.*
import dev.wizard.meta.event.events.EntityEvent
import dev.wizard.meta.event.events.combat.TotemPopEvent
import dev.wizard.meta.gui.hudgui.AbstractHudElement
import dev.wizard.meta.gui.hudgui.LabelHud

object PopCounter : LabelHud("Pop Counter", category = Category.TEXT, description = "Counts and displays the number of totem pops") {

    var popCount = 0

    init {
        safeListener<TotemPopEvent.Pop> {
            if (it.name == player.name) {
                popCount++
                updateText(this)
            }
        }

        safeListener<EntityEvent.Death> {
            if (it.entity == player) {
                popCount = 0
                updateText(this)
            }
        }
    }

    override fun updateText(event: SafeClientEvent) {
        addText("Pops: ")
        addText(popCount.toString(), secondary = true)
    }
}
