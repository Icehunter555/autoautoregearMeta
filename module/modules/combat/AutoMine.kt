package dev.wizard.meta.module.modules.combat

import dev.fastmc.common.MathUtilKt
import dev.fastmc.common.TickTimer
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.WorldEvent
import dev.wizard.meta.manager.managers.CombatManager
import dev.wizard.meta.manager.managers.EntityManager
import dev.wizard.meta.manager.managers.HoleManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.exploit.Burrow
import dev.wizard.meta.module.modules.player.PacketMine
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.math.VectorUtils
import net.minecraft.block.*
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.tileentity.TileEntityShulkerBox
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.fluids.BlockFluidFinite
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import kotlin.math.abs

object AutoMine : Module(
    "AutoMine",
    arrayOf("CombatMine", "City", "CivBreaker", "Civ", "AutoBreaker", "AntiSurround", "AntiRegear"),
    Category.COMBAT,
    "Mines blocks around targets and prevents player trapping",
    130
) {
    private val antiTrapPriority by setting(this, IntegerSetting(settingName("AntiTrap Priority"), 4, 0..5, 1))
    private val antiTrapTimeout by setting(this, IntegerSetting(settingName("AntiTrap Timeout"), 5000, 0..10000, 100, { antiTrapPriority > 0 }))
    private val burrowMiningPriority by setting(this, IntegerSetting(settingName("Burrow Priority"), 3, 0..5, 1))
    private val burrowMode by setting(this, EnumSetting(settingName("Burrow Mode"), BurrowMode.BOTH, { burrowMiningPriority > 0 }))
    private val burrowTimeout by setting(this, IntegerSetting(settingName("Burrow Timeout"), 5000, 0..10000, 100, { burrowMiningPriority > 0 }))
    private val antiSurroundPriority by setting(this, IntegerSetting(settingName("AntiSurround Priority"), 2, 0..5, 1))
    private val antiSurroundTimeout by setting(this, IntegerSetting(settingName("AntiSurround Timeout"), 5000, 0..10000, 100, { antiSurroundPriority > 0 }))
    private val noSelfMine by setting(this, BooleanSetting(settingName("No Self Mine"), true, { antiSurroundPriority > 0 }))
    private val antiRegearPriority by setting(this, IntegerSetting(settingName("AntiRegear Priority"), 5, 0..5, 1))
    private val antiRegearTimeout by setting(this, IntegerSetting(settingName("AntiRegear Timeout"), 5000, 0..10000, 100, { antiRegearPriority > 0 }))
    private val ignoreSelfPlaced by setting(this, BooleanSetting(settingName("Ignore Self Placed"), true, { antiRegearPriority > 0 }))
    private val selfRange by setting(this, FloatSetting(settingName("Self Range"), 1.0f, 0.0f..10.0f, 0.1f, { antiRegearPriority > 0 }))
    private val friendRange by setting(this, FloatSetting(settingName("Friend Range"), 1.0f, 0.0f..10.0f, 0.1f, { antiRegearPriority > 0 }))
    private val otherPlayerRange by setting(this, FloatSetting(settingName("Other Player Range"), 5.0f, 0.0f..10.0f, 0.1f, { antiRegearPriority > 0 }))
    private val antiRegearMineRange by setting(this, FloatSetting(settingName("AntiRegear Mine Range"), 4.5f, 1.0f..10.0f, 0.1f, { antiRegearPriority > 0 }))

    private var lastSurrounded = false
    private var lastTargetPos: BlockPos? = null
    private val antiTrapTimer = TickTimer()
    private var lastBurrowPos: BlockPos? = null
    private val burrowTimer = TickTimer()
    private var lastAntiSurroundPos: BlockPos? = null
    private val antiSurroundTimer = TickTimer()
    private val selfPlacedShulkers = ObjectLinkedOpenHashSet<BlockPos>()
    private val shulkerMineQueue = ObjectLinkedOpenHashSet<BlockPos>()
    private val antiRegearTimer = TickTimer()
    private var currentlyMining: BlockPos? = null
    private var currentMiningPriority = 0
    private val miningStateTimer = TickTimer()
    private const val miningTimeout = 3000
    private val facings = arrayOf(EnumFacing.WEST, EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH)

    init {
        onEnable { PacketMine.enable() }
        onDisable {
            lastSurrounded = false
            lastTargetPos = null
            lastBurrowPos = null
            lastAntiSurroundPos = null
            currentlyMining = null
            currentMiningPriority = 0
            antiTrapTimer.reset(-69420L)
            burrowTimer.reset(-69420L)
            antiSurroundTimer.reset(-69420L)
            antiRegearTimer.reset(-69420L)
            miningStateTimer.reset(-69420L)
            synchronized(selfPlacedShulkers) { selfPlacedShulkers.clear() }
            shulkerMineQueue.clear()
            PacketMine.reset(this)
        }

        listener<PacketEvent.PostSend> {
            if (antiRegearPriority > 0 && it.packet is CPacketPlayerTryUseItemOnBlock) {
                val pos = it.packet.pos.offset(it.packet.direction)
                addSelfPlacedShulker(pos)
            }
        }

        safeListener<WorldEvent.ClientBlockUpdate> { event ->
            if (antiRegearPriority == 0) return@safeListener
            val playerDistance = player.getDistanceSqToCenter(event.pos)
            if (playerDistance > MathUtilKt.getSq(antiRegearMineRange)) return@safeListener

            if (event.newState.block !is BlockShulkerBox) {
                synchronized(selfPlacedShulkers) { selfPlacedShulkers.remove(event.pos) }
                shulkerMineQueue.remove(event.pos)
            } else {
                if (ignoreSelfPlaced && selfPlacedShulkers.contains(event.pos)) return@safeListener
                if (playerDistance <= MathUtilKt.getSq(selfRange)) return@safeListener
                if (shulkerMineQueue.contains(event.pos)) return@safeListener
                if (!otherPlayerNearBy(this, event.pos)) return@safeListener
                shulkerMineQueue.add(event.pos)
                antiRegearTimer.reset()
            }
        }

        safeParallelListener<TickEvent.Post> {
            val target = CombatManager.target
            if (currentlyMining != null && miningStateTimer.tick(miningTimeout.toLong())) {
                currentlyMining = null
                currentMiningPriority = 0
            }

            val tasks = mutableListOf<MiningTask>()
            if (antiRegearPriority > 0) {
                handleAntiRegear(this)?.let { tasks.add(MiningTask(it, antiRegearPriority, "AntiRegear")) }
            }
            if (target != null) {
                if (antiTrapPriority > 0) {
                    handleAntiTrap(this)?.let { tasks.add(MiningTask(it, antiTrapPriority, "AntiTrap")) }
                }
                if (burrowMiningPriority > 0) {
                    handleBurrowMining(this, target)?.let { tasks.add(MiningTask(it, burrowMiningPriority, "Burrow")) }
                }
                if (antiSurroundPriority > 0) {
                    handleAntiSurround(this, target)?.let { tasks.add(MiningTask(it, antiSurroundPriority, "AntiSurround")) }
                }
            }

            processMiningTasks(this, tasks)
        }
    }

    private fun processMiningTasks(event: SafeClientEvent, tasks: List<MiningTask>) {
        if (tasks.isEmpty()) {
            if (currentMiningPriority <= 0) {
                currentlyMining = null
                currentMiningPriority = 0
            }
            return
        }
        val sortedTasks = tasks.sortedByDescending { it.priority }
        val primaryTask = sortedTasks.first()
        if ((currentMiningPriority < primaryTask.priority || currentlyMining == null) && canMineBlock(event, primaryTask.pos)) {
            startMining(primaryTask)
        }
    }

    private fun startMining(task: MiningTask) {
        PacketMine.mineBlock(INSTANCE, task.pos, modulePriority)
        currentlyMining = task.pos
        currentMiningPriority = task.priority
        miningStateTimer.reset()
        when (task.source) {
            "AntiTrap" -> antiTrapTimer.reset()
            "Burrow" -> { lastBurrowPos = task.pos; burrowTimer.reset() }
            "AntiSurround" -> { lastAntiSurroundPos = task.pos; antiSurroundTimer.reset() }
            "AntiRegear" -> antiRegearTimer.reset()
        }
    }

    private fun handleAntiRegear(event: SafeClientEvent): BlockPos? {
        val mineRangeSq = MathUtilKt.getSq(antiRegearMineRange)
        event.world.loadedTileEntityList.asSequence()
            .filterIsInstance<TileEntityShulkerBox>()
            .filter { event.player.getDistanceSqToCenter(it.pos) <= mineRangeSq }
            .filter { otherPlayerNearBy(event, it.pos) }
            .filter { !ignoreSelfPlaced || !selfPlacedShulkers.contains(it.pos) }
            .forEach { shulkerMineQueue.add(it.pos) }

        var pos: BlockPos? = null
        while (shulkerMineQueue.isNotEmpty()) {
            pos = shulkerMineQueue.first()
            if (event.player.getDistanceSqToCenter(pos) <= mineRangeSq && event.world.getBlockState(pos).block is BlockShulkerBox) break
            shulkerMineQueue.removeFirst()
            pos = null
        }

        if (pos == null) {
            if (antiRegearTimeout > 0 && antiRegearTimer.tick(antiRegearTimeout.toLong())) return null
            if (antiRegearTimeout == 0) return null
        } else {
            antiRegearTimer.reset()
        }
        return pos
    }

    private fun otherPlayerNearBy(event: SafeClientEvent, pos: BlockPos): Boolean {
        val otherPlayerRangeSq = MathUtilKt.getSq(otherPlayerRange)
        val friendRangeSq = MathUtilKt.getSq(friendRange)
        val players = EntityManager.players.filter { !EntityUtils.isSelf(it) }
        val noFriendInRange = players.none { EntityUtils.isFriend(it) && it.getDistanceSqToCenter(pos) <= friendRangeSq }
        val othersInRange = players.any { !EntityUtils.isFriend(it) && it.getDistanceSqToCenter(pos) <= otherPlayerRangeSq }
        return noFriendInRange && othersInRange
    }

    private fun addSelfPlacedShulker(pos: BlockPos) {
        synchronized(selfPlacedShulkers) {
            if (selfPlacedShulkers.size > 100) selfPlacedShulkers.removeLast()
            selfPlacedShulkers.addAndMoveToFirst(pos)
        }
    }

    private fun handleAntiTrap(event: SafeClientEvent): BlockPos? {
        val playerPos = EntityUtils.getBetterPosition(event.player)
        val posAbove1 = playerPos.up(1)
        val posAbove2 = playerPos.up(2)

        if (!isHeadSurrounded(event, playerPos)) {
            if (antiTrapTimeout > 0 && antiTrapTimer.tick(antiTrapTimeout.toLong())) return null
            if (antiTrapTimeout == 0) return null
        } else {
            antiTrapTimer.reset()
        }

        return if (CheckKt.canBreakBlock(event.world, posAbove1) && !event.world.isAirBlock(posAbove1) && canMineBlock(event, posAbove1)) {
            posAbove1
        } else if (event.world.isAirBlock(posAbove1) && CheckKt.canBreakBlock(event.world, posAbove2) && canMineBlock(event, posAbove2)) {
            posAbove2
        } else null
    }

    private fun handleBurrowMining(event: SafeClientEvent, target: EntityLivingBase): BlockPos? {
        return when (burrowMode) {
            BurrowMode.BURROW -> mineBurrowTarget(event, target)
            BurrowMode.CORNER -> mineCornerTarget(event, target)
            BurrowMode.BOTH -> mineCornerTarget(event, target) ?: mineBurrowTarget(event, target)
        }
    }

    private fun mineBurrowTarget(event: SafeClientEvent, target: EntityLivingBase): BlockPos? {
        val pos = EntityUtils.getBetterPosition(target)
        if (pos == EntityUtils.getBetterPosition(event.player)) return null
        val burrow = Burrow.isBurrowed(target)
        val isHole = HoleManager.getHoleInfo(pos).isHole
        if (burrow || isHole) {
            burrowTimer.reset()
            return if (canMineBlock(event, pos)) pos else null
        }
        if (pos != lastBurrowPos || (burrowTimeout > 0 && burrowTimer.tick(burrowTimeout.toLong()))) resetBurrow()
        if (burrowTimeout == 0) resetBurrow()
        return null
    }

    private fun mineCornerTarget(event: SafeClientEvent, target: EntityLivingBase): BlockPos? {
        val pos = getClipPos(event, target)
        if (pos != null) {
            burrowTimer.reset()
            return if (canMineBlock(event, pos)) pos else null
        }
        if (burrowTimeout > 0 && burrowTimer.tick(burrowTimeout.toLong())) resetBurrow()
        if (burrowTimeout == 0) resetBurrow()
        return null
    }

    private fun getClipPos(event: SafeClientEvent, target: EntityLivingBase): BlockPos? {
        val detectBB = target.entityBoundingBox
        var minDist = Double.MAX_VALUE
        val minDistPos = BlockPos.MutableBlockPos()
        val y = target.posY.toInt()
        val minX = MathUtilKt.floorToInt(detectBB.minX + 0.001)
        val maxX = MathUtilKt.floorToInt(detectBB.maxX + 0.001)
        val minZ = MathUtilKt.floorToInt(detectBB.minZ + 0.001)
        val maxZ = MathUtilKt.floorToInt(detectBB.maxZ + 0.001)

        for (x in minX..maxX) {
            for (z in minZ..maxZ) {
                val dist = dev.fastmc.common.DistanceKt.distanceSq(x + 0.5, z + 0.5, target.posX, target.posZ)
                if (dist < minDist && !event.world.isAirBlock(BlockPos(x, y, z)) && canMineBlock(event, BlockPos(x, y, z))) {
                    minDist = dist
                    minDistPos.setPos(x, y, z)
                }
            }
        }
        return if (minDist < Double.MAX_VALUE) minDistPos.toImmutable() else null
    }

    private fun handleAntiSurround(event: SafeClientEvent, target: EntityLivingBase): BlockPos? {
        val targetPos = EntityUtils.getBetterPosition(target)
        val currentSurrounded = HoleManager.getHoleInfo(targetPos).isHole
        val surrounded = currentSurrounded && lastSurrounded
        lastSurrounded = currentSurrounded

        if (!surrounded && !currentSurrounded) {
            if (antiSurroundTimeout > 0 && antiSurroundTimer.tick(antiSurroundTimeout.toLong())) {
                lastAntiSurroundPos = null
                return null
            }
            if (antiSurroundTimeout == 0) {
                lastAntiSurroundPos = null
                return null
            }
        } else {
            antiSurroundTimer.reset()
        }

        val diffX = target.posX - (targetPos.x + 0.5)
        val diffZ = target.posZ - (targetPos.z + 0.5)
        val sortedFacings = facings.sortedWith(compareBy<EnumFacing> { it == lastAntiSurroundPos?.let { pos -> VectorUtils.getFacing(targetPos, pos) } }.thenBy {
            val vec = it.directionVec
            diffX * vec.x + diffZ * vec.z
        }).toTypedArray()

        if (shouldMinePosition(event, targetPos, target, false)) return targetPos
        val abovePos = targetPos.up()
        if (shouldMinePosition(event, abovePos, target, false)) return abovePos

        for (facing in sortedFacings) {
            val pos = targetPos.offset(facing)
            if (shouldMineSurround(event, pos, target, surrounded)) return pos
        }
        for (facing in sortedFacings) {
            val pos = targetPos.offset(facing).up()
            if (shouldMineHeadSurround(event, pos, target)) return pos
        }

        return lastAntiSurroundPos?.takeIf { canMineBlock(event, it) }
    }

    private fun shouldMinePosition(event: SafeClientEvent, pos: BlockPos, target: EntityLivingBase, checkCollision: Boolean): Boolean {
        if (!canMineBlock(event, pos)) return false
        if (checkCollision && !CheckKt.checkBlockCollision(event.world, pos, target.entityBoundingBox)) return false
        return !noSelfMine || !isPartOfPlayerSurround(event, pos)
    }

    private fun shouldMineSurround(event: SafeClientEvent, pos: BlockPos, target: EntityLivingBase, surrounded: Boolean): Boolean {
        if (!canMineBlock(event, pos)) return false
        if (!surrounded && !CheckKt.checkBlockCollision(event.world, pos, target.entityBoundingBox)) return false
        return !noSelfMine || !isPartOfPlayerSurround(event, pos)
    }

    private fun shouldMineHeadSurround(event: SafeClientEvent, pos: BlockPos, target: EntityLivingBase): Boolean {
        if (!canMineBlock(event, pos)) return false
        if (!CheckKt.checkBlockCollision(event.world, pos, target.entityBoundingBox)) return false
        return !noSelfMine || !isPartOfPlayerSurround(event, pos)
    }

    private fun isPartOfPlayerSurround(event: SafeClientEvent, pos: BlockPos): Boolean {
        val playerPos = EntityUtils.getBetterPosition(event.player)
        if (pos == playerPos) return true
        if (pos.y == playerPos.y) {
            val xDiff = abs(pos.x - playerPos.x)
            val zDiff = abs(pos.z - playerPos.z)
            if (xDiff <= 1 && zDiff <= 1 && xDiff + zDiff > 0) return true
        }
        return pos.y == playerPos.y + 1 && pos.x == playerPos.x && pos.z == playerPos.z
    }

    private fun canMineBlock(event: SafeClientEvent, pos: BlockPos): Boolean {
        if (!CheckKt.canBreakBlock(event.world, pos) || event.world.isAirBlock(pos)) return false
        val block = event.world.getBlockState(pos).block
        return block != Blocks.BEDROCK && block !is BlockBed && block !is BlockLiquid && block !is BlockFluidFinite && block !is BlockRail && block !is BlockFire && block !is BlockConcretePowder && block !is BlockWeb
    }

    private fun resetBurrow() { lastBurrowPos = null }

    private fun isHeadSurrounded(event: SafeClientEvent, playerPos: BlockPos): Boolean {
        val headPos = playerPos.up()
        if (EnumFacing.HORIZONTALS.any { event.world.isAirBlock(headPos.offset(it)) }) return false
        return !event.world.isAirBlock(headPos.up())
    }

    private enum class BurrowMode { BURROW, CORNER, BOTH }
    private data class MiningTask(val pos: BlockPos, val priority: Int, val source: String)
}
