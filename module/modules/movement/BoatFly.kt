package dev.wizard.meta.module.modules.movement

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.player.PlayerTravelEvent
import dev.wizard.meta.event.listener
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.MovementUtils
import dev.wizard.meta.util.accessor.getY
import dev.wizard.meta.util.accessor.setRotationPitch
import dev.wizard.meta.util.accessor.setRotationYaw
import dev.wizard.meta.util.accessor.setY
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityBoat
import net.minecraft.network.play.client.CPacketInput
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.network.play.client.CPacketSteerBoat
import net.minecraft.network.play.client.CPacketUseEntity
import net.minecraft.network.play.server.SPacketEntityTeleport
import net.minecraft.network.play.server.SPacketMoveVehicle
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.network.play.server.SPacketSetPassengers
import net.minecraft.util.EnumHand
import net.minecraft.util.math.Vec3d
import net.minecraft.world.chunk.EmptyChunk

object BoatFly : Module(
    name = "BoatFly",
    category = Category.MOVEMENT,
    description = "Fly using boats",
    priority = 1010
) {
    private val speed by setting("Speed", 1.0f, 0.1f..50.0f, 0.1f)
    private val upSpeed by setting("Up Speed", 1.0f, 0.0f..10.0f, 0.1f)
    private val glideSpeed by setting("Glide Speed", 0.1f, 0.0f..1.0f, 0.01f)
    private val antiStuck by setting("Anti Stuck", true)
    private val remount by setting("Remount", true)
    private val antiForceLook by setting("Anti Force Look", true)
    private val forceInteract by setting("Force Interact", true)
    private val teleportSpoof by setting("Teleport Spoof", false)
    private val cancelPlayer by setting("Cancel Player Packets", false)
    private val antiDesync by setting("Anti Desync", false)
    val opacity by setting("Boat Opacity", 1.0f, 0.0f..1.0f, 0.01f)
    val size by setting("Boat Scale", 1.0, 0.05..1.5, 0.01)

    override fun getHudInfo(): String {
        return "$speed, $upSpeed, $glideSpeed"
    }

    @JvmStatic
    fun isBoatFlying(entityIn: Entity): Boolean {
        if (!isEnabled) return false
        return mc.player?.ridingEntity == entityIn
    }

    fun steerEntity(event: SafeClientEvent, entity: Entity, speed: Float, antiStuck: Boolean) {
        val yawRad = MovementUtils.calcMoveYaw(
            event.player.rotationYaw,
            Math.signum(event.player.movementInput.moveForward).toDouble(),
            Math.signum(event.player.movementInput.moveStrafe).toDouble()
        )
        val motionX = -Math.sin(yawRad) * speed
        val motionZ = Math.cos(yawRad) * speed
        if (MovementUtils.isInputting() && !isBorderingChunk(event, entity, motionX, motionZ, antiStuck)) {
            entity.motionX = motionX
            entity.motionZ = motionZ
        } else {
            entity.motionX = 0.0
            entity.motionZ = 0.0
        }
    }

    private fun isBorderingChunk(event: SafeClientEvent, entity: Entity, motionX: Double, motionZ: Double, antiStuck: Boolean): Boolean {
        return antiStuck && event.world.getChunk(((entity.posX + motionX).toInt() shr 4), ((entity.posZ + motionZ).toInt() shr 4)) is EmptyChunk
    }

    init {
        onDisable {
            if (antiDesync) {
                SafeClientEvent.instance?.let {
                    it.connection.sendPacket(CPacketInput(0.0f, 0.0f, false, true))
                    it.player.dismountRidingEntity()
                }
            }
        }

        listener<PacketEvent.Send> { event ->
            val ridingEntity = player.ridingEntity
            if (ridingEntity !is EntityBoat || !cancelPlayer) return@listener

            val packet = event.packet
            if (packet is CPacketPlayer || packet is CPacketInput || packet is CPacketSteerBoat) {
                if (packet is CPacketInput && packet.moveForward == 0.0f && packet.moveStrafe == 0.0f && !packet.isJump && packet.isSneak) {
                    return@listener
                }
                event.cancel()
            }
        }

        listener<PacketEvent.Receive> { event ->
            val ridingEntity = player.ridingEntity
            if (ridingEntity !is EntityBoat) return@listener

            val packet = event.packet
            when (packet) {
                is SPacketSetPassengers -> {
                    if (remount) {
                        val entity = world.getEntityByID(packet.entityId)
                        if (entity != null) {
                            if (!packet.passengerIds.contains(player.entityId) && ridingEntity.entityId == packet.entityId) {
                                if (teleportSpoof) event.cancel()
                                connection.sendPacket(CPacketUseEntity(entity, EnumHand.OFF_HAND))
                            } else if (packet.passengerIds.isNotEmpty() && packet.passengerIds.contains(player.entityId) && antiForceLook) {
                                entity.rotationYaw = player.prevRotationYaw
                                entity.rotationPitch = player.prevRotationPitch
                            }
                        }
                    }
                }
                is SPacketPlayerPosLook -> {
                    if (antiForceLook) {
                        packet.setRotationYaw(player.rotationYaw)
                        packet.setRotationPitch(player.rotationPitch)
                    }
                }
                is SPacketEntityTeleport -> {
                    if (teleportSpoof && packet.entityId == ridingEntity.entityId) {
                        val pos = Vec3d(packet.posX, packet.posY, packet.posZ)
                        if (player.positionVector.distanceTo(pos) > 20.0) {
                            world.getEntityByID(packet.entityId)?.let {
                                connection.sendPacket(CPacketUseEntity(it, EnumHand.OFF_HAND))
                            }
                        } else {
                            if (antiForceLook) event.cancel()
                            ridingEntity.posX = packet.posX
                            ridingEntity.posY = packet.posY
                            ridingEntity.posZ = packet.posZ
                        }
                    }
                }
                is SPacketMoveVehicle -> {
                    if (forceInteract) event.cancel()
                }
            }
        }

        listener<PlayerTravelEvent> {
            val ridingEntity = player.ridingEntity
            if (ridingEntity !is EntityBoat) return@listener

            ridingEntity.rotationYaw = player.rotationYaw
            ridingEntity.updateInputs(false, false, false, false)
            ridingEntity.setBoatAllowedToSteer(true)
            ridingEntity.motionY = 0.0

            if (glideSpeed > 0.0f && !mc.gameSettings.keyBindJump.isKeyDown) {
                ridingEntity.motionY = -glideSpeed.toDouble()
            }
            if (mc.gameSettings.keyBindJump.isKeyDown) {
                ridingEntity.motionY = upSpeed.toDouble()
            }
            if (mc.gameSettings.keyBindSneak.isKeyDown) {
                ridingEntity.motionY = -upSpeed.toDouble()
            }

            steerEntity(this, ridingEntity, speed, antiStuck)
        }
    }
}
