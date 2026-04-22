package dev.wizard.meta.module.modules.misc

import dev.fastmc.common.TickTimer
import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.threads.onMainThreadSafe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.network.play.client.CPacketConfirmTransaction
import net.minecraft.network.play.client.CPacketKeepAlive
import net.minecraft.network.play.server.SPacketConfirmTransaction
import net.minecraft.network.play.server.SPacketKeepAlive

object PingSpoof : Module(
    name = "PingSpoof",
    category = Category.MISC,
    description = "Cancels or adds delay to your ping packets"
) {
    private val mode by setting("Mode", Mode.BYPASS)
    private val delay by setting("Delay", 100, 0..1000, 5)
    private val multiplier by setting("Multiplier", 1, 1..100, 1)

    private val packetTimer = TickTimer()

    override fun getHudInfo(): String {
        return (delay * multiplier).toString()
    }

    init {
        onDisable {
            packetTimer.reset(-114514L)
        }

        listener<PacketEvent.Receive> {
            val packet = it.packet
            if (packet is SPacketKeepAlive) {
                packetTimer.reset()
                it.cancel()
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.Default) {
                    delay((delay * multiplier).toLong())
                    onMainThreadSafe {
                        connection.sendPacket(CPacketKeepAlive(packet.id))
                    }
                }
            } else if (packet is SPacketConfirmTransaction && mode == Mode.BYPASS && packet.windowId == 0 && !packet.accepted && !packetTimer.tickAndReset(1L)) {
                packetTimer.reset(-114514L)
                it.cancel()
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.Default) {
                    delay((delay * multiplier).toLong())
                    onMainThreadSafe {
                        connection.sendPacket(CPacketConfirmTransaction(packet.windowId, packet.actionNumber, true))
                    }
                }
            }
        }
    }

    private enum class Mode(override val displayName: CharSequence) : DisplayEnum {
        LEGACY("Legacy"),
        BYPASS("Bypass")
    }
}