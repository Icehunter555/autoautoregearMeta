package dev.wizard.meta.event.events.render

import dev.wizard.meta.event.*
import net.minecraftforge.client.event.RenderBlockOverlayEvent

class RenderBlockOverlayEvent(override val event: net.minecraftforge.client.event.RenderBlockOverlayEvent) : Event, WrappedForgeEvent, EventPosting by Companion {
    override val eventBus: EventBus get() = Companion
    override fun post(event: Any) = Companion.post(event)

    val type: net.minecraftforge.client.event.RenderBlockOverlayEvent.OverlayType get() = event.overlayType

    companion object : EventBus()
}
