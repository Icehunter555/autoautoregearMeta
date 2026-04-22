package dev.wizard.meta.event.events.render

import dev.wizard.meta.event.Event
import dev.wizard.meta.event.EventBus
import dev.wizard.meta.event.EventPosting
import dev.wizard.meta.event.WrappedForgeEvent
import net.minecraft.util.EnumHand
import net.minecraftforge.client.event.RenderSpecificHandEvent

class RenderSpecifiedHandEvent(override val event: RenderSpecificHandEvent) : Event, WrappedForgeEvent, EventPosting by Companion {
    override val eventBus: EventBus get() = Companion
    override fun post(event: Any) = Companion.post(event)

    val hand: () -> EnumHand = event::getHand
    val partialTicks: () -> Float = event::getPartialTicks
    val swingProgress: () -> Float = event::getSwingProgress
    val equipProgress: () -> Float = event::getEquipProgress

    companion object : EventBus()
}
