package dev.wizard.meta.module.modules.combat

import dev.fastmc.common.MathUtilKt
import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.RunGameLoopEvent
import dev.wizard.meta.event.events.WorldEvent
import dev.wizard.meta.event.events.player.OnUpdateWalkingPlayerEvent
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.graphics.ESPRenderer
import dev.wizard.meta.graphics.Easing
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.manager.managers.EntityManager
import dev.wizard.meta.manager.managers.HoleManager
import dev.wizard.meta.manager.managers.HotbarSwitchManager
import dev.wizard.meta.manager.managers.PlayerPacketManager
import dev.wizard.meta.module.AbstractModule
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.client.AntiCheat
import dev.wizard.meta.module.modules.movement.HolePathFinder
import dev.wizard.meta.module.modules.movement.HoleSnap
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.combat.HoleInfo
import dev.wizard.meta.util.combat.HoleType
import dev.wizard.meta.util.inventory.slot.DefinedKt
import dev.wizard.meta.util.inventory.slot.IterableKt
import dev.wizard.meta.util.math.BoundingBoxUtilsKt
import dev.wizard.meta.util.math.RotationUtils
import dev.wizard.meta.util.math.vector.ConversionKt
import dev.wizard.meta.util.math.vector.DistanceKt
import dev.wizard.meta.util.threads.ThreadSafetyKt
import dev.wizard.meta.util.world.BlockKt
import it.unimi.dsi.fastutil.longs.*
import it.unimi.dsi.fastutil.objects.Object2LongMap
import it.unimi.dsi.fastutil.objects.Object2LongMaps
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
import net.minecraft.block.BlockPistonExtension
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.inventory.Slot
import net.minecraft.network.play.client.CPacketAnimation
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

