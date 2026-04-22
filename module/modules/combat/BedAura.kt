package dev.wizard.meta.module.modules.combat

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.fastmc.common.collection.CircularArray
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.EntityEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.RunGameLoopEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.combat.CombatEvent
import dev.wizard.meta.event.events.player.OnUpdateWalkingPlayerEvent
import dev.wizard.meta.event.events.render.Render2DEvent
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.graphics.ESPRenderer
import dev.wizard.meta.graphics.Easing
import dev.wizard.meta.graphics.ProjectionUtils
import dev.wizard.meta.graphics.RenderUtils3D
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
import dev.wizard.meta.module.modules.client.AntiCheat
import dev.wizard.meta.module.modules.client.CombatSetting
import dev.wizard.meta.module.modules.player.PacketMine
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.BindSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.Bind
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.LambdaUtilsKt
import dev.wizard.meta.util.combat.CalcContext
import dev.wizard.meta.util.combat.CombatUtils
import dev.wizard.meta.util.combat.CrystalUtils
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.inventory.InventoryTask
import dev.wizard.meta.util.inventory.TaskKt
import dev.wizard.meta.util.inventory.operation.BasicKt
import dev.wizard.meta.util.inventory.slot.DefinedKt
import dev.wizard.meta.util.inventory.slot.HotbarSlot
import dev.wizard.meta.util.inventory.slot.IterableKt
import dev.wizard.meta.util.math.RotationUtils
import dev.wizard.meta.util.math.VectorUtils
import dev.wizard.meta.util.math.vector.ConversionKt
import dev.wizard.meta.util.math.vector.DistanceKt
import dev.wizard.meta.util.math.vector.Vec2f
import dev.wizard.meta.util.pause.MainHandPause
import dev.wizard.meta.util.pause.OffhandPause
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.threads.ConcurrentScope
import dev.wizard.meta.util.threads.CoroutineUtilsKt
import dev.wizard.meta.util.world.BlockKt
import dev.wizard.meta.util.world.FastRayTraceAction
import dev.wizard.meta.util.world.InteractKt
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.minecraft.block.BlockBed
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.init.SoundEvents
import net.minecraft.inventory.ClickType
import net.minecraft.inventory.Container
import net.minecraft.inventory.ContainerShulkerBox
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemArmor
import net.minecraft.network.play.client.CPacketAnimation
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.network.play.server.SPacketBlockChange
import net.minecraft.network.play.server.SPacketSoundEffect
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import net.minecraft.util.text.TextFormatting
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow

