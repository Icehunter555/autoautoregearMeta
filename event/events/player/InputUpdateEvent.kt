package dev.wizard.meta.event.events.player

import dev.wizard.meta.event.Event
import dev.wizard.meta.event.EventBus
import dev.wizard.meta.event.WrappedForgeEvent
import net.minecraft.util.MovementInput
import net.minecraftforge.client.event.InputUpdateEvent

class InputUpdateEvent(override val event: net.minecraftforge.client.event.InputUpdateEvent) : Event, WrappedForgeEvent {
    override val eventBus: EventBus get() = Companion
    override fun post(event: Any) = Companion.post(event)

    val movementInput: MovementInput get() = event.movementInput

    companion object : EventBus()
}
