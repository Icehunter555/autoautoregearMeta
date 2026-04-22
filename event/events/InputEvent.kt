package dev.wizard.meta.event.events

import dev.wizard.meta.event.Event
import dev.wizard.meta.event.EventBus
import dev.wizard.meta.event.EventPosting

sealed class InputEvent(val state: Boolean) : Event {
    class Keyboard(val key: Int, state: Boolean) : InputEvent(state), EventPosting by Companion {
        companion object : EventBus()
    }

    class Mouse(val button: Int, state: Boolean) : InputEvent(state), EventPosting by Companion {
        companion object : EventBus()
    }
}
