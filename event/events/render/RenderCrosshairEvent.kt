package dev.wizard.meta.event.events.render

import dev.wizard.meta.event.Event
import dev.wizard.meta.event.EventPosting
import dev.wizard.meta.event.NamedProfilerEventBus

sealed class RenderCrosshairEvent : Event {
    class Pre : RenderCrosshairEvent(), EventPosting by NamedProfilerEventBus("pre")
    class Post : RenderCrosshairEvent(), EventPosting by NamedProfilerEventBus("post")
}
