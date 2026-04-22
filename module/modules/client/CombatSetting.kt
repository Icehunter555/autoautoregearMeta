package dev.wizard.meta.module.modules.client

import dev.fastmc.common.MathUtilKt
import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.ConnectionEvent
import dev.wizard.meta.event.events.RunGameLoopEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.render.Render2DEvent
import dev.wizard.meta.event.events.render.RenderEntityEvent
import dev.wizard.meta.graphics.GlStateUtils
import dev.wizard.meta.graphics.ProjectionUtils
import dev.wizard.meta.graphics.RenderUtils2D
import dev.wizard.meta.graphics.RenderUtils3D
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.color.setGLColor
import dev.wizard.meta.manager.managers.CombatManager
import dev.wizard.meta.manager.managers.EntityManager
import dev.wizard.meta.manager.managers.FriendManager
import dev.wizard.meta.module.AbstractModule
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.combat.BedAura
import dev.wizard.meta.module.modules.combat.KillAura
import dev.wizard.meta.module.modules.combat.Surround
import dev.wizard.meta.module.modules.player.AutoEat
import dev.wizard.meta.process.PauseProcess
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.InfoCalculator
import dev.wizard.meta.util.LambdaUtilsKt
import dev.wizard.meta.util.combat.CrystalUtils
import dev.wizard.meta.util.combat.ExposureSample
import dev.wizard.meta.util.combat.MotionTracker
import dev.wizard.meta.util.combat.SurroundUtils
import dev.wizard.meta.util.math.RotationUtils
import dev.wizard.meta.util.math.vector.DistanceKt
import dev.wizard.meta.util.math.vector.toVec2f
import dev.wizard.meta.util.threads.CoroutineUtilsKt
import dev.wizard.meta.util.threads.DefaultScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemFood
import net.minecraft.item.ItemPickaxe
import net.minecraft.util.EnumHand
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11

