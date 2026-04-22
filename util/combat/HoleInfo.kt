package dev.wizard.meta.util.combat

import it.unimi.dsi.fastutil.objects.ObjectSet
import it.unimi.dsi.fastutil.objects.ObjectSets
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World

class HoleInfo(
    val origin: BlockPos,
    val center: Vec3d,
    val boundingBox: AxisAlignedBB,
    val holePos: ObjectSet<BlockPos>,
    val surroundPos: ObjectSet<BlockPos>,
    val type: HoleType,
    val isTrapped: Boolean,
    val isFullyTrapped: Boolean
) {
    val isHole: Boolean = type != HoleType.NONE
    val isSafe: Boolean = type == HoleType.BEDROCK
    val isTwo: Boolean = type == HoleType.TWO
    val isFour: Boolean = type == HoleType.FOUR

    fun canEnter(world: World, pos: BlockPos): Boolean {
        val headPosY = pos.y + 2
        if (origin.y >= headPosY) return false
        val box = boundingBox.expand(0.0, (headPosY - origin.y).toDouble() - 1.0, 0.0)
        return !world.collidesWithAnyBlock(box)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HoleInfo) return false
        return origin == other.origin
    }

    override fun hashCode(): Int {
        return origin.hashCode()
    }

    companion object {
        private val emptyAxisAlignedBB = AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)

        fun empty(pos: BlockPos): HoleInfo {
            return HoleInfo(
                pos,
                Vec3d.ZERO,
                emptyAxisAlignedBB,
                ObjectSets.emptySet(),
                ObjectSets.emptySet(),
                HoleType.NONE,
                isTrapped = false,
                isFullyTrapped = false
            )
        }
    }
}
