package dev.wizard.meta.module.modules.movement

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.player.PlayerTravelEvent
import dev.wizard.meta.event.listener
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.LambdaUtilsKt
import dev.wizard.meta.util.MovementUtils
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityBoat
import net.minecraft.entity.passive.AbstractHorse
import net.minecraft.entity.passive.EntityPig
import net.minecraft.network.play.client.CPacketInput
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.network.play.client.CPacketVehicleMove
import net.minecraft.network.play.server.SPacketMoveVehicle
import net.minecraft.util.EnumHand
import net.minecraft.world.chunk.EmptyChunk

object EntitySpeed : Module(
    name = "EntitySpeed",
    category = Category.MOVEMENT,
    description = "Abuse client-sided movement to shape sound barrier breaking rideables",
    priority = 1010
) {
    private val speed by setting("Speed", 1.0f, 0.1f..25.0f, 0.1f)
    private val antiStuck by setting("Anti Stuck", true)
    private val flight by setting("Flight", false)
    private val glideSpeed by setting("Glide Speed", 0.1f, 0.0f..1.0f, 0.01f, LambdaUtilsKt.atTrue(flight))
    private val upSpeed by setting("Up Speed", 1.0f, 0.0f..5.0f, 0.1f, LambdaUtilsKt.atTrue(flight))
    private val opacity by setting("Boat Opacity", 1.0f, 0.0f..1.0f, 0.01f)
    private val forceInteract by setting("Force Interact", false)
    private val interactTickDelay by setting("Interact Delay", 2, 1..20, 1, LambdaUtilsKt.atTrue(forceInteract), description = "Force interact packet delay, in ticks.")

    override fun getHudInfo(): String {
        return speed.toString()
    }

    private fun SafeClientEvent.steerEntity(entity: Entity) {
        val yawRad = MovementUtils.calcMoveYaw(player)
        val motionX = -Math.sin(yawRad) * speed
        val motionZ = Math.cos(yawRad) * speed
        if (MovementUtils.isInputting() && !isBorderingChunk(entity, motionX, motionZ)) {
            entity.motionX = motionX
            entity.motionZ = motionZ
        } else {
            entity.motionX = 0.0
            entity.motionZ = 0.0
        }
        if (entity is AbstractHorse || entity is EntityBoat) {
            entity.rotationYaw = player.rotationYaw
            if (entity is EntityBoat) {
                entity.updateInputs(false, false, false, false)
            }
        }
    }

    private fun fly(entity: Entity) {
        if (!entity.isInWater && !entity.isInLava) {
            entity.motionY = (-glideSpeed).toDouble()
        }
        if (mc.gameSettings.keyBindJump.isKeyDown) {
            entity.motionY += upSpeed.toDouble() / 2.0
        }
    }

    private fun SafeClientEvent.isBorderingChunk(entity: Entity, motionX: Double, motionZ: Double): Boolean {
        return antiStuck && world.getChunk(((entity.posX + motionX).toInt() shr 4), ((entity.posZ + motionZ).toInt() shr 4)) is EmptyChunk
    }

    @JvmStatic
    fun getOpacity(): Float {
        return if (isEnabled) opacity else 1.0f
    }

    init {
        listener<PacketEvent.Send> { event ->
            val ridingEntity = player.ridingEntity
            if (!forceInteract || ridingEntity !is EntityBoat) return@listener

            val packet = event.packet
            if (packet is CPacketPlayer.Rotation || packet is CPacketInput) {
                event.cancel()
            }
            if (packet is CPacketVehicleMove && player.ticksExisted % interactTickDelay == 0) {
                mc.playerController.interactWithEntity(player, ridingEntity, EnumHand.MAIN_HAND)
            }
        }

        listener<PacketEvent.Receive> { event ->
            if (forceInteract && player.ridingEntity is EntityBoat && event.packet is SPacketMoveVehicle) {
                event.cancel()
            }
        }

        listener<PlayerTravelEvent> {
            player.ridingEntity?.let {
                if (it is EntityPig || it is AbstractHorse || (it is EntityBoat && it.controllingPassenger == player)) {
                    steerEntity(it)
                    if (flight) {
                        fly(it)
                    }
                }
            }
        }
    }
}
