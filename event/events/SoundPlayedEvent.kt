package dev.wizard.meta.event.events

import dev.wizard.meta.event.Event
import dev.wizard.meta.event.EventBus
import dev.wizard.meta.event.WrappedForgeEvent
import net.minecraft.client.audio.ISound
import net.minecraftforge.client.event.sound.PlaySoundEvent

class SoundPlayedEvent(override val event: PlaySoundEvent) : Event, WrappedForgeEvent {
    override val eventBus: EventBus get() = Companion
    override fun post(event: Any) = Companion.post(event)

    var resultSound: ISound?
        get() = event.resultSound
        set(value) {
            event.resultSound = value
        }

    val name: String get() = event.name

    companion object : EventBus()
}
