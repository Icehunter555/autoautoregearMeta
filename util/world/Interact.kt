package dev.wizard.meta.util.world

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.manager.managers.HotbarSwitchManager
import dev.wizard.meta.manager.managers.PlayerPacketManager
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.inventory.blockBlacklist
import dev.wizard.meta.util.inventory.slot.getOffhandSlot
import dev.wizard.meta.util.math.BoundingBoxUtilsKt
import dev.wizard.meta.util.math.vector.toVec3dCenter
import dev.wizard.meta.util.math.vector.toVec3d
import dev.wizard.meta.util.threads.onMainThreadSafe
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemBlock
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import java.util.*

private val SIDES = arrayOf(EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST, EnumFacing.UP, EnumFacing.DOWN)

fun SafeClientEvent.checkPlaceRotation(placeInfo: PlaceInfo): Boolean {
    var eyeHeight = player.eyeHeight
    if (!player.isSneaking) {
        eyeHeight -= 0.08f
    }
    val box = AxisAlignedBB(placeInfo.pos)
    val eyePos = PlayerPacketManager.position.add(0.0, eyeHeight.toDouble(), 0.0)
    return BoundingBoxUtilsKt.isInSight(box, eyePos, PlayerPacketManager.rotation.toViewVec(), 8.0)
}

fun SafeClientEvent.getPlacement(targetPos: BlockPos, vararg options: PlacementSearchOption): PlaceInfo? {
    return getPlacement(targetPos, 3, SIDES, *options)
}

fun SafeClientEvent.getPlacement(targetPos: BlockPos, maxDepth: Int = 3, sides: Array<EnumFacing> = SIDES, vararg options: PlacementSearchOption): PlaceInfo? {
    val queue: Queue<SearchData> = ArrayDeque()
    val visited = mutableSetOf<Long>()
    
    for (side in sides) {
        val pos = targetPos.offset(side)
        queue.add(SearchData(targetPos, side, pos))
    }
    
    while (queue.isNotEmpty()) {
        val data = queue.poll()
        if (options.all { it.check(this, data.pos, data.side, data.placedPos) }) {
            if (!world.getBlockState(data.pos).block.isReplaceable(world, data.pos)) {
                return data.toPlaceInfo(EntityUtils.getEyePosition(player))
            }
        }
        
        if (data.depth < maxDepth && visited.add(data.pos.toLong())) {
            for (side in sides) {
                queue.add(data.next(side))
            }
        }
    }
    return null
}

fun SafeClientEvent.getPlacementSequence(targetPos: BlockPos, vararg options: PlacementSearchOption): List<PlaceInfo>? {
    return getPlacementSequence(targetPos, 3, SIDES, *options)
}

fun SafeClientEvent.getPlacementSequence(targetPos: BlockPos, maxDepth: Int = 3, sides: Array<EnumFacing> = SIDES, vararg options: PlacementSearchOption): List<PlaceInfo>? {
    val queue: Queue<SearchData> = ArrayDeque()
    val visited = mutableSetOf<Long>()
    
    for (side in sides) {
        val pos = targetPos.offset(side)
        queue.add(SearchData(pos, side.opposite, targetPos))
    }
    
    while (queue.isNotEmpty()) {
        val data = queue.poll()
        if (!world.isBlockLoaded(data.placedPos)) continue
        
        if (options.all { it.check(this, data.pos, data.side, data.placedPos) }) {
            if (!world.getBlockState(data.pos).block.isReplaceable(world, data.pos)) {
                return data.toPlacementSequence(EntityUtils.getEyePosition(player))
            }
        }
        
        if (data.depth < maxDepth && visited.add(data.pos.toLong())) {
            for (side in sides) {
                queue.add(data.next(side))
            }
        }
    }
    return null
}

fun SafeClientEvent.getNeighbour(pos: BlockPos, attempts: Int = 3, range: Float = 4.25f, visibleSideCheck: Boolean = false, sides: Array<EnumFacing> = EnumFacing.values()): PlaceInfo? {
    val eyePos = player.getPositionEyes(1.0f)
    return getNeighbour(eyePos, pos, attempts, range, visibleSideCheck, sides, hashSetOf())
}