@CombatManager.CombatModule
object BedAura : Module(
    "BedAura",
    category = Category.COMBAT,
    description = "Place bed and kills enemies",
    modulePriority = 90
) {
    private val page = setting(this, EnumSetting(settingName("Page"), Page.GENERAL))
    private val handMode by setting(this, EnumSetting(settingName("Hand Mode"), EnumHand.OFF_HAND, { page.value == Page.GENERAL }))
    private val rotationPitch by setting(this, IntegerSetting(settingName("Rotation Pitch"), 90, -90..90, 1, { page.value == Page.GENERAL }))
    private val ghostSwitchBypass by setting(this, EnumSetting(settingName("Ghost Switch Bypass"), HotbarSwitchManager.Override.NONE, LambdaUtilsKt.and({ page.value == Page.GENERAL }) { handMode == EnumHand.MAIN_HAND }))
    private val bedSlot by setting(this, IntegerSetting(settingName("Bed Slot"), 3, 1..9, 1, LambdaUtilsKt.and({ page.value == Page.GENERAL }) { handMode == EnumHand.MAIN_HAND }))
    private val assumeInstantMine by setting(this, BooleanSetting(settingName("Assume Instant Mine"), true, { page.value == Page.GENERAL }))
    private val antiBlocker by setting(this, BooleanSetting(settingName("Anti HoleProtect"), true, { page.value == Page.GENERAL }))
    private val antiBlockerSwitch by setting(this, IntegerSetting(settingName("Anti HoleProtect Switch"), 200, 0..500, 10, LambdaUtilsKt.and({ page.value == Page.GENERAL }) { antiBlocker }))
    private val strictDirection by setting(this, BooleanSetting(settingName("Strict Direction"), false, { page.value == Page.GENERAL }))
    private val newPlacement by setting(this, BooleanSetting(settingName("1.13 Placement"), false, { page.value == Page.GENERAL }))
    private val smartDamage by setting(this, BooleanSetting(settingName("Smart Damage"), true, { page.value == Page.GENERAL }))
    private val damageStep by setting(this, FloatSetting(settingName("Damage Step"), 2.0f, 0.0f..5.0f, 0.1f, LambdaUtilsKt.and({ page.value == Page.GENERAL }) { smartDamage }))
    private val noSuicide by setting(this, FloatSetting(settingName("No Suicide"), 8.0f, 0.0f..20.0f, 0.25f, { page.value == Page.GENERAL }))
    private val minDamage by setting(this, FloatSetting(settingName("Min Damage"), 6.0f, 0.0f..20.0f, 0.25f, { page.value == Page.GENERAL }))
    private val maxSelfDamage by setting(this, FloatSetting(settingName("Max Self Damage"), 6.0f, 0.0f..20.0f, 0.25f, { page.value == Page.GENERAL }))
    private val damageBalance by setting(this, FloatSetting(settingName("Damage Balance"), -2.5f, -10.0f..10.0f, 0.25f, { page.value == Page.GENERAL }))
    private val range by setting(this, FloatSetting(settingName("Range"), 5.4f, 0.0f..6.0f, 0.25f, { page.value == Page.GENERAL }))

    private val updateDelay by setting(this, IntegerSetting(settingName("Update Delay"), 50, 5..250, 1, { page.value == Page.TIMING }))
    private val timingMode by setting(this, EnumSetting(settingName("Timing Mode"), TimingMode.INSTANT, { page.value == Page.TIMING }))
    private val delay by setting(this, IntegerSetting(settingName("Delay"), 75, 0..1000, 1, LambdaUtilsKt.and({ page.value == Page.TIMING }) { timingMode != TimingMode.SWITCH }))
    private val placeDelay by setting(this, IntegerSetting(settingName("Place Delay"), 25, 0..1000, 1, LambdaUtilsKt.and({ page.value == Page.TIMING }) { timingMode == TimingMode.SWITCH || timingMode == TimingMode.AUTO }))
    private val breakDelay by setting(this, IntegerSetting(settingName("Break Delay"), 50, 0..1000, 1, LambdaUtilsKt.and({ page.value == Page.TIMING }) { timingMode == TimingMode.SWITCH || timingMode == TimingMode.AUTO }))
    private val slowMode by setting(this, BooleanSetting(settingName("Slow Mode"), true, { page.value == Page.TIMING }))
    private val slowModeDamage by setting(this, FloatSetting(settingName("Slow Mode Damage"), 4.0f, 0.0f..10.0f, 0.25f, LambdaUtilsKt.and({ page.value == Page.TIMING }) { slowMode }))
    private val slowDelay by setting(this, IntegerSetting(settingName("Slow Delay"), 250, 0..1000, 5, LambdaUtilsKt.and({ page.value == Page.TIMING }) { slowMode && timingMode != TimingMode.SWITCH }))
    private val slowPlaceDelay by setting(this, IntegerSetting(settingName("Slow Place Delay"), 250, 0..1000, 5, LambdaUtilsKt.and({ page.value == Page.TIMING }) { slowMode && timingMode == TimingMode.SWITCH }))
    private val slowBreakDelay by setting(this, IntegerSetting(settingName("Slow Break Delay"), 50, 0..1000, 5, LambdaUtilsKt.and({ page.value == Page.TIMING }) { slowMode && (smartDamage || timingMode == TimingMode.SWITCH) }))

    private val forcePlaceBind by setting(this, BindSetting(settingName("Force Place Bind"), Bind(), { if (isEnabled && it) { toggleForcePlace = !toggleForcePlace; NoSpamMessage.sendMessage("${getChatName()} Force placing ${if (toggleForcePlace) "${TextFormatting.GREEN}enabled" else "${TextFormatting.RED}disabled"}${TextFormatting.RESET}") } }, { page.value == Page.FORCE_PLACE }))
    private val forcePlaceHealth by setting(this, FloatSetting(settingName("Force Place Health"), 8.0f, 0.0f..20.0f, 0.5f, { page.value == Page.FORCE_PLACE }))
    private val forcePlaceMinDamage by setting(this, FloatSetting(settingName("Force Place Min Damage"), 1.5f, 0.0f..10.0f, 0.25f, { page.value == Page.FORCE_PLACE }))
    private val forcePlaceDamageBalance by setting(this, FloatSetting(settingName("Force Place Damage Balance"), 0.0f, -10.0f..10.0f, 0.25f, { page.value == Page.FORCE_PLACE }))

    private val basePlace by setting(this, BooleanSetting(settingName("Base Place"), false, { page.value == Page.BASE_PLACE }))
    private val basePlaceUpdateDelay by setting(this, IntegerSetting(settingName("Base Place Update Delay"), 5000, 1000..10000, 100, LambdaUtilsKt.and({ page.value == Page.BASE_PLACE }) { basePlace }))
    private val basePlaceRange by setting(this, FloatSetting(settingName("Base Place Range"), 5.2f, 1.0f..10.0f, 0.1f, LambdaUtilsKt.and({ page.value == Page.BASE_PLACE }) { basePlace }))
    private val basePlaceDelay by setting(this, IntegerSetting(settingName("Base Place Delay"), 250, 10..1000, 10, LambdaUtilsKt.and({ page.value == Page.BASE_PLACE }) { basePlace }))
    private val basePlaceMaxSelf by setting(this, FloatSetting(settingName("Base Place Max Self Damage"), 8.0f, 0.0f..20.0f, 0.25f, LambdaUtilsKt.and({ page.value == Page.BASE_PLACE }) { basePlace }))
    private val basePlaceMinTarget by setting(this, FloatSetting(settingName("Base Place Min Target Damage"), 3.0f, 0.0f..20.0f, 0.25f, LambdaUtilsKt.and({ page.value == Page.BASE_PLACE }) { basePlace }))

    private val motionDetect by setting(this, BooleanSetting(settingName("Motion Detect"), true, { page.value == Page.MOTION_DETECT }))
    private val targetMotion by setting(this, FloatSetting(settingName("Target Motion"), 0.15f, 0.0f..0.3f, 0.01f, LambdaUtilsKt.and({ page.value == Page.MOTION_DETECT }) { motionDetect }))
    private val selfMotion by setting(this, FloatSetting(settingName("Self Motion"), 0.22f, 0.0f..0.3f, 0.01f, LambdaUtilsKt.and({ page.value == Page.MOTION_DETECT }) { motionDetect }))
    private val motionMinDamage by setting(this, FloatSetting(settingName("Motion Min Damage"), 3.0f, 0.0f..20.0f, 0.25f, LambdaUtilsKt.and({ page.value == Page.MOTION_DETECT }) { motionDetect }))
    private val motionMaxSelfDamage by setting(this, FloatSetting(settingName("Motion Max Self Damage"), 8.0f, 0.0f..20.0f, 0.25f, LambdaUtilsKt.and({ page.value == Page.MOTION_DETECT }) { motionDetect }))
    private val motionDamageBalance by setting(this, FloatSetting(settingName("Motion Damage Balance"), -5.0f, -10.0f..10.0f, 0.25f, LambdaUtilsKt.and({ page.value == Page.MOTION_DETECT }) { motionDetect }))

    private val renderMode = setting(this, EnumSetting(settingName("Render Mode"), RenderMode.DUAL, { page.value == Page.RENDER }))
    private val renderFoot by setting(this, BooleanSetting(settingName("Render Foot"), true, LambdaUtilsKt.and({ page.value == Page.RENDER }) { renderMode.value == RenderMode.DUAL }))
    private val renderHead by setting(this, BooleanSetting(settingName("Render Head"), true, LambdaUtilsKt.and({ page.value == Page.RENDER }) { renderMode.value == RenderMode.DUAL }))
    private val renderBase by setting(this, BooleanSetting(settingName("Render Base"), false, { page.value == Page.RENDER }))
    private val renderDamage by setting(this, BooleanSetting(settingName("Render Damage"), true, { page.value == Page.RENDER }))
    private val footColor by setting(this, ColorSetting(settingName("Foot Color"), ColorRGB(255, 160, 255), LambdaUtilsKt.and({ page.value == Page.RENDER && renderFoot }) { renderMode.value == RenderMode.DUAL }))
    private val headColor by setting(this, ColorSetting(settingName("Head Color"), ColorRGB(255, 32, 64), LambdaUtilsKt.and({ page.value == Page.RENDER && renderHead }) { renderMode.value == RenderMode.DUAL }))
    private val joinedColor by setting(this, ColorSetting(settingName("Joined Color"), ColorRGB(255, 32, 64), LambdaUtilsKt.and({ page.value == Page.RENDER }) { renderMode.value == RenderMode.JOINED }))
    private val baseColor by setting(this, ColorSetting(settingName("Base Color"), ColorRGB(32, 255, 32), { page.value == Page.RENDER && renderBase }))
    private val rotateLength by setting(this, IntegerSetting(settingName("Rotate Length"), 250, 0..1000, 50, { page.value == Page.RENDER }))
    private val movingLength by setting(this, IntegerSetting(settingName("Moving Length"), 500, 0..1000, 50, { page.value == Page.RENDER }))
    private val fadeLength by setting(this, IntegerSetting(settingName("Fade Length"), 250, 0..1000, 50, { page.value == Page.RENDER }))

    private const val renderBasePlaced = true
    private val basePlacedColor by setting(this, ColorSetting(settingName("Base Placed Color"), ColorRGB(32, 255, 32), LambdaUtilsKt.and({ page.value == Page.BASE_PLACE && basePlace }) { renderBasePlaced }))
    private val basePlacedFilled by setting(this, IntegerSetting(settingName("Base Placed Filled Alpha"), 63, 0..255, 1, LambdaUtilsKt.and({ page.value == Page.BASE_PLACE && basePlace }) { renderBasePlaced }))
    private val basePlacedOutline by setting(this, IntegerSetting(settingName("Base Placed Outline Alpha"), 200, 0..255, 1, LambdaUtilsKt.and({ page.value == Page.BASE_PLACE && basePlace }) { renderBasePlaced }))
    private val basePlaceAnimateLength by setting(this, IntegerSetting(settingName("Base Place Animate Length"), 500, 0..1000, 50, LambdaUtilsKt.and({ page.value == Page.BASE_PLACE && basePlace }) { renderBasePlaced }))
    private val basePlaceFadeLength by setting(this, IntegerSetting(settingName("Base Place Fade Length"), 3000, 0..5000, 100, LambdaUtilsKt.and({ page.value == Page.BASE_PLACE && basePlace }) { renderBasePlaced }))

    private val updateTimer = TickTimer()
    private val timer = TickTimer()
    private val blockerExists = AtomicBoolean(true)
    private val blockerSwitch = AtomicBoolean(false)
    private val blockerTimer = TickTimer()
    private val basePlacedBlocks = mutableMapOf<BlockPos, BasePlaceRenderInfo>()
    private val basePlaceTimer = TickTimer(TimeUnit.MILLISECONDS)
    private var switchPlacing = false
    private var placeInfo: PlaceInfo? = null
    private var selfMoving = false
    private var targetMoving = false
    private var toggleForcePlace = false
    private var shouldForcePlace = false
    private var lastDamage = 0.0f
    private var lastTask: InventoryTask? = null
    private var job: Job? = null
    private val explosionTimer = TickTimer()
    private val explosionCountArray = CircularArray<Int>(8)
    private var explosionCount = 0
    private var inactiveTicks = 10
    var needOffhandBed = false
        private set

    private val function: (World, BlockPos, IBlockState) -> FastRayTraceAction = { world, pos, state ->
        val block = state.block
        if (block == Blocks.AIR || block == Blocks.BED || (assumeInstantMine && PacketMine.isInstantMining(pos)) || !CrystalUtils.isResistant(state)) FastRayTraceAction.SKIP else FastRayTraceAction.CALC
    }

    init {
        onDisable { reset() }
        onEnable { reset() }

        listener<PacketEvent.Receive> { event ->
            val packet = event.packet
            if (packet is SPacketSoundEffect && packet.category == SoundCategory.BLOCKS && packet.sound == SoundEvents.ENTITY_GENERIC_EXPLODE) {
                placeInfo?.let {
                    if (it.center.distanceTo(packet.x, packet.y, packet.z) <= 4.0) explosionCount++
                }
            } else if (packet is SPacketBlockChange) {
                CombatManager.target?.let {
                    val targetPos = EntityUtils.getBetterPosition(it)
                    if (packet.blockPosition == targetPos && !CrystalUtils.isResistant(packet.blockState)) {
                        blockerExists.set(true)
                    }
                }
            }
        }

        listener<Render3DEvent> { Renderer.onRender3D() }
        safeListener<Render2DEvent.Absolute> { Renderer.onRender2D() }

        listener<EntityEvent.UpdateHealth> {
            if (it.entity == CombatManager.target) {
                val diff = it.prevHealth - it.health
                if (diff > 0.0f) lastDamage += diff
            }
        }

        listener<CombatEvent.UpdateTarget> { lastDamage = 0.0f }

        safeListener<OnUpdateWalkingPlayerEvent.Pre> {
            placeInfo?.let {
                val rotation = if (AntiCheat.blockPlaceRotation) RotationUtils.getRotationTo(this, it.hitVec) else Vec2f(RotationUtils.getYaw(it.direction), rotationPitch.toFloat())
                PlayerPacketManager.sendPlayerPacket { rotate(rotation) }
            }
        }

        safeListener<TickEvent.Post> {
            inactiveTicks++
            update(this)
            runLoop(this)

            if (basePlace && player.ticksExisted > 0) {
                if (!CoroutineUtilsKt.isActiveOrFalse(job) && canRun(this)) {
                    job = runBedBasePlace(this)
                }
                if (CoroutineUtilsKt.isActiveOrFalse(job) && AntiCheat.blockPlaceRotation) {
                    PlayerPacketManager.sendPlayerPacket { cancelAll() }
                }
            }
        }

        safeListener<RunGameLoopEvent.Tick> {
            if (explosionTimer.tickAndReset(250L)) {
                explosionCountArray.add(explosionCount)
                explosionCount = 0
            }
            update(this)
            runLoop(this)
        }

        DefaultScope.launch {
            while (true) {
                if (isEnabled) {
                    val instance = SafeClientEvent.instance
                    if (instance != null) {
                        runLoop(instance)
                    }
                }
                delay(5L)
            }
        }
    }

    override fun getHudInfo(): String = lastType?.name?.toLowerCase(Locale.ROOT) ?: ""

    private fun update(event: SafeClientEvent) {
        if (event.player.dimension == 0 || !IterableKt.hasItem(DefinedKt.getAllSlotsPrioritized(event.player), Items.BED)) {
            reset()
        } else if (updateTimer.tickAndReset(updateDelay.toLong())) {
            ConcurrentScope.launch {
                placeInfo = calcPlaceInfo(event)
            }
        }

        CombatManager.trackerSelf?.let { selfMoving = it.motion.lengthVector() > selfMotion } ?: run { selfMoving = false }
        CombatManager.trackerTarget?.let { targetMoving = it.motion.lengthVector() > targetMotion } ?: run { targetMoving = false }

        CombatManager.target?.let {
            shouldForcePlace = it.health <= forcePlaceHealth
            if (System.currentTimeMillis() - CombatManager.getHurtTime(it) > 500L) lastDamage = 0.0f
        }
    }

    private fun SafeClientEvent.runLoop() {
        val info = placeInfo ?: run { needOffhandBed = false; return }
        needOffhandBed = handMode == EnumHand.OFF_HAND

        if (handMode == EnumHand.MAIN_HAND) {
            if (!TaskKt.getExecutedOrTrue(lastTask)) return
            val slot = DefinedKt.getHotbarSlots(player).get(bedSlot - 1)
            if (slot.stack.item != Items.BED) {
                refillBed(this, slot)
                return
            }
        } else if (player.heldItemOffhand.item != Items.BED) return

        val validDamage = !smartDamage || shouldForcePlace || info.targetDamage - lastDamage >= damageStep

        when (timingMode) {
            TimingMode.INSTANT -> {
                if (validDamage) {
                    if (timer.tick(getDelay(info, delay, slowDelay).toLong())) {
                        placeBed(info)
                        breakBed(this, info)
                    }
                } else breakIfPlaced(this, info, getDelay(info, delay, slowDelay))
            }
            TimingMode.SYNC -> {
                if (validDamage) {
                    if (timer.tick(getDelay(info, delay, slowDelay).toLong())) {
                        if (isBedPlaced(this, info)) breakBed(this, info)
                        else { placeBed(info); breakBed(this, info) }
                    }
                } else breakIfPlaced(this, info, getDelay(info, delay, slowDelay))
            }
            TimingMode.SWITCH -> {
                if (validDamage) {
                    if (switchPlacing) {
                        if (timer.tick(getDelay(info, placeDelay, slowPlaceDelay).toLong())) {
                            breakBed(this, info)
                            switchPlacing = !switchPlacing
                        }
                    } else if (timer.tick(getDelay(info, breakDelay, slowBreakDelay).toLong())) {
                        placeBed(info)
                        switchPlacing = !switchPlacing
                    }
                } else breakIfPlaced(this, info, getDelay(info, breakDelay, slowBreakDelay))
            }
            TimingMode.AUTO -> {
                val target = CombatManager.target
                if (target != null && target.onGround) {
                    if (validDamage && timer.tick(getDelay(info, delay, slowDelay).toLong())) {
                        placeBed(info)
                        breakBed(this, info)
                    } else breakIfPlaced(this, info, getDelay(info, delay, slowDelay))
                } else {
                    if (validDamage) {
                        if (switchPlacing) {
                            if (timer.tick(getDelay(info, placeDelay, slowPlaceDelay).toLong())) {
                                breakBed(this, info)
                                switchPlacing = !switchPlacing
                            }
                        } else if (timer.tick(getDelay(info, breakDelay, slowBreakDelay).toLong())) {
                            placeBed(info)
                            switchPlacing = !switchPlacing
                        }
                    } else breakIfPlaced(this, info, getDelay(info, breakDelay, slowBreakDelay))
                }
            }
        }
    }

    private fun refillBed(event: SafeClientEvent, slot: HotbarSlot) {
        val storage = DefinedKt.getStorageSlots(event.player).firstItem(Items.BED) ?: DefinedKt.getCraftingSlots(event.player).firstItem(Items.BED) ?: return
        InventoryTask.Builder().apply {
            priority(modulePriority)
            swapWith(storage, slot)
        }.build().let {
            InventoryTaskManager.addTask(it)
            lastTask = it
        }
    }

    private fun breakIfPlaced(event: SafeClientEvent, info: PlaceInfo, delay: Int) {
        if (timer.tick(delay.toLong()) && isBedPlaced(event, info)) breakBed(event, info)
    }

    private fun isBedPlaced(event: SafeClientEvent, info: PlaceInfo): Boolean = event.world.getBlockState(info.bedPosFoot).block == Blocks.BED || event.world.getBlockState(info.bedPosHead).block == Blocks.BED

    private fun breakBed(event: SafeClientEvent, info: PlaceInfo) {
        val side = InteractKt.getMiningSide(event, info.bedPosFoot) ?: EnumFacing.UP
        val offset = InteractKt.getHitVecOffset(side)

        if (event.player.isSneaking) {
            event.connection.sendPacket(CPacketEntityAction(event.player, CPacketEntityAction.Action.STOP_SNEAKING))
            event.connection.sendPacket(CPacketPlayerTryUseItemOnBlock(info.bedPosFoot, side, handMode, offset.x, offset.y, offset.z))
            event.connection.sendPacket(CPacketEntityAction(event.player, CPacketEntityAction.Action.START_SNEAKING))
        } else {
            event.connection.sendPacket(CPacketPlayerTryUseItemOnBlock(info.bedPosFoot, side, handMode, offset.x, offset.y, offset.z))
        }
        event.connection.sendPacket(CPacketAnimation(handMode))
        blockerExists.set(false)
        timer.reset()
        inactiveTicks = 0
    }

    private fun SafeClientEvent.placeBed(info: PlaceInfo) {
        val sneak = !player.isSneaking
        if (sneak) connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.START_SNEAKING))

        if (player.heldItemOffhand.item == Items.BED) {
            OffhandPause.withPause(INSTANCE) {
                connection.sendPacket(CPacketPlayerTryUseItemOnBlock(info.basePos, EnumFacing.UP, EnumHand.OFF_HAND, 0.5f, 1.0f, 0.5f))
            }
        } else {
            val packet = CPacketPlayerTryUseItemOnBlock(info.basePos, EnumFacing.UP, EnumHand.MAIN_HAND, 0.5f, 1.0f, 0.5f)
            HotbarSwitchManager.ghostSwitch(this, ghostSwitchBypass, bedSlot - 1) {
                connection.sendPacket(packet)
            }
        }
        connection.sendPacket(CPacketAnimation(handMode))
        if (sneak) connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.STOP_SNEAKING))

        CombatManager.target?.let { player.setLastAttackedEntity(it) }
        timer.reset()
        inactiveTicks = 0
    }

    private fun getDelay(info: PlaceInfo, delay: Int, slowDelay: Int): Int {
        if (slowMode) {
            val target = CombatManager.target
            if (target != null && target.health > forcePlaceHealth && info.targetDamage < slowModeDamage && !targetMoving) {
                return slowDelay
            }
        }
        return delay
    }

    private fun SafeClientEvent.calcPlaceInfo(event: SafeClientEvent): PlaceInfo? {
        val ctxSelf = CombatManager.contextSelf ?: return null
        val ctxTarget = CombatManager.contextTarget ?: return null
        val eyePos = EntityUtils.getEyePosition(player)
        val map = Long2ObjectOpenHashMap<Vec2f>()
        val mutable = BlockPos.MutableBlockPos()

        val ignoreNonFull = if (antiBlocker) {
            if (blockerTimer.tickAndReset(antiBlockerSwitch.toLong())) blockerSwitch.set(!blockerSwitch.get())
            antiBlocker && !blockerExists.get()
        } else false

        return VectorUtils.getBlockPosInSphere(eyePos, range).asSequence()
            .filter { !strictDirection || eyePos.y > it.y + 1.0 }
            .flatMap { pos ->
                if (AntiCheat.blockPlaceRotation) {
                    val hitVec = pos.toVec3d(0.5, 1.0, 0.5)
                    val side = calcDirection(eyePos, hitVec)
                    sequenceOf(newCalcInfo(side, pos, hitVec))
                } else {
                    val hitVec = pos.toVec3d(0.5, 1.0, 0.5)
                    sequenceOf(
                        newCalcInfo(EnumFacing.NORTH, pos, hitVec),
                        newCalcInfo(EnumFacing.SOUTH, pos, hitVec),
                        newCalcInfo(EnumFacing.WEST, pos, hitVec),
                        newCalcInfo(EnumFacing.EAST, pos, hitVec)
                    )
                }
            }
            .filter { DistanceKt.distanceSqToCenter(ctxTarget.entity, it.bedPosHead) <= 100.0 }
            .filter { isValidBasePos(this, it.basePosFoot) && (newPlacement || isValidBasePos(this, it.basePosHead)) }
            .filter { isValidBedPos(this, ignoreNonFull, it) }
            .mapNotNull { info ->
                val scaledHealth = CombatUtils.getScaledHealth(ctxSelf.entity)
                val headCenter = info.bedPosHead.toVec3dCenter()
                val damages = map.computeIfAbsent(info.basePosHead.toLong()) {
                    Vec2f(ctxTarget.calcDamage(headCenter, true, 5.0f, mutable, function),
                          maxOf(ctxSelf.calcDamage(headCenter, false, 5.0f, mutable, function),
                                ctxSelf.calcDamage(headCenter, true, 5.0f, mutable, function)))
                }
                val targetDamage = damages.x
                val selfDamage = damages.y
                val diff = targetDamage - selfDamage

                if (scaledHealth - selfDamage > noSuicide && checkSelfDamage(selfDamage) && (checkDamage(targetDamage, diff) || checkForcePlaceDamage(targetDamage, diff))) {
                    DamageInfo(info.side, info.hitVec, info.basePosFoot, info.bedPosFoot, info.bedPosHead, targetDamage, selfDamage)
                } else null
            }
            .sortedWith(compareByDescending<DamageInfo> { it.targetDamage }.thenBy { player.getDistanceSqToCenter(it.basePos) })
            .firstOrNull()?.let { toPlaceInfo(it) }
    }

    private fun calcDirection(eyePos: Vec3d, hitVec: Vec3d): EnumFacing {
        val x = hitVec.x - eyePos.x
        val z = hitVec.z - eyePos.z
        return EnumFacing.HORIZONTALS.maxByOrNull { (x * it.directionVec.x + z * it.directionVec.z).toDouble() } ?: EnumFacing.NORTH
    }

    private fun newCalcInfo(side: EnumFacing, pos: BlockPos, hitVec: Vec3d): CalcInfo {
        val bedPos = pos.up()
        return CalcInfo(side, hitVec, pos, pos.offset(side), bedPos, bedPos.offset(side))
    }

    private fun isValidBasePos(event: SafeClientEvent, pos: BlockPos): Boolean = event.world.getBlockState(pos).isSideSolid(event.world, pos, EnumFacing.UP)

    private fun isValidBedPos(event: SafeClientEvent, ignoreNonFull: Boolean, info: CalcInfo): Boolean {
        val headState = event.world.getBlockState(info.bedPosHead)
        val footState = event.world.getBlockState(info.bedPosFoot)
        if (!checkBedBlock(ignoreNonFull, info.bedPosFoot, footState) && footState.block != Blocks.BED) return false
        if (checkBedBlock(ignoreNonFull, info.bedPosHead, headState)) return true
        return headState.block == Blocks.BED && headState.getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD && headState.getValue(BlockBed.FACING) == info.side
    }

    private fun checkBedBlock(ignoreNonFull: Boolean, pos: BlockPos, state: IBlockState): Boolean {
        val block = state.block
        return block == Blocks.AIR || (assumeInstantMine && PacketMine.isInstantMining(pos)) || (ignoreNonFull && !dev.wizard.meta.util.inventory.BlockKt.blockBlacklist.contains(block) && block != Blocks.BED && !BlockKt.isFullBox(state))
    }

    private fun checkSelfDamage(selfDamage: Float): Boolean {
        val mMax = if (motionMaxSelfDamage == 0.0f) 10000.0f else motionMaxSelfDamage
        val max = if (maxSelfDamage == 0.0f) 10000.0f else maxSelfDamage
        return if (selfMoving) selfDamage <= mMax else selfDamage <= max
    }

    private fun checkDamage(targetDamage: Float, diff: Float): Boolean {
        val min = if (targetMoving) motionMinDamage else minDamage
        val balance = if (targetMoving) motionDamageBalance else damageBalance
        return targetDamage >= min && diff >= balance
    }

    private fun checkForcePlaceDamage(targetDamage: Float, diff: Float): Boolean = (toggleForcePlace || shouldForcePlace) && targetDamage >= forcePlaceMinDamage && diff >= forcePlaceDamageBalance

    private fun toPlaceInfo(info: DamageInfo): PlaceInfo {
        val dirVec = info.side.directionVec
        val center = info.bedPosFoot.toVec3d().addVector(0.5 + dirVec.x * 0.5, 0.0, 0.5 + dirVec.z * 0.5)
        return PlaceInfo(info.side, info.hitVec, info.basePos, info.bedPosFoot, info.bedPosHead, info.targetDamage, info.selfDamage, center, "%.1f/%.1f".format(info.targetDamage, info.selfDamage))
    }

    private fun canRun(event: SafeClientEvent): Boolean {
        val target = CombatManager.target ?: return false
        val targetPos = EntityUtils.getFlooredPosition(target)
        if (event.player.getDistanceSq(targetPos) > 49.0) return false
        return EnumFacing.HORIZONTALS.any { BlockKt.isReplaceable(event.world.getBlockState(targetPos.down().offset(it))) }
    }

    private fun runBedBasePlace(event: SafeClientEvent): Job {
        return ConcurrentScope.launch {
            while (isEnabled && basePlaceTimer.tickAndReset(basePlaceDelay.toLong()) && CombatManager.target != null && CombatManager.contextSelf != null && CombatManager.contextTarget != null) {
                val eyePos = EntityUtils.getEyePosition(event.player)
                val mutable = BlockPos.MutableBlockPos()
                val now = System.currentTimeMillis()
                basePlacedBlocks.entries.removeIf { now - it.value.placeTime > basePlaceFadeLength }

                val target = CombatManager.target!!
                val targetPos = EntityUtils.getFlooredPosition(target)
                val surrounding = EnumFacing.HORIZONTALS.map { targetPos.down().offset(it) }

                val valid = surrounding.mapNotNull { pos ->
                    val state = event.world.getBlockState(pos)
                    if (!BlockKt.isReplaceable(state) || !EntityManager.checkNoEntityCollision(AxisAlignedBB(pos))) return@mapNotNull null
                    if (event.player.getDistanceSqToCenter(pos) > basePlaceRange * basePlaceRange) return@mapNotNull null
                    if (basePlacedBlocks.containsKey(pos)) return@mapNotNull null
                    if (!event.world.getBlockState(pos.down()).isSideSolid(event.world, pos.down(), EnumFacing.UP)) return@mapNotNull null

                    val bedCenter = pos.up().toVec3dCenter()
                    val targetDmg = CombatManager.contextTarget!!.calcDamage(bedCenter, true, 5.0f, mutable, function)
                    val selfDmg = maxOf(CombatManager.contextSelf!!.calcDamage(bedCenter, false, 5.0f, mutable, function), CombatManager.contextSelf!!.calcDamage(bedCenter, true, 5.0f, mutable, function))

                    if (selfDmg <= basePlaceMaxSelf && targetDmg >= basePlaceMinTarget) pos to targetDmg else null
                }.sortedByDescending { it.second }

                if (valid.isEmpty()) break
                val placePos = valid.first().first
                val pInfo = InteractKt.getPlacement(event, placePos, EnumFacing.HORIZONTALS + EnumFacing.DOWN, arrayOf(PlacementSearchOption.ENTITY_COLLISION, PlacementSearchOption.range(basePlaceRange))) ?: break
                val slot = getObby(event) ?: break

                ThreadSafetyKt.runSafeSuspend {
                    doPlace(it, pInfo, slot)
                    val box = it.world.getBlockState(placePos).getSelectedBoundingBox(it.world, placePos)
                    basePlacedBlocks[placePos] = BasePlaceRenderInfo(placePos, now, box, box, box, now, now, 0.0f)
                }

                if (isSurroundedByAir(event, placePos)) {
                    val side = getClosestSideToTarget(placePos, targetPos)
                    val secondPos = placePos.offset(side)
                    val bedCenter2 = secondPos.up().toVec3dCenter()
                    val targetDmg2 = CombatManager.contextTarget!!.calcDamage(bedCenter2, true, 5.0f, mutable, function)
                    val selfDmg2 = maxOf(CombatManager.contextSelf!!.calcDamage(bedCenter2, false, 5.0f, mutable, function), CombatManager.contextSelf!!.calcDamage(bedCenter2, true, 5.0f, mutable, function))

                    if (selfDmg2 <= basePlaceMaxSelf && targetDmg2 >= basePlaceMinTarget) {
                        if (event.player.getDistanceSqToCenter(secondPos) <= basePlaceRange * basePlaceRange && !basePlacedBlocks.containsKey(secondPos)) {
                            val pInfo2 = InteractKt.getPlacement(event, secondPos, EnumFacing.HORIZONTALS + EnumFacing.DOWN, arrayOf(PlacementSearchOption.ENTITY_COLLISION, PlacementSearchOption.range(basePlaceRange))) ?: break
                            ThreadSafetyKt.runSafeSuspend {
                                doPlace(it, pInfo2, slot)
                                val box2 = it.world.getBlockState(secondPos).getSelectedBoundingBox(it.world, secondPos)
                                basePlacedBlocks[secondPos] = BasePlaceRenderInfo(secondPos, now, box2, box2, box2, now, now, 0.0f)
                            }
                        }
                    }
                }
                delay(basePlaceUpdateDelay.toLong())
            }
        }
    }

    private fun getObby(event: SafeClientEvent): HotbarSlot? {
        val slot = IterableKt.firstBlock(DefinedKt.getHotbarSlots(event.player), Blocks.OBSIDIAN)
        if (slot == null) NoSpamMessage.sendMessage("${getChatName()} No obsidian in hotbar!")
        return slot
    }

    private suspend fun doPlace(event: SafeClientEvent, info: PlaceInfo, slot: HotbarSlot) {
        val packet = InteractKt.toPlacePacket(info, EnumHand.MAIN_HAND)
        if (AntiCheat.blockPlaceRotation) {
            val rotation = RotationUtils.getRotationTo(event, info.hitVec)
            event.connection.sendPacket(CPacketPlayer.PositionRotation(event.player.posX, event.player.posY, event.player.posZ, rotation.x, rotation.y, event.player.onGround))
        }
        val sneak = !event.player.isSneaking
        if (sneak) event.connection.sendPacket(CPacketEntityAction(event.player, CPacketEntityAction.Action.START_SNEAKING))
        HotbarSwitchManager.ghostSwitch(event, slot) { event.connection.sendPacket(packet) }
        event.player.swingArm(EnumHand.MAIN_HAND)
        if (sneak) event.connection.sendPacket(CPacketEntityAction(event.player, CPacketEntityAction.Action.STOP_SNEAKING))
    }

    private fun isSurroundedByAir(event: SafeClientEvent, pos: BlockPos): Boolean = EnumFacing.HORIZONTALS.all { event.world.isAirBlock(pos.offset(it)) }

    private fun getClosestSideToTarget(pos: BlockPos, target: BlockPos): EnumFacing {
        val dx = target.x - pos.x
        val dz = target.z - pos.z
        return if (abs(dx) > abs(dz)) if (dx > 0) EnumFacing.EAST else EnumFacing.WEST else if (dz > 0) EnumFacing.SOUTH else EnumFacing.NORTH
    }

    private fun reset() {
        blockerExists.set(true)
        updateTimer.reset(-69420L)
        timer.reset(-69420L)
        switchPlacing = false
        placeInfo = null
        selfMoving = false
        targetMoving = false
        toggleForcePlace = false
        shouldForcePlace = false
        lastDamage = 0.0f
        lastTask = null
        inactiveTicks = 10
        needOffhandBed = false
        basePlacedBlocks.clear()
        Renderer.reset()
    }

    private enum class Page { GENERAL, TIMING, FORCE_PLACE, BASE_PLACE, MOTION_DETECT, RENDER }
    private enum class TimingMode { INSTANT, SYNC, SWITCH, AUTO }
    private enum class RenderMode { FADE, DUAL, JOINED }
    private data class CalcInfo(val side: EnumFacing, val hitVec: Vec3d, val basePosFoot: BlockPos, val basePosHead: BlockPos, val bedPosFoot: BlockPos, val bedPosHead: BlockPos)
    private data class DamageInfo(val side: EnumFacing, val hitVec: Vec3d, val basePos: BlockPos, val bedPosFoot: BlockPos, val bedPosHead: BlockPos, val targetDamage: Float, val selfDamage: Float)
    private data class TargetInfo(val entity: EntityLivingBase, val pos: Vec3d, val box: AxisAlignedBB, val currentPos: Vec3d, val motion: Vec3d, val sample: dev.wizard.meta.util.combat.ExposureSample)
    private data class BasePlaceRenderInfo(val pos: BlockPos, val placeTime: Long, var prevBox: AxisAlignedBB?, var currentBox: AxisAlignedBB, var lastRenderBox: AxisAlignedBB?, var lastUpdateTime: Long, var startTime: Long, var scale: Float)

    private object Renderer {
        private var lastBedPlacement: Pair<BlockPos, EnumFacing>? = null
        private var lastRotation = Float.NaN
        private var currentRotation = Float.NaN
        private var lastPos: Vec3d? = null
        private var currentPos: Vec3d? = null
        private var lastRenderRotation = Float.NaN
        private var lastRenderPos: Vec3d? = null
        private var lastUpdateTime = 0L
        private var startTime = 0L
        private var scale = 0.0f
        private var lastDamageString = ""
        private val boxBase = AxisAlignedBB(-0.5, -0.4375, -1.0, 0.5, 0.0, 1.0)
        private val boxFoot = AxisAlignedBB(-0.5, 0.0, -1.0, 0.5, 0.5625, 0.0)
        private val boxHead = AxisAlignedBB(-0.5, 0.0, 0.0, 0.5, 0.5625, 1.0)
        private val boxJoined = AxisAlignedBB(-0.5, 0.0, -1.0, 0.5, 0.5625, 1.0)

        fun reset() {
            lastBedPlacement = null
            lastRotation = Float.NaN
            currentRotation = Float.NaN
            lastPos = null
            currentPos = null
            lastRenderRotation = Float.NaN
            lastRenderPos = null
            lastUpdateTime = 0L
            startTime = 0L
            scale = 0.0f
            lastDamageString = ""
        }

        fun onRender3D() {
            val info = placeInfo
            update(info)
            val lPos = lastPos ?: return
            val cPos = currentPos ?: return
            if (lastRotation.isNaN() || currentRotation.isNaN()) return
            
            val rotateMul = Easing.OUT_CUBIC.inc(Easing.toDelta(lastUpdateTime, rotateLength.toLong()))
            val renderRotation = lastRotation + (currentRotation - lastRotation) * rotateMul
            lastRenderRotation = renderRotation
            
            val movingMul = Easing.OUT_QUINT.inc(Easing.toDelta(lastUpdateTime, movingLength.toLong()))
            val renderPos = lPos.add(cPos.subtract(lPos).scale(movingMul.toDouble()))
            lastRenderPos = renderPos
            
            scale = if (info != null) Easing.OUT_CUBIC.inc(Easing.toDelta(startTime, fadeLength.toLong())) else Easing.IN_CUBIC.dec(Easing.toDelta(startTime, fadeLength.toLong()))
            
            GlStateManager.pushMatrix()
            val rm = mc.renderManager
            GlStateManager.translate(renderPos.x - rm.renderPosX, renderPos.y - rm.renderPosY, renderPos.z - rm.renderPosZ)
            GlStateManager.rotate(renderRotation, 0.0f, 1.0f, 0.0f)
            
            val renderer = ESPRenderer()
            renderer.aFilled = (32.0f * scale).toInt()
            renderer.aOutline = (233.0f * scale).toInt()
            
            if (renderBase) renderer.add(boxBase, baseColor)
            
            when (renderMode.value) {
                RenderMode.DUAL -> {
                    if (renderFoot) renderer.add(boxFoot, footColor)
                    if (renderHead) renderer.add(boxHead, headColor)
                }
                RenderMode.JOINED -> renderer.add(boxJoined, joinedColor)
            }
            
            RenderUtils3D.resetTranslation()
            renderer.render(false)
            RenderUtils3D.setTranslation(-rm.renderPosX, -rm.renderPosY, -rm.renderPosZ)
            GlStateManager.popMatrix()
            
            if (renderBasePlaced && basePlace) renderBasePlacedBlocks()
        }

        fun onRender2D() {
            if (scale == 0.0f || !renderDamage) return
            val rPos = lastRenderPos ?: return
            val screenPos = ProjectionUtils.toAbsoluteScreenPos(rPos)
            val alpha = (255.0f * scale).toInt()
            val color = if (scale == 1.0f) ColorRGB(255, 255, 255) else ColorRGB(255, 255, 255, alpha)
            MainFontRenderer.drawString(lastDamageString, screenPos.x.toFloat() - MainFontRenderer.getWidth(lastDamageString, 2.0f) * 0.5f, screenPos.y.toFloat() - MainFontRenderer.getHeight(2.0f) * 0.5f, color, 2.0f)
        }

        private fun renderBasePlacedBlocks() {
            val now = System.currentTimeMillis()
            val it = basePlacedBlocks.entries.iterator()
            while (it.hasNext()) {
                val entry = it.next()
                val info = entry.value
                val age = now - info.placeTime
                if (age > basePlaceFadeLength) {
                    it.remove()
                    continue
                }
                
                val blockState = mc.world.getBlockState(info.pos)
                val rawBox = blockState.getSelectedBoundingBox(mc.world, info.pos)
                val rm = mc.renderManager
                val box = rawBox.offset(-rm.renderPosX, -rm.renderPosY, -rm.renderPosZ)
                info.currentBox = box
                if (info.prevBox == null) {
                    info.prevBox = box
                    info.lastRenderBox = box
                }
                
                val movingMul = Easing.OUT_QUART.inc(Easing.toDelta(info.lastUpdateTime, basePlaceAnimateLength.toLong()))
                val renderBox = interpolateBox(info.prevBox!!, info.currentBox, movingMul.toDouble())
                info.lastRenderBox = renderBox
                
                val alphaScale = Easing.IN_CUBIC.dec(age.toFloat() / basePlaceFadeLength.toFloat())
                if (alphaScale > 0.0f) {
                    val renderer = ESPRenderer()
                    renderer.aFilled = (basePlacedFilled * alphaScale).toInt()
                    renderer.aOutline = (basePlacedOutline * alphaScale).toInt()
                    renderer.add(renderBox, basePlacedColor)
                    renderer.render(false)
                }
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

        private fun update(info: PlaceInfo?) {
            val newPlacement = info?.let { it.basePos to it.direction }
            if (newPlacement != lastBedPlacement) {
                if (info != null) {
                    currentPos = info.center
                    if (lastRenderPos == null) lastPos = currentPos
                    
                    if (currentRotation.isNaN()) {
                        currentRotation = info.direction.horizontalAngle
                    } else {
                        val newAngle = info.direction.horizontalAngle
                        val lastAngle = lastBedPlacement?.second?.horizontalAngle ?: 0.0f
                        currentRotation += RotationUtils.normalizeAngle(newAngle - lastAngle)
                    }
                    lastRotation = if (!lastRenderRotation.isNaN()) lastRenderRotation else currentRotation
                    lastUpdateTime = System.currentTimeMillis()
                    if (lastBedPlacement == null) startTime = System.currentTimeMillis()
                } else {
                    lastUpdateTime = System.currentTimeMillis()
                    startTime = System.currentTimeMillis()
                }
                lastBedPlacement = newPlacement
            }
            if (info != null) lastDamageString = info.string
        }
    }
}
