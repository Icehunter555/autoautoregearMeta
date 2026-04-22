package dev.wizard.meta.module.modules.player

import dev.fastmc.common.MathUtilKt
import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.fastmc.common.toRadians
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.RunGameLoopEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.player.OnUpdateWalkingPlayerEvent
import dev.wizard.meta.event.events.player.PlayerMoveEvent
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.graphics.ESPRenderer
import dev.wizard.meta.graphics.Easing
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.manager.managers.PlayerPacketManager
import dev.wizard.meta.manager.managers.TimerManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.*
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.MovementUtils
import dev.wizard.meta.util.accessor.unpressKey
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.inventory.operation.swapToSlot
import dev.wizard.meta.util.inventory.slot.*
import dev.wizard.meta.util.math.RotationUtils
import dev.wizard.meta.util.math.vector.Vec2f
import net.minecraft.block.Block
import net.minecraft.client.renderer.ActiveRenderInfo
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.init.Blocks
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.EnumHandSide
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import java.util.*

object Scaffold : Module(
    "Scaffold",
    category = Category.PLAYER,
    description = "Places blocks under you",
    modulePriority = 500
) {
    private val safeWalk by setting(this, BooleanSetting(settingName("Safe Walk"), false))
    private val rotate by setting(this, BooleanSetting(settingName("Rotate"), true))
    private val rotateTimer by setting(this, IntegerSetting(settingName("Rotate Timer"), 800, 0..1000, 1, { rotate }))
    private val packet by setting(this, BooleanSetting(settingName("Packet"), false))
    private val placeDelay by setting(this, IntegerSetting(settingName("Place Delay"), 100, 0..500, 1))
    private val towerMode by setting(this, EnumSetting(settingName("Tower Mode"), TowerMode.OFF))
    private val timerTower by setting(this, BooleanSetting(settingName("Timer Tower"), false, { towerMode == TowerMode.TIMER }))
    private val timerSpeed by setting(this, FloatSetting(settingName("Timer Speed"), 1.08f, 1.01f..2.0f, 0.01f, { timerTower }))
    private val bypassDelay by setting(this, DoubleSetting(settingName("Bypass Delay"), 1.0, 0.1..5.0, 0.1, { towerMode == TowerMode.BYPASS }))

    private val down by setting(this, BooleanSetting(settingName("Down"), true))
    private val sameY by setting(this, BooleanSetting(settingName("Same Y"), false))
    private val yCheck by setting(this, FloatSetting(settingName("Y Check"), 2.5f, 2.5f..12.0f, 0.1f, { sameY }))
    private val airCheck by setting(this, BooleanSetting(settingName("Air Check"), true, { sameY }))
    private val search by setting(this, BooleanSetting(settingName("Search"), true))
    private val allowUp by setting(this, BooleanSetting(settingName("Allow Up"), false, { search }))
    private val range by setting(this, FloatSetting(settingName("Range"), 3.5f, 2.5f..5.0f, 0.1f, { search }))

    private val swapBack by setting(this, BooleanSetting(settingName("Swap Back"), true))
    private val swapDelay by setting(this, IntegerSetting(settingName("Swap Delay"), 50, 0..500, 1, { swapBack }))

    private val render by setting(this, BooleanSetting(settingName("Render"), true))
    private val filled by setting(this, BooleanSetting(settingName("Filled"), true, { render }))
    private val outline by setting(this, BooleanSetting(settingName("Outline"), true, { render }))
    private val filledColor by setting(this, ColorSetting(settingName("Filled Color"), ColorRGB(64, 255, 64, 31), true, { render && filled }))
    private val lineWidth by setting(this, FloatSetting(settingName("Line Width"), 2.0f, 0.5f..5.0f, 0.1f, { render && outline }))
    private val animate by setting(this, BooleanSetting(settingName("Animate"), true, { render }))
    private val movingLength by setting(this, IntegerSetting(settingName("Moving Length"), 200, 0..1000, 50, { render && animate }))
    private val fadeLength by setting(this, IntegerSetting(settingName("Fade Length"), 200, 0..1000, 50, { render && animate }))

    private var placePos: BlockPos? = null
    private val placeTimer = TickTimer()
    private val towerTimer = TickTimer()
    private val rotationTimer = TickTimer()
    private val swapBackTimer = TickTimer()
    private val bypassTimer = TickTimer()

    private var lastYaw = 0.0f
    private var lastPitch = 0.0f
    private var originalSlot: HotbarSlot? = null
    private val renderer = ESPRenderer()
    private var isTowering = false

    private var lastRenderPos: BlockPos? = null
    private var prevBox: AxisAlignedBB? = null
    private var currentBox: AxisAlignedBB? = null
    private var lastRenderBox: AxisAlignedBB? = null
    private var lastUpdateTime = 0L
    private var startTime = 0L
    private var scaleValue = 0.0f
    private var isFadingOut = false

    private val blockList = mutableListOf(Blocks.OBSIDIAN, Blocks.ENDER_CHEST, Blocks.TNT)

    override fun isActive(): Boolean = isEnabled

    init {
        onDisable {
            placePos = null
            placeTimer.reset(-69420L)
            towerTimer.reset(-69420L)
            rotationTimer.reset(-69420L)
            swapBackTimer.reset(-69420L)
            bypassTimer.reset(-69420L)
            originalSlot = null
            renderer.clear()
            isTowering = false
            TimerManager.resetTimer(this)
            resetAnimation()
        }

        safeListener<PacketEvent.PostReceive> {
            if (it.packet is SPacketPlayerPosLook) {
                placePos = null
            }
        }

        safeListener<PlayerMoveEvent.Pre>(-100) {
            if (!safeWalk) return@safeListener
            if (down && mc.gameSettings.keyBindSneak.isKeyDown) return@safeListener
            if (!player.onGround) return@safeListener

            var x = it.x
            var z = it.z
            val increment = 0.05
            while (x != 0.0 && world.getCollisionBoxes(player, player.entityBoundingBox.offset(x, -1.0, 0.0)).isEmpty()) {
                x = if (x < increment && x >= -increment) 0.0 else if (x > 0.0) x - increment else x + increment
            }
            while (z != 0.0 && world.getCollisionBoxes(player, player.entityBoundingBox.offset(0.0, -1.0, z)).isEmpty()) {
                z = if (z < increment && z >= -increment) 0.0 else if (z > 0.0) z - increment else z + increment
            }
            while (x != 0.0 && z != 0.0 && world.getCollisionBoxes(player, player.entityBoundingBox.offset(x, -1.0, z)).isEmpty()) {
                x = if (x < increment && x >= -increment) 0.0 else if (x > 0.0) x - increment else x + increment
                z = if (z < increment && z >= -increment) 0.0 else if (z > 0.0) z - increment else z + increment
            }
            it.x = x
            it.z = z
        }

        safeListener<PlayerMoveEvent.Pre> {
            if (!shouldTower(this)) return@safeListener
            when (towerMode) {
                TowerMode.MOTION, TowerMode.STRICT -> {
                    player.motionX = 0.0
                    player.motionZ = 0.0
                    if (towerMode == TowerMode.MOTION && player.ticksExisted % 5 == 0) {
                        player.motionY = 0.42
                    }
                }
                else -> {}
            }
        }

        safeListener<OnUpdateWalkingPlayerEvent.Pre> {
            if (rotate && !rotationTimer.tick(rotateTimer.toLong())) {
                PlayerPacketManager.sendPlayerPacket {
                    rotate(Vec2f(lastYaw, lastPitch))
                }
            }

            if (towerMode == TowerMode.BYPASS && shouldTower(this) && bypassTimer.tick((100.0 * bypassDelay).toLong())) {
                PlayerPacketManager.sendPlayerPacket {
                    onGround(true)
                }
                player.jump()
                bypassTimer.reset()
            }
        }

        safeListener<RunGameLoopEvent.Tick> {
            updatePlacePos(this)
            handleTowerJumpModification(this)
            runScaffold(this)
            handleSwapBack(this)
            handleTimerTower(this)
        }

        safeListener<Render3DEvent> {
            if (render) {
                if (animate) renderAnimated()
                else renderer.render(false)
            }
        }
    }

    private fun shouldTower(event: SafeClientEvent): Boolean {
        return towerMode != TowerMode.OFF && mc.gameSettings.keyBindJump.isKeyDown && !MovementUtils.isMoving(event.player) && getBlockSlot(event) != null
    }

    private fun handleTowerJumpModification(event: SafeClientEvent) {
        if (towerMode == TowerMode.STRICT && shouldTower(event)) {
            if (event.player.motionY > 0.0 && event.player.motionY < 0.42) {
                event.player.motionY = 0.36834
            }
        }
    }

    private fun updatePlacePos(event: SafeClientEvent) {
        if (placePos == null) {
            placePos = BlockPos(event.player.posX, event.player.posY - 1.0, event.player.posZ)
        }
        val currentFloorY = Math.floor(event.player.posY - 1.0).toInt()
        val placeY = placePos!!.y

        val shouldUpdateY = !sameY || (event.player.posY - placeY > yCheck) || (airCheck && !event.world.getBlockState(BlockPos(event.player.posX, event.player.posY - 1.0, event.player.posZ)).material.isReplaceable) || (mc.gameSettings.keyBindJump.isKeyDown && !MovementUtils.isMoving(event.player)) || currentFloorY < placeY

        if (shouldUpdateY) {
            val newY = if (down && mc.gameSettings.keyBindSneak.isKeyDown) event.player.posY - 2.0 else event.player.posY - 1.0
            placePos = BlockPos(event.player.posX, newY, event.player.posZ)
        } else {
            placePos = BlockPos(event.player.posX, placeY.toDouble(), event.player.posZ)
        }
    }

    private fun runScaffold(event: SafeClientEvent) {
        if (!placeTimer.tick(placeDelay.toLong())) return
        val targetPos = placePos ?: return

        if (event.player.getDistanceSqToCenter(targetPos) > 36.0) return
        if (!event.world.getBlockState(targetPos).material.isReplaceable) return

        var pos = targetPos
        if (search && getFirstFacing(event, pos) == null) {
            var bestPos: BlockPos? = null
            var distance = 1000.0
            var onlyDown = !allowUp

            for (searchPos in getBlocksInRange(event, pos, range)) {
                if (!canPlace(event, searchPos)) continue
                val dist = pos.getDistance(searchPos.x, searchPos.y, searchPos.z)
                if ((bestPos == null || dist < distance) && !onlyDown) {
                    bestPos = searchPos
                    distance = dist
                }
                if (bestPos != null && dist < distance && pos.y >= searchPos.y) {
                    bestPos = searchPos
                    distance = dist
                    onlyDown = true
                }
            }
            pos = bestPos ?: return
        }

        if (!canPlace(event, pos)) return
        val slot = getBlockSlot(event) ?: return
        val side = getFirstFacing(event, pos) ?: return

        val neighbour = pos.offset(side)
        val opposite = side.opposite
        val hitVec = Vec3d(neighbour).add(0.5, 0.5, 0.5).add(Vec3d(opposite.directionVec).scale(0.5))

        if (towerMode == TowerMode.VANILLA && shouldTower(event)) {
            event.player.motionX = 0.0
            event.player.motionZ = 0.0
            event.player.jump()
            if (towerTimer.tick(1500L)) {
                event.player.motionY = -0.28
                towerTimer.reset()
            }
        }

        val rotation = RotationUtils.getRotationTo(event, hitVec)
        lastYaw = Vec2f.getX(rotation)
        lastPitch = Vec2f.getY(rotation)
        rotationTimer.reset()

        if (swapBack && originalSlot == null) {
            originalSlot = event.player.hotbarSlots.firstOrNull { it.hotbarSlot == event.player.inventory.currentItem }
        }

        if (!event.player.isSneaking) {
            event.connection.sendPacket(CPacketEntityAction(event.player, CPacketEntityAction.Action.START_SNEAKING))
            event.swapToSlot(slot)
            if (rotate) {
                PlayerPacketManager.sendPlayerPacket { rotate(rotation) }
            }
            placeBlock(event, pos, side, hitVec, packet)
            event.connection.sendPacket(CPacketEntityAction(event.player, CPacketEntityAction.Action.STOP_SNEAKING))
        } else {
            event.swapToSlot(slot)
            if (rotate) {
                PlayerPacketManager.sendPlayerPacket { rotate(rotation) }
            }
            placeBlock(event, pos, side, hitVec, packet)
        }

        if (swapBack && originalSlot != null) swapBackTimer.reset()
        placeTimer.reset()

        if (render) {
            if (animate) updateAnimation(pos)
            else {
                renderer.clear()
                renderer.setAFilled(filledColor.a)
                renderer.setAOutline(255)
                renderer.setThickness(lineWidth)
                if (filled) renderer.add(AxisAlignedBB(pos), filledColor)
            }
        }
    }

    private fun getBlockSlot(event: SafeClientEvent): HotbarSlot? {
        return event.player.hotbarSlots.firstByStack {
            it.item is ItemBlock && (it.item as ItemBlock).block.defaultState.isFullBlock && blockList.contains((it.item as ItemBlock).block)
        }
    }

    private fun canPlace(event: SafeClientEvent, pos: BlockPos): Boolean {
        return event.world.getBlockState(pos).material.isReplaceable && getFirstFacing(event, pos) != null
    }

    private fun getFirstFacing(event: SafeClientEvent, pos: BlockPos): EnumFacing? {
        for (side in EnumFacing.values()) {
            val offset = pos.offset(side)
            if (!event.world.getBlockState(offset).material.isReplaceable) return side
        }
        return null
    }

    private fun getBlocksInRange(event: SafeClientEvent, center: BlockPos, range: Float): List<BlockPos> {
        val positions = ArrayList<BlockPos>()
        val rangeInt = range.toInt()
        for (x in -rangeInt..rangeInt) {
            for (y in -rangeInt..rangeInt) {
                for (z in -rangeInt..rangeInt) {
                    val pos = center.add(x, y, z)
                    if (center.getDistance(pos.x, pos.y, pos.z) <= range) {
                        positions.add(pos)
                    }
                }
            }
        }
        return positions
    }

    private fun placeBlock(event: SafeClientEvent, pos: BlockPos, side: EnumFacing, hitVec: Vec3d, packet: Boolean) {
        val neighbour = pos.offset(side)
        val opposite = side.opposite
        if (packet) {
            event.connection.sendPacket(CPacketPlayerTryUseItemOnBlock(neighbour, opposite, EnumHand.MAIN_HAND, hitVec.x.toFloat(), hitVec.y.toFloat(), hitVec.z.toFloat()))
        } else {
            event.playerController.processRightClickBlock(event.player, event.world, neighbour, opposite, hitVec, EnumHand.MAIN_HAND)
        }
        event.player.swingArm(EnumHand.MAIN_HAND)
    }

    private fun handleSwapBack(event: SafeClientEvent) {
        if (swapBack && originalSlot != null && swapBackTimer.tick(swapDelay.toLong())) {
            event.swapToSlot(originalSlot!!)
            originalSlot = null
            swapBackTimer.reset(-69420L)
        }
    }

    private fun handleTimerTower(event: SafeClientEvent) {
        if (towerMode != TowerMode.TIMER) {
            if (isTowering) {
                isTowering = false
                TimerManager.resetTimer(this)
            }
            return
        }
        val shouldTower = shouldTower(event)
        if (shouldTower && !isTowering) {
            isTowering = true
            if (timerTower) TimerManager.modifyTimer(this, 50.0f / timerSpeed)
        } else if (!shouldTower && isTowering) {
            isTowering = false
            TimerManager.resetTimer(this)
        }
    }

    private fun resetAnimation() {
        lastRenderPos = null
        prevBox = null
        currentBox = null
        lastRenderBox = null
        lastUpdateTime = 0L
        startTime = 0L
        scaleValue = 0.0f
        isFadingOut = false
    }

    private fun updateAnimation(pos: BlockPos?) {
        val posChanged = pos != lastRenderPos
        if (posChanged) {
            if (pos != null) {
                lastRenderPos = pos
                isFadingOut = false
                currentBox = AxisAlignedBB(pos)
                prevBox = lastRenderBox ?: currentBox
                lastUpdateTime = System.currentTimeMillis()
                if (lastRenderBox == null) startTime = System.currentTimeMillis()
            } else {
                lastRenderPos = null
                isFadingOut = true
                lastUpdateTime = System.currentTimeMillis()
                startTime = System.currentTimeMillis()
            }
        } else if (pos != null) {
            currentBox = AxisAlignedBB(pos)
        }
    }

    private fun renderAnimated() {
        if (isFadingOut) {
            lastRenderBox?.let { last ->
                scaleValue = Easing.IN_CUBIC.dec(Easing.toDelta(startTime, fadeLength.toLong()))
                if (scaleValue > 0.0f) {
                    renderWithAlpha(last, scaleValue)
                } else {
                    prevBox = null
                    currentBox = null
                    lastRenderBox = null
                    renderer.clear()
                }
            }
            return
        }

        prevBox?.let { prev ->
            currentBox?.let { curr ->
                val multiplier = Easing.OUT_QUART.inc(Easing.toDelta(lastUpdateTime, movingLength.toLong()))
                val renderBox = interpolateBox(prev, curr, multiplier.toDouble())
                scaleValue = Easing.OUT_CUBIC.inc(Easing.toDelta(startTime, fadeLength.toLong()))
                if (scaleValue > 0.0f) {
                    renderWithAlpha(renderBox, scaleValue)
                }
                lastRenderBox = renderBox
                return
            }
        }

        currentBox?.let { curr ->
            prevBox = curr
            lastRenderBox = curr
            lastUpdateTime = System.currentTimeMillis()
            startTime = System.currentTimeMillis()
        }
    }

    private fun renderWithAlpha(box: AxisAlignedBB, alphaScale: Float) {
        renderer.clear()
        renderer.setThickness(lineWidth)
        val color = ColorRGB(filledColor.r, filledColor.g, filledColor.b, (filledColor.a * alphaScale).toInt())
        if (filled) renderer.add(box, color)
        renderer.setAFilled(if (filled) (filledColor.a * alphaScale).toInt() else 0)
        renderer.setAOutline(if (outline) (255 * alphaScale).toInt() else 0)
        renderer.render(false)
    }

    private fun interpolateBox(from: AxisAlignedBB, to: AxisAlignedBB, progress: Double): AxisAlignedBB {
        return AxisAlignedBB(
            from.minX + (to.minX - from.minX) * progress,
            from.minY + (to.minY - from.minY) * progress,
            from.minZ + (to.minZ - from.minZ) * progress,
            from.maxX + (to.maxX - from.maxX) * progress,
            from.maxY + (to.maxY - from.maxY) * progress,
            from.maxZ + (to.maxZ - from.maxZ) * progress
        )
    }

    private enum class TowerMode(override val displayName: CharSequence) : DisplayEnum {
        OFF("Off"), VANILLA("Vanilla"), MOTION("Motion"), STRICT("Strict"), BYPASS("Bypass"), TIMER("Timer")
    }
}
