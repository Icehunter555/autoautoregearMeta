package dev.wizard.meta.event.events.baritone

import dev.wizard.meta.event.Event
import dev.wizard.meta.event.EventBus
import dev.wizard.meta.event.EventPosting

class BaritoneCommandEvent(val command: String) : Event, EventPosting by Companion {
    companion object : EventBus()
}
