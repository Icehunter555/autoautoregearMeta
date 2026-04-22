package dev.wizard.meta.module.modules.combat

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.graphics.ESPRenderer
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.manager.managers.HotbarSwitchManager
import dev.wizard.meta.manager.managers.PlayerPacketManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.DoubleSetting
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.LambdaUtilsKt
import dev.wizard.meta.util.accessor.setRightClickDelayTimer
import dev.wizard.meta.util.combat.CombatUtils
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.inventory.operation.swapToItem
import dev.wizard.meta.util.inventory.operation.swapToSlot
import dev.wizard.meta.util.math.BoundingBoxUtilsKt
import dev.wizard.meta.util.math.RotationUtils
import dev.wizard.meta.util.math.vector.toVec2f
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.monster.EntityMob
import net.minecraft.entity.passive.EntityAnimal
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Items
import net.minecraft.network.play.client.CPacketPlayerTryUseItem
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.Vec3d
import java.util.*
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.random.Random

object BowBot : Module(
    "BowBot",
    category = Category.COMBAT,
    description = "Automatically aims and shoots bow at targets",
    modulePriority = 20
) {
    private val page = setting(this, EnumSetting(settingName("Page"), Page.GENERAL))
    private val aimBot by setting(this, BooleanSetting(settingName("Aim Bot"), true, { page.value == Page.GENERAL }))
    private val rotationMode by setting(this, EnumSetting(settingName("Rotation Mode"), RotationMode.SPOOF, LambdaUtilsKt.and({ aimBot }) { page.value == Page.GENERAL }))
    private val autoSwap by setting(this, BooleanSetting(settingName("Swap to Bow"), false, { page.value == Page.GENERAL }))
    private val minSwapHealth by setting(this, FloatSetting(settingName("Min Swap Health"), 5.0f, 1.0f..20.0f, 0.5f, { page.value == Page.GENERAL }))
    private val swapDelay by setting(this, IntegerSetting(settingName("Swap Delay"), 10, 0..50, 1, { page.value == Page.GENERAL }))
    val range by setting(this, FloatSetting(settingName("Range"), 4.0f, 0.0f..6.0f, 0.1f, { page.value == Page.GENERAL }))
    private val raytrace by setting(this, BooleanSetting(settingName("Raytrace"), true, { page.value == Page.GENERAL }))
    private val armorDdos = setting(this, BooleanSetting(settingName("Armor Ddos"), false, { page.value == Page.GENERAL }))
    private val autoShoot by setting(this, BooleanSetting(settingName("Auto Shoot"), true, { page.value == Page.GENERAL }))
    private val shootDelay by setting(this, IntegerSetting(settingName("Shoot Delay"), 5, 0..40, 1, LambdaUtilsKt.and({ autoShoot }) { page.value == Page.GENERAL }))
    private val fastBow by setting(this, BooleanSetting(settingName("Fast Bow"), true, { page.value == Page.GENERAL }))
    private val chargeSetting by setting(this, IntegerSetting(settingName("Bow Charge"), 3, 0..20, 1, LambdaUtilsKt.and({ fastBow }) { page.value == Page.GENERAL }))
    private val chargeVariation by setting(this, IntegerSetting(settingName("Charge Variation"), 5, 0..20, 1, LambdaUtilsKt.and({ fastBow }) { page.value == Page.GENERAL }))

    private val targetRange by setting(this, FloatSetting(settingName("Target Range"), 16.0f, 1.0f..32.0f, 1.0f, { page.value == Page.TARGET }))
    private val targetPriority by setting(this, EnumSetting(settingName("Target Priority"), TargetPriority.DISTANCE, { page.value == Page.TARGET }))
    private val targetPlayers by setting(this, BooleanSetting(settingName("Target Players"), true, { page.value == Page.TARGET }))
    private val targetMobs by setting(this, BooleanSetting(settingName("Target Mobs"), true, { page.value == Page.TARGET }))
    private val targetAnimals by setting(this, BooleanSetting(settingName("Target Animals"), false, { page.value == Page.TARGET }))
    private val targetTamed by setting(this, BooleanSetting(settingName("Target Tamed"), false, { page.value == Page.TARGET }))
    private val targetInvisible by setting(this, BooleanSetting(settingName("Target Invisible"), true, { page.value == Page.TARGET }))

    private val render = setting(this, BooleanSetting(settingName("Render"), true, { page.value == Page.RENDER }))
    private val renderColor by setting(this, ColorSetting(settingName("Render Color"), ColorRGB(255, 100, 100), LambdaUtilsKt.and({ render.value }) { page.value == Page.RENDER }))
    private val renderOutlineA by setting(this, IntegerSetting(settingName("Render Outline Alpha"), 200, 0..255, 1, LambdaUtilsKt.and({ render.value }) { page.value == Page.RENDER }))
    private val renderOutlineW by setting(this, FloatSetting(settingName("Render Outline Width"), 2.0f, 0.25f..5.0f, 0.25f, LambdaUtilsKt.and({ render.value }) { page.value == Page.RENDER }))
    private val renderFillA by setting(this, IntegerSetting(settingName("Render Fill Alpha"), 63, 0..255, 1, LambdaUtilsKt.and({ render.value }) { page.value == Page.RENDER }))
    private val renderExpand by setting(this, FloatSetting(settingName("Render Expand"), 0.002f, 0.0f..1.0f, 0.001f, LambdaUtilsKt.and({ render.value }) { page.value == Page.RENDER }))
    private val flashOnHit by setting(this, BooleanSetting(settingName("Flash On Hit"), true, LambdaUtilsKt.and({ render.value }) { page.value == Page.RENDER }))
    private val flashDuration by setting(this, IntegerSetting(settingName("Flash Duration"), 5, 1..20, 1, LambdaUtilsKt.and({ flashOnHit && render.value }) { page.value == Page.RENDER }))

    private var lastUsedHand = EnumHand.MAIN_HAND
    private var randomVariation = 0
    private var shootDelayTicks = 0
    private var isCharging = false
    private var hasShot = false
    private var lastSlot = 0
    private val renderer = ESPRenderer()
    private var flashTicks = 0

    init {
        onDisable {
            randomVariation = 0
            shootDelayTicks = 0
            isCharging = false
            hasShot = false
            flashTicks = 0
            renderer.clear()
        }

        safeListener<TickEvent.Pre> {
            val target = getBowTarget(this)
            if (flashTicks > 0) flashTicks--

            if (player.heldItemMainhand.item != Items.BOW) {
                if (autoSwap && CombatUtils.getScaledHealth(player) > minSwapHealth) {
                    if (swapDelay <= 0 || System.currentTimeMillis() - HotbarSwitchManager.swapTime >= swapDelay.toLong() * 50L) {
                        swapToItem(Items.BOW)
                    }
                } else if (!armorDdos.value) return@safeListener
            }

            if (aimBot && target != null) {
                when (rotationMode) {
                    RotationMode.LEGIT -> RotationUtils.faceEntityClosest(this, target)
                    RotationMode.SPOOF -> {
                        PlayerPacketManager.sendPlayerPacket {
                            rotate(RotationUtils.getRotationToEntityClosest(this@safeListener, target))
                        }
                    }
                }
            }

            if (shootDelayTicks > 0) shootDelayTicks--

            if (player.heldItemMainhand.item == Items.BOW || armorDdos.value) {
                if (player.isHandActive && player.activeItemStack.item == Items.BOW && player.itemInUseCount >= getBowChargeValue()) {
                    if (canShootTarget(this, target)) {
                        randomVariation = 0
                        mc.playerController.onStoppedUsingItem(player)
                        isCharging = false
                        hasShot = true
                        shootDelayTicks = shootDelay
                        mc.setRightClickDelayTimer(0)
                        if (flashOnHit) flashTicks = flashDuration
                        if (armorDdos.value) {
                            swapToSlot(lastSlot)
                            lastSlot = (lastSlot + 1) % 9
                        }
                    }
                } else if (autoShoot && target != null && !player.isHandActive && shootDelayTicks <= 0) {
                    mc.setRightClickDelayTimer(0)
                    mc.playerController.processRightClick(player, world, EnumHand.MAIN_HAND)
                    isCharging = true
                    hasShot = false
                } else if (!player.isHandActive) {
                    isCharging = false
                }
            }
        }

        safeListener<Render3DEvent> {
            if (!render.value) return@safeListener
            val target = getBowTarget(this) ?: return@safeListener

            val color = if (flashTicks > 0) ColorRGB(255, 255, 255) else renderColor
            val box = target.entityBoundingBox.grow(renderExpand.toDouble())
            renderer.add(box, color, 63)
            renderer.setAFilled(if (flashTicks > 0) 150 else renderFillA)
            renderer.setAOutline(if (flashTicks > 0) 255 else renderOutlineA)
            renderer.setThickness(renderOutlineW)
            renderer.render(true)
        }

        listener<PacketEvent.PostSend> {
            val packet = it.packet
            if (packet is CPacketPlayerTryUseItem) lastUsedHand = packet.hand
            if (packet is CPacketPlayerTryUseItemOnBlock) lastUsedHand = packet.hand
        }
    }

    override fun getHudInfo(): String = runSafe { getBowTarget(this)?.name } ?: "Idle"

    fun getBowCharge(): Double? = if (isEnabled && fastBow) (72000.0 - (chargeSetting.toDouble() + chargeVariation.toDouble() / 2.0)) else null

    private fun getBowChargeValue(): Int {
        if (randomVariation == 0) {
            randomVariation = if (chargeVariation == 0) 0 else Random.nextInt(chargeVariation + 1)
        }
        return chargeSetting + randomVariation
    }

    private fun canShootTarget(event: SafeClientEvent, target: EntityLivingBase?): Boolean {
        if (target == null) return false
        if (event.player.getDistance(target) > range) return false
        if (raytrace) {
            val eyePos = PlayerPacketManager.position.addVector(0.0, event.player.eyeHeight.toDouble(), 0.0)
            return BoundingBoxUtilsKt.isInSight(target.entityBoundingBox, eyePos, PlayerPacketManager.rotation)
        }
        return true
    }

    private fun getBowTarget(event: SafeClientEvent): EntityLivingBase? {
        val targetList = mutableListOf<EntityLivingBase>()
        for (entity in event.world.loadedEntityList) {
            if (event.player.getDistance(entity) > targetRange || (entity.isInvisible && !targetInvisible)) continue
            if (EntityUtils.isTamed(entity) && !targetTamed) continue

            if (entity is EntityPlayer && targetPlayers && !EntityUtils.isFriend(entity) && !entity.isCreative && !entity.isSpectator && !EntityUtils.isSelf(entity)) {
                targetList.add(entity)
            } else if (entity is EntityMob && targetMobs && !entity.isDead) {
                targetList.add(entity)
            } else if (entity is EntityAnimal && targetAnimals && !entity.isDead) {
                targetList.add(entity)
            }
        }

        if (targetList.isEmpty()) return null

        return when (targetPriority) {
            TargetPriority.DISTANCE -> targetList.minByOrNull { event.player.getDistance(it) }
            TargetPriority.HEALTH -> targetList.minByOrNull { it.health }
        }
    }

    private enum class Page { GENERAL, TARGET, RENDER }
    private enum class RotationMode { LEGIT, SPOOF }
    private enum class TargetPriority(override val displayName: CharSequence) : DisplayEnum {
        DISTANCE("Distance"), HEALTH("Health")
    }
}
