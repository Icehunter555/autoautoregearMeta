package dev.wizard.meta.manager.managers

import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.RunGameLoopEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.graphics.RenderUtils3D
import dev.wizard.meta.manager.Manager
import dev.wizard.meta.module.AbstractModule
import dev.wizard.meta.util.accessor.getTickLength
import dev.wizard.meta.util.accessor.getTimer
import dev.wizard.meta.util.accessor.setTickLength
import dev.wizard.meta.util.extension.MapKt.lastValueOrNull
import dev.wizard.meta.util.extension.MapKt.synchronized
import java.util.*
import kotlin.math.roundToInt

object TimerManager : Manager() {
    private val modifiers: NavigableMap<AbstractModule, Modifier> = TreeMap<AbstractModule, Modifier>().synchronized()
    private var modified: Boolean = false
    var globalTicks: Int = Int.MIN_VALUE
        private set
    var tickLength: Float = 50.0f
        private set

    fun AbstractModule.resetTimer() {
        modifiers.remove(this)
    }

    fun AbstractModule.modifyTimer(tickLength: Float, timeoutTicks: Int = 1) {
        SafeClientEvent.instance?.let {
            modifiers[this] = Modifier(tickLength, globalTicks + RenderUtils3D.getPartialTicks().roundToInt() + timeoutTicks)
        }
    }

    init {
        listener<RunGameLoopEvent.Start>(priority = Int.MAX_VALUE) {
            val safe = SafeClientEvent.instance
            if (safe != null) {
                synchronized(modifiers) {
                    modifiers.values.removeIf { it.endTick < globalTicks }
                    modifiers.lastValueOrNull()?.let {
                        safe.mc.getTimer().setTickLength(it.tickLength)
                    }
                }
                modified = true
            } else {
                modifiers.clear()
                if (modified) {
                    mc.getTimer().setTickLength(50.0f)
                    modified = false
                }
            }
            tickLength = mc.getTimer().getTickLength()
        }

        listener<TickEvent.Pre>(priority = Int.MAX_VALUE, alwaysListening = true) {
            globalTicks++
        }
    }

    private class Modifier(val tickLength: Float, val endTick: Int)
}
