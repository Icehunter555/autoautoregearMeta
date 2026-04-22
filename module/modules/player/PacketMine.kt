package dev.wizard.meta.module.modules.player

import dev.fastmc.common.MathUtilKt
import dev.fastmc.common.TickTimer
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.ConnectionEvent
import dev.wizard.meta.event.events.InputEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.RunGameLoopEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.player.InteractEvent
import dev.wizard.meta.event.events.player.OnUpdateWalkingPlayerEvent
import dev.wizard.meta.event.events.render.Render2DEvent
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.graphics.ESPRenderer
import dev.wizard.meta.graphics.Easing
import dev.wizard.meta.graphics.ProjectionUtils
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.font.renderer.MainFontRenderer
import dev.wizard.meta.manager.managers.CombatManager
import dev.wizard.meta.manager.managers.EntityManager
import dev.wizard.meta.manager.managers.HotbarSwitchManager
import dev.wizard.meta.manager.managers.InventoryTaskManager
import dev.wizard.meta.manager.managers.PlayerPacketManager
import dev.wizard.meta.module.AbstractModule
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.client.ClickGUI
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.SwingMode
import dev.wizard.meta.util.accessor.*
import dev.wizard.meta.util.combat.CombatUtils
import dev.wizard.meta.util.inventory.InventoryTask
import dev.wizard.meta.util.inventory.ItemKt
import dev.wizard.meta.util.inventory.findBestTool
import dev.wizard.meta.util.math.vector.ConversionKt
import dev.wizard.meta.util.math.vector.DistanceKt
import dev.wizard.meta.util.math.vector.Vec2f
import dev.wizard.meta.util.math.vector.toVec3dCenter
import dev.wizard.meta.util.world.CheckKt
import dev.wizard.meta.util.world.InteractKt
import net.minecraft.block.BlockLiquid
import net.minecraft.block.state.IBlockState
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.init.Blocks
import net.minecraft.init.Enchantments
import net.minecraft.init.MobEffects
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemPickaxe
import net.minecraft.item.ItemStack
import net.minecraft.network.Packet
import net.minecraft.network.play.client.CPacketHeldItemChange
import net.minecraft.network.play.client.CPacketPlayerDigging
import net.minecraft.network.play.server.SPacketBlockChange
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import net.minecraftforge.fluids.BlockFluidFinite
import org.lwjgl.opengl.GL11
import java.util.*

