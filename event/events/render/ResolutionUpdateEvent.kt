package dev.wizard.meta.event.events.render

import dev.wizard.meta.event.Event
import dev.wizard.meta.event.EventBus
import dev.wizard.meta.event.EventPosting

class ResolutionUpdateEvent(val width: Int, val height: Int) : Event, EventPosting by Companion {
    companion object : EventBus()
}
