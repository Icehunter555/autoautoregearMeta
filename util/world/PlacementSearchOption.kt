package dev.wizard.meta.util.world

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.manager.managers.EntityManager
import dev.wizard.meta.util.math.vector.distanceSqTo
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos

fun interface PlacementSearchOption {
    fun check(event: SafeClientEvent, from: BlockPos, side: EnumFacing, to: BlockPos): Boolean

    companion object {
        @JvmField
        val VISIBLE_SIDE = PlacementSearchOption { event, from, side, _ ->
            event.isSideVisible(event.player.posX, event.player.posY + event.player.eyeHeight.toDouble(), event.player.posZ, from, side, false)
        }

        @JvmField
        val ENTITY_COLLISION = PlacementSearchOption { _, _, _, to ->
            EntityManager.checkNoEntityCollision(to)
        }

        @JvmField
        val ENTITY_COLLISION_IGNORE_SELF = PlacementSearchOption { _, _, _, to ->
            EntityManager.checkNoEntityCollision(to)
        }

        @JvmStatic
        fun range(range: Float): PlacementSearchOption = range(range.toDouble())

        @JvmStatic
        fun range(range: Double): PlacementSearchOption {
            val rangeSq = range * range
            return PlacementSearchOption { event, from, side, _ ->
                val sideVec = side.directionVec
                val hitX = from.x.toDouble() + sideVec.x.toDouble() * 0.5
                val hitY = from.y.toDouble() + sideVec.y.toDouble() * 0.5
                val hitZ = from.z.toDouble() + sideVec.z.toDouble() * 0.5
                event.player.distanceSqTo(hitX, hitY, hitZ) <= rangeSq
            }
        }
    }
}
