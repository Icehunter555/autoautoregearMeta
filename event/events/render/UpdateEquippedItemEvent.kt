package dev.wizard.meta.event.events.render

import dev.wizard.meta.event.*

class UpdateEquippedItemEvent : Event, ICancellable by Cancellable(), EventPosting by Companion {
    companion object : NamedProfilerEventBus("trollUpdateEquippedItem")
}
