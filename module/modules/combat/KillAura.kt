package dev.wizard.meta.module.modules.combat

import dev.fastmc.common.TickTimer
import dev.wizard.meta.event.events.RunGameLoopEvent
import dev.wizard.meta.event.events.StepEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.player.OnUpdateWalkingPlayerEvent
import dev.wizard.meta.event.events.player.PlayerMoveEvent
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.graphics.ESPRenderer
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.manager.managers.CombatManager
import dev.wizard.meta.manager.managers.HotbarSwitchManager
import dev.wizard.meta.manager.managers.PlayerPacketManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.client.CombatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.TpsCalculator
import dev.wizard.meta.util.combat.CombatUtils
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.inventory.isWeapon
import dev.wizard.meta.util.inventory.operation.swapToSlot
import dev.wizard.meta.util.math.isInSight
import dev.wizard.meta.util.math.rotation.RotationUtils
import dev.wizard.meta.util.math.vector.add
import dev.wizard.meta.util.math.vector.distanceTo
import dev.wizard.meta.util.pause.MainHandPause
import dev.wizard.meta.util.pause.withPause
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.monster.EntityMob
import net.minecraft.entity.passive.EntityAnimal
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemFood
import net.minecraft.item.ItemPickaxe
import net.minecraft.item.ItemSword
import net.minecraft.network.play.client.CPacketPlayerDigging
import net.minecraft.network.play.client.CPacketPlayerTryUseItem
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import kotlin.random.Random

