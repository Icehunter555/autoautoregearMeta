package dev.wizard.meta.gui.hudgui.elements.text

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.gui.hudgui.AbstractHudElement
import dev.wizard.meta.gui.hudgui.LabelHud

object ServerBrand : LabelHud("Server Brand", category = Category.TEXT, description = "Brand / type of the server") {

    override fun updateText(event: SafeClientEvent) {
        if (event.mc.isIntegratedServerRunning) {
            addText("Singleplayer: ")
            addText(event.player.serverBrand ?: "null", secondary = true)
        } else {
            val serverBrand = event.player.serverBrand ?: "Unknown Server Type"
            addText(serverBrand, secondary = true)
        }
    }
}
