package dev.wizard.meta.util.combat

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.module.modules.client.CombatSetting
import dev.wizard.meta.util.math.vector.toVec3d
import dev.wizard.meta.util.world.*
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectSet
import net.minecraft.block.BlockPistonExtension
import net.minecraft.block.BlockPistonMoving
import net.minecraft.init.Blocks
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

object HoleUtils {
    private val holeOffset1 = arrayOf(BlockPos(0, 0, 0))
    private val holeOffsetCheck1 = arrayOf(BlockPos(0, 0, 0), BlockPos(0, 1, 0))
    private val surroundOffset1 = arrayOf(BlockPos(0, -1, 0), BlockPos(0, 0, -1), BlockPos(1, 0, 0), BlockPos(0, 0, 1), BlockPos(-1, 0, 0))

    private val holeOffset2X = arrayOf(BlockPos(0, 0, 0), BlockPos(1, 0, 0))
    private val holeOffsetCheck2X = arrayOf(BlockPos(0, 0, 0), BlockPos(1, 0, 0), BlockPos(0, 1, 0), BlockPos(1, 1, 0))
    private val holeOffset2Z = arrayOf(BlockPos(0, 0, 0), BlockPos(0, 0, 1))
    private val holeOffsetCheck2Z = arrayOf(BlockPos(0, 0, 0), BlockPos(0, 0, 1), BlockPos(0, 1, 0), BlockPos(0, 1, 1))
    private val surroundOffset2X = arrayOf(BlockPos(0, -1, 0), BlockPos(1, -1, 0), BlockPos(-1, 0, 0), BlockPos(0, 0, -1), BlockPos(0, 0, 1), BlockPos(1, 0, -1), BlockPos(1, 0, 1), BlockPos(2, 0, 0))
    private val surroundOffset2Z = arrayOf(BlockPos(0, -1, 0), BlockPos(0, -1, 1), BlockPos(0, 0, -1), BlockPos(-1, 0, 0), BlockPos(1, 0, 0), BlockPos(-1, 0, 1), BlockPos(1, 0, 1), BlockPos(0, 0, 2))

    private val holeOffset4 = arrayOf(BlockPos(0, 0, 0), BlockPos(0, 0, 1), BlockPos(1, 0, 0), BlockPos(1, 0, 1))
    private val holeOffsetCheck4 = arrayOf(BlockPos(0, 0, 0), BlockPos(0, 0, 1), BlockPos(1, 0, 0), BlockPos(1, 0, 1), BlockPos(0, 1, 0), BlockPos(0, 1, 1), BlockPos(1, 1, 0), BlockPos(1, 1, 1))
    private val surroundOffset4 = arrayOf(BlockPos(0, -1, 0), BlockPos(0, -1, 1), BlockPos(1, -1, 0), BlockPos(1, -1, 1), BlockPos(-1, 0, 0), BlockPos(-1, 0, 1), BlockPos(0, 0, -1), BlockPos(1, 0, -1), BlockPos(0, 0, 2), BlockPos(1, 0, 2), BlockPos(2, 0, 0), BlockPos(2, 0, 1))

    private val mutableBlockPos = ThreadLocal.withInitial { BlockPos.MutableBlockPos() }

    fun SafeClientEvent.checkHoleM(pos: BlockPos): HoleInfo {
        if (pos.y !in 1..255 || !checkAir(world, pos)) {
            return HoleInfo.empty(pos.toImmutable())
        }
        val mutablePos = mutableBlockPos.get().setPos(pos)
        return checkHole1(pos, mutablePos)
            ?: checkHole2(pos, mutablePos)
            ?: checkHole4(pos, mutablePos)
            ?: HoleInfo.empty(pos.toImmutable())
    }

    private fun SafeClientEvent.checkHole1(pos: BlockPos, mutablePos: BlockPos.MutableBlockPos): HoleInfo? {
        if (!CombatSetting.obsidianHole && !CombatSetting.bedrockHole) return null
        if (!checkAirMultiple(world, holeOffsetCheck1, pos, mutablePos)) return null
        val type = checkSurroundPos(pos, mutablePos, surroundOffset1, HoleType.BEDROCK, HoleType.OBBY)
        if (type == HoleType.NONE) return null
        if (!CombatSetting.bedrockHole && type == HoleType.BEDROCK) return null
        if (!CombatSetting.obsidianHole && type == HoleType.OBBY) return null

        val holePosArray = offset(holeOffset1, pos)
        var trapped = false
        var fullyTrapped = true
        for (hp in holePosArray) {
            if (checkAir(world, mutablePos.setPos(hp.x, hp.y + 2, hp.z))) {
                fullyTrapped = false
            } else {
                trapped = true
            }
        }
        return HoleInfo(pos.toImmutable(), pos.toVec3d(0.5, 0.0, 0.5), AxisAlignedBB(pos), holePosArray, offset(surroundOffset1, pos), type, trapped, fullyTrapped)
    }

