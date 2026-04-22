package dev.wizard.meta.module.modules.combat

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.events.RunGameLoopEvent
import dev.wizard.meta.event.events.StepEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.WorldEvent
import dev.wizard.meta.event.events.combat.CrystalSetDeadEvent
import dev.wizard.meta.event.events.player.OnUpdateWalkingPlayerEvent
import dev.wizard.meta.event.events.player.PlayerMoveEvent
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.graphics.ESPRenderer
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.manager.managers.EntityManager
import dev.wizard.meta.manager.managers.HoleManager
import dev.wizard.meta.manager.managers.HotbarSwitchManager
import dev.wizard.meta.manager.managers.PlayerPacketManager
import dev.wizard.meta.manager.managers.TimerManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.client.AntiCheat
import dev.wizard.meta.module.modules.exploit.Clip
import dev.wizard.meta.module.modules.movement.HolePathFinder
import dev.wizard.meta.util.BoxRenderUtils
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.MovementUtils
import dev.wizard.meta.util.combat.CrystalUtils
import dev.wizard.meta.util.combat.HoleType
import dev.wizard.meta.util.extension.synchronized
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.inventory.slot.firstBlock
import dev.wizard.meta.util.math.rotation.RotationUtils
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.threads.onMainThreadSafe
import dev.wizard.meta.util.world.InteractKt
import dev.wizard.meta.util.world.PlaceInfo
import dev.wizard.meta.util.world.PlacementSearchOption
import dev.wizard.meta.util.world.checkPlaceRotation
import dev.wizard.meta.util.world.getGroundPos
import dev.wizard.meta.util.world.toPlacePacket
import it.unimi.dsi.fastutil.longs.Long2LongMaps
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSets
import net.minecraft.block.BlockObsidian
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityEnderCrystal
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.CPacketAnimation
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.client.CPacketUseEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import java.util.EnumMap
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.floor
import kotlin.math.sqrt

