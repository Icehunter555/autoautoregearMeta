package dev.wizard.meta.module.modules.beta

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.ModuleManager
import dev.wizard.meta.module.modules.client.Kit
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.BindSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.util.Bind
import dev.wizard.meta.util.inventory.slot.getAllSlots
import dev.wizard.meta.util.text.MessageSendHelper
import net.minecraft.item.ItemStack
import org.lwjgl.input.Keyboard

object AutomaticallyRegear : Module(
    "AutomaticallyRegear",
    category = Category.BETA,
    description = "Automatically checks kit inventory and presses regear keybind when items are low",
    modulePriority = 2000
) {
    private val threshold by setting(this, IntegerSetting(settingName("Threshold"), 5, 1..64, 1, 
        description = "Minimum item count before triggering regear"))
    
    private val checkDelay by setting(this, IntegerSetting(settingName("Check Delay"), 20, 1..100, 1,
        description = "Ticks between inventory checks"))
    
    private val regearKeybind by setting(this, BindSetting(settingName("Regear Keybind"), Bind(Keyboard.KEY_R),
        description = "Keybind to press when regear is needed"))
    
    private val autoPress by setting(this, BooleanSetting(settingName("Auto Press"), true,
        description = "Automatically press the keybind when items are low"))
    
    private val notifyChat by setting(this, BooleanSetting(settingName("Notify Chat"), true,
        description = "Send chat notification when regear is triggered"))
    
    private val checkTimer = TickTimer()
    private var lastRegearTime = 0L
    private val regearCooldown = 5000L // 5 seconds cooldown between regear triggers
    
    init {
        safeListener<TickEvent.Post> {
            if (!checkTimer.tickAndReset(checkDelay.toLong(), TimeUnit.TICKS)) return@safeListener
            
            // Check if enough time has passed since last regear
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastRegearTime < regearCooldown) return@safeListener
            
            // Get the current kit
            val kitItems = Kit.getKitItemArray() ?: run {
                if (debug) MessageSendHelper.sendChatMessage("No kit selected")
                return@safeListener
            }
            
            // Check inventory for low items
            if (needsRegear(kitItems)) {
                if (notifyChat) {
                    MessageSendHelper.sendChatMessage("§e[AutomaticallyRegear] §cItems below threshold! Triggering regear...")
                }
                
                if (autoPress) {
                    triggerRegear()
                }
                
                lastRegearTime = currentTime
            }
        }
    }
    
    private fun SafeClientEvent.needsRegear(kitItems: Array<Kit.ItemEntry>): Boolean {
        val playerSlots = player.getAllSlots()
        
        // Count each item type in the kit
        val kitItemCounts = mutableMapOf<Kit.ItemEntry, Int>()
        for (kitItem in kitItems) {
            if (kitItem == Kit.ItemEntry.EMPTY) continue
            kitItemCounts[kitItem] = kitItemCounts.getOrDefault(kitItem, 0) + 1
        }
        
        // Count each item type in player inventory
        for ((kitItem, requiredCount) in kitItemCounts) {
            var foundCount = 0
            
            for (slot in playerSlots) {
                val stack = slot.stack
                if (stack.isEmpty) continue
                
                if (kitItem == Kit.ItemEntry.fromStack(stack)) {
                    foundCount += stack.count
                }
            }
            
            // If any item is below threshold, we need to regear
            if (foundCount < threshold) {
                if (debug) {
                    MessageSendHelper.sendChatMessage("§7Item ${kitItem.item.registryName} count: $foundCount < $threshold")
                }
                return true
            }
        }
        
        return false
    }
    
    private fun triggerRegear() {
        // Trigger the AutoRegear module if it exists
        val autoRegear = ModuleManager.getModuleOrNull("AutoRegear")
        if (autoRegear != null) {
            // Trigger the regear by setting the placeShulker flag
            if (autoRegear is dev.wizard.meta.module.modules.combat.AutoRegear) {
                autoRegear.placeShulker = true
                if (debug) {
                    MessageSendHelper.sendChatMessage("§7Triggered AutoRegear module")
                }
            }
        } else {
            // Fallback: just notify the user to press the keybind manually
            val key = regearKeybind.value.getKey()
            if (key > 0) {
                if (debug) {
                    MessageSendHelper.sendChatMessage("§7Please press keybind: ${Keyboard.getKeyName(key)}")
                }
            } else if (key < 0) {
                if (debug) {
                    MessageSendHelper.sendChatMessage("§7Please press mouse button: ${-key - 1}")
                }
            }
        }
    }
}
