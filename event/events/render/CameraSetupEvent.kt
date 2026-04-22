package dev.wizard.meta.event.events.render

import dev.wizard.meta.event.Event
import dev.wizard.meta.event.EventBus
import dev.wizard.meta.event.WrappedForgeEvent
import net.minecraftforge.client.event.EntityViewRenderEvent

class CameraSetupEvent(override val event: EntityViewRenderEvent.CameraSetup) : Event, WrappedForgeEvent {
    override val eventBus: EventBus get() = Companion
    override fun post(event: Any) = Companion.post(event)

    var yaw: Float
        get() = event.yaw
        set(value) { event.yaw = value }

    var pitch: Float
        get() = event.pitch
        set(value) { event.pitch = value }

    var roll: Float
        get() = event.roll
        set(value) { event.roll = value }

    companion object : EventBus()
}
