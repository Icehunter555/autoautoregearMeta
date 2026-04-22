package dev.wizard.meta.event.events

import dev.wizard.meta.event.Event
import dev.wizard.meta.event.EventBus
import dev.wizard.meta.event.EventPosting

sealed class ConnectionEvent : Event {
    object Connect : ConnectionEvent(), EventPosting by EventBus()
    object Disconnect : ConnectionEvent(), EventPosting by EventBus()
}
