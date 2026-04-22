package dev.wizard.meta.module.modules.movement

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.player.PlayerMoveEvent
import dev.wizard.meta.event.listener
import dev.wizard.meta.manager.managers.TimerManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.MovementUtils
import dev.wizard.meta.util.accessor.setFlag
import dev.wizard.meta.util.inventory.slot.chestSlot
import dev.wizard.meta.util.world.getGroundLevel
import net.minecraft.init.Items
import net.minecraft.network.play.client.CPacketEntityAction

object ElytraFly : Module(
    name = "ElytraFly",
    category = Category.MOVEMENT,
    description = "flyyyy",
    priority = 1000
) {
    private val jumpTimer by setting("Jump Timer", 0.5f, 0.1f..2.0f, 0.01f)
    private val fallTimer by setting("Fall Timer", 0.25f, 0.1f..2.0f, 0.01f)
    private val boostTimer by setting("Boost Timer", 1.08f, 1.0f..2.0f, 0.01f)
    private val minTakeoffHeight by setting("Min Takeoff Height", 0.8f, 0.0f..1.5f, 0.1f)
    var speed by setting("Speed", 1.5f, 0.1f..10.0f, 0.05f)
    private val speedFast by setting("Speed Fast", 2.5f, 0.1f..10.0f, 0.05f)
    private val upSpeed by setting("Up Speed", 1.5f, 0.1f..10.0f, 0.05f)
    private val upSpeedFast by setting("Up Speed Fast", 2.5f, 0.1f..10.0f, 0.05f)
    private val downSpeed by setting("Down Speed", 1.5f, 0.1f..10.0f, 0.05f)
    private val downSpeedFast by setting("Down Speed Fast", 2.5f, 0.1f..10.0f, 0.05f)

    private var state = State.ON_GROUND

    override fun isActive(): Boolean {
        return isEnabled && state != State.ON_GROUND
    }

    override fun getHudInfo(): String {
        return "$speed, $speedFast"
    }

    private fun SafeClientEvent.updateState() {
        state = if (player.onGround || player.chestSlot.stack.item != Items.ELYTRA) {
            State.ON_GROUND
        } else if (player.isElytraFlying) {
            State.FLYING
        } else {
            State.TAKEOFF
        }
    }

    private fun onGround() {
        mc.player?.setFlag(7, false)
        TimerManager.resetTimer(this)
    }

    private fun SafeClientEvent.takeoff() {
        if (player.motionY < 0.0) {
            if (player.posY - world.getGroundLevel(player) > minTakeoffHeight) {
                connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.START_FALL_FLYING))
                TimerManager.modifyTimer(this@ElytraFly, 50.0f / fallTimer)
            } else {
                TimerManager.modifyTimer(this@ElytraFly, 25.0f)
            }
        } else {
            TimerManager.modifyTimer(this@ElytraFly, 50.0f / jumpTimer)
        }
    }

    private fun SafeClientEvent.fly(event: PlayerMoveEvent.Pre) {
        player.motionY = 0.0
        val sprint = mc.gameSettings.keyBindSprint.isKeyDown
        if (MovementUtils.isInputting()) {
            val yaw = MovementUtils.calcMoveYaw(player)
            val currentSpeed = if (sprint) speedFast else speed
            event.x = -Math.sin(yaw) * currentSpeed
            event.z = Math.cos(yaw) * currentSpeed
            TimerManager.modifyTimer(this@ElytraFly, 50.0f / boostTimer)
        } else {
            event.x = 0.0
            event.z = 0.0
            TimerManager.resetTimer(this@ElytraFly)
        }

        val jump = player.movementInput.jump
        val sneak = player.movementInput.sneak
        if (jump xor sneak) {
            if (jump) {
                event.y = (if (sprint) upSpeedFast else upSpeed).toDouble()
            } else {
                event.y = (-(if (sprint) downSpeedFast else downSpeed)).toDouble()
            }
        }
        player.motionX = 0.0
        player.motionY = 0.0
        player.motionZ = 0.0
    }

    init {
        onDisable {
            state = State.ON_GROUND
        }

        listener<PlayerMoveEvent.Pre> { event ->
            updateState()
            when (state) {
                State.ON_GROUND -> onGround()
                State.TAKEOFF -> takeoff()
                State.FLYING -> fly(event)
            }
        }
    }

    private enum class State {
        ON_GROUND, TAKEOFF, FLYING
    }
}
