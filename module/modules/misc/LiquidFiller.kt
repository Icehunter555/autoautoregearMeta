package dev.wizard.meta.module.modules.misc

import dev.fastmc.common.TickTimer
import dev.wizard.meta.event.ListenerKt.concurrentListener
import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.events.RunGameLoopEvent
import dev.wizard.meta.event.events.WorldEvent
import dev.wizard.meta.manager.managers.HotbarSwitchManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.inventory.findBestTool
import dev.wizard.meta.util.inventory.slot.allSlotsPrioritized
import dev.wizard.meta.util.inventory.slot.firstBlock
import dev.wizard.meta.util.math.VectorUtils
import dev.wizard.meta.util.math.vector.distanceSqToCenter
import dev.wizard.meta.util.math.vector.toVec3dCenter
import dev.wizard.meta.util.threads.runSafeSuspend
import dev.wizard.meta.util.world.*
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap
import net.minecraft.block.Block
import net.minecraft.block.BlockLiquid
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.CPacketPlayerDigging
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

object LiquidFiller : Module(
    name = "LiquidFiller",
    category = Category.MISC,
    description = "Fills up liquid sources with blocks",
    modulePriority = 100
) {
    private val blockMode by setting("Block Mode", BlockMode.SOFT)
    private val breakBlock by setting("Break Block", true) { blockMode == BlockMode.SOFT }
    private val placeDelay by setting("Place Delay", 200, 0..1000, 10)
    private val mineDelay by setting("Mine Delay", 100, 0..1000, 10) { breakBlock && blockMode == BlockMode.SOFT }
    private val cleanDelay by setting("Clean Delay", 3000, 0..10000, 100) { breakBlock && blockMode == BlockMode.SOFT }
    private val placeTimeout by setting("Place Timeout", 1000, 0..10000, 100)
    private val mineTimeout by setting("Mine Timeout", 1000, 0..10000, 100) { breakBlock && blockMode == BlockMode.SOFT }
    private val range by setting("Range", 4.0f, 1.0f..8.0f, 0.1f)

    private val softBlocks = listOf(Blocks.COBBLESTONE, Blocks.DIRT, Blocks.NETHERRACK, Blocks.SAND, Blocks.GRAVEL)
    private val anyBlocks = listOf(
        Blocks.OBSIDIAN, Blocks.COBBLESTONE, Blocks.DIRT, Blocks.STONE,
        Blocks.NETHERRACK, Blocks.END_STONE, Blocks.ENDER_CHEST, Blocks.SAND, Blocks.GRAVEL
    )

    private val placeTimer = TickTimer()
    private val mineTimer = TickTimer()
    private val placeTimeoutMap = Long2LongOpenHashMap().apply { defaultReturnValue(0L) }
    private val mineTimeoutMap = Long2LongOpenHashMap().apply { defaultReturnValue(0L) }

    init {
        onDisable {
            synchronized(placeTimeoutMap) { placeTimeoutMap.clear() }
            synchronized(mineTimeoutMap) { mineTimeoutMap.clear() }
        }

        listener<WorldEvent.ClientBlockUpdate> {
            if (it.newState.block == Blocks.AIR) {
                synchronized(placeTimeoutMap) { placeTimeoutMap.remove(it.pos.toLong()) }
            } else if (shouldBreakBlock(it.newState.block) && breakBlock && blockMode == BlockMode.SOFT) {
                synchronized(placeTimeoutMap) {
                    if (placeTimeoutMap.remove(it.pos.toLong()) != 0L) {
                        synchronized(mineTimeoutMap) {
                            mineTimeoutMap.put(it.pos.toLong(), System.currentTimeMillis() + cleanDelay)
                        }
                    }
                }
            }
        }

        concurrentListener<RunGameLoopEvent.Tick> {
            runSafeSuspend {
                place()
                if (breakBlock && blockMode == BlockMode.SOFT) {
                    mine()
                }
            }
        }
    }

    private fun shouldBreakBlock(block: Block): Boolean {
        return when (blockMode) {
            BlockMode.SOFT -> softBlocks.contains(block)
            BlockMode.ANY -> anyBlocks.contains(block)
        }
    }

    private fun getUsableBlocks(): List<Block> {
        return when (blockMode) {
            BlockMode.SOFT -> softBlocks
            BlockMode.ANY -> anyBlocks
        }
    }

    private fun mine() {
        if (!mineTimer.tickAndReset(mineDelay.toLong())) return

        if (blockMode == BlockMode.ANY) return

        val blockToMine = softBlocks.firstOrNull { 
            findBestTool(it.defaultState) != null 
        } ?: return

        val currentTime = System.currentTimeMillis()
        synchronized(mineTimeoutMap) {
            val iterator = mineTimeoutMap.values.iterator()
            while (iterator.hasNext()) {
                if (iterator.nextLong() < currentTime) iterator.remove()
            }
        }

        val pos = VectorUtils.getBlockPosInSphere(player, range)
            .filter { !mineTimeoutMap.containsKey(it.toLong()) }
            .filter { softBlocks.contains(world.getBlock(it)) }
            .minByOrNull { player.distanceSqToCenter(it) } ?: return

        synchronized(mineTimeoutMap) {
            mineTimeoutMap.put(pos.toLong(), currentTime + mineTimeout)
        }

        val side = getClosestSide(pos) ?: EnumFacing.UP
        val toolSlot = findBestTool(world.getBlockState(pos))

        if (toolSlot != null) {
            HotbarSwitchManager.ghostSwitch(this, toolSlot) {
                connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, pos, side))
                connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, pos, side))
            }
        }
    }

    private fun place() {
        if (!placeTimer.tickAndReset(placeDelay.toLong())) return

        val usableBlocks = getUsableBlocks()
        val fillSlot = player.allSlotsPrioritized.firstBlock(usableBlocks) ?: return

        val currentTime = System.currentTimeMillis()
        synchronized(placeTimeoutMap) {
            val iterator = placeTimeoutMap.values.iterator()
            while (iterator.hasNext()) {
                if (iterator.nextLong() < currentTime) iterator.remove()
            }
        }

        val pos = VectorUtils.getBlockPosInSphere(player, range)
            .filter { !placeTimeoutMap.containsKey(it.toLong()) }
            .filter {
                val state = world.getBlockState(it)
                (state.block == Blocks.FLOWING_LAVA || state.block == Blocks.FLOWING_WATER) &&
                        state.getValue(BlockLiquid.LEVEL) == 0
            }
            .maxWithOrNull(compareBy<BlockPos> {
                getPlacement(it, 1.0, PlacementSearchOption.range(range), PlacementSearchOption.ENTITY_COLLISION) != null
            }.thenBy { player.distanceSqToCenter(it) }) ?: return

        val directPlace = getPlacement(pos, 1.0, PlacementSearchOption.range(range), PlacementSearchOption.ENTITY_COLLISION)
        if (directPlace != null) {
            placeBlock(directPlace, fillSlot)
            return
        }

        val posUp = pos.up()
        val placeInfo1 = PlaceInfo(pos, EnumFacing.UP, player.getDistance(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()), getHitVecOffset(EnumFacing.UP), getHitVec(pos, EnumFacing.UP), posUp)
        val placeInfo2 = PlaceInfo(posUp, EnumFacing.DOWN, player.getDistance(posUp.x.toDouble(), posUp.y.toDouble(), posUp.z.toDouble()), getHitVecOffset(EnumFacing.DOWN), getHitVec(posUp, EnumFacing.DOWN), pos)

        placeBlock(placeInfo1, fillSlot)
        placeBlock(placeInfo2, fillSlot)

        placeTimer.reset(placeDelay.toLong())
        synchronized(placeTimeoutMap) {
            placeTimeoutMap.put(pos.toLong(), currentTime + placeTimeout)
        }
    }

    private enum class BlockMode(override val displayName: CharSequence) : dev.wizard.meta.util.interfaces.DisplayEnum {
        ANY("Any"),
        SOFT("Soft")
    }
}
