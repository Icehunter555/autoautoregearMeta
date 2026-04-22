package dev.wizard.meta.gui.hudgui.elements.text

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.gui.hudgui.AbstractHudElement
import dev.wizard.meta.gui.hudgui.LabelHud
import dev.wizard.meta.util.math.Direction as MetaDirection

object Direction : LabelHud("Direction", category = Category.TEXT, description = "Direction of player facing to") {

    override fun updateText(event: SafeClientEvent) {
        val entity = event.mc.renderViewEntity ?: event.player
        val direction = MetaDirection.fromEntity(entity)
        addText(direction.displayString)
        addText(" (${direction.displayNameXY})", secondary = true)
    }
}
