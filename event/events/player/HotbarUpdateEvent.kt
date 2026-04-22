package dev.wizard.meta.event.events.player

import dev.wizard.meta.event.Event
import dev.wizard.meta.event.EventBus
import dev.wizard.meta.event.EventPosting

class HotbarUpdateEvent(val oldSlot: Int, val newSlot: Int) : Event, EventPosting by Companion {
    companion object : EventBus()
}
