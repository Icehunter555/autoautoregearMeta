package dev.wizard.meta.gui.hudgui.elements.text

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.gui.hudgui.AbstractHudElement
import dev.wizard.meta.gui.hudgui.LabelHud

object Biome : LabelHud("Biome", category = Category.TEXT, description = "show the biome ur in") {

    override fun updateText(event: SafeClientEvent) {
        val biome = event.world.getBiome(event.player.position).biomeName ?: "Unknown"
        addText(biome)
    }
}
