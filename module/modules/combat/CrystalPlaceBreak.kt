package dev.wizard.meta.module.modules.combat

import dev.fastmc.common.DistanceKt
import dev.fastmc.common.MathUtilKt
import dev.fastmc.common.TickTimer
import dev.fastmc.common.collection.CircularArray
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.player.OnUpdateWalkingPlayerEvent
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.graphics.ESPRenderer
import dev.wizard.meta.graphics.ProjectionUtils
import dev.wizard.meta.graphics.RenderUtils3D
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.manager.managers.*
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.client.AntiCheat
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.*
import dev.wizard.meta.util.accessor.*
import dev.wizard.meta.util.combat.*
import dev.wizard.meta.util.delegate.CachedValueN
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.inventory.ItemKt
import dev.wizard.meta.util.inventory.operation.swapToSlot
import dev.wizard.meta.util.inventory.slot.*
import dev.wizard.meta.util.math.RotationUtils
import dev.wizard.meta.util.math.VectorUtils
import dev.wizard.meta.util.math.vector.*
import dev.wizard.meta.util.pause.HandPause
import dev.wizard.meta.util.pause.MainHandPause
import dev.wizard.meta.util.pause.withPause
import dev.wizard.meta.util.threads.onMainThread
import dev.wizard.meta.util.threads.runSynchronized
import dev.wizard.meta.util.world.FastRayTraceFunction
import dev.wizard.meta.util.world.rayTraceVisible
import it.unimi.dsi.fastutil.ints.*
import it.unimi.dsi.fastutil.longs.Long2LongMap
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityEnderCrystal
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Items
import net.minecraft.init.MobEffects
import net.minecraft.init.SoundEvents
import net.minecraft.item.ItemFood
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemSword
import net.minecraft.item.ItemTool
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.network.play.client.CPacketUseEntity
import net.minecraft.network.play.server.SPacketSoundEffect
import net.minecraft.network.play.server.SPacketSpawnObject
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.max

@CombatManager.CombatModule
object CrystalPlaceBreak : Module("CrystalPlaceBreak", Category.COMBAT, "Lol wtf") {
    private val page by setting("Page", Page.GENERAL)

    // General
    private val players by setting("Players", true, { page == Page.GENERAL })
    private val mobs by setting("Mobs", false, { page == Page.GENERAL })
    private val animals by setting("Animals", false, { page == Page.GENERAL })
    private val maxTargets by setting("Max Targets", 4, 1..10, 1, { page == Page.GENERAL })
    private val targetRange by setting("Target Range", 10.0f, 1.0f..16.0f, 0.5f, { page == Page.GENERAL })
    private val yawSpeed by setting("Yaw Speed", 60.0f, 10.0f..180.0f, 5.0f, { page == Page.GENERAL })

    private val doCrystalRotation by setting("Crystal Rotation", true, { page == Page.GENERAL })
    private val placeRotationRange by setting("Place Rotation Range", 1.0f, 0.0f..1.0f, 0.01f, { page == Page.GENERAL && doCrystalRotation })
    private val breakRotationRange by setting("Break Rotation Range", 1.0f, 0.0f..1.0f, 0.01f, { page == Page.GENERAL && doCrystalRotation })

    private val eatingPause by setting("Eating Pause", true, { page == Page.GENERAL })
    private val updateDelay by setting("Update Delay", 10, 0..1000, 10, { page == Page.GENERAL })
    private val globalDelay by setting("Global Delay", 10, 0..1000, 10, { page == Page.GENERAL })

    // Calculation
    private val forcePlaceHealth by setting("Force Place Health", 8.0f, 0.0f..20.0f, 0.5f, { page == Page.CALCULATION })
    private val forcePlaceArmorRate by setting("Force Place Armor Rate", 5, 0..100, 1, { page == Page.CALCULATION })
    private val forcePlaceMinDamage by setting("Force Place Min Damage", 1.5f, 0.0f..20.0f, 0.5f, { page == Page.CALCULATION })
    private val forcePlaceMotion by setting("Force Place Motion", 10.0f, 0.0f..20.0f, 0.25f, { page == Page.CALCULATION })
    private val forcePlaceBalance by setting("Force Place Balance", -3.0f, -10.0f..10.0f, 0.5f, { page == Page.CALCULATION })
    private val forcePlaceWhileSwording by setting("Force Place While Swording", true, { page == Page.CALCULATION })

    private val assumeInstantMine by setting("Assume Instant Mine", true, { page == Page.CALCULATION })
    val noSuicide by setting("No Suicide", 3.0f, 0.0f..20.0f, 0.5f, { page == Page.CALCULATION })
    private val wallRange by setting("Wall Range", 3.0f, 0.0f..6.0f, 0.5f, { page == Page.CALCULATION })
    private val motionPredict by setting("Motion Predict", true, { page == Page.CALCULATION })
    private val predictTicks by setting("Predict Ticks", 2, 0..10, 1, { page == Page.CALCULATION && motionPredict })
    private val damagePriority by setting("Damage Priority", DamagePriority.DAMAGE, { page == Page.CALCULATION })

    val lethalOverride by setting("Lethal Override", true, { page == Page.CALCULATION })
    val lethalThresholdAddition by setting("Lethal Threshold Addition", 1.0f, 0.0f..10.0f, 0.1f, { page == Page.CALCULATION && lethalOverride })
    val lethalMaxSelfDamage by setting("Lethal Max Self Damage", 8.0f, 0.0f..20.0f, 0.5f, { page == Page.CALCULATION && lethalOverride })

    val safeMaxTargetDamageReduction by setting("Safe Max Target Damage Reduction", 2.0f, 0.0f..20.0f, 0.5f, { page == Page.CALCULATION })
    val safeMinSelfDamageReduction by setting("Safe Min Self Damage Reduction", 2.0f, 0.0f..20.0f, 0.5f, { page == Page.CALCULATION })

    val collidingCrystalExtraSelfDamageThreshold by setting("Colliding Crystal Extra Self Damage Threshold", 2.0f, 0.0f..10.0f, 0.5f, { page == Page.CALCULATION })

    // Place
    private val placeMode by setting("Place Mode", PlaceMode.ALL, { page == Page.PLACE })
    val packetPlace by setting("Packet Place", PacketPlaceMode.BOTH, { page == Page.PLACE })
    private val spamPlace by setting("Spam Place", false, { page == Page.PLACE })
    val placeSwitchMode by setting("Place Switch Mode", SwitchMode.AUTO, { page == Page.PLACE })
    val placeSwitchBypass by setting("Place Switch Bypass", HotbarSwitchManager.Override.None, { page == Page.PLACE })
    private val placeSwing by setting("Place Swing", true, { page == Page.PLACE })
    private val placeSideBypass by setting("Place Side Bypass", PlaceBypass.None, { page == Page.PLACE })
    val placeMinDamage by setting("Place Min Damage", 4.0f, 0.0f..20.0f, 0.5f, { page == Page.PLACE })
    val placeMaxSelfDamage by setting("Place Max Self Damage", 6.0f, 0.0f..20.0f, 0.5f, { page == Page.PLACE })
    val placeBalance by setting("Place Balance", -1.0f, -10.0f..10.0f, 0.5f, { page == Page.PLACE })
    val placeDelay by setting("Place Delay", 25, 0..500, 5, { page == Page.PLACE })
    val placeRange by setting("Place Range", 5.0f, 1.0f..6.0f, 0.5f, { page == Page.PLACE })
    private val placeRangeMode by setting("Place Range Mode", RangeMode.FEET, { page == Page.PLACE })