private fun SafeClientEvent.getNeighbour(eyePos: Vec3d, pos: BlockPos, attempts: Int, range: Float, visibleSideCheck: Boolean, sides: Array<EnumFacing>, toIgnore: HashSet<Pair<BlockPos, EnumFacing>>): PlaceInfo? {
    if (!world.isPlaceable(pos)) return null
    
    for (side in sides) {
        val placeInfo = checkNeighbour(eyePos, pos, side, range, visibleSideCheck, true, toIgnore)
        if (placeInfo != null) return placeInfo
    }
    
    if (attempts < 2) return null
    
    for (side in sides) {
        val newPos = pos.offset(side)
        if (!world.isPlaceable(newPos) || eyePos.distanceTo(newPos.toVec3dCenter()) > (range + 1.0f)) continue
        val placeInfo = getNeighbour(eyePos, newPos, attempts - 1, range, visibleSideCheck, sides, toIgnore)
        if (placeInfo != null) return placeInfo
    }
    return null
}

private fun SafeClientEvent.checkNeighbour(eyePos: Vec3d, pos: BlockPos, side: EnumFacing, range: Float, visibleSideCheck: Boolean, checkReplaceable: Boolean, toIgnore: HashSet<Pair<BlockPos, EnumFacing>>?): PlaceInfo? {
    val offsetPos = pos.offset(side)
    val oppositeSide = side.opposite
    if (toIgnore?.add(offsetPos to oppositeSide) == false) return null
    
    val hitVec = getHitVec(offsetPos, oppositeSide)
    val dist = eyePos.distanceTo(hitVec)
    if (dist > range) return null
    if (visibleSideCheck && !getVisibleSides(offsetPos, true).contains(oppositeSide)) return null
    
    if (checkReplaceable && world.getBlockState(offsetPos).isReplaceable) return null
    if (!world.isPlaceable(pos)) return null
    
    val hitVecOffset = getHitVecOffset(oppositeSide)
    return PlaceInfo(offsetPos, oppositeSide, dist, hitVecOffset, hitVec, pos)
}

fun SafeClientEvent.getClosestSide(pos: BlockPos): EnumFacing {
    val dx = player.posX - pos.x
    val dy = player.posY - pos.y
    val dz = player.posZ - pos.z
    return EnumFacing.values().maxByOrNull {
        val vec = it.directionVec
        dx * vec.x + dy * vec.y + dz * vec.z
    }!!
}

fun SafeClientEvent.getMiningSide(pos: BlockPos): EnumFacing? {
    val eyePos = EntityUtils.getEyePosition(player)
    return getVisibleSides(pos).filter { !world.getBlockState(pos.offset(it)).isFullBox }.minByOrNull {
        eyePos.distanceSq(getHitVec(pos, it))
    }
}

fun SafeClientEvent.getClosestVisibleSide(pos: BlockPos): EnumFacing? {
    val eyePos = EntityUtils.getEyePosition(player)
    return getVisibleSides(pos).minByOrNull {
        eyePos.distanceSq(getHitVec(pos, it))
    }
}

fun SafeClientEvent.getVisibleSides(pos: BlockPos, assumeAirAsFullBox: Boolean = false): Set<EnumFacing> {
    val visibleSides = EnumSet.noneOf(EnumFacing::class.java)
    val eyePos = EntityUtils.getEyePosition(player)
    val blockCenter = pos.toVec3dCenter()
    val isFullBox = if (assumeAirAsFullBox && world.isAir(pos)) true else world.getBlockState(pos).isFullBox
    
    checkAxis(visibleSides, eyePos.x - blockCenter.x, EnumFacing.WEST, EnumFacing.EAST, !isFullBox)
    checkAxis(visibleSides, eyePos.y - blockCenter.y, EnumFacing.DOWN, EnumFacing.UP, true)
    checkAxis(visibleSides, eyePos.z - blockCenter.z, EnumFacing.NORTH, EnumFacing.SOUTH, !isFullBox)
    
    return visibleSides
}

private fun checkAxis(set: EnumSet<EnumFacing>, diff: Double, negative: EnumFacing, positive: EnumFacing, bothIfInRange: Boolean) {
    if (diff < -0.5) set.add(negative)
    else if (diff > 0.5) set.add(positive)
    else if (bothIfInRange) {
        set.add(negative)
        set.add(positive)
    }
}

fun getHitVec(pos: BlockPos, facing: EnumFacing): Vec3d {
    val vec = facing.directionVec
    return Vec3d(vec.x * 0.5 + 0.5 + pos.x, vec.y * 0.5 + 0.5 + pos.y, vec.z * 0.5 + 0.5 + pos.z)
}

fun getHitVecOffset(facing: EnumFacing): Vec3f {
    val vec = facing.directionVec
    return Vec3f(vec.x * 0.5f + 0.5f, vec.y * 0.5f + 0.5f, vec.z * 0.5f + 0.5f)
}

