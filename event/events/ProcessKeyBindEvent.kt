package dev.wizard.meta.event.events

import dev.wizard.meta.event.Event
import dev.wizard.meta.event.EventPosting
import dev.wizard.meta.event.NamedProfilerEventBus

sealed class ProcessKeyBindEvent : Event {
    object Pre : ProcessKeyBindEvent(), EventPosting by NamedProfilerEventBus("pre")
    object Post : ProcessKeyBindEvent(), EventPosting by NamedProfilerEventBus("post")
}
