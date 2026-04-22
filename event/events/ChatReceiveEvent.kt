package dev.wizard.meta.event.events

import dev.wizard.meta.event.Event
import dev.wizard.meta.event.EventBus
import dev.wizard.meta.event.WrappedForgeEvent
import net.minecraft.util.text.ChatType
import net.minecraft.util.text.ITextComponent
import net.minecraftforge.client.event.ClientChatReceivedEvent

class ChatReceiveEvent(override val event: ClientChatReceivedEvent) : Event, WrappedForgeEvent {
    override val eventBus: EventBus get() = Companion
    override fun post(event: Any) = Companion.post(event)

    var message: ITextComponent
        get() = event.message
        set(value) {
            event.message = value
        }

    val type: ChatType get() = event.type

    companion object : EventBus()
}
