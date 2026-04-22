package dev.wizard.meta.module.modules.movement

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.player.PlayerMoveEvent
import dev.wizard.meta.event.listener
import dev.wizard.meta.manager.managers.HoleManager
import dev.wizard.meta.manager.managers.TimerManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.combat.Surround
import dev.wizard.meta.module.modules.exploit.Burrow
import dev.wizard.meta.module.modules.exploit.Clip
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.LambdaUtilsKt
import dev.wizard.meta.util.MovementUtils
import dev.wizard.meta.util.math.vector.toVec3d
import net.minecraft.util.math.Vec3d

object Anchor : Module(
    name = "Anchor",
    category = Category.MOVEMENT,
    description = "Stops your motion when you are above hole",
    priority = 994
) {
    private val stopYMotion by setting("Stop Y Motion", true)
    private val pitchTrigger0 by setting("Pitch Trigger", true)
    private val pitch by setting("Pitch", 75, 0..90, 1, LambdaUtilsKt.atTrue(pitchTrigger0))
    private val yRange by setting("Y Range", 3, 1..5, 1)
    private val fallTimer by setting("Fall Timer", 0.0f, 0.0f..2.0f, 0.01f)

    private var inactiveTicks = 0

    override fun isActive(): Boolean {
        return isEnabled && inactiveTicks < 2
    }

    init {
        listener<PlayerMoveEvent.Pre>(-1000) { event ->
            inactiveTicks++

            if (Burrow.isEnabled || Surround.isEnabled || HoleSnap.isActive() || HolePathFinder.isActive() || Clip.isEnabled) {
                return@listener
            }

            val playerPos = EntityUtils.getBetterPosition(player)
            val isInHole = player.onGround && HoleManager.getHoleInfo(playerPos).isHole()

            if (!pitchTrigger0.value || player.rotationPitch > pitch) {
                val hole = HoleManager.getHoleBelow(playerPos, yRange) { it.canEnter(world, playerPos) }

                if (fallTimer != 0.0f && hole != null && !player.onGround) {
                    TimerManager.modifyTimer(this, 50.0f / fallTimer)
                }

                if (isInHole || hole != null) {
                    val center = hole?.center ?: playerPos.toVec3d(0.5, 0.0, 0.5)
                    if (MovementUtils.isCentered(player, center) && !player.isSneaking) {
                        player.motionX = 0.0
                        player.motionZ = 0.0
                        event.x = 0.0
                        event.z = 0.0
                    }
                }

                if (stopYMotion && isInHole) {
                    player.motionY = -0.08
                    event.y = -0.08
                }
            }
        }
    }
}
