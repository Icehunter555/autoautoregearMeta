package dev.wizard.meta.util.world

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.math.vector.Vec3f
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

class PlaceInfo(
    val pos: BlockPos,
    val direction: EnumFacing,
    val dist: Double,
    val hitVecOffset: Vec3f,
    val hitVec: Vec3d,
    val placedPos: BlockPos
) {
    companion object {
        fun SafeClientEvent.newPlaceInfo(pos: BlockPos, side: EnumFacing): PlaceInfo {
            val hitVecOffset = getHitVecOffset(side)
            val hitVec = getHitVec(pos, side)
            val dist = EntityUtils.getEyePosition(player).distanceTo(hitVec)
            val placedPos = pos.offset(side)
            return PlaceInfo(pos, side, dist, hitVecOffset, hitVec, placedPos)
        }
    }
}
