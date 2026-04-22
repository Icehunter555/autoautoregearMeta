package dev.wizard.meta.event.events.player

import dev.wizard.meta.event.*

class PlayerJumpEvent : Event, ICancellable by Cancellable(), EventPosting by Companion {
    companion object : NamedProfilerEventBus("metaJumpEvent")
}
