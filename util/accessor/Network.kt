package dev.wizard.meta.util.accessor

import dev.wizard.meta.mixins.accessor.network.*
import net.minecraft.network.play.client.CPacketChatMessage
import net.minecraft.network.play.client.CPacketCloseWindow
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.network.play.client.CPacketUseEntity
import net.minecraft.network.play.server.*
import net.minecraft.util.EnumFacing
import net.minecraft.util.text.ITextComponent

var CPacketChatMessage.packetMessage: String
    get() = message
    set(value) {
        (this as AccessorCPacketChatMessage).message = value
    }

val CPacketCloseWindow.windowID: Int
    get() = (this as AccessorCPacketCloseWindow).trollGetWindowID()

var CPacketPlayer.x: Double
    get() = getX(0.0)
    set(value) {
        (this as AccessorCPacketPlayer).x = value
    }

var CPacketPlayer.y: Double
    get() = getY(0.0)
    set(value) {
        (this as AccessorCPacketPlayer).y = value
    }

var CPacketPlayer.z: Double
    get() = getZ(0.0)
    set(value) {
        (this as AccessorCPacketPlayer).z = value
    }

var CPacketPlayer.yaw: Float
    get() = getYaw(0.0f)
    set(value) {
        (this as AccessorCPacketPlayer).yaw = value
    }

var CPacketPlayer.pitch: Float
    get() = getPitch(0.0f)
    set(value) {
        (this as AccessorCPacketPlayer).pitch = value
    }

var CPacketPlayer.onGround: Boolean
    get() = isOnGround
    set(value) {
        (this as AccessorCPacketPlayer).onGround = value
    }

val CPacketPlayer.moving: Boolean
    get() = (this as AccessorCPacketPlayer).moving

val CPacketPlayer.rotating: Boolean
    get() = (this as AccessorCPacketPlayer).rotating

var CPacketPlayerTryUseItemOnBlock.side: EnumFacing
    get() = direction
    set(value) {
        (this as AccessorCPacketPlayerTryUseItemOnBlock).trollSetSide(value)
    }

var CPacketUseEntity.id: Int
    get() = (this as AccessorCPacketUseEntity).id
    set(value) {
        (this as AccessorCPacketUseEntity).id = value
    }

var CPacketUseEntity.packetAction: CPacketUseEntity.Action
    get() = action
    set(value) {
        (this as AccessorCPacketUseEntity).action = value
    }

var SPacketChat.textComponent: ITextComponent
    get() = chatComponent
    set(value) {
        (this as AccessorSPacketChat).chatComponent = value
    }

val SPacketEntityStatus.entityID: Int
    get() = (this as AccessorSPacketEntityStatus).trollGetEntityID()

var SPacketEntityVelocity.packetMotionX: Int
    get() = motionX
    set(value) {
        (this as AccessorSPacketEntityVelocity).motionX = value
    }

var SPacketEntityVelocity.packetMotionY: Int
    get() = motionY
    set(value) {
        (this as AccessorSPacketEntityVelocity).motionY = value
    }

var SPacketEntityVelocity.packetMotionZ: Int
    get() = motionZ
    set(value) {
        (this as AccessorSPacketEntityVelocity).motionZ = value
    }

var SPacketExplosion.packetMotionX: Float
    get() = motionX
    set(value) {
        (this as AccessorSPacketExplosion).motionX = value
    }

var SPacketExplosion.packetMotionY: Float
    get() = motionY
    set(value) {
        (this as AccessorSPacketExplosion).motionY = value
    }

var SPacketExplosion.packetMotionZ: Float
    get() = motionZ
    set(value) {
        (this as AccessorSPacketExplosion).motionZ = value
    }

var SPacketPlayerPosLook.rotationYaw: Float
    get() = yaw
    set(value) {
        (this as AccessorSPacketPosLook).yaw = value
    }

var SPacketPlayerPosLook.rotationPitch: Float
    get() = pitch
    set(value) {
        (this as AccessorSPacketPosLook).pitch = value
    }

var SPacketSetSlot.packetWindowID: Int
    get() = windowId
    set(value) {
        (this as AccessorSPacketSetSlot).trollSetWindowId(value)
    }

var SPacketSetSlot.packetSlot: Int
    get() = slot
    set(value) {
        (this as AccessorSPacketSetSlot).trollSetSlot(value)
    }
