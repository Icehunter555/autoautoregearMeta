package dev.wizard.meta.module.modules.combat

import dev.fastmc.common.TickTimer
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.WorldEvent
import dev.wizard.meta.event.events.player.OnUpdateWalkingPlayerEvent
import dev.wizard.meta.manager.managers.EntityManager
import dev.wizard.meta.manager.managers.HotbarSwitchManager
import dev.wizard.meta.manager.managers.PlayerPacketManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.accessor.entityID
import dev.wizard.meta.util.accessor.opCode
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.inventory.ItemKt
import dev.wizard.meta.util.inventory.slot.DefinedKt
import dev.wizard.meta.util.inventory.slot.IterableKt
import dev.wizard.meta.util.inventory.slot.isHotbarSlot
import dev.wizard.meta.util.math.vector.toVec2f
import dev.wizard.meta.util.world.CheckKt
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityAreaEffectCloud
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.projectile.EntityPotion
import net.minecraft.init.Items
import net.minecraft.init.MobEffects
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketPlayerTryUseItem
import net.minecraft.network.play.server.SPacketDestroyEntities
import net.minecraft.network.play.server.SPacketEntityStatus
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionUtils
import net.minecraft.util.EnumHand
import kotlin.math.abs

object AutoDebuff : Module(
    "AutoDebuff",
    category = Category.COMBAT,
    description = "splash ur enemies with bad potions",
    modulePriority = 90
) {
    private val ghostSwitchBypass by setting(this, EnumSetting(settingName("Ghost Switch Bypass"), HotbarSwitchManager.Override.PICK))
    private val weakness by setting(this, BooleanSetting(settingName("Weakness"), true))
    private val weaknessFriendRange by setting(this, FloatSetting(settingName("Weakness Friend Range"), 5.0f, 0.0f..10.0f, 0.5f, { weakness }))
    private val weaknessRange by setting(this, FloatSetting(settingName("Weakness Range"), 1.8f, 0.0f..4.0f, 0.1f, { weakness }))
    private val weaknessDelay by setting(this, IntegerSetting(settingName("Weakness Delay"), 1500, 0..10000, 50, { weakness }))

    private val harming by setting(this, BooleanSetting(settingName("Harming"), true))
    private val harmingFriendRange by setting(this, FloatSetting(settingName("Harming Friend Range"), 5.0f, 0.0f..10.0f, 0.5f, { harming }))
    private val harmingRange by setting(this, FloatSetting(settingName("Harming Range"), 1.8f, 0.0f..4.0f, 0.1f, { harming }))
    private val harmingDelay by setting(this, IntegerSetting(settingName("Harming Delay"), 1500, 0..10000, 50, { harming }))

    private val jumpBoost by setting(this, BooleanSetting(settingName("Jump Boost"), false))
    private val jumpBoostFriendRange by setting(this, FloatSetting(settingName("Jump Boost Friend Range"), 5.0f, 0.0f..10.0f, 0.5f, { jumpBoost }))
    private val jumpBoostRange by setting(this, FloatSetting(settingName("Jump Boost Range"), 1.8f, 0.0f..4.0f, 0.1f, { jumpBoost }))
    private val jumpBoostDelay by setting(this, IntegerSetting(settingName("Jump Boost Delay"), 1500, 0..10000, 50, { jumpBoost }))

    private val slowness by setting(this, BooleanSetting(settingName("Slowness"), false))
    private val slownessFriendRange by setting(this, FloatSetting(settingName("Slowness Friend Range"), 5.0f, 0.0f..10.0f, 0.5f, { slowness }))
    private val slownessRange by setting(this, FloatSetting(settingName("Slowness Range"), 1.8f, 0.0f..4.0f, 0.1f, { slowness }))
    private val slownessDelay by setting(this, IntegerSetting(settingName("Slowness Delay"), 1500, 0..10000, 50, { slowness }))

    private val weaknessTimeMap = Int2LongOpenHashMap().apply { defaultReturnValue(34L) }
    private val jumpBoostTimeMap = Int2LongOpenHashMap().apply { defaultReturnValue(34L) }
    private val slownessTimeMap = Int2LongOpenHashMap().apply { defaultReturnValue(34L) }
    private var currentPotion = PotionType.NONE

    init {
        onDisable {
            currentPotion = PotionType.NONE
        }

        listener<PacketEvent.PostReceive> { event ->
            val packet = event.packet
            if (packet is SPacketDestroyEntities) {
                packet.entityIDs.forEach { id ->
                    weaknessTimeMap.remove(id)
                    jumpBoostTimeMap.remove(id)
                    slownessTimeMap.remove(id)
                }
            } else if (packet is SPacketEntityStatus && packet.opCode == 35.toByte()) {
                val id = packet.entityID
                weaknessTimeMap.remove(id)
                jumpBoostTimeMap.remove(id)
                slownessTimeMap.remove(id)
            }
        }

        safeListener<WorldEvent.Entity.Remove> { event ->
            if (event.entity is EntityPotion) {
                val effects = PotionUtils.getEffectsFromStack((event.entity as EntityPotion).potion)
                val box = event.entity.entityBoundingBox.grow(4.0, 2.0, 4.0)
                val targets = EntityManager.players.filter {
                    it.isEntityAlive && !EntityUtils.isFakeOrSelf(it) && !EntityUtils.isFriend(it) && box.intersects(it.entityBoundingBox)
                }

                effects.forEach { effect ->
                    val potion = effect.potion
                    val timeMap = when (potion) {
                        MobEffects.WEAKNESS -> weaknessTimeMap
                        MobEffects.JUMP_BOOST -> jumpBoostTimeMap
                        MobEffects.SLOWNESS -> slownessTimeMap
                        else -> null
                    } ?: return@forEach

                    targets.forEach { target ->
                        val distSq = event.entity.getDistanceSq(target)
                        if (distSq < 16.0) {
                            val factor = Math.sqrt(distSq) * 0.75
                            val duration = factor * effect.duration.toDouble() + 0.5
                            timeMap.put(target.entityId, System.currentTimeMillis() + duration.toLong() * 50L)
                        }
                    }
                }
            }
        }

        safeListener<OnUpdateWalkingPlayerEvent.Pre> {
            if (!groundCheck(this)) {
                currentPotion = PotionType.NONE
                return@safeListener
            }
            if (currentPotion == PotionType.NONE) {
                currentPotion = PotionType.VALUES.firstOrNull { it.check(this) } ?: PotionType.NONE
            }
            if (currentPotion != PotionType.NONE) {
                PlayerPacketManager.sendPlayerPacket {
                    rotate(player.rotationYaw.toVec2f(90.0f))
                }
            }
        }

        safeListener<OnUpdateWalkingPlayerEvent.Post> {
            val potionType = currentPotion
            if (potionType == PotionType.NONE) return@safeListener
            if (PlayerPacketManager.prevRotation.y <= 85.0f || PlayerPacketManager.rotation.y <= 85.0f) return@safeListener

            val slot = getSlot(this, potionType)
            if (slot != null) {
                HotbarSwitchManager.ghostSwitch(this, ghostSwitchBypass, slot) {
                    connection.sendPacket(CPacketPlayerTryUseItem(EnumHand.MAIN_HAND))
                    potionType.timer.reset()
                    currentPotion = PotionType.NONE
                }
            }
        }
    }

    override fun getHudInfo(): String = currentPotion.displayString

    private fun groundCheck(event: SafeClientEvent): Boolean {
        return event.player.onGround || event.player.posY - CheckKt.getGroundLevel(event.world, event.player) < 3.0
    }

    private fun getSlot(event: SafeClientEvent, potionType: PotionType): Slot? {
        return findPotion(DefinedKt.getAllSlotsPrioritized(event.player), potionType)
    }

    private fun findPotion(slots: List<Slot>, potionType: PotionType): Slot? {
        val requiredItem = if (potionType == PotionType.HARMING) Items.LINGERING_POTION else Items.SPLASH_POTION
        return slots.asSequence()
            .filter { slot ->
                val stack = slot.stack
                stack.item == requiredItem && ItemKt.hasPotion(stack, potionType.potion)
            }
            .minByOrNull { slot -> if (slot.isHotbarSlot) -1 else slot.stack.count }
    }

    private fun hasPotion(slots: List<Slot>, potionType: PotionType): Boolean {
        val requiredItem = if (potionType == PotionType.HARMING) Items.LINGERING_POTION else Items.SPLASH_POTION
        return slots.any { slot ->
            slot.stack.item == requiredItem && ItemKt.hasPotion(slot.stack, potionType.potion)
        }
    }

    private enum class PotionType(override val displayName: CharSequence, val potion: Potion) : DisplayEnum {
        WEAKNESS("Weakness", MobEffects.WEAKNESS) {
            override fun check(event: SafeClientEvent): Boolean {
                if (!weakness || !timer.tick(weaknessDelay.toLong()) || !super.check(event)) return false
                if (weaknessFriendRange != 0.0f) {
                    if (EntityManager.players.any { !EntityUtils.isFakeOrSelf(it) && EntityUtils.isFriend(it) && event.player.getDistanceSq(it) <= weaknessFriendRange * weaknessFriendRange }) return false
                }
                return EntityManager.players.any { !EntityUtils.isFriend(it) && !EntityUtils.isFakeOrSelf(it) && isInRange(event, it, weaknessRange) && !weaknessTimeMap.containsKey(it.entityId) }
            }
        },
        HARMING("Harming", MobEffects.INSTANT_DAMAGE) {
            override fun check(event: SafeClientEvent): Boolean {
                if (!harming || !timer.tick(harmingDelay.toLong()) || !super.check(event)) return false
                if (harmingFriendRange != 0.0f) {
                    if (EntityManager.players.any { !EntityUtils.isFakeOrSelf(it) && EntityUtils.isFriend(it) && event.player.getDistanceSq(it) <= harmingFriendRange * harmingFriendRange }) return false
                }
                return EntityManager.players.any { !EntityUtils.isFriend(it) && !EntityUtils.isFakeOrSelf(it) && isInRange(event, it, harmingRange) && !isInHarmingCloud(event, it) }
            }

            private fun isInHarmingCloud(event: SafeClientEvent, entity: Entity): Boolean {
                return event.world.loadedEntityList.asSequence()
                    .filterIsInstance<EntityAreaEffectCloud>()
                    .filter { cloud -> PotionUtils.getEffectsFromTag(cloud.func_184221_q()).any { it.potion == MobEffects.INSTANT_DAMAGE } }
                    .any { it.entityBoundingBox.intersects(entity.entityBoundingBox) }
            }
        },
        JUMP_BOOST("Jump Boost", MobEffects.JUMP_BOOST) {
            override fun check(event: SafeClientEvent): Boolean {
                if (!jumpBoost || !timer.tick(jumpBoostDelay.toLong()) || !super.check(event)) return false
                if (jumpBoostFriendRange != 0.0f) {
                    if (EntityManager.players.any { !EntityUtils.isFakeOrSelf(it) && EntityUtils.isFriend(it) && event.player.getDistanceSq(it) <= jumpBoostFriendRange * jumpBoostFriendRange }) return false
                }
                return EntityManager.players.any { !EntityUtils.isFriend(it) && !EntityUtils.isFakeOrSelf(it) && isInRange(event, it, jumpBoostRange) && !jumpBoostTimeMap.containsKey(it.entityId) }
            }
        },
        SLOWNESS("Slowness", MobEffects.SLOWNESS) {
            override fun check(event: SafeClientEvent): Boolean {
                if (!slowness || !timer.tick(slownessDelay.toLong()) || !super.check(event)) return false
                if (slownessFriendRange != 0.0f) {
                    if (EntityManager.players.any { !EntityUtils.isFakeOrSelf(it) && EntityUtils.isFriend(it) && event.player.getDistanceSq(it) <= slownessFriendRange * slownessFriendRange }) return false
                }
                return EntityManager.players.any { !EntityUtils.isFriend(it) && !EntityUtils.isFakeOrSelf(it) && isInRange(event, it, slownessRange) && !slownessTimeMap.containsKey(it.entityId) }
            }
        },
        NONE("", MobEffects.LUCK) {
            override fun check(event: SafeClientEvent): Boolean = false
        };

        val timer = TickTimer()

        open fun check(event: SafeClientEvent): Boolean {
            return INSTANCE.hasPotion(DefinedKt.getAllSlots(event.player), this)
        }

        protected fun isInRange(event: SafeClientEvent, entity: Entity, range: Float): Boolean {
            return abs(event.player.posX - entity.posX) <= 4.125 &&
                   abs(event.player.posY - entity.posY) <= 2.125 &&
                   abs(event.player.posZ - entity.posZ) <= 4.125 &&
                   event.player.getDistanceSq(entity) <= range * range
        }

        companion object {
            val VALUES = values()
        }
    }
}