object AutoHoleFill : Module(
    "AutoHoleFill",
    category = Category.COMBAT,
    description = "Automatically fills hole while enemy walks into hole",
    modulePriority = 99
) {
    private val bedrockHole by setting(this, BooleanSetting(settingName("Bedrock Hole"), true))
    private val obbyHole by setting(this, BooleanSetting(settingName("Obby Hole"), true))
    private val twoBlocksHole by setting(this, BooleanSetting(settingName("2 Blocks Hole"), true))
    var fourBlocksHole by setting(this, BooleanSetting(settingName("4 Blocks Hole"), true))
    private val targetHole by setting(this, BooleanSetting(settingName("Target Hole"), false))
    private val predictTicks by setting(this, IntegerSetting(settingName("Predict Ticks"), 8, 0..50, 1))
    private val detectRange by setting(this, FloatSetting(settingName("Detect Range"), 5.0f, 0.0f..16.0f, 0.25f))
    var hRange by setting(this, FloatSetting(settingName("H Range"), 0.5f, 0.0f..4.0f, 0.1f))
    private val vRange by setting(this, FloatSetting(settingName("V Range"), 3.0f, 0.0f..8.0f, 0.1f))
    private val distanceBalance by setting(this, FloatSetting(settingName("Distance Balance"), 1.0f, -5.0f..5.0f, 0.1f))
    private val fillDelay by setting(this, IntegerSetting(settingName("Fill Delay"), 50, 0..1000, 10))
    private val fillTimeout by setting(this, IntegerSetting(settingName("Fill Timeout"), 100, 0..1000, 10))
    private val fillRange by setting(this, FloatSetting(settingName("Fill Range"), 5.0f, 1.0f..6.0f, 0.1f))
    private val targetColor by setting(this, ColorSetting(settingName("Target Color"), ColorRGB(32, 255, 32)))
    private val otherColor by setting(this, ColorSetting(settingName("Other Color"), ColorRGB(255, 222, 32)))
    private val filledColor by setting(this, ColorSetting(settingName("Filled Color"), ColorRGB(255, 32, 32)))

    private val placeMap: Long2LongMap = Long2LongMaps.synchronize(Long2LongOpenHashMap().apply { defaultReturnValue(0L) })
    private val updateTimer = TickTimer()
    private val placeTimer = TickTimer()
    private var holeInfos: List<IntermediateHoleInfo> = emptyList()
    private var nextHole: BlockPos? = null
    private val renderBlockMap: Object2LongMap<BlockPos> = Object2LongMaps.synchronize(Object2LongOpenHashMap<BlockPos>().apply { defaultReturnValue(-1L) })
    private val renderer = ESPRenderer().apply {
        aFilled = 33
        aOutline = 233
    }

    init {
        onDisable {
            holeInfos = emptyList()
            nextHole = null
            renderBlockMap.clear()
            renderer.clear()
        }

        listener<WorldEvent.ClientBlockUpdate> {
            if (!BlockKt.isReplaceable(it.newState)) {
                placeMap.remove(it.pos.toLong())
                if (it.pos == nextHole) nextHole = null
                renderBlockMap.replace(it.pos, System.currentTimeMillis())
            }
        }

        listener<Render3DEvent> {
            synchronized(renderBlockMap) {
                renderBlockMap.object2LongEntrySet().forEach { entry ->
                    val color = if (entry.key == nextHole) targetColor else if (entry.longValue == -1L) otherColor else filledColor
                    if (entry.longValue == -1L) {
                        renderer.add(entry.key, color)
                    } else {
                        val progress = Easing.IN_CUBIC.dec(Easing.toDelta(entry.longValue, 1000L))
                        val size = progress * 0.5
                        val n2 = 0.5 - size
                        val p = 0.5 + size
                        val box = AxisAlignedBB(entry.key.x + n2, entry.key.y + n2, entry.key.z + n2, entry.key.x + p, entry.key.y + p, entry.key.z + p)
                        renderer.add(box, color.alpha((255f * progress).toInt()))
                    }
                }
            }
            renderer.render(true)
        }

        safeListener<OnUpdateWalkingPlayerEvent.Pre> {
            if (AntiCheat.blockPlaceRotation) {
                val blockPos = nextHole ?: getRotationPos(holeInfos)
                blockPos?.let { pos ->
                    PlayerPacketManager.sendPlayerPacket {
                        rotate(RotationUtils.getRotationTo(this@safeListener, pos.toVec3d(0.5, -0.5, 0.5)))
                    }
                }
            }
        }

        safeConcurrentListener<RunGameLoopEvent.Tick> {
            val slot = IterableKt.firstBlock(DefinedKt.getAllSlotsPrioritized(player), Blocks.OBSIDIAN)
            val place = placeTimer.tick(fillDelay.toLong()) && slot != null
            if (place || updateTimer.tickAndReset(5L)) {
                val newHoleInfo = getHoleInfos()
                holeInfos = newHoleInfo
                val current = System.currentTimeMillis()
                synchronized(placeMap) {
                    placeMap.values.removeIf { it <= current }
                    nextHole?.let { if (!placeMap.containsKey(it.toLong())) nextHole = null }
                }
                if (place) {
                    getPos(newHoleInfo, AntiCheat.blockPlaceRotation)?.let {
                        nextHole = it
                        placeBlock(slot!!, it)
                    }
                } else {
                    updatePosRender(newHoleInfo)
                }
            }
        }
    }

    private fun SafeClientEvent.updatePosRender(holeInfos: List<IntermediateHoleInfo>) {
        val set = LongOpenHashSet()
        calcPosSequence(holeInfos).forEach {
            set.add(it.second.blockPos.toLong())
            renderBlockMap.putIfAbsent(it.second.blockPos, -1L)
        }
        renderBlockMap.object2LongEntrySet().removeIf { entry ->
            entry.longValue == -1L && !placeMap.containsKey(entry.key.toLong()) && !set.contains(entry.key.toLong())
        }
    }

    private fun SafeClientEvent.getPos(holeInfos: List<IntermediateHoleInfo>, checkRotation: Boolean): BlockPos? {
        val set = LongOpenHashSet()
        val eyePos = PlayerPacketManager.eyePosition
        var sequence = calcPosSequence(holeInfos).onEach {
            set.add(it.second.blockPos.toLong())
            renderBlockMap.putIfAbsent(it.second.blockPos, -1L)
        }
        if (checkRotation) {
            sequence = sequence.filter { pair ->
                val pos = pair.second.blockPos
                BoundingBoxUtilsKt.isInSight(AxisAlignedBB(pos.x.toDouble(), pos.y.toDouble() - 1.0, pos.z.toDouble(), pos.x + 1.0, pos.y.toDouble(), pos.z + 1.0), eyePos, PlayerPacketManager.rotation)
            }
        }
        val targetPos = sequence.minByOrNull { horizontalDist(it.first, it.second.center) }?.second?.blockPos
        renderBlockMap.object2LongEntrySet().removeIf { entry ->
            entry.longValue == -1L && !placeMap.containsKey(entry.key.toLong()) && !set.contains(entry.key.toLong())
        }
        return targetPos
    }

    private fun SafeClientEvent.calcPosSequence(holeInfos: List<IntermediateHoleInfo>) = sequence {
        val detectRangeSq = detectRange * detectRange
        for (entity in EntityManager.players) {
            if (EntityUtils.isSelf(entity) || !entity.isEntityAlive || EntityUtils.isFriend(entity) || player.getDistanceSq(entity) > detectRangeSq) continue
            val current = entity.positionVector
            val predict = calcPredict(entity, current)
            for (holeInfo in holeInfos) {
                if (entity.posY <= holeInfo.blockPos.y + 0.5) {
                    val dist = horizontalDist(entity, holeInfo.center)
                    if ((holeInfo.toward && holeInfo.playerDist - dist < distanceBalance.toDouble()) ||
                        (holeInfo.detectBox.contains(current) || (holeInfo.toward && holeInfo.detectBox.calculateIntercept(current, predict) != null))) {
                        yield(entity to holeInfo)
                    }
                }
            }
        }
    }

    private fun SafeClientEvent.getRotationPos(holeInfos: List<IntermediateHoleInfo>): BlockPos? {
        return calcPosSequence(holeInfos).minByOrNull { horizontalDist(it.first, it.second.center) }?.second?.blockPos
    }

    private fun SafeClientEvent.getHoleInfos(): List<IntermediateHoleInfo> {
        val eyePos = EntityUtils.getEyePosition(player)
        val rangeSq = fillRange * fillRange
        val entities = EntityManager.entity.filter { it.isEntityAlive && it.canBeCollidedWith() }

        return HoleManager.holeInfos.asSequence()
            .filter { it.isFullyTrapped }
            .filter {
                when (it.type) {
                    HoleType.BEDROCK -> bedrockHole
                    HoleType.OBBY -> obbyHole
                    HoleType.TWO -> twoBlocksHole
                    HoleType.FOUR -> fourBlocksHole
                    else -> false
                }
            }
            .filter {
                if (targetHole) true
                else it.origin != HoleSnap.hole?.origin && it.origin != HolePathFinder.hole?.origin
            }
            .filterNot { player.entityBoundingBox.intersects(it.boundingBox) }
            .filter { holeInfo -> entities.none { it.entityBoundingBox.intersects(holeInfo.boundingBox) } }
            .flatMap { holeInfo ->
                holeInfo.holePos.asSequence()
                    .filterNot { placeMap.containsKey(it.toLong()) }
                    .filter { eyePos.distanceSqToCenter(it) <= rangeSq }
                    .filter { pos -> val state = world.getBlockState(pos); BlockKt.isReplaceable(state) || state.block is BlockPistonExtension }
                    .map { pos ->
                        val box = AxisAlignedBB(holeInfo.boundingBox.minX - hRange, holeInfo.boundingBox.minY, holeInfo.boundingBox.minZ - hRange, holeInfo.boundingBox.maxX + hRange, holeInfo.boundingBox.maxY + vRange, holeInfo.boundingBox.maxZ + hRange)
                        val dist = horizontalDist(player, holeInfo.center)
                        val prevDist = DistanceKt.distance(player.prevPosX, player.prevPosZ, holeInfo.center.x, holeInfo.center.z)
                        IntermediateHoleInfo(holeInfo.center, pos, box, dist, holeInfo.origin == HoleSnap.hole?.origin || dist - prevDist < -0.15)
                    }
            }.toList()
    }

    private fun horizontalDist(entity: Entity, vec: Vec3d): Double = DistanceKt.distance(entity.posX, entity.posZ, vec.x, vec.z)

    private fun calcPredict(entity: Entity, current: Vec3d): Vec3d = if (predictTicks == 0) current else Vec3d(entity.posX + (entity.posX - entity.prevPosX) * predictTicks, entity.posY + (entity.posY - entity.prevPosY) * predictTicks, entity.posZ + (entity.posZ - entity.prevPosZ) * predictTicks)

    private fun SafeClientEvent.placeBlock(slot: Slot, pos: BlockPos) {
        val target = pos.down()
        val packet = CPacketPlayerTryUseItemOnBlock(target, EnumFacing.UP, EnumHand.MAIN_HAND, 0.5f, 1.0f, 0.5f)
        ThreadSafetyKt.onMainThread {
            if (!player.isSneaking) {
                connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.START_SNEAKING))
                HotbarSwitchManager.ghostSwitch(this, slot) { connection.sendPacket(packet) }
                connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.STOP_SNEAKING))
            } else {
                HotbarSwitchManager.ghostSwitch(this, slot) { connection.sendPacket(packet) }
            }
            connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))
        }
        placeMap.put(pos.toLong(), System.currentTimeMillis() + fillTimeout)
        placeTimer.reset()
    }

    private class IntermediateHoleInfo(val center: Vec3d, val blockPos: BlockPos, val detectBox: AxisAlignedBB, val playerDist: Double, val toward: Boolean)
}
