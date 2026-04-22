package dev.wizard.meta.module.modules.player

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.util.EntityUtils
import net.minecraft.inventory.EntityEquipmentSlot
import java.util.*

object ArmorWarner : Module(
    "ArmorWarner",
    category = Category.PLAYER,
    description = "Warn your friends when they have low armor"
) {
    private val armorThreshold by setting(this, IntegerSetting(settingName("Armor Amount Threshold"), 20, 1..64, 1))
    private val scanDelay by setting(this, IntegerSetting(settingName("Scan Delay"), 3, 1..10, 1, description = "Seconds between scans"))
    private val messageDelay by setting(this, IntegerSetting(settingName("Message Delay"), 3, 1..10, 1, description = "Seconds between messages"))

    private val warnedPlayers = LinkedHashSet<String>()
    private val messageQueue = LinkedList<String>()
    private val scanTimer = TickTimer(TimeUnit.SECONDS)
    private val messageTimer = TickTimer(TimeUnit.SECONDS)

    private val slotNames = mapOf(
        EntityEquipmentSlot.HEAD to "helmets",
        EntityEquipmentSlot.CHEST to "chestplates",
        EntityEquipmentSlot.LEGS to "leggings",
        EntityEquipmentSlot.FEET to "boots"
    )

    init {
        onEnable {
            warnedPlayers.clear()
            messageQueue.clear()
        }

        onDisable {
            warnedPlayers.clear()
            messageQueue.clear()
        }

        safeListener<TickEvent.Post> {
            if (scanTimer.tickAndReset(scanDelay.toLong())) {
                for (friend in world.playerEntities) {
                    if (!EntityUtils.isFriend(friend) || EntityUtils.isSelf(friend) || friend.isDead || EntityUtils.isNaked(friend)) continue

                    val armorToWarn = EntityUtils.getArmorCounts(friend).filter { it.value < armorThreshold }

                    if (armorToWarn.isNotEmpty()) {
                        if (warnedPlayers.contains(friend.name)) continue
                        val message = buildArmorWarningMessage(armorToWarn)
                        messageQueue.add("/w ${friend.name} $message")
                        warnedPlayers.add(friend.name)
                    } else {
                        warnedPlayers.remove(friend.name)
                    }
                }
            }

            if (messageQueue.isNotEmpty() && messageTimer.tickAndReset(messageDelay.toLong())) {
                player.sendChatMessage(messageQueue.poll()!!)
            }
        }
    }

    private fun buildArmorWarningMessage(lowArmor: Map<EntityEquipmentSlot, Int>): String {
        val sortedArmor = lowArmor.entries.sortedBy {
            when (it.key) {
                EntityEquipmentSlot.HEAD -> 0
                EntityEquipmentSlot.CHEST -> 1
                EntityEquipmentSlot.LEGS -> 2
                EntityEquipmentSlot.FEET -> 3
                else -> 4
            }
        }

        val armorParts = sortedArmor.mapNotNull { entry ->
            slotNames[entry.key]?.let { "${entry.value} $it" }
        }

        val armorList = when (armorParts.size) {
            0 -> ""
            1 -> armorParts[0]
            2 -> "${armorParts[0]} and ${armorParts[1]}"
            else -> armorParts.dropLast(1).joinToString(", ") + ", and " + armorParts.last()
        }

        return "You are low on armor! Only $armorList left!"
    }
}