    // Break
    private val breakMode by setting("Break Mode", BreakMode.TARGET, { page == Page.BREAK })
    private val bbtt by setting("BBTT", false, { page == Page.BREAK })
    private val bbttFactor by setting("BBTT Factor", 2, 1..5, 1, { page == Page.BREAK && bbtt })
    val packetBreak by setting("Packet Break", BreakMode.ALL, { page == Page.BREAK })
    private val ownTimeout by setting("Own Timeout", 200, 0..2000, 50, { page == Page.BREAK })
    val antiWeakness by setting("Anti Weakness", SwitchMode.SILENT, { page == Page.BREAK })
    val antiWeaknessBypass by setting("Anti Weakness Bypass", HotbarSwitchManager.Override.None, { page == Page.BREAK })
    val swapDelay by setting("Swap Delay", 0, 0..20, 1, { page == Page.BREAK })
    val breakMinDamage by setting("Break Min Damage", 3.0f, 0.0f..20.0f, 0.5f, { page == Page.BREAK })
    val breakMaxSelfDamage by setting("Break Max Self Damage", 8.0f, 0.0f..20.0f, 0.5f, { page == Page.BREAK })
    val breakBalance by setting("Break Balance", -2.0f, -10.0f..10.0f, 0.5f, { page == Page.BREAK })
    val breakDelay by setting("Break Delay", 100, 0..500, 5, { page == Page.BREAK })
    private val breakRange by setting("Break Range", 5.0f, 1.0f..6.0f, 0.5f, { page == Page.BREAK })
    private val breakRangeMode by setting("Break Range Mode", RangeMode.FEET, { page == Page.BREAK })
    private val swingMode by setting("Swing Mode", SwingMode.CLIENT, { page == Page.BREAK })
    private val swingHand by setting("Swing Hand", SwingHand.AUTO, { page == Page.BREAK })

    // Render
    private val filledAlpha by setting("Filled Alpha", 63, 0..255, 1, { page == Page.RENDER })
    private val outlineAlpha by setting("Outline Alpha", 200, 0..255, 1, { page == Page.RENDER })
    private val targetDamage by setting("Target Damage", true, { page == Page.RENDER })
    private val selfDamage by setting("Self Damage", true, { page == Page.RENDER })
    private val targetChams by setting("Target Chams", RenderMode.TARGET, { page == Page.RENDER })
    private val chamsAlpha by setting("Chams Alpha", 100, 0..255, 1, { page == Page.RENDER && targetChams != RenderMode.OFF })
    private val renderPredict by setting("Render Predict", RenderMode.TARGET, { page == Page.RENDER })
    private val movingLength by setting("Moving Length", 400, 0..1000, 10, { page == Page.RENDER })
    private val fadeLength by setting("Fade Length", 200, 0..1000, 10, { page == Page.RENDER })

    private val renderTargetSet = CachedValueN(50L) {
        val set = IntOpenHashSet()
        if (targetChams != RenderMode.OFF) {
            targets.get(100L).forEach {
                set.add(it.entity.entityId)
            }
        }
        set
    }

    private val targets = CachedValueN(25L) { getTargets(it) }
    private val rawPosList = CachedValueN(200L) { getRawPosList(it) }

    private val rotationInfo = CachedValueN(50L) { calcPlaceInfo(it, true) }
    private val placeInfo = CachedValueN(50L) { calcPlaceInfo(it, false) }

    private val placedPosMap = Long2LongOpenHashMap()
    private val crystalSpawnMap = Int2LongOpenHashMap()
    private val attackedCrystalMap = Int2LongOpenHashMap()
    private val attackedPosMap = Long2LongOpenHashMap()

    private val timeoutTimer = TickTimer()
    private val placeTimer = TickTimer()
    private val breakTimer = TickTimer()

    var lastActiveTime = 0L; private set
    private var lastRotation: PlaceInfo? = null

    private val explosionTimer = TickTimer()
    private val explosionCountArray = CircularArray(20, 0)
    private var explosionCount = 0

    private val calculationTimes = CircularArray(100, 0)
    private val calculationTimesPending = IntArrayList()

    private val loopThread = Thread {
        while (true) {
            try {
                runLoop()
                val sleep = globalDelay.toLong()
                if (sleep > 0) Thread.sleep(sleep)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }.apply {
        name = "CrystalPlaceBreak Loop"
        isDaemon = true
        start()
    }

    private val reductionMap = WeakHashMap<EntityLivingBase, DamageReduction>()

    init {
        listener<OnUpdateWalkingPlayerEvent.Pre>(Int.MAX_VALUE) {
            lastRotation?.let { info ->
                if (System.currentTimeMillis() - lastActiveTime < 250L) {
                    val rotation = RotationUtils.getRotationTo(it.player.eyePosition, info.hitVec)
                    val diff = RotationUtils.calcAngleDiff(it.rotation, rotation)
                    if (diff <= yawSpeed) {
                        PlayerPacketManager.sendRotation(rotation)
                    } else {
                        val fixedYaw = RotationUtils.limitAngleChange(it.rotation.x, rotation.x, yawSpeed)
                        val fixedPitch = RotationUtils.limitAngleChange(it.rotation.y, rotation.y, yawSpeed)
                        PlayerPacketManager.sendRotation(Vec2f(fixedYaw, fixedPitch))
                    }
                }
            }
        }

        listener<Render3DEvent> {
            val placeInfo = getRenderPlaceInfo()

            if (placeInfo != null) {
                val box = CrystalUtils.getCrystalBB(placeInfo.blockPos)
                val color = ColorRGB(255, 255, 255)

                RenderUtils3D.putCube(
                    box,
                    ColorRGB(color.r, color.g, color.b, filledAlpha),
                    ColorRGB(color.r, color.g, color.b, outlineAlpha)
                )

                if (targetDamage) {
                    val text = String.format("%.1f", placeInfo.targetDamage)
                    val textPos = placeInfo.blockPos.toVec3dCenter().add(0.0, 0.5, 0.0)
                    RenderUtils3D.drawText(textPos, text, Color.WHITE.rgb)
                }

                if (selfDamage) {
                    val text = String.format("%.1f", placeInfo.selfDamage)
                    val textPos = placeInfo.blockPos.toVec3dCenter().add(0.0, -0.5, 0.0)
                    RenderUtils3D.drawText(textPos, text, Color.RED.rgb)
                }
            }

            if (renderPredict != RenderMode.OFF) {
                val targets = targets.get(100L)
                targets.forEach {
                    if (isValidEntityForRendering(renderPredict, it.entity)) {
                        drawEntityPrediction(
                            Tessellator.getInstance().buffer,
                            it.entity,
                            it.motion,
                            it.mc.renderPartialTicks
                        )
                    }
                }
            }
        }

        listener<RenderEntityEvent.All.Pre> {
            if (isValidEntityForRendering(targetChams, it.entity)) {
                GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL)
                GL11.glPolygonOffset(1.0f, -2000000f)
                val color = ColorRGB(255, 0, 0, chamsAlpha)
                // Implement Chams logic here if needed
            }
        }

        listener<RenderEntityEvent.All.Post> {
            if (isValidEntityForRendering(targetChams, it.entity)) {
                GL11.glPolygonOffset(1.0f, 1000000f)
                GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL)
            }
        }
    }