fun SafeClientEvent.placeBlock(placeInfo: PlaceInfo, hand: EnumHand = EnumHand.MAIN_HAND) {
    if (!world.isPlaceable(placeInfo.placedPos)) return
    
    val sneak = !player.isSneaking && blockBlacklist.contains(world.getBlock(placeInfo.pos))
    if (sneak) {
        connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.START_SNEAKING))
    }
    
    connection.sendPacket(placeInfo.toPlacePacket(hand))
    player.swingArm(hand)
    
    if (sneak) {
        connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.STOP_SNEAKING))
    }
    
    val itemStack = HotbarSwitchManager.getServerSideItem(player)
    val itemBlock = itemStack.item as? ItemBlock ?: return
    val block = itemBlock.block
    val meta = itemStack.metadata
    val state = block.getStateForPlacement(world, placeInfo.pos, placeInfo.direction, placeInfo.hitVecOffset.x, placeInfo.hitVecOffset.y, placeInfo.hitVecOffset.z, meta, player, hand)
    val sound = state.block.getSoundType(state, world, placeInfo.pos, player)
    
    onMainThreadSafe {
        world.playSound(player, placeInfo.pos, sound.placeSound, SoundCategory.BLOCKS, (sound.volume + 1.0f) / 2.0f, sound.pitch * 0.8f)
    }
}

fun SafeClientEvent.isSideVisible(eyeX: Double, eyeY: Double, eyeZ: Double, blockPos: BlockPos, side: EnumFacing, assumeAirAsFullBox: Boolean = true): Boolean {
    return when (side) {
        EnumFacing.DOWN -> eyeY <= blockPos.y
        EnumFacing.UP -> eyeY >= (blockPos.y + 1)
        EnumFacing.NORTH -> eyeZ.toInt() < blockPos.z || (eyeZ.toInt() == blockPos.z && isFullBox(blockPos, assumeAirAsFullBox))
        EnumFacing.SOUTH -> eyeZ.toInt() > blockPos.z + 1 || (eyeZ.toInt() == blockPos.z + 1 && isFullBox(blockPos, assumeAirAsFullBox))
        EnumFacing.WEST -> eyeX.toInt() < blockPos.x || (eyeX.toInt() == blockPos.x && isFullBox(blockPos, assumeAirAsFullBox))
        EnumFacing.EAST -> eyeX.toInt() > blockPos.x + 1 || (eyeX.toInt() == blockPos.x + 1 && isFullBox(blockPos, assumeAirAsFullBox))
    }
}

private fun SafeClientEvent.isFullBox(pos: BlockPos, assumeAirAsFullBox: Boolean): Boolean {
    val state = world.getBlockState(pos)
    return if (assumeAirAsFullBox && state.block === Blocks.AIR) true else state.isFullBox
}

fun SafeClientEvent.placeBlock(placeInfo: PlaceInfo, slot: Slot) {
    if (!world.isPlaceable(placeInfo.placedPos)) return
    
    val hand = if (slot == player.getOffhandSlot()) EnumHand.OFF_HAND else EnumHand.MAIN_HAND
    val sneak = !player.isSneaking && blockBlacklist.contains(world.getBlock(placeInfo.pos))
    
    if (sneak) {
        connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.START_SNEAKING))
    }
    
    val packet = placeInfo.toPlacePacket(hand)
    if (hand == EnumHand.OFF_HAND) {
        connection.sendPacket(packet)
    } else {
        HotbarSwitchManager.ghostSwitch(this, slot) {
            connection.sendPacket(packet)
        }
    }
    
    player.swingArm(hand)
    
    if (sneak) {
        connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.STOP_SNEAKING))
    }
    
    val itemStack = HotbarSwitchManager.getServerSideItem(player)
    val itemBlock = itemStack.item as? ItemBlock ?: return
    val block = itemBlock.block
    val meta = itemStack.metadata
    val state = block.getStateForPlacement(world, placeInfo.pos, placeInfo.direction, placeInfo.hitVecOffset.x, placeInfo.hitVecOffset.y, placeInfo.hitVecOffset.z, meta, player, hand)
    val sound = state.block.getSoundType(state, world, placeInfo.pos, player)
    
    onMainThreadSafe {
        world.playSound(player, placeInfo.pos, sound.placeSound, SoundCategory.BLOCKS, (sound.volume + 1.0f) / 2.0f, sound.pitch * 0.8f)
    }
}

fun PlaceInfo.toPlacePacket(hand: EnumHand): CPacketPlayerTryUseItemOnBlock {
    return CPacketPlayerTryUseItemOnBlock(pos, direction, hand, hitVecOffset.x, hitVecOffset.y, hitVecOffset.z)
}
