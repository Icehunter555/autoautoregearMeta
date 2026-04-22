package dev.wizard.meta.util.world

import dev.fastmc.common.collection.FastObjectArrayList
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

data class SearchData(
    val prev: SearchData?,
    val pos: BlockPos,
    val side: EnumFacing,
    val placedPos: BlockPos,
    val depth: Int
) {
    constructor(pos: BlockPos, side: EnumFacing, placedPos: BlockPos) : this(null, pos, side, placedPos, 1)

    fun next(side: EnumFacing): SearchData {
        return SearchData(this, pos.offset(side.opposite), side, pos, depth + 1)
    }

    fun toPlaceInfo(src: Vec3d): PlaceInfo {
        val hitVecOffset = getHitVecOffset(side)
        val hitVec = getHitVec(pos, side)
        return PlaceInfo(pos, side, src.distanceTo(hitVec), hitVecOffset, hitVec, placedPos)
    }

    fun toPlacementSequence(src: Vec3d): List<PlaceInfo> {
        val sequence = FastObjectArrayList<PlaceInfo>()
        var data: SearchData? = this
        while (data != null) {
            sequence.add(data.toPlaceInfo(src))
            data = data.prev
        }
        return sequence
    }
}
