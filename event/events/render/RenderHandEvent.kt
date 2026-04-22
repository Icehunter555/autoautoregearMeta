package dev.wizard.meta.event.events.render

import dev.wizard.meta.event.*
import net.minecraftforge.client.event.RenderHandEvent

sealed class RenderHandEvent(override val event: net.minecraftforge.client.event.RenderHandEvent) : Event, WrappedForgeEvent {
    val partialTicks: Float get() = event.partialTicks
    val renderPass: Int get() = event.renderPass

    class Pre(event: net.minecraftforge.client.event.RenderHandEvent) : RenderHandEvent(event), EventPosting by Companion {
        override fun cancel() {
            event.isCanceled = true
        }
        companion object : EventBus()
    }

    class Post(event: net.minecraftforge.client.event.RenderHandEvent) : RenderHandEvent(event), EventPosting by Companion {
        companion object : EventBus()
    }
}
