package dev.wizard.meta.util.delegate

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.AlwaysListening
import dev.wizard.meta.event.listener
import dev.wizard.meta.event.events.RunGameLoopEvent
import kotlin.reflect.KProperty

class FrameFloat(private val block: FloatSupplier) {
    private var value0: Float = Float.NaN
    private var lastUpdateFrame: Int = Int.MIN_VALUE

    init {
        instances.add(this)
    }

    val value: Float get() = get()

    fun get(): Float {
        return if (lastUpdateFrame == frame) getLazy() else getForce()
    }

    fun getLazy(): Float {
        var v = value0
        if (v.isNaN()) {
            v = getForce()
        }
        return v
    }

    fun getForce(): Float {
        val v = block.get()
        value0 = v
        lastUpdateFrame = frame
        return v
    }

    fun updateLazy() {
        value0 = Float.NaN
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Float = get()

    private companion object : AlwaysListening {
        private val instances = mutableListOf<FrameFloat>()
        private val timer = TickTimer(TimeUnit.SECONDS)
        private var frame = 0

        init {
            listener<RunGameLoopEvent.Start>(priority = Int.MAX_VALUE) {
                if (frame == Int.MAX_VALUE) {
                    frame = Int.MIN_VALUE
                    instances.forEach { it.updateLazy() }
                } else {
                    frame++
                }
            }
        }
    }
}
