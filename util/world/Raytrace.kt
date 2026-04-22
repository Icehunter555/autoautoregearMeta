package dev.wizard.meta.util.world

import dev.fastmc.common.MathUtilKt
import net.minecraft.block.state.IBlockState
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import kotlin.math.*

fun World.rayTrace(start: Vec3d, end: Vec3d, stopOnLiquid: Boolean, ignoreBlockWithoutBoundingBox: Boolean, returnLastUncollidableBlock: Boolean): RayTraceResult? {
    return rayTraceImpl(start, end, stopOnLiquid, ignoreBlockWithoutBoundingBox, returnLastUncollidableBlock)
}

@JvmName("rayTraceImpl")
fun World.rayTraceImpl(start: Vec3d, end: Vec3d, stopOnLiquid: Boolean, ignoreBlockWithoutBoundingBox: Boolean, returnLastUncollidableBlock: Boolean): RayTraceResult? {
    var currentX = start.x
    var currentY = start.y
    var currentZ = start.z
    var currentBlockX = MathUtilKt.floorToInt(currentX)
    var currentBlockY = MathUtilKt.floorToInt(currentY)
    var currentBlockZ = MathUtilKt.floorToInt(currentZ)
    
    val blockPos = BlockPos.MutableBlockPos(currentBlockX, currentBlockY, currentBlockZ)
    val startBlockState = getBlockState(blockPos)
    
    val endX = end.x
    val endY = end.y
    val endZ = end.z
    
    if ((!ignoreBlockWithoutBoundingBox || startBlockState.getCollisionBoundingBox(this, blockPos) != null) && startBlockState.block.canCollideCheck(startBlockState, stopOnLiquid)) {
        val result = rayTraceBlock(startBlockState, this, blockPos, currentX, currentY, currentZ, endX, endY, endZ)
        if (result != null) return result
    }

    val endBlockX = MathUtilKt.floorToInt(endX)
    val endBlockY = MathUtilKt.floorToInt(endY)
    val endBlockZ = MathUtilKt.floorToInt(endZ)
    
    var count = 200
    while (count-- >= 0 && (currentBlockX != endBlockX || currentBlockY != endBlockY || currentBlockZ != endBlockZ)) {
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
        
        blockPos.setPos(currentBlockX, currentBlockY, currentBlockZ)
        val blockState = getBlockState(blockPos)
        
        if (ignoreBlockWithoutBoundingBox && blockState.getCollisionBoundingBox(this, blockPos) == null) continue
        if (!blockState.block.canCollideCheck(blockState, stopOnLiquid)) continue
        
        val result = rayTraceBlock(blockState, this, blockPos, currentX, currentY, currentZ, endX, endY, endZ)
        if (result != null) return result
    }
    
    return if (returnLastUncollidableBlock) {
        val side = if (currentX == floor(currentX)) {
            if (endX > start.x) EnumFacing.WEST else EnumFacing.EAST
        } else if (currentY == floor(currentY)) {
            if (endY > start.y) EnumFacing.DOWN else EnumFacing.UP
        } else {
            if (endZ > start.z) EnumFacing.NORTH else EnumFacing.SOUTH
        }
        RayTraceResult(RayTraceResult.Type.MISS, Vec3d(currentX, currentY, currentZ), side, blockPos.toImmutable())
    } else {
        null
    }
}

fun World.rayTrace(start: Vec3d, end: Vec3d, maxAttempt: Int = 50, function: RayTraceFunction = RayTraceFunction.DEFAULT): RayTraceResult? {
    var currentX = start.x
    var currentY = start.y
    var currentZ = start.z
    var currentBlockX = MathUtilKt.floorToInt(currentX)
    var currentBlockY = MathUtilKt.floorToInt(currentY)
    var currentBlockZ = MathUtilKt.floorToInt(currentZ)
    
    val blockPos = BlockPos.MutableBlockPos(currentBlockX, currentBlockY, currentBlockZ)
    val startBlockState = getBlockState(blockPos) ?: return null
    
    val endX = end.x
    val endY = end.y
    val endZ = end.z
    
    val action = function.invoke(this, blockPos, startBlockState)
    if (action is RayTraceAction.Null) return null
    if (action is RayTraceAction.Calc) {
        val result = rayTraceBlock(startBlockState, this, blockPos, currentX, currentY, currentZ, endX, endY, endZ)
        if (result != null) return result
    } else if (action is RayTraceAction.Result) {
        return action.rayTraceResult
    }

    val endBlockX = MathUtilKt.floorToInt(endX)
    val endBlockY = MathUtilKt.floorToInt(endY)
    val endBlockZ = MathUtilKt.floorToInt(endZ)
    
    var count = maxAttempt
    while (count-- >= 0 && (currentBlockX != endBlockX || currentBlockY != endBlockY || currentBlockZ != endBlockZ)) {
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
        
        blockPos.setPos(currentBlockX, currentBlockY, currentBlockZ)
        val blockState = getBlockState(blockPos) ?: continue
        
        val action2 = function.invoke(this, blockPos, blockState)
        if (action2 is RayTraceAction.Null) return null
        if (action2 is RayTraceAction.Calc) {
            val result = rayTraceBlock(blockState, this, blockPos, currentX, currentY, currentZ, endX, endY, endZ)
            if (result != null) return result
        } else if (action2 is RayTraceAction.Result) {
            return action2.rayTraceResult
        }
    }
    return null
}

