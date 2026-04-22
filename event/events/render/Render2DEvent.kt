package dev.wizard.meta.event.events.render

import dev.wizard.meta.event.Event
import dev.wizard.meta.event.EventBus
import dev.wizard.meta.event.EventPosting
import dev.wizard.meta.event.NamedProfilerEventBus

sealed class Render2DEvent : Event {
    object Absolute : Render2DEvent(), EventPosting by NamedProfilerEventBus("absolute")
    object Mc : Render2DEvent(), EventPosting by NamedProfilerEventBus("mc")
    object Troll : Render2DEvent(), EventPosting by NamedProfilerEventBus("troll")
}
