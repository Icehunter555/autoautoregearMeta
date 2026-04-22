package dev.wizard.meta.event.events

import dev.wizard.meta.event.Event
import dev.wizard.meta.event.EventBus
import dev.wizard.meta.event.EventPosting
import dev.wizard.meta.module.AbstractModule

class ModuleToggleEvent(val module: AbstractModule) : Event, EventPosting by Companion {
    companion object : EventBus()
}
