package dev.wizard.meta.manager.managers

import com.google.common.collect.MapMaker
import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.player.OnUpdateWalkingPlayerEvent
import dev.wizard.meta.event.events.render.RenderEntityEvent
import dev.wizard.meta.manager.Manager
import dev.wizard.meta.module.AbstractModule
import dev.wizard.meta.util.Wrapper
import dev.wizard.meta.util.accessor.NetworkKt.getMoving
import dev.wizard.meta.util.accessor.NetworkKt.getRotating
import dev.wizard.meta.util.accessor.NetworkKt.getX
import dev.wizard.meta.util.accessor.NetworkKt.getY
import dev.wizard.meta.util.accessor.NetworkKt.getZ
import dev.wizard.meta.util.accessor.NetworkKt.getYaw
import dev.wizard.meta.util.accessor.NetworkKt.getPitch
import dev.wizard.meta.util.math.vector.Vec2f
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.Vec3d
import java.util.*
import java.util.concurrent.atomic.AtomicReference

object PlayerPacketManager : Manager() {
    private val ignoreUpdateSet: MutableSet<CPacketPlayer> = Collections.newSetFromMap(MapMaker().weakKeys().makeMap())
    private val pendingPacket = AtomicReference<Packet?>()

    var position: Vec3d = Vec3d.ZERO
        private set
    var prevPosition: Vec3d = Vec3d.ZERO
        private set
    var eyePosition: Vec3d = Vec3d.ZERO
        private set
    var boundingBox: AxisAlignedBB = AxisAlignedBB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        private set

    var rotation: Vec2f = Vec2f.ZERO
        private set
    var prevRotation: Vec2f = Vec2f.ZERO
        private set
    private var clientSidePitch: Vec2f = Vec2f.ZERO

    val rotationX: Float get() = rotation.x
    val rotationY: Float get() = rotation.y

    fun ignoreUpdate(packet: CPacketPlayer) {
        ignoreUpdateSet.add(packet)
    }

    fun applyPacket(event: OnUpdateWalkingPlayerEvent.Pre) {
        pendingPacket.getAndSet(null)?.let {
            event.apply(it)
        }
    }

    fun AbstractModule.sendPlayerPacket(block: Packet.Builder.() -> Unit) {
        sendPlayerPacket(this.modulePriority, block)
    }

    fun sendPlayerPacket(priority: Int, block: Packet.Builder.() -> Unit) {
        val builder = Packet.Builder(priority)
        builder.block()
        builder.build()?.let {
            sendPlayerPacket(it)
        }
    }

    fun sendPlayerPacket(packet: Packet) {
        pendingPacket.updateAndGet { current ->
            if (current == null || current.priority < packet.priority) packet else current
        }
    }

    init {
        listener<PacketEvent.PostSend>(priority = -6969) { event ->
            val packet = event.packet
            if (packet !is CPacketPlayer || ignoreUpdateSet.remove(packet)) return@listener
            
            val safe = SafeClientEvent.instance ?: return@listener
            if (packet.getMoving()) {
                position = Vec3d(packet.getX(), packet.getY(), packet.getZ())
                eyePosition = Vec3d(packet.getX(), packet.getY() + safe.player.getEyeHeight().toDouble(), packet.getZ())
                val halfWidth = safe.player.width / 2.0
                boundingBox = AxisAlignedBB(
                    packet.getX() - halfWidth, packet.getY(), packet.getZ() - halfWidth,
                    packet.getX() + halfWidth, packet.getY() + safe.player.height.toDouble(), packet.getZ() + halfWidth
                )
            }
            if (packet.getRotating()) {
                rotation = Vec2f(packet.getYaw(), packet.getPitch())
            }
        }

        listener<TickEvent.Pre>(priority = Int.MAX_VALUE) {
            prevPosition = position
            prevRotation = rotation
        }

        listener<RenderEntityEvent.All.Pre> { event ->
            if (event.entity != Wrapper.player || event.entity.isRiding) return@listener
            clientSidePitch = Vec2f(event.entity.prevRotationPitch, event.entity.rotationPitch)
            event.entity.prevRotationPitch = prevRotation.y
            event.entity.rotationPitch = rotation.y
        }

        listener<RenderEntityEvent.All.Post> { event ->
            if (event.entity != mc.player || event.entity.isRiding) return@listener
            event.entity.prevRotationPitch = clientSidePitch.x
            event.entity.rotationPitch = clientSidePitch.y
        }
    }

    class Packet private constructor(
        val priority: Int,
        val position: Vec3d?,
        val rotation: Vec2f?,
        val onGround: Boolean?,
        val cancelMove: Boolean,
        val cancelRotate: Boolean,
        val cancelAll: Boolean
    ) {
        class Builder(private val priority: Int) {
            private var position: Vec3d? = null
            private var rotation: Vec2f? = null
            private var onGround: Boolean? = null
            private var cancelMove = false
            private var cancelRotate = false
            private var cancelAll = false
            private var empty = true

            fun onGround(onGround: Boolean) {
                this.onGround = onGround
                this.empty = false
            }

            fun move(position: Vec3d) {
                this.position = position
                this.cancelMove = false
                this.empty = false
            }

            fun rotate(rotation: Vec2f) {
                this.rotation = rotation
                this.cancelRotate = false
                this.empty = false
            }

            fun cancelAll() {
                this.cancelMove = true
                this.cancelRotate = true
                this.cancelAll = true
                this.empty = false
            }

            fun cancelMove() {
                this.position = null
                this.cancelMove = true
                this.empty = false
            }

            fun cancelRotate() {
                this.rotation = null
                this.cancelRotate = true
                this.empty = false
            }

            fun build(): Packet? {
                return if (!empty) Packet(priority, position, rotation, onGround, cancelMove, cancelRotate, cancelAll) else null
            }
        }
    }
}
