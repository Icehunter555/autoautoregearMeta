package dev.wizard.meta.event.events.render

import dev.wizard.meta.event.Event
import dev.wizard.meta.event.EventBus
import dev.wizard.meta.event.WrappedForgeEvent
import net.minecraftforge.client.event.EntityViewRenderEvent

class FogColorEvent(override val event: EntityViewRenderEvent.FogColors) : Event, WrappedForgeEvent {
    override val eventBus: EventBus get() = Companion
    override fun post(event: Any) = Companion.post(event)

    var red: Float
        get() = event.red
        set(value) { event.red = value }

    var green: Float
        get() = event.green
        set(value) { event.green = value }

    var blue: Float
        get() = event.blue
        set(value) { event.blue = value }

    companion object : EventBus()
}
