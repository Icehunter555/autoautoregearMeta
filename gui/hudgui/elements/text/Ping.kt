package dev.wizard.meta.gui.hudgui.elements.text

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.gui.hudgui.AbstractHudElement
import dev.wizard.meta.gui.hudgui.LabelHud
import dev.wizard.meta.module.modules.client.FastLatency
import dev.wizard.meta.module.modules.misc.PingSpoof
import dev.wizard.meta.setting.settings.SettingRegister.Companion.setting
import dev.wizard.meta.util.InfoCalculator

object Ping : LabelHud("Ping", category = Category.TEXT, description = "Delay between client and server") {

    private val showFastLatency by setting(this, "Show Fastlatency", false, visibility = { FastLatency.isEnabled })
    private val showPingSpoof by setting(this, "Show PingSpoof", false, visibility = { PingSpoof.isEnabled })

    override fun updateText(event: SafeClientEvent) {
        addText(InfoCalculator.modifiedPing.toString())
        addText("ms", secondary = true)
        addText(" ")
        if (showPingSpoof && PingSpoof.isEnabled) {
            addText("+${PingSpoof.delay}", secondary = true)
        }
        if (showFastLatency && FastLatency.isEnabled) {
            addText("(${FastLatency.lastPacketPing}ms)", secondary = true)
        }
    }
}
