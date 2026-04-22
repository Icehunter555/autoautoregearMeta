package dev.wizard.meta.util

import dev.fastmc.common.collection.CircularArray
import dev.wizard.meta.event.AlwaysListening
import dev.wizard.meta.event.listener
import dev.wizard.meta.event.events.ConnectionEvent
import dev.wizard.meta.event.events.PacketEvent
import net.minecraft.network.play.server.SPacketTimeUpdate

object TpsCalculator : AlwaysListening {
    private val tickRates = CircularArray(120, 20.0f)
    private var timeLastTimeUpdate = -1L

    val tickRate: Float
        get() = CircularArray.average(tickRates).coerceAtLeast(1.0f)

    val multiplier: Float
        get() = 20.0f / tickRate

    private fun reset() {
        tickRates.clear()
        timeLastTimeUpdate = -1L
    }

    init {
        listener<PacketEvent.Receive> {
            if (it.packet is SPacketTimeUpdate) {
                if (timeLastTimeUpdate != -1L) {
                    val timeElapsed = (System.nanoTime() - timeLastTimeUpdate).toDouble() / 1.0E9
                    tickRates.add((20.0 / timeElapsed).toFloat().coerceIn(0.0f, 20.0f))
                }
                timeLastTimeUpdate = System.nanoTime()
            }
        }

        listener<ConnectionEvent.Connect> {
            reset()
        }
    }
}
