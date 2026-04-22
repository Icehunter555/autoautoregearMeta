package dev.wizard.meta.module.modules.misc

import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import net.minecraft.network.play.client.CPacketConfirmTeleport

object PortalTweaks : Module(
    name = "PortalTweaks",
    category = Category.MISC,
    description = "tweaks for portals"
) {
    private val godMode = setting("GodMode", false)
    private val instantSend = setting("Instant Send", true) { godMode.value }
    private val portalChat = setting("PortalChat", true)
    private val fastPortal = setting("Fast Portal", false)
    private val fastPortalCooldown = setting("Fast Portal Cooldown", 5, 1..10, 1) { fastPortal.value }
    private val fastPortalTime = setting("Fast Portal Time", 5, 0..80, 2) { fastPortal.value }

    private var packet: CPacketConfirmTeleport? = null

    init {
        onEnable {
            packet = null
        }

        listener<PacketEvent.Send> {
            if (godMode.value && it.packet is CPacketConfirmTeleport) {
                it.cancel()
                packet = it.packet as CPacketConfirmTeleport
            }
        }

        godMode.listeners.add {
            if (!godMode.value && instantSend.value) {
                packet?.let {
                    mc.connection?.sendPacket(it)
                }
            }
        }
    }

    @JvmStatic
    fun portalChat() = isEnabled && portalChat.value

    @JvmStatic
    fun fastPortal() = isEnabled && fastPortal.value
}
