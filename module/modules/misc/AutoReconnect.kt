package dev.wizard.meta.module.modules.misc

import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.events.ConnectionEvent
import dev.wizard.meta.event.events.GuiEvent
import dev.wizard.meta.event.events.WorldEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.accessor.message
import dev.wizard.meta.util.accessor.parentScreen
import dev.wizard.meta.util.accessor.reason
import net.minecraft.client.gui.GuiDisconnected
import net.minecraft.client.multiplayer.GuiConnecting
import net.minecraft.client.multiplayer.ServerData
import kotlin.math.max

object AutoReconnect : Module(
    name = "AutoReconnect",
    category = Category.MISC,
    description = "Automatically reconnects after being disconnected",
    visible = true
) {
    private val delay by setting("Delay", 5.0f, 0.5f..100.0f, 0.5f)
    private var prevServerDate: ServerData? = null
    var hasKicked = false

    init {
        listener<ConnectionEvent.Connect> {
            if (hasKicked) hasKicked = false
        }

        listener<WorldEvent.Load> {
            if (hasKicked) hasKicked = false
        }

        listener<GuiEvent.Closed> {
            if (it.screen is GuiConnecting) {
                prevServerDate = mc.currentServerData
            }
        }

        listener<GuiEvent.Displayed> {
            if (isDisabled || (prevServerDate == null && mc.currentServerData == null)) return@listener
            if (hasKicked) return@listener

            val screen = it.screen
            if (screen is GuiDisconnected) {
                it.screen = TrollGuiDisconnected(screen)
            }
        }
    }

    private class TrollGuiDisconnected(disconnected: GuiDisconnected) : GuiDisconnected(
        disconnected.parentScreen,
        disconnected.reason,
        disconnected.message
    ) {
        private val time = System.currentTimeMillis()

        override fun updateScreen() {
            if (System.currentTimeMillis() - time >= delay * 1000.0f) {
                val serverData = mc.currentServerData ?: prevServerDate ?: return
                mc.displayGuiScreen(GuiConnecting(parentScreen, mc, serverData))
            }
        }

        override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
            super.drawScreen(mouseX, mouseY, partialTicks)
            val ms = max(delay * 1000.0f - (System.currentTimeMillis() - time), 0.0f).toInt()
            val text = "Reconnecting in ${ms}ms"
            fontRenderer.drawStringWithShadow(text, width * 0.5f - fontRenderer.getStringWidth(text) * 0.5f, height - 32.0f, 0xFFFFFF)
        }
    }
}
