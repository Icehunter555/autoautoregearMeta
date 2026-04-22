package dev.wizard.meta.event.events.player

import dev.wizard.meta.event.Cancellable
import dev.wizard.meta.event.Event
import dev.wizard.meta.event.EventBus
import dev.wizard.meta.event.EventPosting
import dev.wizard.meta.manager.managers.PlayerPacketManager
import dev.wizard.meta.util.math.vector.Vec2f
import net.minecraft.util.math.Vec3d

sealed class OnUpdateWalkingPlayerEvent(
    var position: Vec3d,
    var rotation: Long,
    var onGround: Boolean
) : Cancellable(), Event {

    var cancelMove: Boolean = false
        private set
    var cancelRotate: Boolean = false
        private set
    var cancelAll: Boolean = false
        private set

    val rotationX: Float get() = Vec2f.getX(rotation)
    val rotationY: Float get() = Vec2f.getY(rotation)

    fun apply(packet: PlayerPacketManager.Packet) {
        cancel()
        packet.position?.let { position = it }
        packet.rotation?.let { rotation = it.unbox() }
        packet.onGround?.let { onGround = it }
        cancelMove = packet.cancelMove
        cancelRotate = packet.cancelRotate
        cancelAll = packet.cancelAll
    }

    class Pre(position: Vec3d, rotation: Long, onGround: Boolean) : OnUpdateWalkingPlayerEvent(position, rotation, onGround), EventPosting by Companion {
        constructor(position: Vec3d, rotationX: Float, rotationY: Float, onGround: Boolean) : this(position, Vec2f(rotationX, rotationY).unbox(), onGround)
        companion object : EventBus()
    }

    class Post(position: Vec3d, rotation: Long, onGround: Boolean) : OnUpdateWalkingPlayerEvent(position, rotation, onGround), EventPosting by Companion {
        constructor(position: Vec3d, rotationX: Float, rotationY: Float, onGround: Boolean) : this(position, Vec2f(rotationX, rotationY).unbox(), onGround)
        companion object : EventBus()
    }
}
