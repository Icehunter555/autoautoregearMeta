package dev.wizard.meta.event

import dev.wizard.meta.util.interfaces.Helper

open class ProfilerEventBus : EventBus(), Helper {
    @Suppress("UNCHECKED_CAST")
    override fun post(event: Any) {
        mc.profiler.startSection("serial/concurrent")
        listeners.forEach { listener ->
            mc.profiler.startSection(listener.ownerName)
            (listener.function as (Any) -> Unit)(event)
            mc.profiler.endSection()
        }
        mc.profiler.endStartSection("parallel")
        invokeParallel(event)
        mc.profiler.endSection()
    }
}
