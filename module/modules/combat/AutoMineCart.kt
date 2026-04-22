package dev.wizard.meta.module.modules.combat

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.graphics.ESPRenderer
import dev.wizard.meta.graphics.Easing
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.manager.managers.CombatManager
import dev.wizard.meta.manager.managers.EntityManager
import dev.wizard.meta.manager.managers.HoleManager
import dev.wizard.meta.manager.managers.HotbarSwitchManager
import dev.wizard.meta.manager.managers.PlayerPacketManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.inventory.slot.DefinedKt
import dev.wizard.meta.util.inventory.slot.IterableKt
import dev.wizard.meta.util.math.RotationUtils
import dev.wizard.meta.util.math.vector.ConversionKt
import dev.wizard.meta.util.math.vector.DistanceKt
import dev.wizard.meta.util.world.BlockKt
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityMinecartTNT
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.network.play.client.CPacketPlayerDigging
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos

object AutoMineCart : Module(
    "AutoMineCart",
    category = Category.COMBAT,
    description = "minecarter",
    modulePriority = 100
) {
    private val placeDelay by setting(this, IntegerSetting(settingName("Place Delay"), 100, 0..5000, 10))
    private val range by setting(this, FloatSetting(settingName("Range"), 6.0f, 1.0f..10.0f, 0.5f))
    private val ghostSwitchB by setting(this, EnumSetting(settingName("Ghost Switch Bypass"), HotbarSwitchManager.Override.NONE))
    private val onlyHole by setting(this, BooleanSetting(settingName("Only Safe Target"), false))
    private val onlySelfHole by setting(this, BooleanSetting(settingName("Only when safe"), false))
    private val limitPlacements by setting(this, BooleanSetting(settingName("Limit Placements"), false))
    private val autoDisable by setting(this, BooleanSetting(settingName("Auto Disable"), false))
    private val autoIgnite by setting(this, BooleanSetting(settingName("Auto Ignite"), false))
    private val disableThreshold by setting(this, IntegerSetting(settingName("Disable Threshold"), 21, 1..30, 1, { autoDisable }))
    private val igniteDelay by setting(this, IntegerSetting(settingName("Ignite Delay"), 20, 0..50, 1, { autoIgnite }))
    private val placeMode by setting(this, EnumSetting(settingName("Place Mode"), PlaceMode.PACKET))
    private val render by setting(this, BooleanSetting(settingName("Render"), true))
    private val filled by setting(this, BooleanSetting(settingName("Filled"), true, { render }))
    private val outline by setting(this, BooleanSetting(settingName("Outline"), true, { render }))
    private val railStageColor by setting(this, ColorSetting(settingName("Rail Stage Color"), ColorRGB(255, 165, 0, 100), { filled }))
    private val cartStageColor by setting(this, ColorSetting(settingName("Cart Stage Color"), ColorRGB(255, 64, 64, 100), { filled }))
    private val igniteStageColor by setting(this, ColorSetting(settingName("Ignite Stage Color"), ColorRGB(64, 255, 64, 100), { filled && autoIgnite }))
    private val lineWidth by setting(this, FloatSetting(settingName("Line Width"), 2.0f, 0.5f..5.0f, 0.1f, { outline }))
    private val animate by setting(this, BooleanSetting(settingName("Animate"), true, { render }))
    private val movingLength by setting(this, IntegerSetting(settingName("Moving Length"), 200, 0..1000, 50, { animate && render }))
    private val fadeLength by setting(this, IntegerSetting(settingName("Fade Length"), 200, 0..1000, 50, { animate && render }))

    private var placeCounter = 0
    private var currentTarget: EntityLivingBase? = null
    private var igniteWait = 0
    private var isPlacing = true
    private val placeTimer = TickTimer(TimeUnit.TICKS)
    private val webTimer = TickTimer()
    private val renderer = ESPRenderer()

    private var lastRenderPos: BlockPos? = null
    private var prevBox: AxisAlignedBB? = null
    private var currentBox: AxisAlignedBB? = null
    private var lastRenderBox: AxisAlignedBB? = null
    private var lastUpdateTime = 0L
    private var startTime = 0L
    private var scale = 0.0f
    private var isFadingOut = false
    private var currentStage = Stage.RAIL

    init {
        onEnable {
            resetState()
        }
        onDisable {
            resetState()
        }

        safeListener<TickEvent.Post> {
            if (autoIgnite) runIgnitePlacement() else runNormalPlacement()
        }

        safeListener<Render3DEvent> {
            if (render) {
                if (animate) renderAnimated() else renderer.render(false)
            }
        }
    }

    private fun resetState() {
        placeCounter = 0
        placeTimer.reset()
        webTimer.reset()
        currentTarget = null
        igniteWait = 0
        isPlacing = true
        resetAnimation()
    }

    override fun getHudInfo(): String {
        return runSafe {
            val status = if (isPlacing) "Placing" else "Breaking"
            if (autoIgnite) {
                currentTarget?.let { "${it.name}, $status" } ?: status
            } else {
                currentTarget?.let { "${it.name}, $placeCounter" } ?: "$placeCounter"
            }
        } ?: super.getHudInfo()
    }

    private fun resetAnimation() {
        lastRenderPos = null
        prevBox = null
        currentBox = null
        lastRenderBox = null
        lastUpdateTime = 0L
        startTime = 0L
        scale = 0.0f
        isFadingOut = false
        currentStage = Stage.RAIL
        renderer.clear()
    }

    private fun updateAnimation(pos: BlockPos?, stage: Stage) {
        val posChanged = pos != lastRenderPos
        if (posChanged) {
            if (pos != null) {
                lastRenderPos = pos
                currentStage = stage
                isFadingOut = false
                val box = when (stage) {
                    Stage.RAIL, Stage.CART, Stage.IGNITE -> AxisAlignedBB(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), pos.x + 1.0, pos.y + 0.125, pos.z + 1.0)
                }
                currentBox = box
                prevBox = lastRenderBox ?: box
                lastUpdateTime = System.currentTimeMillis()
                if (lastRenderBox == null) startTime = System.currentTimeMillis()
            } else {
                lastRenderPos = null
                isFadingOut = true
                lastUpdateTime = System.currentTimeMillis()
                startTime = System.currentTimeMillis()
            }
        } else if (pos != null) {
            currentStage = stage
            currentBox = AxisAlignedBB(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), pos.x + 1.0, pos.y + 0.125, pos.z + 1.0)
        }
    }

    private fun renderAnimated() {
        val renderPos = lastRenderPos
        if (renderPos == null || isFadingOut) {
            lastRenderBox?.let { last ->
                scale = Easing.IN_CUBIC.dec(Easing.toDelta(startTime, fadeLength.toLong()))
                if (scale > 0.0f) renderWithAlpha(last, currentStage, scale)
                else {
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
                scale = Easing.OUT_CUBIC.inc(Easing.toDelta(startTime, fadeLength.toLong()))
                if (scale > 0.0f) renderWithAlpha(renderBox, currentStage, scale)
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

    private fun renderWithAlpha(box: AxisAlignedBB, stage: Stage, alphaScale: Float) {
        renderer.clear()
        renderer.setThickness(lineWidth)
        val baseColor = when (stage) {
            Stage.RAIL -> railStageColor
            Stage.CART -> cartStageColor
            Stage.IGNITE -> igniteStageColor
        }
        if (filled) {
            val finalColor = baseColor.alpha((baseColor.a * alphaScale).toInt())
            renderer.add(box, finalColor)
        }
        renderer.setAFilled(if (filled) (baseColor.a * alphaScale).toInt() else 0)
        renderer.setAOutline(if (outline) (255 * alphaScale).toInt() else 0)
        renderer.render(false)
    }

    private fun SafeClientEvent.runIgnitePlacement() {
        val target = CombatManager.target ?: return
        if (onlyHole && !HoleManager.getHoleInfo(target).isHole) return
        if (onlySelfHole && !HoleManager.getHoleInfo(player).isHole) return
        if (player.getDistance(target) > range) return

        if (currentTarget == null || !currentTarget!!.isEntityAlive || player.getDistance(currentTarget!!) > range) {
            currentTarget = target
            igniteWait = 0
            isPlacing = true
        }

        val targetPos = EntityUtils.getBetterPosition(currentTarget!!)
        if (world.isAirBlock(targetPos.down())) return

        val cartCount = world.getEntitiesWithinAABB(EntityMinecartTNT::class.java, AxisAlignedBB(targetPos)).count { it.isEntityAlive }

        if (isPlacing) {
            if (world.getBlockState(targetPos).block != Blocks.RAIL && cartCount == 0) {
                if (!world.isAirBlock(targetPos)) return
                placeRail(targetPos.down())
                if (render && animate) updateAnimation(targetPos.down(), Stage.RAIL)
                igniteWait = 0
                return
            }
            if (world.getBlockState(targetPos).block == Blocks.RAIL) {
                if (render && animate) updateAnimation(targetPos.down(), Stage.RAIL)
                if (placeTimer.tickAndReset(placeDelay.toLong(), TimeUnit.MILLISECONDS)) {
                    placeCart(targetPos)
                    if (render && animate) {
                        updateAnimation(targetPos, Stage.CART)
                    }
                }
            }
            if (igniteWait < igniteDelay) {
                igniteWait++
                return
            }
            isPlacing = false
            igniteWait = 0
        } else {
            if (world.getBlockState(targetPos).block == Blocks.RAIL && cartCount > 0) {
                breakRail(targetPos)
                if (render && animate) updateAnimation(targetPos, Stage.IGNITE)
            }
            if (world.isAirBlock(targetPos) && cartCount > 0) {
                igniteCart(targetPos)
                if (render && animate) updateAnimation(targetPos, Stage.IGNITE)
                if (autoDisable) disable() else {
                    isPlacing = true
                    igniteWait = 0
                }
            }
        }
    }

    private fun SafeClientEvent.runNormalPlacement() {
        if (autoDisable && placeCounter > disableThreshold) disable()
        val target = CombatManager.target ?: return
        if (onlyHole && !HoleManager.getHoleInfo(target).isHole) return
        if (onlySelfHole && !HoleManager.getHoleInfo(player).isHole) return
        if (player.getDistance(target) > range) return

        if (currentTarget == null || !currentTarget!!.isEntityAlive || player.getDistance(currentTarget!!) > range) {
            currentTarget = target
        }

        val targetPos = EntityUtils.getBetterPosition(currentTarget!!)
        if (world.isAirBlock(targetPos.down())) return

        val cartCount = world.getEntitiesWithinAABB(EntityMinecartTNT::class.java, AxisAlignedBB(targetPos)).count { it.isEntityAlive }

        if (world.getBlockState(targetPos).block != Blocks.RAIL) {
            if (!world.isAirBlock(targetPos)) return
            placeRail(targetPos.down())
            if (render && animate) updateAnimation(targetPos.down(), Stage.RAIL)
        } else {
            if (render && animate) updateAnimation(targetPos.down(), Stage.RAIL)
            if (placeTimer.tickAndReset(placeDelay.toLong(), TimeUnit.MILLISECONDS) && (cartCount < 26 || !limitPlacements)) {
                placeCart(targetPos)
                if (render && animate) updateAnimation(targetPos, Stage.CART)
            }
        }
    }

    private fun SafeClientEvent.placeRail(pos: BlockPos) {
        val slot = IterableKt.firstItem(DefinedKt.getInventorySlots(player), Item.getItemFromBlock(Blocks.RAIL)) ?: return
        val packet = CPacketPlayerTryUseItemOnBlock(pos, EnumFacing.UP, EnumHand.MAIN_HAND, 0.5f, 1.0f, 0.5f)
        if (placeMode == PlaceMode.CONTROLLER) {
            PlayerPacketManager.sendPlayerPacket {
                rotate(RotationUtils.getRotationTo(this@placeRail, ConversionKt.toVec3d(pos)))
            }
        }
        HotbarSwitchManager.ghostSwitch(this, ghostSwitchB, slot) {
            when (placeMode) {
                PlaceMode.PACKET -> connection.sendPacket(packet)
                PlaceMode.CONTROLLER -> playerController.processRightClickBlock(player, world, pos, EnumFacing.UP, player.lookVec, EnumHand.MAIN_HAND)
            }
        }
    }

    private fun SafeClientEvent.placeCart(targetPos: BlockPos) {
        val slot = IterableKt.firstItem(DefinedKt.getInventorySlots(player), Items.TNT_MINECART) ?: return
        val packet = CPacketPlayerTryUseItemOnBlock(targetPos, EnumFacing.DOWN, EnumHand.MAIN_HAND, 0.5f, 1.0f, 0.5f)
        if (placeMode == PlaceMode.CONTROLLER) {
            PlayerPacketManager.sendPlayerPacket {
                rotate(RotationUtils.getRotationTo(this@placeCart, ConversionKt.toVec3d(targetPos)))
            }
        }
        HotbarSwitchManager.ghostSwitch(this, ghostSwitchB, slot) {
            when (placeMode) {
                PlaceMode.PACKET -> connection.sendPacket(packet)
                PlaceMode.CONTROLLER -> playerController.processRightClickBlock(player, world, targetPos, EnumFacing.UP, player.lookVec, EnumHand.MAIN_HAND)
            }
        }
        placeCounter++
    }

    private fun SafeClientEvent.breakRail(pos: BlockPos) {
        if (placeMode == PlaceMode.CONTROLLER) {
            PlayerPacketManager.sendPlayerPacket {
                rotate(RotationUtils.getRotationTo(this@breakRail, ConversionKt.toVec3d(pos)))
            }
        }
        val slot = IterableKt.firstItem(DefinedKt.getAllSlotsPrioritized(player), Items.DIAMOND_PICKAXE) ?: DefinedKt.getCurrentHotbarSlot(player)
        HotbarSwitchManager.ghostSwitch(this, ghostSwitchB, slot) {
            connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, pos, EnumFacing.UP))
            connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, pos, EnumFacing.UP))
        }
    }

    private fun SafeClientEvent.igniteCart(targetPos: BlockPos) {
        val slot = IterableKt.firstItem(DefinedKt.getAllSlotsPrioritized(player), Items.FLINT_AND_STEEL) ?: return
        val hitVec = ConversionKt.toVec3d(targetPos).addVector(0.5, -0.5, 0.5)
        if (placeMode == PlaceMode.CONTROLLER) {
            PlayerPacketManager.sendPlayerPacket {
                rotate(RotationUtils.getRotationTo(this@igniteCart, hitVec))
            }
        }
        HotbarSwitchManager.ghostSwitch(this, ghostSwitchB, slot) {
            connection.sendPacket(CPacketPlayerTryUseItemOnBlock(targetPos.down(), EnumFacing.UP, EnumHand.MAIN_HAND, 0.5f, 1.0f, 0.5f))
        }
    }

    private enum class PlaceMode { PACKET, CONTROLLER }
    private enum class Stage { RAIL, CART, IGNITE }
}