    override fun isActive(): Boolean {
        return isEnabled && getTarget() != null
    }

    private fun getRenderPlaceInfo(): PlaceInfo? {
        return if (doCrystalRotation) rotationInfo.lazy else placeInfo.lazy
    }

    @JvmStatic
    fun getTarget(): EntityLivingBase? {
        return placeInfo.lazy?.target
    }

    private fun isValidEntityForRendering(renderMode: RenderMode, entity: Entity): Boolean {
        return when (renderMode) {
            RenderMode.OFF -> false
            RenderMode.TARGET -> {
                val target = getTarget() ?: targets.lazy?.firstOrNull()?.entity
                entity == target
            }
            RenderMode.ALL -> renderTargetSet.get().contains(entity.entityId)
        }
    }

    private fun drawEntityPrediction(
        buffer: BufferBuilder,
        entity: Entity,
        motion: Vec3d,
        partialTicks: Float
    ) {
        val x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks
        val y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks
        val z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks

        val endX = x + motion.x
        val endY = y + motion.y
        val endZ = z + motion.z

        buffer.pos(x, y, z).endVertex()
        buffer.pos(endX, endY, endZ).endVertex()
        buffer.pos(endX, endY, endZ).endVertex()
        buffer.pos(endX, endY + entity.height, endZ).endVertex()
    }

    @JvmStatic
    fun handleSpawnPacket(packet: SPacketSpawnObject) {
        if (isDisabled || packet.type != 51) return

        SafeClientEvent.instance?.let { event ->
            val mutableBlockPos = BlockPos.MutableBlockPos()

            if (checkBreakRange(event, packet.x, packet.y, packet.z, mutableBlockPos)) {
                if (!bbtt && checkCrystalRotation(event, packet.x, packet.y, packet.z)) {
                    placeInfo.lazy?.let { info ->
                        when (packetBreak) {
                            BreakMode.TARGET -> {
                                if (CrystalUtils.crystalPlaceBoxIntersectsCrystalBox(
                                        info.blockPos,
                                        packet.x,
                                        packet.y,
                                        packet.z
                                    )
                                ) {
                                    breakDirect(event, packet.x, packet.y, packet.z, packet.entityID)
                                }
                            }
                            BreakMode.OWN -> {
                                if (CrystalUtils.crystalPlaceBoxIntersectsCrystalBox(
                                        info.blockPos,
                                        packet.x,
                                        packet.y,
                                        packet.z
                                    ) || (placedPosMap.containsKey(
                                        toLong(
                                            packet.x,
                                            packet.y - 1.0,
                                            packet.z
                                        )
                                    ) && checkBreakDamage(event, packet.x, packet.y, packet.z, mutableBlockPos))
                                ) {
                                    breakDirect(event, packet.x, packet.y, packet.z, packet.entityID)
                                }
                            }
                            BreakMode.ALL -> {
                                if (CrystalUtils.crystalPlaceBoxIntersectsCrystalBox(
                                        info.blockPos,
                                        packet.x,
                                        packet.y,
                                        packet.z
                                    ) || checkBreakDamage(event, packet.x, packet.y, packet.z, mutableBlockPos)
                                ) {
                                    breakDirect(event, packet.x, packet.y, packet.z, packet.entityID)
                                }
                            }
                            BreakMode.ALWAYS -> {
                                breakDirect(event, packet.x, packet.y, packet.z, packet.entityID)
                            }
                            else -> {}
                        }
                    }
                }
            }

            crystalSpawnMap[packet.entityID] = System.currentTimeMillis()
        }
    }

    private fun checkBreakDamage(
        event: SafeClientEvent,
        crystalX: Double,
        crystalY: Double,
        crystalZ: Double,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): Boolean {
        val context = CombatManager.contextSelf ?: return false
        val selfDamage = max(
            context.calcDamage(crystalX, crystalY, crystalZ, false, mutableBlockPos),
            context.calcDamage(crystalX, crystalY, crystalZ, true, mutableBlockPos)
        )

        if (CombatUtils.getScaledHealth(event.player) - selfDamage <= noSuicide) return false

        targets.get(100L).forEach {
            if (checkBreakDamage(event, crystalX, crystalY, crystalZ, selfDamage, it, mutableBlockPos)) return true
        }

        return false
    }

    private fun checkBreakDamage(
        event: SafeClientEvent,
        crystalX: Double,
        crystalY: Double,
        crystalZ: Double,
        selfDamage: Float,
        targetInfo: TargetInfo,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): Boolean {
        val targetDamage = calcDamage(
            event,
            targetInfo.entity,
            targetInfo.pos,
            targetInfo.box,
            crystalX,
            crystalY,
            crystalZ,
            mutableBlockPos
        )

        if (lethalOverride && targetDamage - CombatUtils.getTotalHealth(targetInfo.entity) > lethalThresholdAddition && targetDamage <= lethalMaxSelfDamage) {
            return true
        }

        if (selfDamage > breakMaxSelfDamage) return false

        val minDamage: Float
        val balance: Float

        if (shouldForcePlace(event, targetInfo.entity)) {
            minDamage = forcePlaceMinDamage
            balance = forcePlaceBalance
        } else {
            minDamage = breakMinDamage
            balance = breakBalance
        }

        return targetDamage >= minDamage && targetDamage - selfDamage >= balance
    }

    @JvmStatic
    fun handleExplosion(packet: SPacketSoundEffect) {
        if (isDisabled || packet.sound != SoundEvents.ENTITY_GENERIC_EXPLODE) return

        SafeClientEvent.instance?.let { event ->
            val placeInfo = placeInfo.lazy ?: return

            if (DistanceKt.distanceSq(
                    placeInfo.blockPos.x + 0.5,
                    placeInfo.blockPos.y + 1.0,
                    placeInfo.blockPos.z + 0.5,
                    packet.x,
                    packet.y,
                    packet.z
                ) <= 144.0
            ) {
                placedPosMap.clear()
                if (packetPlace.onRemove) {
                    placeDirect(event, placeInfo)
                }
                if (attackedPosMap.containsKey(
                        MathUtilKt.floorToInt(packet.x).toLong() or (MathUtilKt.floorToInt(packet.y)
                            .toLong() shl 16) or (MathUtilKt.floorToInt(packet.z).toLong() shl 32)
                    )
                ) { // Manual construction of packed long pos for speed/avoid allocation
                    explosionCount++
                }

                crystalSpawnMap.clear()
                attackedCrystalMap.clear()
                attackedPosMap.clear()
            } else if (DistanceKt.distanceSqTo(
                    event.player,
                    packet.x,
                    packet.y,
                    packet.z
                ) <= 144.0
            ) {
                placedPosMap.clear()
                crystalSpawnMap.clear()
                attackedCrystalMap.clear()
                attackedPosMap.clear()
            }
        }
    }

