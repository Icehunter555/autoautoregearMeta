package dev.wizard.meta.event.events.player

import dev.wizard.meta.event.*

class PlayerTravelEvent : Event, ICancellable by Cancellable(), EventPosting by Companion {
    companion object : NamedProfilerEventBus("metaPlayerTravel")
}