    private fun SafeClientEvent.checkHole2(pos: BlockPos, mutablePos: BlockPos.MutableBlockPos): HoleInfo? {
        if (!CombatSetting.twoBlocksHole) return null
        var isX = true
        if (!checkAir(world, mutablePos.setPos(pos.x + 1, pos.y, pos.z))) {
            if (!checkAir(world, mutablePos.setPos(pos.x, pos.y, pos.z + 1))) {
                return null
            }
            isX = false
        }

        val checkArray = if (isX) holeOffsetCheck2X else holeOffsetCheck2Z
        if (!checkAirMultiple(world, checkArray, pos, mutablePos)) return null

        val surroundOffset = if (isX) surroundOffset2X else surroundOffset2Z
        val holeOffset = if (isX) holeOffset2X else holeOffset2Z
        val centerX = if (isX) 1.0 else 0.5
        val centerZ = if (isX) 0.5 else 1.0

        val type = checkSurroundPos(pos, mutablePos, surroundOffset, HoleType.TWO, HoleType.TWO)
        if (type == HoleType.NONE) return null

        val holePosArray = offset(holeOffset, pos)
        var trapped = false
        var fullyTrapped = true
        for (hp in holePosArray) {
            if (checkAir(world, mutablePos.setPos(hp.x, hp.y + 2, hp.z))) {
                fullyTrapped = false
            } else {
                trapped = true
            }
        }

        val boundingBox = if (isX) {
            AxisAlignedBB(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), pos.x + 2.0, pos.y + 1.0, pos.z + 1.0)
        } else {
            AxisAlignedBB(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), pos.x + 1.0, pos.y + 1.0, pos.z + 2.0)
        }

        return HoleInfo(pos.toImmutable(), pos.toVec3d(centerX, 0.0, centerZ), boundingBox, holePosArray, offset(surroundOffset, pos), type, trapped, fullyTrapped)
    }

    private fun SafeClientEvent.checkHole4(pos: BlockPos, mutablePos: BlockPos.MutableBlockPos): HoleInfo? {
        if (!CombatSetting.fourBlocksHole) return null
        if (!checkAirMultiple(world, holeOffsetCheck4, pos, mutablePos)) return null
        val type = checkSurroundPos(pos, mutablePos, surroundOffset4, HoleType.FOUR, HoleType.FOUR)
        if (type == HoleType.NONE) return null

        val holePosArray = offset(holeOffset4, pos)
        var trapped = false
        var fullyTrapped = true
        for (hp in holePosArray) {
            if (checkAir(world, mutablePos.setPos(hp.x, hp.y + 2, hp.z))) {
                fullyTrapped = false
            } else {
                trapped = true
            }
        }

        return HoleInfo(pos.toImmutable(), pos.toVec3d(1.0, 0.0, 1.0), AxisAlignedBB(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), pos.x + 2.0, pos.y + 1.0, pos.z + 2.0), holePosArray, offset(surroundOffset4, pos), type, trapped, fullyTrapped)
    }

    private fun checkAirMultiple(world: World, array: Array<BlockPos>, pos: BlockPos, mutablePos: BlockPos.MutableBlockPos): Boolean {
        return array.all {
            checkAir(world, mutablePos.setPos(pos.x + it.x, pos.y + it.y, pos.z + it.z))
        }
    }

    private fun checkAir(world: World, pos: BlockPos): Boolean {
        val blockState = world.getBlockState(pos)
        if (blockState.block === Blocks.WEB) return false
        if (CombatSetting.ignoreReplaceableFilling && blockState.isReplaceable) return true
        
        val collisionBox = blockState.getCollisionBoundingBox(world, pos)
        val lowCollisionBox = collisionBox == null || collisionBox.maxY <= 0.5
        
        if (blockState.block is BlockPistonExtension || blockState.block is BlockPistonMoving) return true
        if (CombatSetting.ignoreNonFullBoxFilling && !blockState.isFullBox && lowCollisionBox) return true
        if (CombatSetting.ignoreNonCollidingFilling && lowCollisionBox) return true
        
        return blockState.isAir
    }

    private fun offset(offsets: Array<BlockPos>, pos: BlockPos): ObjectSet<BlockPos> {
        val result = ObjectOpenHashSet<BlockPos>(offsets.size)
        for (offset in offsets) {
            result.add(pos.add(offset))
        }
        return result
    }

    private fun SafeClientEvent.checkSurroundPos(pos: BlockPos, mutablePos: BlockPos.MutableBlockPos, surroundOffset: Array<BlockPos>, expectType: HoleType, obbyType: HoleType): HoleType {
        var type = expectType
        for (offset in surroundOffset) {
            val blockState = world.getBlockState(mutablePos.setPos(pos.x + offset.x, pos.y + offset.y, pos.z + offset.z))
            val block = blockState.block
            if (block === Blocks.BEDROCK) continue
            if (block !== Blocks.AIR && CrystalUtils.isResistant(blockState)) {
                type = obbyType
                continue
            }
            return HoleType.NONE
        }
        return type
    }
}
