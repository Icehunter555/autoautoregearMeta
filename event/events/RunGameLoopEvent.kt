package dev.wizard.meta.event.events

import dev.wizard.meta.event.Event
import dev.wizard.meta.event.EventPosting
import dev.wizard.meta.event.NamedProfilerEventBus

sealed class RunGameLoopEvent : Event {
    object Start : RunGameLoopEvent(), EventPosting by NamedProfilerEventBus("start")
    object Tick : RunGameLoopEvent(), EventPosting by NamedProfilerEventBus("tick")
    object Render : RunGameLoopEvent(), EventPosting by NamedProfilerEventBus("render")
    object End : RunGameLoopEvent(), EventPosting by NamedProfilerEventBus("end")
}
