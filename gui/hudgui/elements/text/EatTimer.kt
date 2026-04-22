package dev.wizard.meta.gui.hudgui.elements.text

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.gui.hudgui.AbstractHudElement
import dev.wizard.meta.gui.hudgui.LabelHud
import dev.wizard.meta.setting.settings.SettingRegister.Companion.setting
import net.minecraft.item.ItemFood

object EatTimer : LabelHud("EatTimer", category = Category.TEXT, description = "Display eating progress percentage.") {

    private val timerTicks by setting(this, "Timer Ticks", 32, 0..100, 1, description = "The tick threshold for eating detection")
    private var tick = 100
    private var holding = false

    override fun onDisplayed() {
        holding = false
        tick = 100
    }

    override fun updateText(event: SafeClientEvent) {
        tick++
        holding = event.player.heldItemMainhand.item is ItemFood || event.player.heldItemOffhand.item is ItemFood
        if (event.player.isHandActive && holding && tick > timerTicks) {
            tick = 0
        }
        if (!holding || tick > timerTicks) {
            tick = 100
            return
        }
        val percent = tick.toFloat() / timerTicks.toFloat() * 100.0f
        addText("%.1f%%".format(percent))
    }
}
