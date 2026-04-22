package dev.wizard.meta.event

import net.minecraftforge.fml.common.eventhandler.Event

interface WrappedForgeEvent : ICancellable {
    val event: Event

    override var cancelled: Boolean
        get() = event.isCanceled
        set(value) {
            event.isCanceled = value
        }
}