@CombatManager.CombatModule
object PacketMine : Module(
    "PacketMine",
    alias = arrayOf("InstantMine", "DoubleMine", "SpeedMine"),
    category = Category.PLAYER,
    description = "Break block with packet",
    modulePriority = 200
) {
    private val page by setting(this, EnumSetting(settingName("Page"), Page.GENERAL))

    private val autoSwitch by setting(this, BooleanSetting(settingName("Auto Switch"), true, { page == Page.GENERAL }))
    var ghostSwitchBypass by setting(this, EnumSetting(settingName("Ghost Switch Bypass"), HotbarSwitchManager.Override.SWAP, { page == Page.GENERAL }))
    private val rotation by setting(this, BooleanSetting(settingName("Rotation"), false, { page == Page.GENERAL }))
    private val rotateTime by setting(this, IntegerSetting(settingName("Rotate Time"), 100, 0..1000, 10, { page == Page.GENERAL && rotation }))

    private val startPacketOnClick by setting(this, BooleanSetting(settingName("Start Packet On Click"), true, { page == Page.PACKETS }))
    private val endPacketOnBreak by setting(this, BooleanSetting(settingName("End Packet On Break"), true, { page == Page.PACKETS }))
    private val startPacketAfterBreak by setting(this, BooleanSetting(settingName("Start Packet After Break"), false, { page == Page.PACKETS }))
    private val endPacketAfterBreak by setting(this, BooleanSetting(settingName("End Packet After Break"), false, { page == Page.PACKETS }))
    private val spamPackets by setting(this, BooleanSetting(settingName("Spam Packets"), true, { page == Page.PACKETS }))
    private val noSwing by setting(this, BooleanSetting(settingName("No Swing"), false, { page == Page.PACKETS }))
    private val noAnimation by setting(this, BooleanSetting(settingName("No Animation"), false, { page == Page.GENERAL }))
    private val swingMode by setting(this, EnumSetting(settingName("Swing Mode"), SwingMode.CLIENT, { page == Page.GENERAL }))
    private val packetDelay by setting(this, IntegerSetting(settingName("Packet Delay"), 100, 0..1000, 5, { page == Page.PACKETS }))

    private val breakTimeMultiplier by setting(this, FloatSetting(settingName("Break Time Multiplier"), 0.8f, 0.5f..2.0f, 0.01f, { page == Page.GENERAL }))
    private val breakTimeBias by setting(this, IntegerSetting(settingName("Break Time Bias"), 0, -5000..5000, 50, { page == Page.GENERAL }))
    private val miningTaskTimeout by setting(this, IntegerSetting(settingName("Mining Task Timeout"), 3000, 0..10000, 50, { page == Page.GENERAL }))
    private val range by setting(this, FloatSetting(settingName("Range"), 4.5f, 0.0f..10.0f, 0.1f, { page == Page.GENERAL }))
    private val removeOutOfRange by setting(this, BooleanSetting(settingName("Remove Out Of Range"), false, { page == Page.GENERAL }))

    val doubleMine by setting(this, BooleanSetting(settingName("Double Mine"), false, { page == Page.DOUBLEMINE }))
    private val doubleCalc by setting(this, IntegerSetting(settingName("DoubleMine Calc"), 70, 50..100, 1, { page == Page.DOUBLEMINE && doubleMine }))
    private val maxTick by setting(this, IntegerSetting(settingName("MaxTick"), 20, 0..100, 1, { page == Page.DOUBLEMINE && doubleMine }))
    private val breakAgain by setting(this, BooleanSetting(settingName("Break Again"), false, { page == Page.DOUBLEMINE && doubleMine }))
    private val minHealth by setting(this, FloatSetting(settingName("Min Health"), 16.0f, 0.0f..36.0f, 0.5f, { page == Page.DOUBLEMINE && doubleMine }))
    private val updateController by setting(this, BooleanSetting(settingName("Update Controller"), true, { page == Page.DOUBLEMINE && doubleMine }))

    val useCustomColor by setting(this, BooleanSetting(settingName("Use Custom Color"), false, { page == Page.RENDER }))
    val customColor by setting(this, ColorSetting(settingName("Custom Color"), ColorRGB(255, 255, 255), true, { page == Page.RENDER && useCustomColor }))
    val doubleColor by setting(this, ColorSetting(settingName("Doublemine Color"), ColorRGB(128, 128, 128), true, { page == Page.RENDER && useCustomColor }))
    private val renderMode by setting(this, EnumSetting(settingName("Render Mode"), RenderMode.NORMAL, { page == Page.RENDER }))
    private val showPercentage by setting(this, BooleanSetting(settingName("Show Percentage"), true, { page == Page.RENDER }))
    private val animate by setting(this, BooleanSetting(settingName("Animate"), false, { page == Page.RENDER }))
    private val movingLength by setting(this, IntegerSetting(settingName("Moving Length"), 200, 0..1000, 50, { page == Page.RENDER && animate }))
    private val fadeLength by setting(this, IntegerSetting(settingName("Fade Length"), 200, 0..1000, 50, { page == Page.RENDER && animate }))
    private val progressColorGradient by setting(this, BooleanSetting(settingName("Progress Color Gradient"), false, { page == Page.RENDER && showPercentage }))

    private val clickTimer = TickTimer()
    private val mainRenderer = ESPRenderer()
    private val doubleRenderer = ESPRenderer()
    private val packetTimer = TickTimer()

    private var miningInfo0: MiningInfo? = null
    private var breakConfirm: BreakConfirmInfo? = null
    private val miningQueue = Collections.synchronizedMap(HashMap<AbstractModule, MiningTask>())

    private var doubleMineInfo: DoubleMineInfo? = null
    private var hadDouble = false
    private var switched = false
    private var bypassSlot = -1
    private var originalSlot = -1
    private var needsResync = false
    private val doubleMineTimer = TickTimer()

    private var lastMiningPos: BlockPos? = null
    private var currentNormalBox: AxisAlignedBB? = null
    private var lastNormalRenderBox: AxisAlignedBB? = null
    private var currentReverseBox: AxisAlignedBB? = null
    private var lastReverseRenderBox: AxisAlignedBB? = null
    private var lastMiningUpdateTime = 0L
    private var miningStartTime = 0L
    private var miningScale = 0.0f
    private var isMiningFadingOut = false
    private var currentMiningTextPos: Vec3d? = null
    private var lastMiningTextRenderPos: Vec3d? = null

    private var lastDoublePos: BlockPos? = null
    private var currentDoubleNormalBox: AxisAlignedBB? = null
    private var lastDoubleNormalRenderBox: AxisAlignedBB? = null
    private var currentDoubleReverseBox: AxisAlignedBB? = null
    private var lastDoubleReverseRenderBox: AxisAlignedBB? = null
    private var lastDoubleUpdateTime = 0L
    private var doubleStartTime = 0L
    private var doubleScale = 0.0f
    private var isDoubleFadingOut = false
    private var currentDoubleTextPos: Vec3d? = null
    private var lastDoubleTextRenderPos: Vec3d? = null

    override fun isActive(): Boolean = isEnabled && miningInfo0 != null

    init {
        onDisable {
            SafeClientEvent.instance?.let {
                if (needsResync && originalSlot != -1) switchToOriginalSlot(it)
            }
            miningQueue.clear()
            resetDoublemine()
            reset()
            resetMiningAnimation()
            resetDoubleAnimation()
        }

        listener<ConnectionEvent.Disconnect> {
            miningQueue.clear()
            reset()
            resetMiningAnimation()
            resetDoubleAnimation()
        }

        listener<InputEvent.Mouse> {
            if (it.button == 0 && it.state && mc.currentScreen == null && !clickTimer.tickAndReset(250L)) {
                reset(INSTANCE)
            }
        }

        safeListener<Render2DEvent.Absolute> {
            miningInfo0?.let { info ->
                if (showPercentage) {
                    val progress = ((System.currentTimeMillis() - info.startTime).toFloat() / info.length).coerceIn(0.0f, 1.0f)
                    val progressText = "${(progress * 100).toInt()} %"
                    val usernameText = mc.player.name

                    val center = if (animate) {
                        val multiplier = Easing.OUT_QUART.inc(Easing.toDelta(lastMiningUpdateTime, movingLength.toLong()))
                        interpolateVec(prevMiningTextPos ?: info.pos.toVec3dCenter(), currentMiningTextPos ?: info.pos.toVec3dCenter(), multiplier.toDouble())
                    } else {
                        info.pos.toVec3dCenter()
                    }
                    lastMiningTextRenderPos = center
                    val screenPos = ProjectionUtils.toAbsoluteScreenPos(center)
                    val distFactor = (ProjectionUtils.distToCamera(center) - 1.0).coerceAtLeast(0.0)
                    val baseScale = (6.0f / Math.pow(2.0, distFactor).toFloat()).coerceAtLeast(1.0f) * 1.5f
                    val scaleFactor = if (animate) baseScale * miningScale else baseScale

                    if (!animate || miningScale > 0.0f) {
                        val textAlpha = if (animate) (255 * miningScale).toInt() else 255
                        val textColor = if (progressColorGradient) ColorRGB((255 * (1.0f - progress)).toInt(), (255 * progress).toInt(), 0, textAlpha) else ColorRGB(255, 255, 255, textAlpha)
                        val progressWidth = MainFontRenderer.getWidth(progressText, scaleFactor)
                        val progressHeight = MainFontRenderer.getHeight(scaleFactor)
                        val progressX = screenPos.x.toFloat() - progressWidth * 0.5f
                        val progressY = screenPos.y.toFloat() - progressHeight * 0.5f
                        MainFontRenderer.drawString(progressText, progressX, progressY, textColor, scaleFactor)

                        val usernameScale = scaleFactor * 0.8f
                        val usernameWidth = MainFontRenderer.getWidth(usernameText, usernameScale)
                        val usernameX = screenPos.x.toFloat() - usernameWidth * 0.5f
                        val usernameY = progressY + progressHeight
                        MainFontRenderer.drawString(usernameText, usernameX, usernameY, textColor, usernameScale)
                    }
                }
            }

            doubleMineInfo?.let { info ->
                if (showPercentage) {
                    val progress = ((System.currentTimeMillis() - info.startTime).toFloat() / info.length).coerceIn(0.0f, 1.0f)
                    val progressText = "${(progress * 100).toInt()} %"
                    val usernameText = mc.player.name
                    val doubleText = "double"

                    val center = if (animate) {
                        val multiplier = Easing.OUT_QUART.inc(Easing.toDelta(lastDoubleUpdateTime, movingLength.toLong()))
                        interpolateVec(prevDoubleTextPos ?: info.pos.toVec3dCenter(), currentDoubleTextPos ?: info.pos.toVec3dCenter(), multiplier.toDouble())
                    } else {
                        info.pos.toVec3dCenter()
                    }
                    lastDoubleTextRenderPos = center
                    val screenPos = ProjectionUtils.toAbsoluteScreenPos(center)
                    val distFactor = (ProjectionUtils.distToCamera(center) - 1.0).coerceAtLeast(0.0)
                    val baseScale = (6.0f / Math.pow(2.0, distFactor).toFloat()).coerceAtLeast(1.0f) * 1.5f
                    val scaleFactor = if (animate) baseScale * doubleScale else baseScale

                    if (!animate || doubleScale > 0.0f) {
                        val textAlpha = if (animate) (255 * doubleScale).toInt() else 255
                        val textColor = if (progressColorGradient) ColorRGB((255 * (1.0f - progress)).toInt(), (255 * progress).toInt(), 0, textAlpha) else ColorRGB(255, 255, 255, textAlpha)
                        val doubleColorVal = ColorRGB(ClickGUI.primary.rgba).withAlpha(textAlpha)
                        val progressWidth = MainFontRenderer.getWidth(progressText, scaleFactor)
                        val progressHeight = MainFontRenderer.getHeight(scaleFactor)
                        val progressX = screenPos.x.toFloat() - progressWidth * 0.5f
                        val progressY = screenPos.y.toFloat() - progressHeight * 0.5f
                        MainFontRenderer.drawString(progressText, progressX, progressY, textColor, scaleFactor)

                        val usernameScale = scaleFactor * 0.8f
                        val usernameWidth = MainFontRenderer.getWidth(usernameText, usernameScale)
                        val usernameHeight = MainFontRenderer.getHeight(usernameScale)
                        val usernameX = screenPos.x.toFloat() - usernameWidth * 0.5f
                        val usernameY = progressY + progressHeight
                        MainFontRenderer.drawString(usernameText, usernameX, usernameY, textColor, usernameScale)

                        val subDoubleScale = scaleFactor * 0.7f
                        val doubleWidth = MainFontRenderer.getWidth(doubleText, subDoubleScale)
                        val doubleX = screenPos.x.toFloat() - doubleWidth * 0.5f
                        val doubleY = usernameY + usernameHeight
                        MainFontRenderer.drawString(doubleText, doubleX, doubleY, doubleColorVal, subDoubleScale)
                    }
                }
            }
        }

        safeListener<Render3DEvent> {
            miningInfo0?.let { info ->
                val progress = ((System.currentTimeMillis() - info.startTime).toFloat() / info.length).coerceIn(0.0f, 1.0f)
                val color = if (useCustomColor) customColor else if (info.isAir) ColorRGB(32, 255, 32) else ColorRGB(255, 32, 32)
                var normalBox: AxisAlignedBB? = null
                var reverseBox: AxisAlignedBB? = null
                val pos = info.pos

                when (renderMode) {
                    RenderMode.GROW -> {
                        normalBox = AxisAlignedBB(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), pos.x + 1.0, pos.y + progress.toDouble(), pos.z + 1.0)
                    }
                    RenderMode.RISE -> {
                        val height = 1.0 - progress.toDouble()
                        normalBox = AxisAlignedBB(pos.x.toDouble(), pos.y + height, pos.z.toDouble(), pos.x + 1.0, pos.y + 1.0, pos.z + 1.0)
                    }
                    RenderMode.REVERSE -> {
                        normalBox = AxisAlignedBB(pos).grow((1.0 - progress).toDouble() * -0.5)
                    }
                    RenderMode.BOTH -> {
                        normalBox = AxisAlignedBB(pos).grow(progress.toDouble() * -0.5)
                        reverseBox = AxisAlignedBB(pos).grow((1.0 - progress).toDouble() * -0.5)
                    }
                    else -> {
                        normalBox = AxisAlignedBB(pos).grow(progress.toDouble() * -0.5)
                    }
                }

                if (animate) {
                    updateMiningAnimation(pos, normalBox, reverseBox)
                    renderAnimatedMining(color.rgba)
                } else {
                    mainRenderer.setAFilled(31)
                    mainRenderer.setAOutline(233)
                    normalBox?.let { mainRenderer.add(it, color) }
                    reverseBox?.let { mainRenderer.add(it, color) }
                }
            } ?: run {
                if (animate) {
                    updateMiningAnimation(null, null, null)
                    renderAnimatedMining(ColorRGB(255, 32, 32).rgba)
                }
            }

            doubleMineInfo?.let { info ->
                val progress = ((System.currentTimeMillis() - info.startTime).toFloat() / info.length).coerceIn(0.0f, 1.0f)
                val color = if (useCustomColor) ColorRGB(doubleColor.r, doubleColor.g, doubleColor.b, 128) else ColorRGB(255, 32, 32, 128)
                var normalBox: AxisAlignedBB? = null
                var reverseBox: AxisAlignedBB? = null
                val pos = info.pos

                when (renderMode) {
                    RenderMode.GROW -> {
                        normalBox = AxisAlignedBB(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), pos.x + 1.0, pos.y + progress.toDouble(), pos.z + 1.0)
                    }
                    RenderMode.RISE -> {
                        val height = 1.0 - progress.toDouble()
                        normalBox = AxisAlignedBB(pos.x.toDouble(), pos.y + height, pos.z.toDouble(), pos.x + 1.0, pos.y + 1.0, pos.z + 1.0)
                    }
                    RenderMode.REVERSE -> {
                        normalBox = AxisAlignedBB(pos).grow((1.0 - progress).toDouble() * -0.5)
                    }
                    RenderMode.BOTH -> {
                        normalBox = AxisAlignedBB(pos).grow(progress.toDouble() * -0.5)
                        reverseBox = AxisAlignedBB(pos).grow((1.0 - progress).toDouble() * -0.5)
                    }
                    else -> {
                        normalBox = AxisAlignedBB(pos).grow(progress.toDouble() * -0.5)
                    }
                }

                if (animate) {
                    updateDoubleAnimation(pos, normalBox, reverseBox)
                    renderAnimatedDouble(color.rgba)
                } else {
                    doubleRenderer.setAFilled(31)
                    doubleRenderer.setAOutline(233)
                    normalBox?.let { doubleRenderer.add(it, color) }
                    reverseBox?.let { doubleRenderer.add(it, color) }
                }
            } ?: run {
                if (animate) {
                    updateDoubleAnimation(null, null, null)
                    renderAnimatedDouble(ColorRGB(255, 32, 32, 128).rgba)
                }
            }

            mainRenderer.render(true)
            doubleRenderer.render(true)
        }

        safeListener<PacketEvent.Receive> { event ->
            val packet = event.packet
            if (packet is SPacketBlockChange) {
                miningInfo0?.let { info ->
                    if (packet.blockPosition == info.pos) {
                        info.miningTimeout = System.currentTimeMillis()
                        val newState = packet.blockState
                        val currentState = world.getBlockState(info.pos)
                        if (newState.block != Blocks.AIR) {
                            breakConfirm = null
                            if (newState.block != currentState.block) {
                                info.isAir = false
                                if (info.mined) finishMining(this, newState, info)
                            }
                        } else if (newState.block != currentState.block) {
                            breakConfirm = BreakConfirmInfo(info.pos, System.currentTimeMillis() + 50L, info.mined || currentState.getBlockHardness(world, info.pos) > 0.0f)
                        }
                    }
                }
            }
        }

        safeListener<OnUpdateWalkingPlayerEvent.Pre> {
            if (rotation) {
                miningInfo0?.let { info ->
                    if (!info.isAir && (info.mined || info.endTime - System.currentTimeMillis() <= rotateTime)) {
                        val rotation = RotationUtils.getRotationTo(this, info.pos.toVec3dCenter())
                        PlayerPacketManager.sendPlayerPacket {
                            rotate(rotation)
                        }
                    }
                }
            }
        }

        listener<InteractEvent.Block.LeftClick> {
            mineBlock(INSTANCE, it.pos, getModulePriority(), false)
            if (miningInfo0?.pos == it.pos) it.cancel()
        }

        listener<InteractEvent.Block.Damage> {
            mineBlock(INSTANCE, it.pos, getModulePriority(), false)
            if (miningInfo0?.pos == it.pos) it.cancel()
        }

        safeListener<RunGameLoopEvent.Tick> {
            miningInfo0?.let { info ->
                handleNormalDoubleMine(this)
                breakConfirm?.let { confirm ->
                    if (System.currentTimeMillis() < confirm.time) {
                        info.isAir = true
                        info.mined = confirm.instantMineable
                        if (!confirm.instantMineable) {
                            info.updateLength(this)
                            connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, info.pos, info.side))
                        } else {
                            if (startPacketAfterBreak) connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, info.pos, info.side))
                            if (endPacketAfterBreak) connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, info.pos, info.side))
                        }
                        breakConfirm = null
                    }
                }

                if (player.getDistanceSqToCenter(info.pos) > range * range) {
                    reset()
                    return@safeListener
                }

                val state = world.getBlockState(info.pos)
                info.isAir = state.block == Blocks.AIR
                if (isFinished(info, state) && checkRotation(this, info)) {
                    if (packetTimer.tick(packetDelay.toLong()) && finishMining(this, state, info)) {
                        // Success
                    }
                } else if (spamPackets) {
                    if (packetTimer.tick(packetDelay.toLong())) {
                        sendMiningPacket(this, info, false)
                    }
                } else {
                    packetTimer.reset(-114514)
                }
            }
        }
    }

    private fun finishMining(event: SafeClientEvent, state: IBlockState, info: MiningInfo): Boolean {
        val bestTool = findBestTool(event, state) ?: return false
        synchronized(InventoryTaskManager) {
            if (bestTool.slotIndex - 36 == HotbarSwitchManager.serverSideHotbar) {
                sendMiningPacket(event, info, true)
                return true
            } else if (autoSwitch) {
                HotbarSwitchManager.ghostSwitch(event, ghostSwitchBypass, bestTool) {
                    sendMiningPacket(event, info, true)
                }
                return true
            }
        }
        return false
    }

    private fun sendMiningPacket(event: SafeClientEvent, info: MiningInfo, end: Boolean) {
        if (endPacketOnBreak && end) {
            event.connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, info.pos, info.side))
        }
        if (noAnimation && !info.mined) {
            event.connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, info.pos, info.side))
        }
        packetTimer.reset()
        if (end) swingMode.swingHand(event, EnumHand.MAIN_HAND)
    }

    private fun checkRotation(event: SafeClientEvent, info: MiningInfo): Boolean {
        if (!rotation) return true
        val eyePos = PlayerPacketManager.position.add(0.0, event.player.eyeHeight.toDouble(), 0.0)
        return BoundingBoxUtilsKt.isInSight(AxisAlignedBB(info.pos), eyePos, PlayerPacketManager.rotation)
    }

    private fun isFinished(info: MiningInfo, state: IBlockState): Boolean {
        return (info.isAir && state.block == Blocks.AIR) || (info.mined || System.currentTimeMillis() > info.endTime)
    }

    private fun reset() {
        SafeClientEvent.instance?.let {
            if (needsResync && originalSlot != -1) switchToOriginalSlot(it)
        }
        packetTimer.reset(-69420)
        miningInfo0 = null
    }

    private fun resetDoublemine() {
        doubleMineInfo = null
        hadDouble = false
        resetDoubleMineState()
        needsResync = false
    }

    private fun resetDoubleMineState() {
        switched = false
        bypassSlot = -1
        originalSlot = -1
        doubleMineTimer.reset()
    }

    private fun switchToOriginalSlot(event: SafeClientEvent) {
        if (originalSlot in 0..8) {
            event.connection.sendPacket(CPacketHeldItemChange(originalSlot))
            if (updateController) event.playerController.updateController()
            needsResync = false
        }
    }

    private fun handleNormalDoubleMine(event: SafeClientEvent) {
        val info = doubleMineInfo ?: return
        val canBreakDouble = info.endTime <= System.currentTimeMillis() && event.player.health + event.player.absorptionAmount >= minHealth
        
        if (!canBreakDouble) {
            if (bypassSlot != -1) {
                switchDouble(event, bypassSlot, true)
                bypassSlot = -1
            }
        } else {
            if (bypassSlot == -1) {
                val state = event.world.getBlockState(info.pos)
                val bestTool = findBestTool(event, state)
                if (bestTool != null && autoSwitch && bestTool.slotIndex - 36 != event.player.inventory.currentItem) {
                    bypassSlot = bestTool.slotIndex - 36
                    switchDouble(event, bypassSlot, false)
                    return
                }
            }
            if (breakAgain) sendDoubleMinePacket(event, info)
            if (bypassSlot != -1) {
                switchDouble(event, bypassSlot, true)
                bypassSlot = -1
            }
        }
        
        if (info.endTime <= System.currentTimeMillis()) {
            if (doubleMineTimer.tick(maxTick * 50L)) {
                doubleMineInfo = null
                resetDoubleMineState()
            }
        } else {
            doubleMineTimer.reset()
        }
    }

    private fun switchDouble(event: SafeClientEvent, slot: Int, back: Boolean) {
        if (slot < 0 || slot == event.player.inventory.currentItem) return
        val targetSlot = if (back) event.player.inventory.currentItem else slot
        event.connection.sendPacket(CPacketHeldItemChange(targetSlot))
        if (updateController) event.playerController.updateController()
    }

    private fun sendDoubleMinePacket(event: SafeClientEvent, info: DoubleMineInfo) {
        event.connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, info.pos, info.side))
        event.connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, info.pos, info.side))
        if (!noSwing) swingMode.swingHand(event, EnumHand.MAIN_HAND)
    }

    private fun calcBreakTime(event: SafeClientEvent, pos: BlockPos): Int {
        val state = event.world.getBlockState(pos)
        val hardness = state.getBlockHardness(event.world, pos)
        if (hardness == 0.0f) return 0
        val breakSpeed = getBreakSpeed(event, state)
        val relativeDamage = breakSpeed / hardness / 30.0f
        val ticks = MathUtilKt.ceilToInt(0.7f / relativeDamage)
        if (ticks <= 0) return 0
        return MathUtilKt.ceilToInt((ticks * 50).toFloat() * breakTimeMultiplier + breakTimeBias.toFloat())
    }

    private fun getBreakSpeed(event: SafeClientEvent, state: IBlockState): Float {
        var maxSpeed = 1.0f
        for (slot in DefinedKt.getAllSlots(event.player)) {
            val stack = slot.stack
            if (stack.isEmpty) continue
            if (!ItemKt.isTool(stack.item)) continue
            var speed = stack.getDestroySpeed(state)
            if (speed > 1.0f) {
                val eff = EnchantmentHelper.getEnchantmentLevel(Enchantments.EFFICIENCY, stack)
                if (eff > 0) speed += (eff * eff + 1).toFloat()
            }
            if (speed > maxSpeed) maxSpeed = speed
        }
        event.player.getActivePotionEffect(MobEffects.HASTE)?.let {
            maxSpeed *= 1.0f + (it.amplifier + 1) * 0.2f
        }
        return maxSpeed
    }

    private fun updateMiningAnimation(pos: BlockPos?, normalBox: AxisAlignedBB?, reverseBox: AxisAlignedBB?) {
        val targetChanged = pos != lastMiningPos
        if (targetChanged) {
            lastMiningPos = pos
            isMiningFadingOut = pos == null
            lastMiningUpdateTime = System.currentTimeMillis()
            if (normalBox != null) {
                currentNormalBox = normalBox
                prevNormalBox = lastNormalRenderBox ?: normalBox
                currentReverseBox = reverseBox
                prevReverseBox = lastReverseRenderBox ?: reverseBox
                if (lastNormalRenderBox == null) miningStartTime = System.currentTimeMillis()
            } else {
                miningStartTime = System.currentTimeMillis()
            }
            currentMiningTextPos = pos?.toVec3dCenter()
            prevMiningTextPos = lastMiningTextRenderPos ?: currentMiningTextPos
        } else {
            currentNormalBox = normalBox
            currentReverseBox = reverseBox
            currentMiningTextPos = pos?.toVec3dCenter()
        }
    }

    private fun updateDoubleAnimation(pos: BlockPos?, normalBox: AxisAlignedBB?, reverseBox: AxisAlignedBB?) {
        val targetChanged = pos != lastDoublePos
        if (targetChanged) {
            lastDoublePos = pos
            isDoubleFadingOut = pos == null
            lastDoubleUpdateTime = System.currentTimeMillis()
            if (normalBox != null) {
                currentDoubleNormalBox = normalBox
                prevDoubleNormalBox = lastDoubleNormalRenderBox ?: normalBox
                currentDoubleReverseBox = reverseBox
                prevDoubleReverseBox = lastDoubleReverseRenderBox ?: reverseBox
                if (lastDoubleNormalRenderBox == null) doubleStartTime = System.currentTimeMillis()
            } else {
                doubleStartTime = System.currentTimeMillis()
            }
            currentDoubleTextPos = pos?.toVec3dCenter()
            prevDoubleTextPos = lastDoubleTextRenderPos ?: currentDoubleTextPos
        } else {
            currentDoubleNormalBox = normalBox
            currentDoubleReverseBox = reverseBox
            currentDoubleTextPos = pos?.toVec3dCenter()
        }
    }

    private fun renderAnimatedMining(color: Int) {
        if (isMiningFadingOut) {
            if (lastNormalRenderBox != null || lastReverseRenderBox != null) {
                miningScale = Easing.IN_CUBIC.dec(Easing.toDelta(miningStartTime, fadeLength.toLong()))
                if (miningScale > 0.0f) {
                    mainRenderer.setAFilled((31 * miningScale).toInt())
                    mainRenderer.setAOutline((233 * miningScale).toInt())
                    lastNormalRenderBox?.let { mainRenderer.add(it, color) }
                    lastReverseRenderBox?.let { mainRenderer.add(it, color) }
                } else {
                    resetMiningAnimation()
                }
            }
            return
        }
        var rendered = false
        prevNormalBox?.let { prev ->
            currentNormalBox?.let { curr ->
                val multiplier = Easing.OUT_QUART.inc(Easing.toDelta(lastMiningUpdateTime, movingLength.toLong()))
                val renderBox = interpolateBox(prev, curr, multiplier)
                lastNormalRenderBox = renderBox
                rendered = true
            }
        }
        prevReverseBox?.let { prev ->
            currentReverseBox?.let { curr ->
                val multiplier = Easing.OUT_QUART.inc(Easing.toDelta(lastMiningUpdateTime, movingLength.toLong()))
                val renderBox = interpolateBox(prev, curr, multiplier)
                lastReverseRenderBox = renderBox
                rendered = true
            }
        }
        if (rendered) {
            miningScale = Easing.OUT_CUBIC.inc(Easing.toDelta(miningStartTime, fadeLength.toLong()))
            if (miningScale > 0.0f) {
                mainRenderer.setAFilled((31 * miningScale).toInt())
                mainRenderer.setAOutline((233 * miningScale).toInt())
                lastNormalRenderBox?.let { mainRenderer.add(it, color) }
                lastReverseRenderBox?.let { mainRenderer.add(it, color) }
            }
        }
    }

    private fun renderAnimatedDouble(color: Int) {
        if (isDoubleFadingOut) {
            if (lastDoubleNormalRenderBox != null || lastDoubleReverseRenderBox != null) {
                doubleScale = Easing.IN_CUBIC.dec(Easing.toDelta(doubleStartTime, fadeLength.toLong()))
                if (doubleScale > 0.0f) {
                    doubleRenderer.setAFilled((31 * doubleScale).toInt())
                    doubleRenderer.setAOutline((233 * doubleScale).toInt())
                    lastDoubleNormalRenderBox?.let { doubleRenderer.add(it, color) }
                    lastDoubleReverseRenderBox?.let { doubleRenderer.add(it, color) }
                } else {
                    resetDoubleAnimation()
                }
            }
            return
        }
        var rendered = false
        prevDoubleNormalBox?.let { prev ->
            currentDoubleNormalBox?.let { curr ->
                val multiplier = Easing.OUT_QUART.inc(Easing.toDelta(lastDoubleUpdateTime, movingLength.toLong()))
                val renderBox = interpolateBox(prev, curr, multiplier)
                lastDoubleNormalRenderBox = renderBox
                rendered = true
            }
        }
        prevDoubleReverseBox?.let { prev ->
            currentDoubleReverseBox?.let { curr ->
                val multiplier = Easing.OUT_QUART.inc(Easing.toDelta(lastDoubleUpdateTime, movingLength.toLong()))
                val renderBox = interpolateBox(prev, curr, multiplier)
                lastDoubleReverseRenderBox = renderBox
                rendered = true
            }
        }
        if (rendered) {
            doubleScale = Easing.OUT_CUBIC.inc(Easing.toDelta(doubleStartTime, fadeLength.toLong()))
            if (doubleScale > 0.0f) {
                doubleRenderer.setAFilled((31 * doubleScale).toInt())
                doubleRenderer.setAOutline((233 * doubleScale).toInt())
                lastDoubleNormalRenderBox?.let { doubleRenderer.add(it, color) }
                lastDoubleReverseRenderBox?.let { doubleRenderer.add(it, color) }
            }
        }
    }

    private fun interpolateBox(from: AxisAlignedBB, to: AxisAlignedBB, progress: Float): AxisAlignedBB {
        return AxisAlignedBB(
            from.minX + (to.minX - from.minX) * progress,
            from.minY + (to.minY - from.minY) * progress,
            from.minZ + (to.minZ - from.minZ) * progress,
            from.maxX + (to.maxX - from.maxX) * progress,
            from.maxY + (to.maxY - from.maxY) * progress,
            from.maxZ + (to.maxZ - from.maxZ) * progress
        )
    }

    private fun interpolateVec(from: Vec3d, to: Vec3d, progress: Double): Vec3d {
        return Vec3d(
            from.x + (to.x - from.x) * progress,
            from.y + (to.y - from.y) * progress,
            from.z + (to.z - from.z) * progress
        )
    }

    fun mineBlock(module: AbstractModule, pos: BlockPos, priority: Int, once: Boolean) {
        val safe = SafeClientEvent.instance ?: return
        val prev = miningQueue[module]
        if (prev == null || prev.pos != pos || prev.priority != priority) {
            miningQueue[module] = MiningTask(module, pos, priority, once)
        }
        updateMining(safe)
    }

    fun reset(module: AbstractModule) {
        val safe = SafeClientEvent.instance ?: return
        miningQueue.remove(module)
        updateMining(safe)
    }

    private fun startMining(event: SafeClientEvent, task: MiningTask) {
        val breakTime = calcBreakTime(event, task.pos)
        if (breakTime == -1) return

        if (doubleMine && miningInfo0 != null && !hadDouble) {
            val current = miningInfo0!!
            if (event.world.isAirBlock(current.pos) || event.world.getBlockState(current.pos).block is BlockLiquid) {
                // can double mine
            }
            doubleMineInfo = DoubleMineInfo(current.pos, current.side, current.startTime, calcDoubleBreakTime(event, current.pos))
            resetDoubleMineState()
            originalSlot = event.player.inventory.currentItem
        }
        
        reset()
        val side = InteractKt.getMiningSide(event, task.pos) ?: EnumFacing.UP
        miningInfo0 = MiningInfo(event, task.owner, task.pos, side)
        packetTimer.reset(-69420)
        
        if (startPacketOnClick || breakTime == 0) {
            event.connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, task.pos, side))
        }
        event.connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, task.pos, side))
        if (noAnimation) event.connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, task.pos, side))
        if (!noSwing) swingMode.swingHand(event, EnumHand.MAIN_HAND)
    }

    private class MiningTask(val owner: AbstractModule, val pos: BlockPos, val priority: Int, val once: Boolean) : Comparable<MiningTask> {
        override fun compareTo(other: MiningTask): Int {
            val res = other.priority.compareTo(priority)
            return if (res != 0) res else pos.compareTo(other.pos)
        }
    }

    interface IMiningInfo {
        val pos: BlockPos
        val side: EnumFacing
        val startTime: Long
        val length: Int
        val endTime: Long get() = startTime + length
    }

    private class MiningInfo(event: SafeClientEvent, val owner: AbstractModule, override val pos: BlockPos, override val side: EnumFacing) : IMiningInfo {
        override val startTime = System.currentTimeMillis()
        override var length = INSTANCE.calcBreakTime(event, pos)
        var mined = false
        var isAir = false
        var miningTimeout = System.currentTimeMillis()

        fun updateLength(event: SafeClientEvent) {
            length = INSTANCE.calcBreakTime(event, pos)
        }
    }

    private class DoubleMineInfo(override val pos: BlockPos, override val side: EnumFacing, override val startTime: Long, override val length: Int) : IMiningInfo

    private class BreakConfirmInfo(val pos: BlockPos, val time: Long, val instantMineable: Boolean)

    private enum class Page { GENERAL, PACKETS, DOUBLEMINE, RENDER }
    private enum class RenderMode { NORMAL, GROW, RISE, REVERSE, BOTH }
}
