package dev.wizard.meta.event.events.render

import dev.wizard.meta.event.*
import net.minecraftforge.client.event.RenderGameOverlayEvent

sealed class RenderOverlayEvent(override val event: RenderGameOverlayEvent) : Event, WrappedForgeEvent {
    val type: RenderGameOverlayEvent.ElementType get() = event.type

    class Pre(event: RenderGameOverlayEvent.Pre) : RenderOverlayEvent(event), ICancellable by Cancellable(), EventPosting by Companion {
        companion object : EventBus()
    }

    class Post(event: RenderGameOverlayEvent.Post) : RenderOverlayEvent(event), ICancellable by Cancellable(), EventPosting by Companion {
        companion object : EventBus()
    }
}
