package dev.wizard.meta.util.world

import dev.fastmc.common.MathUtilKt
import net.minecraft.block.state.IBlockState
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World

fun World.rayTraceVisible(start: Vec3d, end: Vec3d, maxAttempt: Int = 50, mutableBlockPos: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos()): Boolean {
    return !fastRayTrace(start, end, maxAttempt, mutableBlockPos, FastRayTraceFunction.DEFAULT)
}

fun World.rayTraceVisible(startX: Double, startY: Double, startZ: Double, end: Vec3d, maxAttempt: Int = 50, mutableBlockPos: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos()): Boolean {
    return !fastRayTrace(startX, startY, startZ, end.x, end.y, end.z, maxAttempt, mutableBlockPos, FastRayTraceFunction.DEFAULT)
}

fun World.rayTraceVisible(start: Vec3d, endX: Double, endY: Double, endZ: Double, maxAttempt: Int = 50, mutableBlockPos: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos()): Boolean {
    return !fastRayTrace(start.x, start.y, start.z, endX, endY, endZ, maxAttempt, mutableBlockPos, FastRayTraceFunction.DEFAULT)
}

fun World.rayTraceVisible(startX: Double, startY: Double, startZ: Double, endX: Double, endY: Double, endZ: Double, maxAttempt: Int = 50, mutableBlockPos: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos()): Boolean {
    return !fastRayTrace(startX, startY, startZ, endX, endY, endZ, maxAttempt, mutableBlockPos, FastRayTraceFunction.DEFAULT)
}

fun World.fastRayTraceCorners(x: Double, y: Double, z: Double, blockX: Int, blockY: Int, blockZ: Int, maxAttempt: Int = 50, mutableBlockPos: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos()): Int {
    val minX = blockX.toDouble() + 0.05
    val minY = blockY.toDouble() + 0.05
    val minZ = blockZ.toDouble() + 0.05
    val maxX = blockX.toDouble() + 0.95
    val maxY = blockY.toDouble() + 0.95
    val maxZ = blockZ.toDouble() + 0.95
    
    var count = 0
    if (fastRayTrace(x, y, z, minX, minY, minZ, maxAttempt, mutableBlockPos, FastRayTraceFunction.DEFAULT)) count++
    if (fastRayTrace(x, y, z, maxX, minY, minZ, maxAttempt, mutableBlockPos, FastRayTraceFunction.DEFAULT)) count++
    if (fastRayTrace(x, y, z, minX, minY, maxZ, maxAttempt, mutableBlockPos, FastRayTraceFunction.DEFAULT)) count++
    if (fastRayTrace(x, y, z, maxX, minY, maxZ, maxAttempt, mutableBlockPos, FastRayTraceFunction.DEFAULT)) count++
    if (fastRayTrace(x, y, z, minX, maxY, minZ, maxAttempt, mutableBlockPos, FastRayTraceFunction.DEFAULT)) count++
    if (fastRayTrace(x, y, z, maxX, maxY, minZ, maxAttempt, mutableBlockPos, FastRayTraceFunction.DEFAULT)) count++
    if (fastRayTrace(x, y, z, minX, maxY, maxZ, maxAttempt, mutableBlockPos, FastRayTraceFunction.DEFAULT)) count++
    if (fastRayTrace(x, y, z, maxX, maxY, maxZ, maxAttempt, mutableBlockPos, FastRayTraceFunction.DEFAULT)) count++
    return count
}

fun World.fastRayTrace(start: Vec3d, end: Vec3d, maxAttempt: Int = 50, mutableBlockPos: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos(), function: FastRayTraceFunction = FastRayTraceFunction.DEFAULT): Boolean {
    return fastRayTrace(start.x, start.y, start.z, end.x, end.y, end.z, maxAttempt, mutableBlockPos, function)
}

fun World.fastRayTrace(startX: Double, startY: Double, startZ: Double, end: Vec3d, maxAttempt: Int = 50, mutableBlockPos: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos(), function: FastRayTraceFunction = FastRayTraceFunction.DEFAULT): Boolean {
    return fastRayTrace(startX, startY, startZ, end.x, end.y, end.z, maxAttempt, mutableBlockPos, function)
}

fun World.fastRayTrace(start: Vec3d, endX: Double, endY: Double, endZ: Double, maxAttempt: Int = 50, mutableBlockPos: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos(), function: FastRayTraceFunction = FastRayTraceFunction.DEFAULT): Boolean {
    return fastRayTrace(start.x, start.y, start.z, endX, endY, endZ, maxAttempt, mutableBlockPos, function)
}