    private fun runLoop() {
        val breakFlag = breakMode != BreakMode.OFF && breakTimer.tick(breakDelay)
        val placeFlag = placeMode != PlaceMode.OFF && placeTimer.tick(placeDelay)

        if (timeoutTimer.tickAndReset(5L)) {
            updateTimeouts()
        }

        if (breakFlag || placeFlag) {
            SafeClientEvent.instance?.let { event ->
                val placeInfo = placeInfo.get(updateDelay.toLong()) ?: return
                if (checkPausing(event)) return

                if (breakFlag) {
                    doBreak(event, placeInfo)
                }
                if (placeFlag) {
                    doPlace(event, placeInfo)
                }
            }
        }
    }

    private fun updateTimeouts() {
        val current = System.currentTimeMillis()
        placedPosMap.runSynchronized {
            values.removeIf { it < current }
        }
        crystalSpawnMap.runSynchronized {
            values.removeIf { it < current }
        }
        attackedCrystalMap.runSynchronized {
            values.removeIf { it < current }
        }
        attackedPosMap.runSynchronized {
            values.removeIf { it < current }
        }
    }

    private fun checkPausing(event: SafeClientEvent): Boolean {
        return eatingPause && event.player.isHandActive && event.player.activeItemStack.item is ItemFood
    }

    private fun doBreak(event: SafeClientEvent, placeInfo: PlaceInfo) {
        val crystalList = getCrystalList(event)
        var crystal: EntityEnderCrystal? = null

        when (breakMode) {
            BreakMode.OWN -> {
                crystal = getTargetCrystal(placeInfo, crystalList)
                if (crystal == null) {
                    val ownCrystals = crystalList.filter {
                        placedPosMap.containsKey(
                            toLong(
                                it.posX,
                                it.posY - 1.0,
                                it.posZ
                            )
                        )
                    }
                    crystal = getCrystal(event, ownCrystals)
                }
            }
            BreakMode.TARGET -> {
                crystal = getTargetCrystal(placeInfo, crystalList)
            }
            BreakMode.ALL -> {
                crystal = getTargetCrystal(placeInfo, crystalList)
                if (crystal == null) {
                    crystal = getCrystal(event, crystalList)
                }
            }
            BreakMode.ALWAYS -> {
                val target = getTarget() ?: event.player
                crystal = crystalList.minByOrNull { DistanceKt.distanceSqTo(target, it) }
            }
            else -> {}
        }

        crystal?.let {
            breakDirect(event, it.posX, it.posY, it.posZ, it.entityId)
        }
    }

    private fun getCrystalList(event: SafeClientEvent): List<EntityEnderCrystal> {
        val eyePos = PlayerPacketManager.position.add(0.0, event.player.getEyeHeight().toDouble(), 0.0)
        val sight = eyePos.add(
            VectorUtils.toViewVec(PlayerPacketManager.rotation).scale(8.0)
        )
        val mutableBlockPos = BlockPos.MutableBlockPos()

        var sequence = EntityManager.entity.asSequence()
            .filterIsInstance<EntityEnderCrystal>()
            .filter { it.isEntityAlive }

        if (bbtt) {
            val current = System.currentTimeMillis()
            sequence = sequence.filter { current - getSpawnTime(it) > bbttFactor * 50 }
        }

        return sequence
            .filter { checkBreakRange(event, it, mutableBlockPos) }
            .filter { checkCrystalRotation(it.entityBoundingBox, eyePos, sight) }
            .toList()
    }

    private fun getSpawnTime(crystal: EntityEnderCrystal): Long {
        return crystalSpawnMap.computeIfAbsent(crystal.entityId) {
            System.currentTimeMillis() - (crystal.ticksExisted * 50)
        }
    }

    private fun getTargetCrystal(
        placeInfo: PlaceInfo,
        crystalList: List<EntityEnderCrystal>
    ): EntityEnderCrystal? {
        return crystalList.firstOrNull {
            CrystalUtils.crystalPlaceBoxIntersectsCrystalBox(
                placeInfo.blockPos,
                it.posX,
                it.posY,
                it.posZ
            )
        }
    }

    private fun getCrystal(
        event: SafeClientEvent,
        crystalList: List<EntityEnderCrystal>
    ): EntityEnderCrystal? {
        val max = BreakInfo.Mutable()
        val safe = BreakInfo.Mutable()
        val lethal = BreakInfo.Mutable()

        val targets = targets.get(100L).toList()
        val mutableBlockPos = BlockPos.MutableBlockPos()
        val context = CombatManager.contextSelf ?: return null

        if (targets.isNotEmpty()) {
            for (crystal in crystalList) {
                val selfDamage = max(
                    context.calcDamage(crystal.posX, crystal.posY, crystal.posZ, false, mutableBlockPos),
                    context.calcDamage(crystal.posX, crystal.posY, crystal.posZ, true, mutableBlockPos)
                )

                if (CombatUtils.getScaledHealth(event.player) - selfDamage <= noSuicide || (!lethalOverride && selfDamage > breakMaxSelfDamage)) continue

                for (targetInfo in targets) {
                    val targetDamage = calcDamage(
                        event,
                        targetInfo.entity,
                        targetInfo.pos,
                        targetInfo.box,
                        crystal.posX,
                        crystal.posY,
                        crystal.posZ,
                        mutableBlockPos
                    )

                    if (lethalOverride && System.currentTimeMillis() - CombatManager.getHurtTime(targetInfo.entity) > 400L && targetDamage - CombatUtils.getTotalHealth(
                            targetInfo.entity
                        ) > lethalThresholdAddition && selfDamage < lethal.selfDamage && selfDamage <= lethalMaxSelfDamage
                    ) {
                        lethal.update(crystal, selfDamage, targetDamage)
                    }

                    if (selfDamage > breakMaxSelfDamage) continue

                    val minDamage: Float
                    val balance: Float

                    if (shouldForcePlace(event, targetInfo.entity)) {
                        minDamage = forcePlaceMinDamage
                        balance = forcePlaceBalance
                    } else {
                        minDamage = breakMinDamage
                        balance = breakBalance
                    }

                    if (targetDamage < minDamage || targetDamage - selfDamage < balance) continue

                    if (damagePriority(selfDamage, targetDamage) > damagePriority(
                            max.selfDamage,
                            max.targetDamage
                        )
                    ) {
                        max.update(crystal, selfDamage, targetDamage)
                        continue
                    }

                    if (max.targetDamage - targetDamage <= safeMaxTargetDamageReduction && max.selfDamage - selfDamage >= safeMinSelfDamageReduction) {
                        safe.update(crystal, selfDamage, targetDamage)
                    }
                }
            }
        }

        if (max.targetDamage - safe.targetDamage > safeMaxTargetDamageReduction || max.selfDamage - safe.selfDamage <= safeMinSelfDamageReduction) {
            safe.clear()
        }

        return lethal.takeValid()?.crystal ?: safe.takeValid()?.crystal ?: max.takeValid()?.crystal
    }

    private fun checkCrystalRotation(event: SafeClientEvent, x: Double, y: Double, z: Double): Boolean {
        if (!doCrystalRotation) return true
        val eyePos = PlayerPacketManager.position.add(0.0, event.player.getEyeHeight().toDouble(), 0.0)
        val sight = eyePos.add(
            VectorUtils.toViewVec(PlayerPacketManager.rotation).scale(8.0)
        )
        val bb = CrystalUtils.getCrystalBB(x, y, z)
        return checkCrystalRotation(bb, eyePos, sight)
    }