private fun rayTraceBlock(state: IBlockState, world: World, blockPos: BlockPos.MutableBlockPos, x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double): RayTraceResult? {
    val x1f = (x1 - blockPos.x).toFloat()
    val y1f = (y1 - blockPos.y).toFloat()
    val z1f = (z1 - blockPos.z).toFloat()
    val x2f = (x2 - blockPos.x).toFloat()
    val y2f = (y2 - blockPos.y).toFloat()
    val z2f = (z2 - blockPos.z).toFloat()
    
    val diffX = x2f - x1f
    val diffY = y2f - y1f
    val diffZ = z2f - z1f
    
    val box = state.getBoundingBox(world, blockPos)
    val minX = box.minX.toFloat()
    val maxX = box.maxX.toFloat()
    val minY = box.minY.toFloat()
    val maxY = box.maxY.toFloat()
    val minZ = box.minZ.toFloat()
    val maxZ = box.maxZ.toFloat()
    
    var hitX = 0.0f
    var hitY = 0.0f
    var hitZ = 0.0f
    var lastDist = 0.0f
    var hitDirection: EnumFacing? = null
    
    if (diffX * diffX >= 1.0E-7f) {
        val factorMin = (minX - x1f) / diffX
        if (factorMin in 0.0f..1.0f) {
            val resX = x1f + diffX * factorMin
            val resY = y1f + diffY * factorMin
            val resZ = z1f + diffZ * factorMin
            if (resY in minY..maxY && resZ in minZ..maxZ) {
                hitX = resX; hitY = resY; hitZ = resZ; hitDirection = EnumFacing.WEST
                lastDist = (x1f - resX) * (x1f - resX) + (y1f - resY) * (y1f - resY) + (z1f - resZ) * (z1f - resZ)
            }
        }
        val factorMax = (maxX - x1f) / diffX
        if (factorMax in 0.0f..1.0f) {
            val resX = x1f + diffX * factorMax
            val resY = y1f + diffY * factorMax
            val resZ = z1f + diffZ * factorMax
            if (resY in minY..maxY && resZ in minZ..maxZ) {
                val d = (x1f - resX) * (x1f - resX) + (y1f - resY) * (y1f - resY) + (z1f - resZ) * (z1f - resZ)
                if (hitDirection == null || d < lastDist) {
                    hitX = resX; hitY = resY; hitZ = resZ; hitDirection = EnumFacing.EAST; lastDist = d
                }
            }
        }
    }
    
    if (diffY * diffY >= 1.0E-7f) {
        val factorMin = (minY - y1f) / diffY
        if (factorMin in 0.0f..1.0f) {
            val resX = x1f + diffX * factorMin
            val resY = y1f + diffY * factorMin
            val resZ = z1f + diffZ * factorMin
            if (resX in minX..maxX && resZ in minZ..maxZ) {
                val d = (x1f - resX) * (x1f - resX) + (y1f - resY) * (y1f - resY) + (z1f - resZ) * (z1f - resZ)
                if (hitDirection == null || d < lastDist) {
                    hitX = resX; hitY = resY; hitZ = resZ; hitDirection = EnumFacing.DOWN; lastDist = d
                }
            }
        }
        val factorMax = (maxY - y1f) / diffY
        if (factorMax in 0.0f..1.0f) {
            val resX = x1f + diffX * factorMax
            val resY = y1f + diffY * factorMax
            val resZ = z1f + diffZ * factorMax
            if (resX in minX..maxX && resZ in minZ..maxZ) {
                val d = (x1f - resX) * (x1f - resX) + (y1f - resY) * (y1f - resY) + (z1f - resZ) * (z1f - resZ)
                if (hitDirection == null || d < lastDist) {
                    hitX = resX; hitY = resY; hitZ = resZ; hitDirection = EnumFacing.UP; lastDist = d
                }
            }
        }
    }
    
    if (diffZ * diffZ >= 1.0E-7f) {
        val factorMin = (minZ - z1f) / diffZ
        if (factorMin in 0.0f..1.0f) {
            val resX = x1f + diffX * factorMin
            val resY = y1f + diffY * factorMin
            val resZ = z1f + diffZ * factorMin
            if (resX in minX..maxX && resY in minY..maxY) {
                val d = (x1f - resX) * (x1f - resX) + (y1f - resY) * (y1f - resY) + (z1f - resZ) * (z1f - resZ)
                if (hitDirection == null || d < lastDist) {
                    hitX = resX; hitY = resY; hitZ = resZ; hitDirection = EnumFacing.NORTH; lastDist = d
                }
            }
        }
        val factorMax = (maxZ - z1f) / diffZ
        if (factorMax in 0.0f..1.0f) {
            val resX = x1f + diffX * factorMax
            val resY = y1f + diffY * factorMax
            val resZ = z1f + diffZ * factorMax
            if (resX in minX..maxX && resY in minY..maxY) {
                val d = (x1f - resX) * (x1f - resX) + (y1f - resY) * (y1f - resY) + (z1f - resZ) * (z1f - resZ)
                if (hitDirection == null || d < lastDist) {
                    hitX = resX; hitY = resY; hitZ = resZ; hitDirection = EnumFacing.SOUTH
                }
            }
        }
    }
    
    return if (hitDirection != null) {
        RayTraceResult(Vec3d(blockPos.x + hitX.toDouble(), blockPos.y + hitY.toDouble(), blockPos.z + hitZ.toDouble()), hitDirection, blockPos.toImmutable())
    } else null
}