object CombatSetting : Module(
    "CombatSettings",
    category = Category.CLIENT,
    description = "Settings for combat module targeting",
    alwaysEnabled = true
) {
    private val page = setting(this, EnumSetting(settingName("Page"), Page.TARGET_TYPE))

    val players = setting(this, BooleanSetting(settingName("Players"), true, { page.value == Page.TARGET_TYPE }))
    private val friends by setting(this, BooleanSetting(settingName("Friends"), false, LambdaUtilsKt.and({ page.value == Page.TARGET_TYPE }, { players.value })))
    private val teammate by setting(this, BooleanSetting(settingName("Teammate"), true, LambdaUtilsKt.and({ page.value == Page.TARGET_TYPE }, { players.value })))
    val mobs = setting(this, BooleanSetting(settingName("Mobs"), true, { page.value == Page.TARGET_TYPE }))
    private val passive by setting(this, BooleanSetting(settingName("Passive"), false, LambdaUtilsKt.and({ page.value == Page.TARGET_TYPE }, { mobs.value })))
    private val neutral by setting(this, BooleanSetting(settingName("Neutral"), false, LambdaUtilsKt.and({ page.value == Page.TARGET_TYPE }, { mobs.value })))
    private val hostile by setting(this, BooleanSetting(settingName("Hostile"), false, LambdaUtilsKt.and({ page.value == Page.TARGET_TYPE }, { mobs.value })))
    private val tamed by setting(this, BooleanSetting(settingName("Tamed"), false, LambdaUtilsKt.and({ page.value == Page.TARGET_TYPE }, { mobs.value })))
    private val invisible by setting(this, BooleanSetting(settingName("Invisible"), true, { page.value == Page.TARGET_TYPE }))
    private val ignoreWalls by setting(this, BooleanSetting(settingName("Ignore Walls"), false, { page.value == Page.TARGET_TYPE }))
    val targetRange by setting(this, FloatSetting(settingName("Target Range"), 16.0f, 4.0f..64.0f, 2.0f, { page.value == Page.TARGET_TYPE }))
    val wallRange by setting(this, FloatSetting(settingName("Wall Range"), 3.0f, 0.0f..6.0f, 0.1f, { page.value == Page.TARGET_TYPE }))

    val distancePriority by setting(this, FloatSetting(settingName("Distance Priority"), 0.5f, 0.0f..1.0f, 0.01f, { page.value == Page.TARGET_PRIORITY }))
    var healthPriority by setting(this, FloatSetting(settingName("Health Priority"), 0.5f, 0.0f..1.0f, 0.01f, { page.value == Page.TARGET_PRIORITY }))
    val armorPriority by setting(this, FloatSetting(settingName("Armor Priority"), 0.5f, 0.0f..1.0f, 0.01f, { page.value == Page.TARGET_PRIORITY }))
    val holePriority by setting(this, FloatSetting(settingName("Hole Priority"), 0.5f, 0.0f..1.0f, 0.01f, { page.value == Page.TARGET_PRIORITY }))
    var crosshairPriority by setting(this, FloatSetting(settingName("Crosshair Priority"), 0.5f, 0.0f..1.0f, 0.01f, { page.value == Page.TARGET_PRIORITY }))

    val pauseForDigging = setting(this, BooleanSetting(settingName("Pause For Digging"), false, { page.value == Page.IN_COMBAT }))
    val pauseForEating = setting(this, BooleanSetting(settingName("Pause For Eating"), false, { page.value == Page.IN_COMBAT }))
    val ignoreOffhandEating = setting(this, BooleanSetting(settingName("Ignore Offhand Eating"), true, LambdaUtilsKt.and({ page.value == Page.IN_COMBAT }, { pauseForEating.value })))
    val pauseBaritone = setting(this, BooleanSetting(settingName("Pause Baritone"), true, { page.value == Page.IN_COMBAT }))
    val resumeDelay = setting(this, IntegerSetting(settingName("Resume Delay"), 3, 1..10, 1, LambdaUtilsKt.and({ page.value == Page.IN_COMBAT }, { pauseBaritone.value })))

    val collisionMargin by setting(this, FloatSetting(settingName("Collision Margin"), 1.0E-4f, 0.0f..0.1f, 1.0E-4f, { page.value == Page.CALCULATION }))
    val crystalSetDead by setting(this, BooleanSetting(settingName("Crystal Set Dead"), false, { page.value == Page.CALCULATION }))
    val newCrystalPlacement by setting(this, BooleanSetting(settingName("1.14 Crystal Placement"), false, { page.value == Page.CALCULATION }))
    val assumeResistance by setting(this, BooleanSetting(settingName("Assume Resistance"), true, { page.value == Page.CALCULATION }))
    val motionPredict = setting(this, BooleanSetting(settingName("Motion Predict"), true, { page.value == Page.CALCULATION }))
    val pingSync = setting(this, BooleanSetting(settingName("Ping Sync"), false, LambdaUtilsKt.and({ page.value == Page.CALCULATION }, { motionPredict.value })))
    private val ticksAhead by setting(this, IntegerSetting(settingName("Ticks Ahead"), 6, 1..20, 1, LambdaUtilsKt.and({ page.value == Page.CALCULATION }, { motionPredict.value }, { !pingSync.value })))
    val selfPredict = setting(this, BooleanSetting(settingName("Self Predict"), true, LambdaUtilsKt.and({ page.value == Page.CALCULATION }, { motionPredict.value })))
    val pingSyncSelf = setting(this, BooleanSetting(settingName("Ping Sync Self"), false, LambdaUtilsKt.and({ page.value == Page.CALCULATION }, { motionPredict.value }, { selfPredict.value })))
    private val ticksAheadSelf by setting(this, IntegerSetting(settingName("Ticks Ahead Self"), 3, 1..20, 1, LambdaUtilsKt.and({ page.value == Page.CALCULATION }, { motionPredict.value }, { selfPredict.value }, { !pingSyncSelf.value })))
    val crystalUpdateDelay by setting(this, IntegerSetting(settingName("Crystal Update Delay"), 25, 5..500, 1, { page.value == Page.CALCULATION }))
    val horizontalCenterSampling by setting(this, BooleanSetting(settingName("Horizontal Center Sampling"), false, { page.value == Page.CALCULATION })).apply {
        listeners.add { ExposureSample.resetSamplePoints() }
    }
    val verticalCenterSampling by setting(this, BooleanSetting(settingName("Vertical Center Sampling"), true, { page.value == Page.CALCULATION })).apply {
        listeners.add { ExposureSample.resetSamplePoints() }
    }
    val backSideSampling by setting(this, BooleanSetting(settingName("Back Side Sampling"), true, { page.value == Page.CALCULATION }))

    val bedrockHole by setting(this, BooleanSetting(settingName("Bedrock Hole"), true, { page.value == Page.HOLE_DETECTION }))
    val obsidianHole by setting(this, BooleanSetting(settingName("Obsidian Hole"), true, { page.value == Page.HOLE_DETECTION }))
    val twoBlocksHole by setting(this, BooleanSetting(settingName("2 Blocks Hole"), true, { page.value == Page.HOLE_DETECTION }))
    val fourBlocksHole by setting(this, BooleanSetting(settingName("4 Blocks Hole"), false, { page.value == Page.HOLE_DETECTION }))
    val ignoreReplaceableFilling by setting(this, BooleanSetting(settingName("Ignore Replaceable Filling"), true, { page.value == Page.HOLE_DETECTION }))
    val ignoreNonFullBoxFilling by setting(this, BooleanSetting(settingName("Ignore Non-Full Cube Filling"), false, { page.value == Page.HOLE_DETECTION }))
    val ignoreNonCollidingFilling by setting(this, BooleanSetting(settingName("Ignore Non-Colliding Filling"), true, LambdaUtilsKt.and({ page.value == Page.HOLE_DETECTION }, { !ignoreNonFullBoxFilling })))

    val renderPrediction = setting(this, BooleanSetting(settingName("Render Prediction"), false, LambdaUtilsKt.and({ page.value == Page.RENDER }, { motionPredict.value })))
    var chams0 = setting(this, BooleanSetting(settingName("Chams"), false, { page.value == Page.RENDER }))
    var chams by chams0
    private val chamsColor by setting(this, ColorSetting(settingName("Chams Color"), ColorRGB(255, 32, 255, 127), { page.value == Page.RENDER && chams }))

    private var overrideRange = targetRange.value
    private var paused = false
    private val resumeTimer = TickTimer(TimeUnit.SECONDS)
    private var job: Job? = null
    private val timer = TickTimer()

    init {
        listener<ConnectionEvent.Disconnect> {
            CombatManager.targetOverride = null
        }

        listener<RenderEntityEvent.Model.Pre> {
            if (it.cancelled || !chams || it.entity != CombatManager.target) return@listener
            GL11.glDepthRange(0.0, 0.01)
            chamsColor.setGLColor()
            GlStateUtils.texture2d(false)
            GlStateUtils.lighting(false)
            GlStateUtils.blend(true)
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        }

        listener<RenderEntityEvent.Model.Post> {
            if (it.cancelled || !chams || it.entity != CombatManager.target) return@listener
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
            GlStateUtils.texture2d(true)
            GlStateUtils.lighting(true)
        }

        listener<RenderEntityEvent.All.Post> {
            if (!it.cancelled && chams && it.entity == CombatManager.target) {
                GL11.glDepthRange(0.0, 1.0)
            }
        }

        listener<Render2DEvent.Mc> {
            if (!motionPredict.value || !renderPrediction.value) return@listener
            val target = CombatManager.target ?: return@listener
            val ticks = getPredictTicksTarget()
            val posCurrent = EntityUtils.getInterpolatedPos(target, RenderUtils3D.partialTicks)
            val posAhead = CombatManager.trackerTarget?.calcPosAhead(ticks, true) ?: return@listener
            val posAheadEye = posAhead.addVector(0.0, target.eyeHeight.toDouble(), 0.0)

            val posCurrentScreen = ProjectionUtils.toScaledScreenPos(posCurrent)
            val posAheadScreen = ProjectionUtils.toScaledScreenPos(posAhead)
            val posAheadEyeScreen = ProjectionUtils.toScaledScreenPos(posAheadEye)

            val vertices = arrayOf(posCurrentScreen.toVec2f(), posAheadScreen.toVec2f(), posAheadEyeScreen.toVec2f())
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            RenderUtils2D.drawLineStrip(vertices, 2.0f, ColorRGB(80, 255, 80))
            GL11.glEnable(GL11.GL_TEXTURE_2D)
        }

        safeListener<RunGameLoopEvent.Tick>(priority = 5000) {
            if (timer.tickAndReset(25) && !CoroutineUtilsKt.isActiveOrFalse(job)) {
                job = DefaultScope.launch {
                    updateTarget(this@safeListener)
                }
            }
        }

        listener<TickEvent.Post> {
            if (isActive() && pauseBaritone.value) {
                PauseProcess.pauseBaritone(this)
                resumeTimer.reset()
                paused = true
            } else if (resumeTimer.tick(resumeDelay.value.toLong())) {
                PauseProcess.unpauseBaritone(this)
                paused = false
            }
        }
    }

    fun getPause(): Boolean {
        return runSafeOrFalse {
            player.ticksExisted < 10 || checkDigging(this) || checkEating(this)
        }
    }

    private fun checkDigging(event: SafeClientEvent): Boolean {
        return pauseForDigging.value && event.player.heldItemMainhand.item is ItemPickaxe && event.mc.playerController.isHittingBlock
    }

    private fun checkEating(event: SafeClientEvent): Boolean {
        return pauseForEating.value && (PauseProcess.isPausing(AutoEat) || (event.player.isHandActive && event.player.activeItemStack.item is ItemFood)) && (!ignoreOffhandEating.value || event.player.activeHand != EnumHand.OFF_HAND)
    }

    fun getPredictTicksTarget(): Int = if (motionPredict.value) getPredictTicks(pingSync.value, ticksAhead) else 0
    fun getPredictTicksSelf(): Int = if (motionPredict.value && selfPredict.value) getPredictTicks(pingSyncSelf.value, ticksAheadSelf) else 0
    private fun getPredictTicks(pingSync: Boolean, ticksAhead: Int): Int = if (pingSync) MathUtilKt.ceilToInt(InfoCalculator.ping() / 25.0f) else ticksAhead

    private fun updateTarget(event: SafeClientEvent) {
        val topModule = CombatManager.getTopModule()
        overrideRange = if (topModule is KillAura) topModule.range else targetRange.value

        val overrideTarget = CombatManager.targetOverride?.get()
        val eyePos = EntityUtils.getEyePosition(event.player)
        val ignoreWall = shouldIgnoreWall()
        val targets = getTargetList(event)
        val wallRangeSq = MathUtilKt.getSq(wallRange.value)

        val newTarget = if (overrideTarget != null && DistanceKt.distanceTo(overrideTarget, eyePos) < overrideRange.toDouble()) {
            overrideTarget
        } else {
            targets.firstOrNull {
                (overrideRange == targetRange.value || DistanceKt.distanceTo(it, eyePos) < overrideRange.toDouble()) &&
                (ignoreWall || event.player.canEntityBeSeen(it) && event.player.getDistanceSq(it) <= wallRangeSq.toDouble())
            }
        }

        val tracker = CombatManager.trackerTarget?.let { if (it.entity == newTarget) it else null } ?: newTarget?.let { MotionTracker(it) }

        CombatManager.targetList = targets
        CombatManager.target = newTarget
        CombatManager.trackerTarget = tracker
    }

    private fun getTargetList(event: SafeClientEvent): Set<EntityLivingBase> {
        val eyePos = EntityUtils.getEyePosition(event.player)
        val list = mutableListOf<Pair<EntityLivingBase, Float>>()

        for (entity in EntityManager.livingBase) {
            if (entity == event.player || entity == event.mc.renderViewEntity || !entity.isEntityAlive || (!invisible.value && entity.isInvisible)) continue
            val dist = DistanceKt.distanceTo(entity, eyePos)
            if (dist > targetRange.value.toDouble() || !checkEntityType(entity)) continue

            val distFactor = if (distancePriority == 0.0f) 0.0f else reverseAndClamp(dist.toFloat() / 8.0f) * distancePriority
            val healthFactor = if (healthPriority == 0.0f) 0.0f else reverseAndClamp(entity.health / 20.0f) * healthPriority
            val armorFactor = if (armorPriority == 0.0f) 0.0f else calcArmorFactor(entity) * armorPriority
            val holeFactor = if (holePriority == 0.0f) 0.0f else calcHoleFactor(event, entity) * holePriority
            val crosshairFactor = if (crosshairPriority == 0.0f) 0.0f else reverseAndClamp(RotationUtils.getRelativeRotation(event, entity) / 30.0f) * crosshairPriority

            list.add(entity to (distFactor + healthFactor + armorFactor + holeFactor + crosshairFactor))
        }

        return list.sortedByDescending { it.second }.map { it.first }.toSet()
    }

    private fun calcArmorFactor(entity: EntityLivingBase): Float {
        return CombatManager.getDamageReduction(entity)?.let { it.calcDamage(20.0f, true) / 20.0f } ?: 0.0f
    }

    private fun calcHoleFactor(event: SafeClientEvent, entity: EntityLivingBase): Float {
        val pos = EntityUtils.getFlooredPosition(entity)
        var count = 0
        for (offset in SurroundUtils.surroundOffsetNoFloor) {
            if (!CrystalUtils.isResistant(event.world.getBlockState(pos.add(offset)))) {
                count++
            }
        }
        return count.toFloat() / 4.0f
    }

    private fun reverseAndClamp(input: Float): Float = 1.0f - input.coerceIn(0.0f, 1.0f)

    private fun checkEntityType(entity: EntityLivingBase): Boolean {
        if (entity is EntityPlayer) {
            if (!players.value) return false
            if (!friends && FriendManager.isFriend(entity.name)) return false
            if (teammate) return true
            return !isTeammate(entity)
        }
        if (!mobs.value) return false
        if (!passive && EntityUtils.isPassive(entity)) return false
        if (!neutral && EntityUtils.isNeutral(entity)) return false
        if (!hostile && EntityUtils.isHostile(entity)) return false
        if (tamed) return true
        return !EntityUtils.isTamed(entity)
    }

    private fun isTeammate(entity: EntityPlayer): Boolean {
        return runSafeOrFalse {
            val playerTeam = player.team
            val targetTeam = entity.team
            if (playerTeam != null && targetTeam != null && playerTeam.isSameTeam(targetTeam)) return@runSafeOrFalse true

            val targetName = entity.displayName.formattedText.replace("\u00a7r", "")
            val clientName = player.displayName.formattedText.replace("\u00a7r", "")
            if (targetName.startsWith("T") && clientName.startsWith("T") && targetName[1] == targetName[2] && Character.isDigit(targetName[1])) return@runSafeOrFalse true

            targetName.startsWith("\u00a7${clientName[1]}")
        }
    }

    private fun shouldIgnoreWall(): Boolean = if (CombatManager.getTopModule() is KillAura) ignoreWalls else true

    private enum class Page { TARGET_TYPE, TARGET_PRIORITY, IN_COMBAT, CALCULATION, HOLE_DETECTION, RENDER }
}