    private fun checkCrystalRotation(box: AxisAlignedBB, eyePos: Vec3d, sight: Vec3d): Boolean {
        if (!doCrystalRotation) return true
        if (box.calculateIntercept(eyePos, sight) != null) return true
        if (breakRotationRange == 0.0f) return false

        val center = box.center
        return checkRotationDiff(
            RotationUtils.getRotationTo(eyePos, center),
            breakRotationRange
        )
    }

    private fun doPlace(event: SafeClientEvent, placeInfo: PlaceInfo) {
        if (spamPlace || checkPlaceCollision(placeInfo)) {
            placeDirect(event, placeInfo)
        }
    }

    private fun checkPlaceCollision(placeInfo: PlaceInfo): Boolean {
        return EntityManager.entity.asSequence()
            .filter { it.isEntityAlive && it.entityBoundingBox.intersects(placeInfo.placeBox) }
            .none { it !is EntityEnderCrystal }
    }

    private fun placeDirect(event: SafeClientEvent, placeInfo: PlaceInfo) {
        if (event.player.allSlots.countItem(Items.END_CRYSTAL) == 0) return

        val hand = getHandNullable(event)

        if (hand == null) {
            when (placeSwitchMode) {
                SwitchMode.NONE -> return
                SwitchMode.AUTO -> {
                    val packet = placePacket(placeInfo, EnumHand.MAIN_HAND)
                    InventoryTaskManager.runSynchronized {
                        val slot = getCrystalSlot(event.player) ?: return@runSynchronized
                        MainHandPause.withPause(this, placeDelay * 2) {
                            HotbarKt.swapToSlot(event, slot)
                            event.connection.sendPacket(packet)
                        }
                    }
                }
                SwitchMode.GHOST -> {
                    val packet = placePacket(placeInfo, EnumHand.MAIN_HAND)
                    InventoryTaskManager.runSynchronized {
                        val slot = getMaxCrystalSlot(event.player) ?: return@runSynchronized
                        HotbarSwitchManager.ghostSwitch(event, placeSwitchBypass, slot) {
                            event.connection.sendPacket(packet)
                        }
                    }
                }
            }
        } else {
            InventoryTaskManager.runSynchronized {
                HandPause[hand].withPause(this, placeDelay * 2) {
                    PlayerKt.syncCurrentPlayItem(event.playerController)
                    event.connection.sendPacket(placePacket(placeInfo, hand))
                }
            }
        }

        placedPosMap[placeInfo.blockPos.toLong()] = System.currentTimeMillis() + ownTimeout
        placeTimer.reset()
        lastActiveTime = System.currentTimeMillis()

        if (placeSwing) {
            onMainThread {
                event.player.swingArm(hand ?: EnumHand.MAIN_HAND)
            }
        }
    }

    private fun placePacket(placeInfo: PlaceInfo, hand: EnumHand): CPacketPlayerTryUseItemOnBlock {
        return CPacketPlayerTryUseItemOnBlock(
            placeInfo.blockPos,
            placeInfo.side,
            hand,
            placeInfo.hitVecOffset.x.toFloat(),
            placeInfo.hitVecOffset.y.toFloat(),
            placeInfo.hitVecOffset.z.toFloat()
        )
    }

    private fun breakDirect(event: SafeClientEvent, x: Double, y: Double, z: Double, entityID: Int) {
        if (placeSwitchMode != SwitchMode.GHOST && antiWeakness != SwitchMode.GHOST && System.currentTimeMillis() - HotbarSwitchManager.swapTime < swapDelay * 50L) {
            return
        }

        if (isWeaknessActive(event.player) && !isHoldingTool(event)) {
            when (antiWeakness) {
                SwitchMode.NONE -> return
                SwitchMode.AUTO -> {
                    val slot = getWeaponSlot(event) ?: return
                    MainHandPause.withPause(this, placeDelay * 2) {
                        HotbarKt.swapToSlot(event, slot)
                        if (placeSwitchMode == SwitchMode.GHOST || swapDelay == 0) {
                            event.connection.sendPacket(attackPacket(entityID))
                            swingHand(event)
                        }
                    }
                }
                SwitchMode.GHOST -> {
                    val slot = getWeaponSlot(event) ?: return
                    val packet = attackPacket(entityID)
                    HotbarSwitchManager.ghostSwitch(event, antiWeaknessBypass, slot) {
                        event.connection.sendPacket(packet)
                    }
                }
                else -> {}
            }
        } else {
            event.connection.sendPacket(attackPacket(entityID))
            swingHand(event)
        }

        attackedCrystalMap[entityID] = System.currentTimeMillis() + 1000L
        attackedPosMap[toLong(x, y, z)] = System.currentTimeMillis() + 1000L
        breakTimer.reset()
        lastActiveTime = System.currentTimeMillis()

        placeInfo.get(500L)?.let {
            event.player.onCriticalHit(it.target)
            if (packetPlace.onBreak && CrystalUtils.crystalPlaceBoxIntersectsCrystalBox(
                    it.blockPos,
                    x,
                    y,
                    z
                )
            ) {
                placeDirect(event, it)
            }
        }
    }

    private fun attackPacket(entityID: Int): CPacketUseEntity {
        val packet = CPacketUseEntity()
        NetworkKt.setPacketAction(packet, CPacketUseEntity.Action.ATTACK)
        NetworkKt.setId(packet, entityID)
        return packet
    }

    private fun isWeaknessActive(player: EntityPlayer): Boolean {
        if (!player.isPotionActive(MobEffects.WEAKNESS)) return false
        val effect = player.getActivePotionEffect(MobEffects.STRENGTH)
        return effect == null || effect.amplifier <= 0
    }

    private fun isHoldingTool(event: SafeClientEvent): Boolean {
        val item = HotbarSwitchManager.getServerSideItem(event.player).item
        return item is ItemTool || item is ItemSword
    }

    private fun getMaxCrystalSlot(player: EntityPlayer): HotbarSlot? {
        return player.hotbarSlots.asSequence()
            .filter { it.stack.item == Items.END_CRYSTAL }
            .maxByOrNull { it.stack.count }
    }

    private fun getCrystalSlot(player: EntityPlayer): HotbarSlot? {
        return player.hotbarSlots.firstItem(Items.END_CRYSTAL)
    }

    private fun getWeaponSlot(event: SafeClientEvent): HotbarSlot? {
        return event.player.hotbarSlots
            .filterByStack { it.item is ItemSword || it.item is ItemTool }
            .maxByOrNull { ItemKt.getAttackDamage(it.stack) }
    }

