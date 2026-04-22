package dev.wizard.meta.event.events

import dev.wizard.meta.event.Event
import dev.wizard.meta.event.EventBus
import dev.wizard.meta.event.EventPosting
import net.minecraft.client.gui.GuiScreen

sealed class GuiEvent : Event {
    abstract val screen: GuiScreen?

    class Closed(override val screen: GuiScreen) : GuiEvent(), EventPosting by Companion {
        companion object : EventBus()
    }

    class Displayed(override var screen: GuiScreen?) : GuiEvent(), EventPosting by Companion {
        companion object : EventBus()
    }
}
