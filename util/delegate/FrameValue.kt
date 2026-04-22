package dev.wizard.meta.util.delegate

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.AlwaysListening
import dev.wizard.meta.event.listener
import dev.wizard.meta.event.events.RunGameLoopEvent
import kotlin.reflect.KProperty

class FrameValue<T>(private val block: () -> T) {
    private var value0: T? = null
    private var lastUpdateFrame: Int = Int.MIN_VALUE

    init {
        @Suppress("UNCHECKED_CAST")
        instances.add(this as FrameValue<Any?>)
    }

    val value: T get() = get()

    fun get(): T {
        return if (lastUpdateFrame == frame) getLazy() else getForce()
    }

    fun getLazy(): T {
        return value0 ?: getForce()
    }

    fun getForce(): T {
        return block().also {
            value0 = it
            lastUpdateFrame = frame
        }
    }

    fun updateLazy() {
        value0 = null
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = get()

    private companion object : AlwaysListening {
        private val instances = mutableListOf<FrameValue<Any?>>()
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