fun World.fastRayTrace(startX: Double, startY: Double, startZ: Double, endX: Double, endY: Double, endZ: Double, maxAttempt: Int = 50, mutableBlockPos: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos(), function: FastRayTraceFunction = FastRayTraceFunction.DEFAULT): Boolean {
    var currentX = startX
    var currentY = startY
    var currentZ = startZ
    var currentBlockX = MathUtilKt.floorToInt(currentX)
    var currentBlockY = MathUtilKt.floorToInt(currentY)
    var currentBlockZ = MathUtilKt.floorToInt(currentZ)
    
    mutableBlockPos.setPos(currentBlockX, currentBlockY, currentBlockZ)
    val startBlockState = getBlockState(mutableBlockPos) ?: return false
    
    when (function.invoke(this, mutableBlockPos, startBlockState)) {
        FastRayTraceAction.MISS -> return false
        FastRayTraceAction.CALC -> if (rayTraceBlock(startBlockState, this, mutableBlockPos, currentX, currentY, currentZ, endX, endY, endZ)) return true
        FastRayTraceAction.HIT -> return true
        FastRayTraceAction.SKIP -> {}
    }

    val endBlockX = MathUtilKt.floorToInt(endX)
    val endBlockY = MathUtilKt.floorToInt(endY)
    val endBlockZ = MathUtilKt.floorToInt(endZ)
    
    var count = maxAttempt
    while (count-- >= 0) {
        if (currentBlockX == endBlockX && currentBlockY == endBlockY && currentBlockZ == endBlockZ) return false
        
        var nextX = 999
        var nextY = 999
        var nextZ = 999
        var stepX = 999.0
        var stepY = 999.0
        var stepZ = 999.0
        val diffX = endX - currentX
        val diffY = endY - currentY
        val diffZ = endZ - currentZ
        
        if (endBlockX > currentBlockX) {
            nextX = currentBlockX + 1
            stepX = (nextX - currentX) / diffX
        } else if (endBlockX < currentBlockX) {
            nextX = currentBlockX
            stepX = (nextX - currentX) / diffX
        }
        
        if (endBlockY > currentBlockY) {
            nextY = currentBlockY + 1
            stepY = (nextY - currentY) / diffY
        } else if (endBlockY < currentBlockY) {
            nextY = currentBlockY
            stepY = (nextY - currentY) / diffY
        }
        
        if (endBlockZ > currentBlockZ) {
            nextZ = currentBlockZ + 1
            stepZ = (nextZ - currentZ) / diffZ
        } else if (endBlockZ < currentBlockZ) {
            nextZ = currentBlockZ
            stepZ = (nextZ - currentZ) / diffZ
        }
        
        if (stepX < stepY && stepX < stepZ) {
            currentX = nextX.toDouble()
            currentY += diffY * stepX
            currentZ += diffZ * stepX
            currentBlockX = nextX - (if (endBlockX < currentBlockX) 1 else 0)
            currentBlockY = MathUtilKt.floorToInt(currentY)
            currentBlockZ = MathUtilKt.floorToInt(currentZ)
        } else if (stepY < stepZ) {
            currentX += diffX * stepY
            currentY = nextY.toDouble()
            currentZ += diffZ * stepY
            currentBlockX = MathUtilKt.floorToInt(currentX)
            currentBlockY = nextY - (if (endBlockY < currentBlockY) 1 else 0)
            currentBlockZ = MathUtilKt.floorToInt(currentZ)
        } else {
            currentX += diffX * stepZ
            currentY += diffY * stepZ
            currentZ = nextZ.toDouble()
            currentBlockX = MathUtilKt.floorToInt(currentX)
            currentBlockY = MathUtilKt.floorToInt(currentY)
            currentBlockZ = nextZ - (if (endBlockZ < currentBlockZ) 1 else 0)
        }
        
        mutableBlockPos.setPos(currentBlockX, currentBlockY, currentBlockZ)
        if (isOutsideBuildHeight(mutableBlockPos) || !worldBorder.contains(mutableBlockPos)) continue
        val blockState = getBlockState(mutableBlockPos) ?: continue
        
        when (function.invoke(this, mutableBlockPos, blockState)) {
            FastRayTraceAction.MISS -> return false
            FastRayTraceAction.CALC -> if (rayTraceBlock(blockState, this, mutableBlockPos, currentX, currentY, currentZ, endX, endY, endZ)) return true
            FastRayTraceAction.HIT -> return true
            FastRayTraceAction.SKIP -> {}
        }
    }
    return false
}

private fun rayTraceBlock(state: IBlockState, world: World, blockPos: BlockPos.MutableBlockPos, x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double): Boolean {
    val x1f = (x1 - blockPos.x).toFloat()
    val y1f = (y1 - blockPos.y).toFloat()
    val z1f = (z1 - blockPos.z).toFloat()
    val x2f = (x2 - blockPos.x).toFloat()
    val y2f = (y2 - blockPos.y).toFloat()
    val z2f = (z2 - blockPos.z).toFloat()
    
    val box = state.getBoundingBox(world, blockPos)
    val minX = box.minX.toFloat()
    val minY = box.minY.toFloat()
    val minZ = box.minZ.toFloat()
    val maxX = box.maxX.toFloat()
    val maxY = box.maxY.toFloat()
    val maxZ = box.maxZ.toFloat()
    
    val xDiff = x2f - x1f
    val yDiff = y2f - y1f
    val zDiff = z2f - z1f
    
    if (xDiff * xDiff >= 1.0E-7f) {
        var factor = (minX - x1f) / xDiff
        if (factor !in 0.0f..1.0f) factor = (maxX - x1f) / xDiff
        if (factor in 0.0f..1.0f) {
            val f = y1f + yDiff * factor
            if (f in minY..maxY) {
                val f2 = z1f + zDiff * factor
                if (f2 in minZ..maxZ) return true
            }
        }
    }
    if (yDiff * yDiff >= 1.0E-7f) {
        var factor = (minY - y1f) / yDiff
        if (factor !in 0.0f..1.0f) factor = (maxY - y1f) / yDiff
        if (factor in 0.0f..1.0f) {
            val f = x1f + xDiff * factor
            if (f in minX..maxX) {
                val f2 = z1f + zDiff * factor
                if (f2 in minZ..maxZ) return true
            }
        }
    }
    if (zDiff * zDiff >= 1.0E-7f) {
        var factor = (minZ - z1f) / zDiff
        if (factor !in 0.0f..1.0f) factor = (maxZ - z1f) / zDiff
        if (factor in 0.0f..1.0f) {
            val f = x1f + xDiff * factor
            if (f in minX..maxX) {
                val f2 = y1f + yDiff * factor
                if (f2 in minY..maxY) return true
            }
        }
    }
    return false
}
