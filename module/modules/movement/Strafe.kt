package dev.wizard.meta.module.modules.movement

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.player.PlayerMoveEvent
import dev.wizard.meta.event.listener
import dev.wizard.meta.manager.managers.TimerManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.BaritoneUtils
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.MovementUtils
import net.minecraft.entity.EntityLivingBase

object Strafe : Module(
    name = "Strafe",
    category = Category.MOVEMENT,
    description = "Improves control",
    priority = 50
) {
    var timerBoost by setting("Timer Boost", 1.09f, 1.0f..1.5f, 0.01f)
    private val groundBoost by setting("Ground Boost", false)
    private val airBoost by setting("Air Boost", true)
    private val speedPotionBoost by setting("Speed Potion Boost", true)
    private val speed by setting("Speed", 0.22, 0.1..0.5, 1.0E-4)
    private val sprintSpeed by setting("Sprint Speed", 0.2873, 0.1..0.5, 1.0E-4)
    private val autoJump by setting("Auto Jump", false)

    private var running = false
    private var burrowTicks = 34
    private var jumpTicks = 0

    private fun SafeClientEvent.shouldStrafe(): Boolean {
        return !player.isOnLadder && !BaritoneUtils.isPathing() && !EntityUtils.isFlying(player) && !EntityUtils.isInOrAboveLiquid(player)
    }

    init {
        onDisable {
            running = false
            burrowTicks = 34
            jumpTicks = 0
        }

        listener<PlayerMoveEvent.Pre>(500) { event ->
            jumpTicks++
            val playerPos = EntityUtils.getBetterPosition(player)
            val box = world.getBlockState(playerPos).getCollisionBoundingBox(world, playerPos)
            if (box != null && box.maxY + playerPos.y > player.posY) {
                burrowTicks = 0
            } else {
                burrowTicks++
            }

            if (shouldStrafe()) {
                val onGround = player.onGround
                val inputting = MovementUtils.isInputting()
                if ((onGround && groundBoost) || (!onGround && airBoost)) {
                    if (inputting) {
                        val yaw = MovementUtils.calcMoveYaw(player)
                        var baseSpeed = if (player.isSprinting) sprintSpeed else speed
                        if (player.isSneaking) {
                            baseSpeed *= 0.2
                        }
                        if (speedPotionBoost) {
                            baseSpeed = MovementUtils.applySpeedPotionEffects(player as EntityLivingBase, baseSpeed)
                        }
                        val speed = Math.max(MovementUtils.getSpeed(player), baseSpeed)
                        event.x = -Math.sin(yaw) * speed
                        event.z = Math.cos(yaw) * speed
                        if (burrowTicks >= 10) {
                            TimerManager.modifyTimer(this, 50.0f / timerBoost)
                        }
                        running = true
                    } else if (running) {
                        player.motionX = 0.0
                        player.motionZ = 0.0
                        running = false
                    }
                }
                if (autoJump && inputting && onGround && jumpTicks >= 5) {
                    player.jump()
                    event.x *= 1.2
                    event.z *= 1.2
                }
            }
        }
    }
}