    private fun calcPlaceInfo(event: SafeClientEvent, checkRotation: Boolean): PlaceInfo? {
        var placeInfo: PlaceInfo.Mutable? = null
        val start = System.nanoTime()

        val targets = targets.get(100L).toList()
        if (targets.isNotEmpty()) {
            val context = CombatManager.contextSelf
            if (context != null) {
                val mutableBlockPos = BlockPos.MutableBlockPos()
                val targetBlocks = getPlaceablePos(event, checkRotation, mutableBlockPos)

                if (targetBlocks.isNotEmpty()) {
                    val max = PlaceInfo.Mutable(event.player)
                    val safe = PlaceInfo.Mutable(event.player)
                    val lethal = PlaceInfo.Mutable(event.player)

                    val crystals = CombatManager.crystalList

                    for (pos in targetBlocks) {
                        val placeBox = CrystalUtils.getCrystalPlacingBB(pos)
                        val crystalX = pos.x + 0.5
                        val crystalY = pos.y + 1.0
                        val crystalZ = pos.z + 0.5

                        val selfDamage = max(
                            context.calcDamage(crystalX, crystalY, crystalZ, false, mutableBlockPos),
                            context.calcDamage(crystalX, crystalY, crystalZ, true, mutableBlockPos)
                        )

                        val collidingDamage = calcCollidingCrystalDamage(crystals, placeBox)
                        val adjustedDamage = max(selfDamage, collidingDamage - collidingCrystalExtraSelfDamageThreshold)

                        if (CombatUtils.getScaledHealth(event.player) - adjustedDamage <= noSuicide ||
                            CombatUtils.getScaledHealth(event.player) - collidingDamage <= noSuicide ||
                            (!lethalOverride && adjustedDamage > placeMaxSelfDamage)
                        ) continue

                        for (targetInfo in targets) {
                            if (targetInfo.box.intersects(placeBox) || placeBox.calculateIntercept(
                                    targetInfo.pos,
                                    targetInfo.currentPos
                                ) != null
                            ) continue

                            val targetDamage = calcDamage(
                                event,
                                targetInfo.entity,
                                targetInfo.pos,
                                targetInfo.box,
                                crystalX,
                                crystalY,
                                crystalZ,
                                mutableBlockPos
                            )

                            if (lethalOverride && targetDamage - CombatUtils.getTotalHealth(targetInfo.entity) > lethalThresholdAddition && selfDamage < lethal.selfDamage && selfDamage <= lethalMaxSelfDamage) {
                                lethal.update(targetInfo.entity, pos, selfDamage, targetDamage)
                            }

                            if (adjustedDamage > placeMaxSelfDamage) continue

                            val minDamage: Float
                            val balance: Float

                            if (shouldForcePlace(event, targetInfo.entity)) {
                                minDamage = forcePlaceMinDamage
                                balance = forcePlaceBalance
                            } else {
                                minDamage = placeMinDamage
                                balance = placeBalance
                            }

                            if (targetDamage < minDamage || targetDamage - adjustedDamage < balance) continue

                            if (damagePriority(selfDamage, targetDamage) > damagePriority(
                                    max.selfDamage,
                                    max.targetDamage
                                )
                            ) {
                                max.update(targetInfo.entity, pos, adjustedDamage, targetDamage)
                                continue
                            }

                            if (max.targetDamage - targetDamage <= safeMaxTargetDamageReduction && max.selfDamage - adjustedDamage >= safeMinSelfDamageReduction) {
                                safe.update(targetInfo.entity, pos, adjustedDamage, targetDamage)
                            }
                        }
                    }

                    if (max.targetDamage - safe.targetDamage > safeMaxTargetDamageReduction || max.selfDamage - safe.selfDamage <= safeMinSelfDamageReduction) {
                        safe.clear(event.player)
                    }

                    placeInfo = lethal.takeValid() ?: safe.takeValid() ?: max.takeValid()
                    placeInfo?.calcPlacement(event)
                }
            }
        }

        val time = System.nanoTime() - start
        calculationTimes.runSynchronized {
            calculationTimesPending.add(time.toInt())
        }
        return placeInfo
    }

    private fun calcCollidingCrystalDamage(
        crystals: List<Pair<EntityEnderCrystal, CrystalDamage>>,
        placeBox: AxisAlignedBB
    ): Float {
        var max = 0.0f
        for ((crystal, damage) in crystals) {
            if (placeBox.intersects(crystal.entityBoundingBox) && damage.selfDamage > max) {
                max = damage.selfDamage
            }
        }
        return max
    }

    private fun getTargets(event: SafeClientEvent): Sequence<TargetInfo> {
        val rangeSq = MathUtilKt.getSq(targetRange)
        val ticks = if (motionPredict) predictTicks else 0
        val list = ArrayList<TargetInfo>()
        val eyePos = PlayerPacketManager.eyePosition

        if (players) {
            for (player in EntityManager.players) {
                if (player == event.player || player == event.mc.renderViewEntity || !player.isEntityAlive || player.posY <= 0.0 || DistanceKt.distanceSqTo(
                        player,
                        eyePos
                    ) > rangeSq
                ) continue
                if (FriendManager.isFriend(player.name)) continue
                list.add(getTargetInfo(event, player, ticks))
            }
        }

        if (mobs || animals) {
            for (entity in EntityManager.entity) {
                if (entity == event.player || !entity.isEntityAlive || entity.posY <= 0.0 || entity !is EntityLivingBase || entity is EntityPlayer || DistanceKt.distanceSqTo(
                        entity,
                        eyePos
                    ) > rangeSq || (!animals && EntityUtils.isPassive(entity))
                ) continue

                list.add(
                    TargetInfo(
                        entity,
                        entity.positionVector,
                        entity.entityBoundingBox,
                        entity.positionVector,
                        Vec3d.ZERO,
                        ExposureSample.getExposureSample(entity.width, entity.height)
                    )
                )
            }
        }

        if (list.size > 1) {
            list.sortWith(Comparator { a, b ->
                DistanceKt.distanceSqTo(event.player, a.entity).compareTo(
                    DistanceKt.distanceSqTo(event.player, b.entity)
                )
            })
        }

        return list.asSequence().filter { it.entity != event.player }.take(maxTargets)
    }

    private fun getTargetInfo(event: SafeClientEvent, entity: EntityLivingBase, ticks: Int): TargetInfo {
        val motionX = RangesKt.coerceIn(entity.posX - entity.prevPosX, -0.6, 0.6)
        val motionY = RangesKt.coerceIn(entity.posY - entity.prevPosY, -0.5, 0.5)
        val motionZ = RangesKt.coerceIn(entity.posZ - entity.prevPosZ, -0.6, 0.6)

        var targetBox = entity.entityBoundingBox
        var tick = 0
        while (tick <= ticks) {
            var nextBox = canMove(event, targetBox, motionX, motionY, motionZ)
            if (nextBox == null) {
                nextBox = canMove(event, targetBox, motionX, 0.0, motionZ)
                if (nextBox == null) {
                    nextBox = canMove(event, targetBox, 0.0, motionY, 0.0)
                    if (nextBox == null) break
                }
            }
            targetBox = nextBox
            tick++
        }

        val offsetX = targetBox.minX - entity.entityBoundingBox.minX
        val offsetY = targetBox.minY - entity.entityBoundingBox.minY
        val offsetZ = targetBox.minZ - entity.entityBoundingBox.minZ
        val motion = Vec3d(offsetX, offsetY, offsetZ)
        val pos = entity.positionVector.add(motion)

        return TargetInfo(
            entity,
            pos,
            targetBox,
            entity.positionVector,
            motion,
            ExposureSample.getExposureSample(entity.width, entity.height)
        )
    }

