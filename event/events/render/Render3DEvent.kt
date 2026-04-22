package dev.wizard.meta.event.events.render

import dev.wizard.meta.event.Event
import dev.wizard.meta.event.EventPosting
import dev.wizard.meta.event.NamedProfilerEventBus

object Render3DEvent : Event, EventPosting by NamedProfilerEventBus("trollRender3D")
