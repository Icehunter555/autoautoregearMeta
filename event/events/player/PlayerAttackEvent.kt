package dev.wizard.meta.event.events.player

import dev.wizard.meta.event.Cancellable
import dev.wizard.meta.event.Event
import dev.wizard.meta.event.EventBus
import dev.wizard.meta.event.EventPosting
import net.minecraft.entity.Entity

class PlayerAttackEvent(val entity: Entity) : Cancellable(), Event, EventPosting by Companion {
    companion object : EventBus()
}