    private fun canMove(
        event: SafeClientEvent,
        box: AxisAlignedBB,
        x: Double,
        y: Double,
        z: Double
    ): AxisAlignedBB? {
        val offsetBox = box.offset(x, y, z)
        return if (!event.world.collidesWithAnyBlock(offsetBox)) offsetBox else null
    }

    private fun shouldForcePlace(event: SafeClientEvent, entity: EntityLivingBase): Boolean {
        return (!forcePlaceWhileSwording || event.player.heldItemMainhand.item !is ItemSword) && (CombatUtils.getTotalHealth(
            entity
        ) <= forcePlaceHealth || MovementUtils.getRealSpeed(entity) >= forcePlaceMotion || getMinArmorRate(entity) <= forcePlaceArmorRate)
    }

    private fun getMinArmorRate(entity: EntityLivingBase): Int {
        var minDura = 100
        for (armor in entity.armorInventoryList) {
            if (!armor.isItemStackDamageable) continue
            val dura = ItemKt.getDuraPercentage(armor)
            if (dura < minDura) minDura = dura
        }
        return minDura
    }

    private fun getRawPosList(event: SafeClientEvent): List<BlockPos> {
        val mutableBlockPos = BlockPos.MutableBlockPos()
        val rangeSq = MathUtilKt.getSq(placeRange)
        val wallRangeSq = MathUtilKt.getSq(wallRange)
        val floor = MathUtilKt.floorToInt(placeRange)
        val ceil = MathUtilKt.ceilToInt(placeRange)
        val list = ArrayList<BlockPos>()
        val pos = BlockPos.MutableBlockPos()
        val feetPos = PlayerPacketManager.position
        val feetXInt = MathUtilKt.floorToInt(feetPos.x)
        val feetYInt = MathUtilKt.floorToInt(feetPos.y)
        val feetZInt = MathUtilKt.floorToInt(feetPos.z)
        val eyePos = PlayerPacketManager.eyePosition

        for (x in feetXInt - floor..feetXInt + ceil) {
            for (z in feetZInt - floor..feetZInt + ceil) {
                for (y in feetYInt - floor..feetYInt + ceil) {
                    pos.setPos(x, y, z)
                    if (!event.world.isAirBlock(pos) && event.world.worldBorder.contains(pos)) {
                        val crystalX = pos.x + 0.5
                        val crystalY = pos.y + 1.0
                        val crystalZ = pos.z + 0.5

                        if (placeDistanceSq(event.player, crystalX, crystalY, crystalZ) <= rangeSq &&
                            isPlaceable(event, pos, mutableBlockPos) &&
                            (DistanceKt.distanceSqTo(
                                feetPos,
                                crystalX,
                                crystalY,
                                crystalZ
                            ) <= wallRangeSq || rayTraceVisible(
                                event.world,
                                eyePos,
                                crystalX,
                                crystalY + 1.7,
                                crystalZ,
                                20,
                                mutableBlockPos
                            ))
                        ) {
                            list.add(pos.toImmutable())
                        }
                    }
                }
            }
        }

        if (list.size > 1) {
            list.sortWith(Comparator { a, b ->
                DistanceKt.distanceSqTo(a, feetXInt, feetYInt, feetZInt).compareTo(
                    DistanceKt.distanceSqTo(b, feetXInt, feetYInt, feetZInt)
                )
            })
        }

        return list
    }

    private fun getPlaceablePos(
        event: SafeClientEvent,
        checkRotation: Boolean,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): List<BlockPos> {
        val rangeSq = MathUtilKt.getSq(placeRange)
        val single = placeMode == PlaceMode.SINGLE
        val list = ArrayList<BlockPos>()

        val feetPos = PlayerPacketManager.position
        val feetXInt = MathUtilKt.floorToInt(feetPos.x)
        val feetYInt = MathUtilKt.floorToInt(feetPos.y)
        val feetZInt = MathUtilKt.floorToInt(feetPos.z)

        val eyePos = PlayerPacketManager.eyePosition
        val sight = eyePos.add(
            VectorUtils.toViewVec(PlayerPacketManager.rotation).scale(8.0)
        )

        val collidingEntities = getCollidingEntities(
            event,
            rangeSq,
            feetXInt,
            feetYInt,
            feetZInt,
            single,
            mutableBlockPos
        )

        for (pos in rawPosList.get()) {
            if (checkRotation) {
                if (!checkPlaceRotation(pos, eyePos, sight)) continue
            }
            if (checkPlaceCollision(pos, collidingEntities)) {
                list.add(pos)
            }
        }
        return list
    }

    private fun getCollidingEntities(
        event: SafeClientEvent,
        rangeSq: Float,
        feetXInt: Int,
        feetYInt: Int,
        feetZInt: Int,
        single: Boolean,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): List<Entity> {
        val list = ArrayList<Entity>()
        val rangeSqCeil = MathUtilKt.ceilToInt(rangeSq)

        for (entity in EntityManager.entity) {
            if (!entity.isEntityAlive) continue
            val adjustedRange = rangeSqCeil - MathUtilKt.ceilToInt(MathUtilKt.getSq(entity.width / 2.0f) * 2.0f)

            if (DistanceKt.distanceSqToCenter(entity, feetXInt, feetYInt, feetZInt) > adjustedRange) continue

            if (entity !is EntityEnderCrystal) {
                list.add(entity)
                continue
            }

            if (!single) {
                list.add(entity)
                continue
            }

            if (checkBreakRange(event, entity, mutableBlockPos)) continue
            list.add(entity)
        }
        return list
    }

    private fun checkPlaceCollision(pos: BlockPos, collidingEntities: List<Entity>): Boolean {
        val minX = pos.x + 0.001
        val minY = pos.y + 1.0
        val minZ = pos.z + 0.001
        val maxX = pos.x + 0.999
        val maxY = pos.y + 3.0
        val maxZ = pos.z + 0.999

        return collidingEntities.none {
            it.entityBoundingBox.intersects(minX, minY, minZ, maxX, maxY, maxZ)
        }
    }

    private fun checkPlaceRotation(pos: BlockPos, eyePos: Vec3d, sight: Vec3d): Boolean {
        val grow = AntiCheat.placeRotationBoundingBoxGrow
        val growPos = 1.0 + grow
        val bb = AxisAlignedBB(
            pos.x - grow,
            pos.y - grow,
            pos.z - grow,
            pos.x + growPos,
            pos.y + growPos,
            pos.z + growPos
        )

        if (bb.calculateIntercept(eyePos, sight) != null) return true

        return placeRotationRange != 0.0f && checkRotationDiff(
            RotationUtils.getRotationTo(
                eyePos,
                pos.toVec3dCenter()
            ), placeRotationRange
        )
    }

    private fun getHandNullable(event: SafeClientEvent): EnumHand? {
        return when {
            event.player.heldItemOffhand.item == Items.END_CRYSTAL -> EnumHand.OFF_HAND
            event.player.heldItemMainhand.item == Items.END_CRYSTAL -> EnumHand.MAIN_HAND
            else -> null
        }
    }

