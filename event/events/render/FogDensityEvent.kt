package dev.wizard.meta.event.events.render

import dev.wizard.meta.event.Event
import dev.wizard.meta.event.EventBus
import dev.wizard.meta.event.EventPosting
import dev.wizard.meta.event.WrappedForgeEvent
import net.minecraftforge.client.event.EntityViewRenderEvent

class FogDensityEvent(override val event: EntityViewRenderEvent.FogDensity) : Event, WrappedForgeEvent, EventPosting by Companion {
    override val eventBus: EventBus get() = Companion
    override fun post(event: Any) = Companion.post(event)

    var density: Float
        get() = event.density
        set(value) { event.density = value }

    companion object : EventBus()
}
