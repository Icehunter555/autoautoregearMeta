package dev.wizard.meta.event.events.player

import dev.wizard.meta.event.Event
import dev.wizard.meta.event.EventBus
import dev.wizard.meta.event.WrappedForgeEvent
import net.minecraftforge.client.event.PlayerSPPushOutOfBlocksEvent

class PlayerPushOutOfBlockEvent(override val event: PlayerSPPushOutOfBlocksEvent) : Event, WrappedForgeEvent {
    override val eventBus: EventBus get() = Companion
    override fun post(event: Any) = Companion.post(event)

    companion object : EventBus()
}
