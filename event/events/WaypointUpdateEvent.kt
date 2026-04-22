package dev.wizard.meta.event.events

import dev.wizard.meta.event.Event
import dev.wizard.meta.event.EventBus
import dev.wizard.meta.event.EventPosting
import dev.wizard.meta.manager.managers.WaypointManager

class WaypointUpdateEvent(val type: Type, val waypoint: WaypointManager.Waypoint?) : Event, EventPosting by Companion {
    enum class Type {
        GET, ADD, REMOVE, CLEAR, RELOAD
    }

    companion object : EventBus()
}
