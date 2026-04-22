package dev.wizard.meta.module.modules.movement

import dev.fastmc.common.MathUtilKt
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.player.Scaffold
import dev.wizard.meta.util.BaritoneUtils
import dev.wizard.meta.util.Wrapper
import net.minecraft.util.math.BlockPos

object SafeWalk : Module(
    name = "SafeWalk",
    category = Category.MOVEMENT,
    description = "Keeps you from walking off edges",
    priority = 1010
) {
    private val checkFallDist by setting("Check Fall Distance", true, description = "Check fall distance from edge")

    @JvmStatic
    fun shouldSafewalk(entityID: Int, motionX: Double, motionZ: Double): Boolean {
        val event = SafeClientEvent.instance ?: return false
        if (entityID != event.player.entityId) return false
        if (event.player.isSneaking) return false

        return (isEnabled && (!checkFallDist && !BaritoneUtils.isPathing() || !isEdgeSafe(motionX, motionZ))) || Scaffold.shouldSafeWalk
    }

    @JvmStatic
    fun setSneaking(state: Boolean) {
        Wrapper.player?.movementInput?.sneak = state
    }

    private fun isEdgeSafe(motionX: Double, motionZ: Double): Boolean {
        val event = SafeClientEvent.instance ?: return false
        return checkFallDist(event, event.player.posX, event.player.posZ) && checkFallDist(event, event.player.posX + motionX, event.player.posZ + motionZ)
    }

    private fun checkFallDist(event: SafeClientEvent, posX: Double, posZ: Double): Boolean {
        val startY = MathUtilKt.floorToInt(event.player.posY - 0.5)
        val pos = BlockPos.PooledMutableBlockPos.retain(MathUtilKt.floorToInt(posX), startY, MathUtilKt.floorToInt(posZ))
        for (y in startY downTo startY - 2) {
            pos.setY(y)
            if (event.world.getBlockState(pos).getCollisionBoundingBox(event.world, pos) != null) {
                pos.release()
                return true
            }
        }
        pos.setY(startY - 3)
        val box = event.world.getBlockState(pos).getCollisionBoundingBox(event.world, pos)
        val safe = box != null && box.maxY >= 1.0
        pos.release()
        return safe
    }

    init {
        onToggle {
            BaritoneUtils.settings?.assumeSafeWalk?.value = it
        }
    }
}
