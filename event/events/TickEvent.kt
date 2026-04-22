package dev.wizard.meta.event.events

import dev.wizard.meta.event.Event
import dev.wizard.meta.event.EventBus
import dev.wizard.meta.event.EventPosting
import dev.wizard.meta.event.NamedProfilerEventBus

sealed class TickEvent : Event {
    object Pre : TickEvent(), EventPosting by NamedProfilerEventBus("trollTickPre")
    object Post : TickEvent(), EventPosting by NamedProfilerEventBus("trollTickPost")
}