object Surround : Module(
    "Surround",
    category = Category.COMBAT,
    description = "Surrounds you with obsidian",
    priority = 200
) {
    private val ghostSwitchBypass by setting("Ghost Switch Bypass", HotbarSwitchManager.Override.DEFAULT)
    private val placeDelay by setting("Place delay", 25, 0..1000, 1)
    private val multiPlace by setting("Multi Place", 2, 1..12, 1)
    private val placeTimeout by setting("Place Timeout", 100, 0..1000, 10)
    private val strictDirection by setting("Strict Direction", false)
    private val autoCenter by setting("Auto Center", AutoCenterMode.FULL)
    private val centerTimeoutTicks by setting("Auto Center Timeout", 5, 0..20, 1, { autoCenter != AutoCenterMode.NONE })
    private val centerTimer by setting("Auto Center Timer", 2.0f, 1.0f..4.0f, 0.01f, { autoCenter != AutoCenterMode.NONE })
    private val extender by setting("Extend", 1, 1..4, 1, { autoCenter == AutoCenterMode.NONE })
    private val extendMove by setting("Move Extend", false, { extender > 1 })
    private val basePlace by setting("Base Place", true)
    private val autoDisable by setting("Auto Disable", AutoDisableMode.OUT_OF_HOLE)
    private val enableInHole by setting("Enable In Hole", false)
    private val inHoleTimeout by setting("In Hole Timeout", 50, 1..100, 5, { enableInHole }, "Delay before enabling Surround when you are in hole, in ticks")

    private val render by setting("Render", true)
    private val renderMode by setting("Render Mode", RenderMode.FADE, { render })
    private val renderColor by setting("Render Color", ColorRGB(255, 0, 0), { render })
    private val renderTime by setting("Render Time", 2000, 500..5000, 100, { render })

    private val toggleTimer = TickTimer(TimeUnit.TICKS)
    private val placeTimer = TickTimer()
    
    private val placing = EnumMap<SurroundOffset, MutableList<PlaceInfo>>(SurroundOffset::class.java).synchronized()
    private val placingSet = LongOpenHashSet()
    private val pendingPlacing = Long2LongMaps.synchronize(Long2LongOpenHashMap().apply { defaultReturnValue(-1L) })
    private val placed = LongSets.synchronize(LongOpenHashSet())
    private val renderBlocks = LinkedHashMap<BlockPos, Long>()
    private val extendingBlocks = HashSet<Vec3d>()
    private var extenders = 1
    private val renderer = ESPRenderer()
    private var holePos: BlockPos? = null
    private var enableTicks = 0

    override val isActive: Boolean
        get() = isEnabled && placing.isNotEmpty()

    init {
        onEnable {
            HolePathFinder.disable()
            Clip.disable()
            extendingBlocks.clear()
            extenders = 1
            
            if (autoCenter == AutoCenterMode.FULL) {
                SafeClientEvent.instance?.let {
                    val playerPos = EntityUtils.getBetterPosition(it.player)
                    if (!MovementUtils.isCentered(it.player, playerPos)) {
                        it.centerPlayer(playerPos, centerTimeoutTicks, centerTimer)
                    }
                }
            }
        }

        onDisable {
            placeTimer.reset(-114514L)
            toggleTimer.reset()
            placing.clear()
            placingSet.clear()
            pendingPlacing.clear()
            placed.clear()
            renderBlocks.clear()
            extendingBlocks.clear()
            extenders = 1
            holePos = null
            enableTicks = 0
        }

        safeListener<CrystalSetDeadEvent> { event ->
            if (event.crystals.none { it.getDistance(player) < 6.0 }) return@safeListener

            var placeCount = 0
            synchronized(placing) {
                val iterator = placing.values.iterator()
                while (iterator.hasNext()) {
                    val list = iterator.next()
                    var allPlaced = true
                    val subIterator = list.iterator()

                    while (subIterator.hasNext()) {
                        val placeInfo = subIterator.next()
                        if (event.crystals.none { CrystalUtils.blockPlaceBoxIntersectsCrystalBox(placeInfo.placedPos, it) }) {
                            if (placed.contains(placeInfo.placedPos.toLong())) continue
                            allPlaced = false
                            if (System.currentTimeMillis() <= pendingPlacing[placeInfo.placedPos.toLong()] || !checkRotation(placeInfo)) continue
                            
                            placeBlock(placeInfo)
                            if (++placeCount >= multiPlace) return@safeListener
                        }
                    }
                    if (allPlaced) iterator.remove()
                }
            }
        }

        safeListener<WorldEvent.ServerBlockUpdate> { event ->
            val pos = event.pos
            if (!event.newState.material.isReplaceable) {
                if (placingSet.contains(pos.toLong())) {
                    pendingPlacing.remove(pos.toLong())
                    placed.add(pos.toLong())
                }
            } else {
                val relative = pos.subtract(EntityUtils.getBetterPosition(player))
                if (SurroundOffset.values().any { it.offset == relative } && checkColliding(pos)) {
                    getNeighbor(pos)?.let {
                        if (checkRotation(it)) {
                            placingSet.add(it.placedPos.toLong())
                            placeBlock(it)
                        }
                    }
                }
            }
        }

        safeListener<OnUpdateWalkingPlayerEvent.Pre> {
            if (AntiCheat.blockPlaceRotation) {
                synchronized(placing) {
                    for (list in placing.values) {
                        for (placeInfo in list) {
                            if (placed.contains(placeInfo.placedPos.toLong())) continue
                            
                            PlayerPacketManager.sendPlayerPacket(modulePriority) {
                                var eyeHeight = player.getEyeHeight()
                                if (!player.isSneaking) eyeHeight -= 0.08f
                                rotate(RotationUtils.getRotationTo(Vec3d(player.posX, player.posY + eyeHeight, player.posZ), placeInfo.hitVec))
                            }
                            return@safeListener
                        }
                    }
                }
            }
        }

        safeListener<TickEvent.Pre> {
            enableTicks++
        }

        listener<StepEvent> {
            if (autoDisable == AutoDisableMode.NEVER) {
                placing.clear()
                placingSet.clear()
                pendingPlacing.clear()
                placed.clear()
                renderBlocks.clear()
                extendingBlocks.clear()
                extenders = 1
                holePos = null
            } else {
                disable()
            }
        }

        safeListener<RunGameLoopEvent.Tick>(true) {
            if (!player.onGround) {
                if (isEnabled) disable()
                return@safeListener
            }

            var playerPos = EntityUtils.getBetterPosition(player)
            val isInHole = player.onGround && MovementUtils.getRealSpeed(player) < 0.1 && HoleManager.getHoleInfo(playerPos).type == HoleType.OBBY
            
            if (isDisabled) {
                enableInHoleCheck(isInHole)
                return@safeListener
            }

            if (world.getBlockState(playerPos.down()).getCollisionBoundingBox(world, playerPos) == null) {
                playerPos = getGroundPos(world, player).up()
            }

            if (isInHole || holePos == null) {
                holePos = playerPos
            }

            if (HolePathFinder.isActive) {
                if (autoDisable == AutoDisableMode.NEVER) {
                     placing.clear()
                     placingSet.clear()
                     pendingPlacing.clear()
                     placed.clear()
                     renderBlocks.clear()
                     extendingBlocks.clear()
                     extenders = 1
                     holePos = null
                } else {
                    disable()
                }
                return@safeListener
            }

            if (basePlace) {
                val basePos = playerPos.down()
                if (world.getBlockState(basePos).material.isReplaceable) {
                     val seq = InteractKt.getPlacementSequence(this, basePos, 2, 
                        PlacementSearchOption.range(5.0),
                        if (strictDirection) PlacementSearchOption.VISIBLE_SIDE else null,
                        { _, _, _, to -> to != playerPos }
                     )
                     
                     seq?.forEach {
                         if (checkRotation(it)) {
                             placeBlock(it)
                         }
                     }
                }
            }

            extenders = 1
            updatePlacingMap(playerPos)
            
            if (placing.isNotEmpty() && placeTimer.tickAndReset(placeDelay.toLong())) {
                runPlacing()
            }

            if (autoCenter == AutoCenterMode.NONE) {
                processExtendingBlocks(playerPos)
            }
        }

        safeListener<PlayerMoveEvent.Pre> {
            val holePos = holePos ?: return@safeListener
            
            when (autoCenter) {
                AutoCenterMode.SOFT -> {
                    if (shouldCenterForPlacement(holePos)) {
                        centerPlayer(holePos, centerTimeoutTicks, centerTimer)
                    }
                }
                AutoCenterMode.FULL -> {
                    if (!MovementUtils.isCentered(player, holePos) && (placing.isNotEmpty() || !isSurroundPlaceable(holePos))) {
                        centerPlayer(holePos, centerTimeoutTicks, centerTimer)
                    }
                }
                else -> {}
            }
        }

        safeListener<Render3DEvent> {
            if (render) {
                renderer.clear()
                val iterator = renderBlocks.entries.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    val pos = entry.key
                    val timestamp = entry.value
                    val timeElapsed = System.currentTimeMillis() - timestamp
                    val timeLeft = renderTime - timeElapsed
                    val progress = timeElapsed.toFloat() / renderTime
                    
                    if (timeLeft <= 0) continue

                    val box = when (renderMode) {
                        RenderMode.FADE, RenderMode.STATIC -> AxisAlignedBB(pos)
                        RenderMode.GROW -> BoxRenderUtils.calcGrowBox(pos, progress.toDouble())
                        RenderMode.SHRINK -> BoxRenderUtils.calcGrowBox(pos, 1.0 - progress.toDouble())
                        RenderMode.RISE -> BoxRenderUtils.calcRiseBox(pos, progress.toDouble())
                    }

                    val alpha = if (renderMode == RenderMode.STATIC) 255 else ((timeLeft.toFloat() / renderTime * 255).toInt()).coerceIn(0, 255)
                    
                    renderer.aFilled = if (renderMode == RenderMode.FADE) (alpha * 0.15f).toInt() else 31
                    renderer.aOutline = if (renderMode == RenderMode.FADE) (alpha * 0.9f).toInt() else 233
                    renderer.add(box, renderColor.alpha(alpha))
                }
                renderer.render(true)
                renderBlocks.entries.removeIf { System.currentTimeMillis() - it.value >= renderTime }
            }
        }
    }

    private fun SafeClientEvent.processExtendingBlocks(playerPos: BlockPos) {
        if (extendingBlocks.size == 2 && extenders < extender) {
            val closeVec = areClose(extendingBlocks.toTypedArray(), playerPos)
            if (closeVec != null) {
                val closePos = BlockPos(closeVec)
                val unsafeBlocks = getUnsafeBlocksAround(closePos)
                
                for (offset in unsafeBlocks) {
                    val offsetPos = closePos.add(offset)
                    if (!world.getBlockState(offsetPos).material.isReplaceable) continue
                    
                    val seq = InteractKt.getPlacementSequence(this, offsetPos, 2, 
                        PlacementSearchOption.range(5.0),
                        if (strictDirection) PlacementSearchOption.VISIBLE_SIDE else null,
                        { _, _, _, to -> to != closePos }
                    ) ?: continue
                    
                    if (seq.isEmpty()) continue

                    val surroundOffset = SurroundOffset.values().firstOrNull { it.offset == offset } ?: continue
                    placing[surroundOffset] = seq.toMutableList()
                    seq.forEach { placingSet.add(it.placedPos.toLong()) }
                    extendingBlocks.clear()
                    return
                }
            }
        } else if (extendingBlocks.size > 2 || extenders >= extender) {
            extendingBlocks.clear()
        }
    }

    private fun SafeClientEvent.areClose(vecs: Array<Vec3d>, playerPos: BlockPos): Vec3d? {
        var matches = 0
        val unsafeBlocks = getUnsafeBlocksAround(playerPos)
        
        for (vec in vecs) {
            for (offset in unsafeBlocks) {
                if (vec.x == offset.x.toDouble() && vec.y == offset.y.toDouble() && vec.z == offset.z.toDouble()) {
                    matches++
                }
            }
        }
        
        if (matches == 2) {
             return player.positionVector.add(vecs[0].add(vecs[1]))
        }
        return null
    }

    private fun SafeClientEvent.getUnsafeBlocksAround(pos: BlockPos): List<BlockPos> {
        val unsafeBlocks = ArrayList<BlockPos>()
        for (offset in SurroundOffset.values()) {
            val offsetPos = pos.add(offset.offset)
            if (world.getBlockState(offsetPos).material.isReplaceable) {
                unsafeBlocks.add(offset.offset)
            }
        }
        return unsafeBlocks
    }

    private fun SafeClientEvent.isSurroundPlaceable(holePos: BlockPos): Boolean {
        for (offset in SurroundOffset.values()) {
            val offsetPos = holePos.add(offset.offset)
            if (world.getBlockState(offsetPos).material.isReplaceable) return true
        }
        return false
    }

    private fun enableInHoleCheck(isInHole: Boolean) {
        if (enableInHole && isInHole) {
            if (toggleTimer.tickAndReset(inHoleTimeout.toLong())) {
                enable()
            }
        } else {
            toggleTimer.reset()
        }
    }

    private fun SafeClientEvent.updatePlacingMap(playerPos: BlockPos) {
        synchronized(pendingPlacing) {
            pendingPlacing.values.removeIf { System.currentTimeMillis() > it }
            if (placing.isEmpty() && pendingPlacing.values.all { System.currentTimeMillis() > it }) {
                placing.clear()
                placed.clear()
            }
        }

        for (offset in SurroundOffset.values()) {
            val offsetPos = playerPos.add(offset.offset)
            if (!world.getBlockState(offsetPos).material.isReplaceable) continue

            if (autoCenter == AutoCenterMode.NONE && extender > 1) {
                val canPlace = InteractKt.getPlacementSequence(this, offsetPos, 2,
                     PlacementSearchOption.range(5.0),
                     if (strictDirection) PlacementSearchOption.VISIBLE_SIDE else null,
                     { _, _, _, to -> to != playerPos }
                ) != null
                
                if (!canPlace && (!extendMove || MovementUtils.getRealSpeed(player) != 0.0) && extenders < extender) {
                    extendingBlocks.add(Vec3d(offset.offset.x.toDouble(), offset.offset.y.toDouble(), offset.offset.z.toDouble()))
                    extenders++
                    continue
                }
            }

            val seq = InteractKt.getPlacementSequence(this, offsetPos, 2,
                 PlacementSearchOption.range(5.0),
                 if (strictDirection) PlacementSearchOption.VISIBLE_SIDE else null,
                 { _, _, _, to -> to != playerPos }
            ) ?: continue

            placing[offset] = seq.toMutableList()
            seq.forEach { placingSet.add(it.placedPos.toLong()) }
        }
    }

    private fun SafeClientEvent.runPlacing() {
        var placeCount = 0
        synchronized(placing) {
            val iterator = placing.values.iterator()
            while (iterator.hasNext()) {
                val list = iterator.next()
                var allPlaced = true
                val crystalsToBreak = LinkedHashSet<Entity>()
                
                for (placeInfo in list) {
                    if (placed.contains(placeInfo.placedPos.toLong())) continue
                    allPlaced = false
                    
                    if (System.currentTimeMillis() <= pendingPlacing[placeInfo.placedPos.toLong()] || !checkRotation(placeInfo)) continue
                    
                    for (entity in EntityManager.entity) {
                        if (!entity.isEntityAlive || !entity.preventEntitySpawning) continue
                        if (entity !is EntityEnderCrystal) {
                            if (entity.entityBoundingBox.intersectsWith(AxisAlignedBB(placeInfo.placedPos))) {
                                // Blocked by non-crystal entity, skip this position
                                break // continue outer loop logic by breaking inner? No, we need to skip this placeInfo
                            }
                        } else {
                             if (entity.entityBoundingBox.intersectsWith(AxisAlignedBB(placeInfo.placedPos))) {
                                 crystalsToBreak.add(entity)
                             }
                        }
                    }

                    if (crystalsToBreak.isNotEmpty()) {
                        for (crystal in crystalsToBreak) {
                            connection.sendPacket(CPacketUseEntity(crystal))
                            connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))
                        }
                        crystalsToBreak.clear()
                        continue
                    }

                    placeBlock(placeInfo)
                    if (++placeCount >= multiPlace) return
                }
                
                if (allPlaced) iterator.remove()
            }
        }

        if (autoDisable == AutoDisableMode.ONE_TIME && placing.isEmpty()) {
            disable()
        }
    }

    private fun SafeClientEvent.getNeighbor(pos: BlockPos): PlaceInfo? {
        for (side in EnumFacing.values()) {
            val offsetPos = pos.offset(side)
            val oppositeSide = side.opposite
            
            if (strictDirection) {
                if (!InteractKt.getVisibleSides(this, offsetPos, true).contains(oppositeSide)) continue
            }

            if (world.getBlockState(offsetPos).material.isReplaceable) continue

            val hitVec = InteractKt.getHitVec(offsetPos, oppositeSide)
            val hitVecOffset = InteractKt.getHitVecOffset(oppositeSide)
            
            return PlaceInfo(offsetPos, oppositeSide, 0.0, hitVecOffset, hitVec, pos)
        }
        return null
    }

    private fun SafeClientEvent.shouldCenterForPlacement(holePos: BlockPos): Boolean {
        val playerBox = player.entityBoundingBox
        for (offset in SurroundOffset.values()) {
             val offsetPos = holePos.add(offset.offset)
             if (!world.getBlockState(offsetPos).material.isReplaceable) continue
             if (playerBox.intersectsWith(AxisAlignedBB(offsetPos))) return true
        }
        return false
    }

    private fun SafeClientEvent.centerPlayer(target: Any, timeoutTicks: Int, timerSpeed: Float) {
        val (centerX, centerZ) = when (target) {
            is BlockPos -> target.x + 0.5 to target.z + 0.5
            is Vec3d -> target.x to target.z
            else -> {
                val pos = BlockPos(floor(player.posX), floor(player.posY), floor(player.posZ))
                pos.x + 0.5 to pos.z + 0.5
            }
        }

        for (i in 0 until timeoutTicks) {
            if (!MovementUtils.isCentered(player, centerX, centerZ)) {
                var x = centerX - player.posX
                var z = centerZ - player.posZ
                val speed = sqrt(x * x + z * z)
                val baseSpeed = if (player.isSneaking) 0.05746 else 0.2873
                val maxSpeed = MovementUtils.applySpeedPotionEffects(player, baseSpeed)
                
                if (speed > maxSpeed) {
                    val mult = maxSpeed / speed
                    x *= mult
                    z *= mult
                }
                
                player.motionX = 0.0
                player.rotationYaw = 0.0f
                player.setPosition(player.posX + x, player.posY, player.posZ + z)
                
                TimerManager.modifyTimer(Surround, 50.0f / timerSpeed, 2)
                
                if (MovementUtils.isCentered(player, centerX, centerZ)) {
                    TimerManager.resetTimer(Surround)
                }
            } else {
                break
            }
        }
        TimerManager.resetTimer(Surround)
    }

    private fun SafeClientEvent.checkColliding(pos: BlockPos): Boolean {
        val box = AxisAlignedBB(pos)
        return EntityManager.entity.none { 
            it.isEntityAlive && it.preventEntitySpawning && it.entityBoundingBox.intersectsWith(box)
        }
    }

    private fun SafeClientEvent.placeBlock(placeInfo: PlaceInfo) {
        val slot = getSlot()
        if (slot == null) {
            disable()
            return
        }

        val sneak = !player.isSneaking
        if (sneak) {
             connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.START_SNEAKING))
        }

        HotbarSwitchManager.ghostSwitch(this, ghostSwitchBypass, slot) {
            connection.sendPacket(InteractKt.toPlacePacket(placeInfo, EnumHand.MAIN_HAND))
        }
        connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))
        
        if (sneak) {
            connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.STOP_SNEAKING))
        }

        onMainThreadSafe {
             Blocks.OBSIDIAN.getStateForPlacement(world, placeInfo.pos, placeInfo.direction, placeInfo.hitVecOffset.x, placeInfo.hitVecOffset.y, placeInfo.hitVecOffset.z, 0, player, EnumHand.MAIN_HAND)
        }

        pendingPlacing[placeInfo.placedPos.toLong()] = System.currentTimeMillis() + placeTimeout
        renderBlocks[placeInfo.placedPos] = System.currentTimeMillis()
    }

    private fun SafeClientEvent.getSlot() = player.inventory.firstBlock(Blocks.OBSIDIAN) ?: run {
        NoSpamMessage.sendMessage("$chatName No obsidian in inventory!")
        null
    }

    private fun SafeClientEvent.checkRotation(placeInfo: PlaceInfo): Boolean {
        return !AntiCheat.blockPlaceRotation || InteractKt.checkPlaceRotation(this, placeInfo)
    }

    private enum class AutoCenterMode(override val displayName: String) : DisplayEnum {
        NONE("Off"),
        SOFT("Soft"),
        FULL("Full")
    }

    private enum class AutoDisableMode(override val displayName: String) : DisplayEnum {
        NEVER("Never"),
        ONE_TIME("Once"),
        OUT_OF_HOLE("Out of Hole")
    }

    private enum class RenderMode(override val displayName: String) : DisplayEnum {
        FADE("Fade"),
        GROW("Grow"),
        SHRINK("Shrink"),
        RISE("Rise"),
        STATIC("Static")
    }

    private enum class SurroundOffset(val offset: BlockPos) {
        DOWN(BlockPos(0, -1, 0)),
        NORTH(BlockPos(0, 0, -1)),
        EAST(BlockPos(1, 0, 0)),
        SOUTH(BlockPos(0, 0, 1)),
        WEST(BlockPos(-1, 0, 0))
    }
}