object KillAura : Module(
    "KillAura",
    alias = arrayOf("KA", "Aura", "TriggerBot"),
    category = Category.COMBAT,
    description = "Hits entities around you"
) {
    private val page by setting("Page", Page.GENERAL)

    // General
    private val mode by setting("Mode", Mode.COOLDOWN, { page == Page.GENERAL })
    private val rotationMode by setting("Rotation Mode", RotationMode.SPOOF, { page == Page.GENERAL })
    private val delayTicks by setting("Delay Ticks", 5, 0..40, 1, { mode == Mode.TICKS && page == Page.GENERAL })
    private val delayMs by setting("Delay ms", 50, 0..1000, 1, { mode == Mode.MS && page == Page.GENERAL })
    private val randomDelay by setting("Random Delay", 0.0f, 0.0f..5.0f, 0.1f, { mode != Mode.COOLDOWN && page == Page.GENERAL })
    private val disableOnDeath by setting("Disable On Death", false, { page == Page.GENERAL })
    private val tpsSync by setting("TPS Sync", false, { page == Page.GENERAL })
    private val armorDdos by setting("Armor Ddos", false, { page == Page.GENERAL })
    private val weaponOnly by setting("Weapon Only", false, { !armorDdos && page == Page.GENERAL })
    private val pickaxeIsWeapon by setting("Pickaxe is weapon", false, { !armorDdos && page == Page.GENERAL })
    private val autoWeapon by setting("Auto Weapon", true, { !armorDdos && page == Page.GENERAL })
    private val autoBlock by setting("Auto Block", false, { page == Page.GENERAL })
    private val noSwapWhileEating by setting("No Swap While Eating", true, { autoWeapon && page == Page.GENERAL })
    private val prefer by setting("Prefer", CombatUtils.PreferWeapon.SWORD, { !armorDdos && autoWeapon && page == Page.GENERAL })
    private val minSwapHealth by setting("Min Swap Health", 5.0f, 1.0f..20.0f, 0.5f, { page == Page.GENERAL })
    private val swapDelay by setting("Swap Delay", 10, 0..50, 1, { page == Page.GENERAL })
    val range by setting("Range", 4.0f, 0.0f..6.0f, 0.1f, { page == Page.GENERAL })
    private val wallRange by setting("Wall Range", 0.0f, 0.0f..6.0f, 0.1f, { page == Page.GENERAL })

    // Render
    private val render by setting("Render", false, { page == Page.RENDER })
    private val renderColor by setting("Render Color", ColorRGB(255, 255, 255), { render && page == Page.RENDER })
    private val renderOutlineA by setting("Render Outline Alpha", 200, 0..255, 1, { render && page == Page.RENDER })
    private val renderOutlineW by setting("Render Outline Width", 2.0f, 0.25f..5.0f, 0.25f, { render && page == Page.RENDER })
    private val renderFillA by setting("Render Fill Alpha", 63, 0..255, 1, { render && page == Page.RENDER })
    private val renderExpand by setting("Render Expand", 0.002f, 0.0f..1.0f, 0.001f, { render && page == Page.RENDER })
    private val flashOnHit by setting("Flash On Hit", true, { render && page == Page.RENDER })
    private val flashDuration by setting("Flash Duration", 5, 1..20, 1, { flashOnHit && render && page == Page.RENDER })

    // Target
    private val targetRange by setting("Target Range", 6.0f, 1.0f..16.0f, 1.0f, { page == Page.TARGET })
    private val targetPriority by setting("Target Priority", TargetPriority.DISTANCE)
    private val targetPlayers by setting("Target Players", true, { page == Page.TARGET })
    private val targetMobs by setting("Target Mobs", true, { page == Page.TARGET })
    private val targetAnimals by setting("Target Animals", false, { page == Page.TARGET })
    private val targetTamed by setting("Target Tamed", false, { page == Page.TARGET })
    private val targetInvisible by setting("Target Invisible", true, { page == Page.TARGET })

    private val timer = TickTimer()
    private var inactiveTicks = 0
    private var random = 0L
    private var lastSlot = 0
    private var blocking = false
    private val renderer = ESPRenderer()
    private var flashTicks = 0

    override val isActive: Boolean
        get() = isEnabled && inactiveTicks <= 5

    override val hudInfo: String
        get() = SafeClientEvent.instance?.let {
            val target = getAuraTarget(it)
            target?.name ?: "Idle"
        } ?: ""

    init {
        onDisable {
            SafeClientEvent.instance?.let { stopBlocking(it) }
        }

        safeListener<RunGameLoopEvent.Tick> {
            if (mode == Mode.MS) {
                runKillAura()
            }
        }

        safeListener<TickEvent.Post> {
            inactiveTicks++
            if (flashTicks > 0) flashTicks--

            runKillAura()

            if (autoBlock && inactiveTicks < 5 && player.heldItemMainhand.item is ItemSword) {
                startBlocking()
            } else {
                stopBlocking()
            }
        }

        safeListener<Render3DEvent> {
            val target = getAuraTarget(this) ?: return@safeListener
            
            val color = if (flashTicks > 0) ColorRGB(255, 255, 255) else renderColor
            val box = target.entityBoundingBox.grow(renderExpand.toDouble())
            
            renderer.add(box, color, 63)
            renderer.aFilled = if (flashTicks > 0) 150 else renderFillA
            renderer.aOutline = if (flashTicks > 0) 255 else renderOutlineA
            renderer.thickness = renderOutlineW
            renderer.render(true)
        }
    }

    private fun SafeClientEvent.runKillAura() {
        if (!player.isEntityAlive) {
            if (disableOnDeath) disable()
            return
        }

        val target = getAuraTarget(this) ?: return
        if (CombatSetting.pause) return

        val distance = player.getDistance(target)
        if (distance >= range) return

        if (!target.entityBoundingBox.isInSight(player.positionVector.add(0.0, player.eyeHeight.toDouble(), 0.0), PlayerPacketManager.rotation) && distance > wallRange) {
            return
        }

        if (swapDelay > 0 && System.currentTimeMillis() - HotbarSwitchManager.swapTime < swapDelay * 50L) return

        rotate(target)

        MainHandPause.withPause(this@KillAura) {
            inactiveTicks = 0
            
            if (!armorDdos) {
                val isEating = noSwapWhileEating && player.isHandActive && player.activeItemStack.item is ItemFood
                
                if (autoWeapon && CombatUtils.getScaledHealth(player) > minSwapHealth && !isEating) {
                    CombatUtils.equipBestWeapon(this@runKillAura, prefer)
                }
                
                if (weaponOnly && !isWeaponHeld()) return@withPause
            }

            if (canAttack(target)) {
                if (armorDdos) {
                    swapToSlot(lastSlot)
                    lastSlot = (lastSlot + 1) % 9
                }
                
                stopBlocking()
                attack(target)
                
                if (flashOnHit) {
                    flashTicks = flashDuration
                }
            }
        }
    }

    private fun SafeClientEvent.rotate(target: EntityLivingBase) {
        when (rotationMode) {
            RotationMode.SPOOF -> {
                PlayerPacketManager.sendPlayerPacket(modulePriority) {
                    rotate(RotationUtils.getRotationToEntityClosest(this@rotate, target))
                }
            }
            RotationMode.VIEW_LOCK -> {
                RotationUtils.faceEntityClosest(this, target)
            }
            RotationMode.OFF -> {}
        }
    }

    private fun SafeClientEvent.canAttack(target: EntityLivingBase): Boolean {
        val delay = when (mode) {
            Mode.COOLDOWN -> {
                val adjustTicks = if (!tpsSync) 0.0f else TpsCalculator.tickRate - 20.0f
                if (player.getCooledAttackStrength(adjustTicks) <= 0.9f) return false
                return true
            }
            Mode.TICKS -> (delayTicks * 50L + random)
            Mode.MS -> (delayMs + random)
        }

        if (mode != Mode.COOLDOWN && !timer.tickAndReset(delay as Long)) return false

        return target.entityBoundingBox.isInSight(player.positionVector.add(0.0, player.eyeHeight.toDouble(), 0.0), PlayerPacketManager.rotation)
    }

    private fun SafeClientEvent.attack(entity: Entity) {
        playerController.attackEntity(player, entity)
        player.swingArm(EnumHand.MAIN_HAND)
        
        random = if (mode != Mode.COOLDOWN && randomDelay > 0.0f) {
            Random.nextLong((getDelay() * randomDelay).toLong() + 1L)
        } else {
            0L
        }
    }

    private fun SafeClientEvent.startBlocking() {
        if (!blocking) {
            connection.sendPacket(CPacketPlayerTryUseItem(EnumHand.MAIN_HAND))
            blocking = true
        }
    }

    private fun SafeClientEvent.stopBlocking() {
        if (blocking) {
            connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
            blocking = false
        }
    }

    private fun getDelay(): Long {
        return if (mode == Mode.TICKS) (delayTicks * 50L) else delayMs.toLong()
    }

    private fun SafeClientEvent.isWeaponHeld(): Boolean {
        return if (player.heldItemMainhand.item is ItemPickaxe && pickaxeIsWeapon) {
            true
        } else {
            player.heldItemMainhand.item.isWeapon
        }
    }

    private fun getAuraTarget(event: SafeClientEvent): EntityLivingBase? {
        val targetList = ArrayList<EntityLivingBase>()

        for (entity in event.world.loadedEntityList) {
            if (entity.getDistance(event.player) > targetRange || (entity.isInvisible && !targetInvisible)) continue
            
            if (EntityUtils.isTamed(entity) && !targetTamed) continue

            if (entity is EntityPlayer && targetPlayers && !EntityUtils.isFriend(entity) && !entity.isSpectator && !entity.isCreative && !EntityUtils.isSelf(entity)) {
                targetList.add(entity)
                continue
            }

            if (entity is EntityMob && targetMobs) {
                targetList.add(entity)
                continue
            }

            if (entity is EntityAnimal && targetAnimals) {
                targetList.add(entity)
                continue
            }
        }

        if (targetList.isEmpty()) return null

        return when (targetPriority) {
            TargetPriority.DISTANCE -> targetList.minByOrNull { event.player.getDistance(it) }
            TargetPriority.HEALTH -> targetList.minByOrNull { it.health }
        }
    }

    private enum class Mode(override val displayName: String) : DisplayEnum {
        COOLDOWN("Cooldown"),
        TICKS("Ticks"),
        MS("ms")
    }

    private enum class Page(override val displayName: String) : DisplayEnum {
        GENERAL("General"),
        RENDER("Render"),
        TARGET("Target")
    }

    private enum class RotationMode(override val displayName: String) : DisplayEnum {
        OFF("Off"),
        SPOOF("Spoof"),
        VIEW_LOCK("View Lock")
    }

    private enum class TargetPriority(override val displayName: String) : DisplayEnum {
        DISTANCE("Distance"),
        HEALTH("Health")
    }
}
