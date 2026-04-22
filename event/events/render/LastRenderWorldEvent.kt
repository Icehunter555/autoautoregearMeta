package dev.wizard.meta.event.events.render

import dev.wizard.meta.event.Event
import dev.wizard.meta.event.EventBus
import dev.wizard.meta.event.WrappedForgeEvent
import net.minecraftforge.client.event.RenderWorldLastEvent

class LastRenderWorldEvent(override val event: RenderWorldLastEvent) : Event, WrappedForgeEvent {
    override val eventBus: EventBus get() = Companion
    override fun post(event: Any) = Companion.post(event)

    val partialTicks: Float = event.partialTicks

    companion object : EventBus()
}