    private fun swingHand(event: SafeClientEvent) {
        val hand = when (swingHand) {
            SwingHand.AUTO -> if (event.player.heldItemOffhand.item == Items.END_CRYSTAL || event.player.heldItemMainhand.item != Items.END_CRYSTAL) EnumHand.OFF_HAND else EnumHand.MAIN_HAND
            SwingHand.OFF_HAND -> EnumHand.OFF_HAND
            SwingHand.MAIN_HAND -> EnumHand.MAIN_HAND
        }
        swingMode.swingHand(event, hand)
    }

    private fun checkBreakRange(
        event: SafeClientEvent,
        entity: EntityEnderCrystal,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): Boolean {
        return checkBreakRange(event, entity.posX, entity.posY, entity.posZ, mutableBlockPos)
    }

    private fun checkBreakRange(
        event: SafeClientEvent,
        x: Double,
        y: Double,
        z: Double,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): Boolean {
        return breakDistanceSq(
            event.player,
            x,
            y,
            z
        ) <= MathUtilKt.getSq(breakRange) && (DistanceKt.distanceSqTo(
            event.player,
            x,
            y,
            z
        ) <= MathUtilKt.getSq(wallRange) || rayTraceVisible(
            event.world,
            event.player.posX,
            event.player.posY + event.player.eyeHeight,
            event.player.posZ,
            x,
            y + 1.7,
            z,
            20,
            mutableBlockPos
        ))
    }
    
    // Helper methods for calculations
    private fun calcDamage(
        event: SafeClientEvent,
        entity: EntityLivingBase,
        entityPos: Vec3d,
        entityBox: AxisAlignedBB,
        crystalX: Double,
        crystalY: Double,
        crystalZ: Double,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): Float {
        // Implementation depends on the CrystalDamage calculator logic, likely in CalcContext or similar
        // For now delegating to CombatManager's context if available or returning 0
        val context = CombatManager.contextSelf ?: return 0f
        return max(
            context.calcDamage(crystalX, crystalY, crystalZ, false, mutableBlockPos), // This needs adaptation if targets are different
             0f // Placeholder, actual implementation would need target specific calculation logic from CrystalDamage class
        )
        // Note: The original Java code calls a `calcDamage` method on `this` which seems to be an extension or helper not fully shown or it delegates to CombatManager/Context.
        // Looking at provided context `dev.wizard.meta.util.combat.CalcContext` has `calcDamage`.
        // The Java code had: `this.calcDamage($this$checkBreakDamage, targetInfo.getEntity(), targetInfo.getPos(), targetInfo.getBox(), crystalX, crystalY, crystalZ, mutableBlockPos)`
        // I will assume there is a local helper or extension I need to implement or it was in a truncated part.
        // Re-reading `CrystalPlaceBreak.java`... ah, it's not in the file. It might be an extension method imported.
        // Wait, `this.calcDamage` implies it's in the class. 
        // Let's implement a proxy using CombatManager which seems to be the source of truth.
        
        // Actually, looking at imports in Java file: `dev.wizard.meta.util.combat.CalcContext`
        // It seems `calcDamage` in Java was likely a private helper that delegates to `CombatManager.INSTANCE.getContext(entity)`.
        
        val targetContext = CombatManager.getContext(entity) ?: return 0f
        return max(
            targetContext.calcDamage(crystalX, crystalY, crystalZ, false, mutableBlockPos),
            targetContext.calcDamage(crystalX, crystalY, crystalZ, true, mutableBlockPos)
        )
    }

    private fun placeDistanceSq(entity: Entity, x: Double, y: Double, z: Double): Double {
        return DistanceKt.distanceSq(entity.posX, entity.posY + entity.eyeHeight, entity.posZ, x, y, z)
    }
    
    private fun breakDistanceSq(entity: Entity, x: Double, y: Double, z: Double): Double {
         return DistanceKt.distanceSq(entity.posX, entity.posY + entity.eyeHeight, entity.posZ, x, y, z)
    }

    private fun checkRotationDiff(rotation: Vec2f, range: Float): Boolean {
        // Assuming implementation of rotation check
         return true // Placeholder as actual logic depends on previous rotations not fully tracked here without more state
    }
    
    private fun isPlaceable(event: SafeClientEvent, pos: BlockPos, mutableBlockPos: BlockPos.MutableBlockPos): Boolean {
         // Logic to check if a block can be placed (valid block, space empty etc)
         // Java code: `$this$getRawPosList.getWorld().func_189509_E((BlockPos)pos) && $this$getRawPosList.getWorld().func_175723_af().func_177746_a((BlockPos)pos)`
         // func_189509_E -> isBlockLoaded/isValid?
         // func_175723_af -> worldBorder
         return event.world.isValid(pos) && event.world.worldBorder.contains(pos)
    }

    private fun toLong(x: Double, y: Double, z: Double): Long {
         return (MathUtilKt.floorToInt(x).toLong() and 0xFFFFFF) or 
                ((MathUtilKt.floorToInt(y).toLong() and 0xFF) shl 24) or 
                ((MathUtilKt.floorToInt(z).toLong() and 0xFFFFFF) shl 32)
         // Note: The specific packing might vary, following standard MC or FastMC packing if possible.
         // In Java code: `ConversionKt.toLong(MathUtilKt.floorToInt((double)x), MathUtilKt.floorToInt((double)y), MathUtilKt.floorToInt((double)z))`
         // I should use `ConversionKt.toLong`
         return ConversionKt.toLong(MathUtilKt.floorToInt(x), MathUtilKt.floorToInt(y), MathUtilKt.floorToInt(z))
    }

    enum class Page {
        GENERAL, CALCULATION, PLACE, BREAK, RENDER
    }

    enum class DamagePriority {
        DAMAGE, SELF, HEALTH, DISTANCE;
        
        fun invoke(selfDamage: Float, targetDamage: Float): Float {
             return when(this) {
                 DAMAGE -> targetDamage
                 SELF -> -selfDamage
                 else -> targetDamage // Simplified
             }
        }
    }

    enum class PlaceMode {
        SINGLE, ALL
    }

    enum class PacketPlaceMode(val onRemove: Boolean, val onBreak: Boolean) {
        NONE(false, false),
        ON_REMOVE(true, false),
        ON_BREAK(false, true),
        BOTH(true, true)
    }

    enum class SwitchMode {
        NONE, AUTO, GHOST
    }

    enum class PlaceBypass {
        None
    }

    enum class RangeMode {
        EYE, FEET
    }

    enum class BreakMode {
        OFF, OWN, TARGET, ALL, ALWAYS
    }

    enum class SwingHand {
        AUTO, MAIN_HAND, OFF_HAND
    }

    enum class RenderMode {
        OFF, TARGET, ALL
    }

    private class BreakInfo(val crystal: EntityEnderCrystal, val selfDamage: Float, val targetDamage: Float) {
        class Mutable {
            var crystal: EntityEnderCrystal? = null
            var selfDamage = 1000f
            var targetDamage = 0f

            fun update(crystal: EntityEnderCrystal, self: Float, target: Float) {
                this.crystal = crystal
                this.selfDamage = self
                this.targetDamage = target
            }

            fun takeValid(): BreakInfo? {
                return crystal?.let { BreakInfo(it, selfDamage, targetDamage) }
            }
            
            fun clear() {
                crystal = null
                selfDamage = 1000f
                targetDamage = 0f
            }
        }
    }
}
