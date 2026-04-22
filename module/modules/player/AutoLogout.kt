package dev.wizard.meta.module.modules.player

import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.combat.TotemPopEvent
import dev.wizard.meta.manager.managers.CombatManager
import dev.wizard.meta.manager.managers.EntityManager
import dev.wizard.meta.manager.managers.FriendManager
import dev.wizard.meta.manager.managers.UUIDManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.combat.CombatUtils
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.inventory.slot.allSlotsPrioritized
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Items
import net.minecraft.init.MobEffects
import net.minecraft.item.ItemShulkerBox
import net.minecraft.item.ItemSplashPotion
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.potion.PotionUtils
import net.minecraft.util.text.TextComponentString
import java.util.*

object AutoLogout : Module(
    "AutoLogout",
    category = Category.PLAYER,
    description = "Automatically log/relog when in danger"
) {
    private val disableMode by setting(this, EnumSetting(settingName("Disable Mode"), DisableMode.ALWAYS))
    private val doHealth by setting(this, EnumSetting(settingName("Do Health"), ModuleMode.DISABLED))
    private val health by setting(this, IntegerSetting(settingName("Health"), 10, 6..36, 1, { doHealth != ModuleMode.DISABLED }))

    private val crystals by setting(this, EnumSetting(settingName("Crystals"), ModuleMode.DISABLED))
    private val totemPops by setting(this, EnumSetting(settingName("Totem Pops"), ModuleMode.DISABLED))
    private val totemPopCount by setting(this, IntegerSetting(settingName("Totem Pop Count"), 3, 1..10, 1, { totemPops != ModuleMode.DISABLED }))
    private val totemPopDelay by setting(this, IntegerSetting(settingName("Totem Pop Delay"), 10, 1..60, 1, { totemPops != ModuleMode.DISABLED }))

    private val potions by setting(this, EnumSetting(settingName("Health Potions"), ModuleMode.DISABLED))
    private val minPotions by setting(this, IntegerSetting(settingName("Min Potions"), 5, 1..64, 1, { potions != ModuleMode.DISABLED }))

    private val shulkers by setting(this, EnumSetting(settingName("Shulker Boxes"), ModuleMode.DISABLED))
    private val minShulkers by setting(this, IntegerSetting(settingName("Min Shulkers"), 2, 1..64, 1, { shulkers != ModuleMode.DISABLED }))

    private val armor by setting(this, EnumSetting(settingName("Armor"), ModuleMode.DISABLED))
    private val minHelmet by setting(this, IntegerSetting(settingName("Min Helmet"), 10, 1..127, 1, { armor != ModuleMode.DISABLED }))
    private val minChestplate by setting(this, IntegerSetting(settingName("Min Chestplate"), 10, 1..127, 1, { armor != ModuleMode.DISABLED }))
    private val minLeggings by setting(this, IntegerSetting(settingName("Min Leggings"), 10, 1..127, 1, { armor != ModuleMode.DISABLED }))
    private val minBoots by setting(this, IntegerSetting(settingName("Min Boots"), 10, 1..127, 1, { armor != ModuleMode.DISABLED }))

    private val players by setting(this, EnumSetting(settingName("Players"), ModuleMode.DISABLED))
    private val playerDistance by setting(this, IntegerSetting(settingName("Player Distance"), 64, 4..128, 4, { players != ModuleMode.DISABLED }))
    private val friends by setting(this, BooleanSetting(settingName("Friends"), false, { players != ModuleMode.DISABLED }))

    private val requirePlayer by setting(this, BooleanSetting(settingName("Require Player"), false))
    private val requirePlayerDistance by setting(this, IntegerSetting(settingName("Require Player Distance"), 32, 4..128, 4, { requirePlayer }))

    private val antiPing by setting(this, EnumSetting(settingName("Anti Ping"), ModuleMode.DISABLED))
    private val pingTimeout by setting(this, IntegerSetting(settingName("Ping Timeout"), 3000, 500..30000, 100, { antiPing != ModuleMode.DISABLED }))

    private val kick by setting(this, BooleanSetting(settingName("Kick"), true))

    private val popTimes = ArrayList<Long>()
    private var lastPacketTime = Long.MAX_VALUE

    init {
        onEnable {
            lastPacketTime = System.currentTimeMillis()
        }

        onDisable {
            popTimes.clear()
            lastPacketTime = Long.MAX_VALUE
        }

        listener<PacketEvent.Receive> {
            if (antiPing != ModuleMode.DISABLED) {
                lastPacketTime = System.currentTimeMillis()
            }
        }

        safeListener<TotemPopEvent.Pop> {
            if (it.entity != player) return@safeListener
            if (totemPops == ModuleMode.DISABLED) return@safeListener

            val currentTime = System.currentTimeMillis()
            popTimes.add(currentTime)
            popTimes.removeIf { currentTime - it > totemPopDelay * 1000L }

            if (popTimes.size >= totemPopCount) {
                handleEvent(this, totemPops, Reasons.TOTEM_POPS, popTimes.size.toString())
            }
        }

        safeListener<TickEvent.Post>(-1000) {
            if (isEnabled(antiPing) && checkPingTimeout()) {
                handleEvent(this, antiPing, Reasons.PING_TIMEOUT, pingTimeout.toString())
            } else if (isEnabled(doHealth) && CombatUtils.getScaledHealth(player as EntityLivingBase) < health.toFloat()) {
                handleEvent(this, doHealth, Reasons.HEALTH)
            } else if (isEnabled(crystals) && checkCrystals(this)) {
                handleEvent(this, crystals, Reasons.END_CRYSTAL)
            } else if (isEnabled(potions) && checkPotions(this)) {
                handleEvent(this, potions, Reasons.POTIONS)
            } else if (isEnabled(shulkers) && checkShulkers(this)) {
                handleEvent(this, shulkers, Reasons.SHULKERS)
            } else if (isEnabled(armor) && checkArmor(this)) {
                handleEvent(this, armor, Reasons.ARMOR)
            } else if (isEnabled(players)) {
                checkPlayers(this)
            }
        }
    }

    private fun checkPingTimeout(): Boolean {
        return System.currentTimeMillis() - lastPacketTime >= pingTimeout
    }

    private fun checkPotions(event: SafeClientEvent): Boolean {
        var count = 0
        for (slot in event.player.allSlotsPrioritized) {
            val stack = slot.stack
            if (stack.isEmpty || stack.item !is ItemSplashPotion) continue
            if (PotionUtils.getEffectsFromStack(stack).any { it.potion == MobEffects.INSTANT_HEALTH }) {
                count += stack.count
            }
        }
        return count < minPotions
    }

    private fun checkShulkers(event: SafeClientEvent): Boolean {
        var count = 0
        for (slot in event.player.allSlotsPrioritized) {
            if (slot.stack.item is ItemShulkerBox) {
                count += slot.stack.count
            }
        }
        return count < minShulkers
    }

    private fun checkArmor(event: SafeClientEvent): Boolean {
        val inv = event.player.inventory
        return inv.armorItemInSlot(3).count < minHelmet ||
                inv.armorItemInSlot(2).count < minChestplate ||
                inv.armorItemInSlot(1).count < minLeggings ||
                inv.armorItemInSlot(0).count < minBoots
    }

    private fun checkCrystals(event: SafeClientEvent): Boolean {
        val maxSelfDamage = CombatManager.crystalMap.values.maxOfOrNull { it.selfDamage } ?: 0.0f
        return CombatUtils.getScaledHealth(event.player as EntityLivingBase) - maxSelfDamage < health.toFloat()
    }

    private fun checkPlayers(event: SafeClientEvent): Boolean {
        for (entity in EntityManager.entity) {
            if (entity !is EntityPlayer || EntityUtils.isFakeOrSelf(entity)) continue
            if (event.player.getDistance(entity) > playerDistance) continue
            if (!friends && FriendManager.isFriend(entity.name)) continue

            handleEvent(event, players, Reasons.PLAYER, entity.name)
            return true
        }
        return false
    }

    private fun isEnemyNearby(event: SafeClientEvent): Boolean {
        for (entity in EntityManager.entity) {
            if (entity !is EntityPlayer || EntityUtils.isFakeOrSelf(entity)) continue
            if (event.player.getDistance(entity) > requirePlayerDistance) continue
            if (FriendManager.isFriend(entity.name)) continue
            return true
        }
        return false
    }

    private fun handleEvent(event: SafeClientEvent, mode: ModuleMode, reason: Reasons, additionalInfo: String = "") {
        if (requirePlayer && !isEnemyNearby(event)) return
        if (mode == ModuleMode.DISABLED) return

        if (mode == ModuleMode.LOGOUT) log(event, reason, additionalInfo)
        else if (mode == ModuleMode.RELOG) relog(event)
    }

    private fun log(event: SafeClientEvent, reason: Reasons, additionalInfo: String) {
        if (kick) {
            sendLog("Disconnected due to $reason ($additionalInfo)")
            event.connection.sendPacket(CPacketPlayer.Position(Double.POSITIVE_INFINITY, 255.0, Double.POSITIVE_INFINITY, true))
        } else {
            event.connection.networkManager.closeChannel(TextComponentString(""))
        }
        if (disableMode == DisableMode.ALWAYS) disable()
    }

    private fun relog(event: SafeClientEvent) {
        event.player.sendChatMessage("/server queue")
        popTimes.clear()
    }

    private fun isEnabled(setting: ModuleMode) = setting != ModuleMode.DISABLED

    private enum class DisableMode { NEVER, ALWAYS, NOT_PLAYER }
    private enum class ModuleMode(override val displayName: CharSequence) : DisplayEnum {
        DISABLED("Disabled"), RELOG("Relog"), LOGOUT("Logout")
    }
    private enum class Reasons { HEALTH, TOTEM_POPS, PLAYER, END_CRYSTAL, POTIONS, SHULKERS, ARMOR, PING_TIMEOUT }
}
