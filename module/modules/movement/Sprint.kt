package dev.wizard.meta.module.modules.movement

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.listener
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.BaritoneUtils
import dev.wizard.meta.util.MovementUtils
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.client.CPacketUseEntity

object Sprint : Module(
    name = "AutoSprint",
    category = Category.MOVEMENT,
    description = "Automatically makes the player sprint",
    priority = 1010
) {
    private val multiDirection by setting("Multi Direction", true, description = "Sprint in any direction")
    private val checkFlying by setting("Check Flying", true, description = "Cancels while flying")
    private val checkCollide by setting("Check Collide", true, description = "Cancels on colliding with blocks")
    private val checkCriticals by setting("Check Criticals", false, description = "Cancels on attack for criticals")

    private fun SafeClientEvent.checkCriticals(event: PacketEvent): Boolean {
        val packet = event.packet
        return packet is CPacketUseEntity && checkCriticals && player.isSprinting && 
               packet.action == CPacketUseEntity.Action.ATTACK && packet.getEntityFromWorld(world) is EntityLivingBase
    }

    @JvmStatic
    fun shouldSprint(): Boolean {
        val event = SafeClientEvent.instance ?: return false
        if (!isEnabled) return false
        if (mc.gameSettings.keyBindSneak.isKeyDown) return false
        if (event.player.isElytraFlying) return false
        if (event.player.foodStats.foodLevel <= 6) return false
        if (BaritoneUtils.isPathing()) return false
        if (!checkMovementInput(event)) return false
        if (checkFlying && event.player.capabilities.isFlying) return false
        if (checkCollide && event.player.collidedHorizontally) return false
        return true
    }

    private fun checkMovementInput(event: SafeClientEvent): Boolean {
        return if (multiDirection) MovementUtils.isInputting() else event.player.movementInput.moveForward > 0.0f
    }

    init {
        listener<PacketEvent.Send>(-69420) { event ->
            if (!event.cancelled && checkCriticals(event)) {
                connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.STOP_SPRINTING))
            }
        }

        listener<PacketEvent.PostSend> { event ->
            if (checkCriticals(event)) {
                connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.START_SPRINTING))
            }
        }

        listener<TickEvent.Pre> {
            player.setSprinting(shouldSprint())
        }
    }
}
