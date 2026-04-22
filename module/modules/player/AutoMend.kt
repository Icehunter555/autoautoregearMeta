package dev.wizard.meta.module.modules.player

import dev.fastmc.common.MathUtilKt
import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.RunGameLoopEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.combat.CrystalSpawnEvent
import dev.wizard.meta.graphics.Easing
import dev.wizard.meta.manager.managers.CombatManager
import dev.wizard.meta.manager.managers.EntityManager
import dev.wizard.meta.manager.managers.HoleManager
import dev.wizard.meta.manager.managers.HotbarSwitchManager
import dev.wizard.meta.manager.managers.InventoryTaskManager
import dev.wizard.meta.manager.managers.PlayerPacketManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.MovementUtils
import dev.wizard.meta.util.combat.CombatUtils
import dev.wizard.meta.util.inventory.InventoryTask
import dev.wizard.meta.util.inventory.getDuraPercentage
import dev.wizard.meta.util.inventory.inventoryTask
import dev.wizard.meta.util.inventory.operation.quickMove
import dev.wizard.meta.util.inventory.operation.pickUp
import dev.wizard.meta.util.inventory.slot.*
import dev.wizard.meta.util.math.vector.Vec2f
import dev.wizard.meta.util.text.NoSpamMessage
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.init.Enchantments
import net.minecraft.init.Items
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketPlayerTryUseItem
import net.minecraft.network.play.server.SPacketSpawnExperienceOrb
import net.minecraft.network.play.server.SPacketSpawnObject
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB

@CombatManager.CombatModule
object AutoMend : Module(
    "AutoEXP",
    category = Category.PLAYER,
    description = "Automatically mends armor",
    modulePriority = 100
) {
    private val minHealth by setting(this, FloatSetting(settingName("Min Health"), 8.0f, 0.0f..20.0f, 0.5f))
    private val targetDurability by setting(this, IntegerSetting(settingName("Target Durability"), 85, 50..100, 1))
    private val takeOffInHole by setting(this, BooleanSetting(settingName("Take Off In Hole"), true))
    private val useCraftingSlot by setting(this, BooleanSetting(settingName("Use Crafting Slot"), true))
    private val slowThreshold by setting(this, FloatSetting(settingName("Slow Threshold"), 0.7f, 0.0f..1.0f, 0.01f))
    private val fastThrow by setting(this, IntegerSetting(settingName("Fast Throw"), 1, 1..10, 1))
    private val minDelay by setting(this, IntegerSetting(settingName("Min Delay"), 10, 0..250, 5))
    private val maxDelay by setting(this, IntegerSetting(settingName("Max Delay"), 50, 0..250, 5))

    private val disableMessageID = Any()
    private val timer = TickTimer()
    private val lastTasks = arrayOfNulls<InventoryTask>(4)
    private val armorSlots = arrayOfNulls<Pair<Slot, ItemStack>>(4)

    private var throwAmount = 0
    private var confirmedAmount = 0
    private var xpDiff = 0
    private var lastBottlePacket = 0L
    private var xpSlot: HotbarSlot? = null
    private var inHole = false

    override fun isActive(): Boolean = isEnabled && (!takeOffInHole || inHole)

    init {
        onEnable {
            val safe = SafeClientEvent.instance ?: return@onEnable
            val noArmor = safe.player.armorSlots.all {
                it.stack.isEmpty || it.stack.itemDamage == 0 || EnchantmentHelper.getEnchantmentLevel(Enchantments.MENDING, it.stack) == 0
            }
            if (noArmor) {
                NoSpamMessage.sendMessage(disableMessageID, "${getChatName()} No armor to repair")
                disable()
            } else {
                updateInHole(safe)
            }
        }

        onDisable {
            lastTasks.fill(null)
            armorSlots.fill(null)
            throwAmount = 0
            confirmedAmount = 0
            xpDiff = 0
            lastBottlePacket = 0L
            xpSlot = null
        }

        safeListener<PacketEvent.Receive> { event ->
            val packet = event.packet
            if (packet is SPacketSpawnObject) {
                if (packet.type == 75 && player.getDistanceSq(packet.x, packet.y, packet.z) < 5.0) {
                    confirmedAmount--
                    lastBottlePacket = System.currentTimeMillis()
                }
            } else if (packet is SPacketSpawnExperienceOrb && player.getDistanceSq(packet.x, packet.y, packet.z) < 5.0) {
                if (player.armorInventoryList.all { it.itemDamage <= 100 }) {
                    xpDiff += 11 - packet.xpValue
                    if (xpDiff > 0) {
                        while (xpDiff >= 11) {
                            throwAmount++
                            confirmedAmount++
                            xpDiff -= 11
                        }
                    } else {
                        while (xpDiff <= -11) {
                            throwAmount--
                            confirmedAmount--
                            xpDiff += 11
                        }
                    }
                }
            }
        }

        safeListener<CrystalSpawnEvent> {
            if (CombatUtils.getScaledHealth(player as EntityLivingBase) - it.crystalDamage.selfDamage <= minHealth) {
                NoSpamMessage.sendMessage(disableMessageID, "${getChatName()} Lethal crystal nearby")
                disable()
            }
        }

        safeListener<TickEvent.Pre> {
            if (!preCheck(this)) return@safeListener
            updateThrowAmount()
            PlayerPacketManager.sendPlayerPacket {
                rotate(Vec2f(player.rotationYaw, 90.0f))
            }
        }

        safeConcurrentListener<TickEvent.Post> {
            updateInHole(this)
        }

        safeListener<RunGameLoopEvent.Start> {
            if (!preCheck(this)) return@safeListener
            findAndMoveArmor(this)
        }

        safeListener<RunGameLoopEvent.Render> {
            if (!preCheck(this)) return@safeListener
            val slot = xpSlot ?: return@safeListener

            if (throwAmount > 0 && PlayerPacketManager.rotation.y > 85.0f && timer.tick(minDelay.toLong())) {
                if (lastTasks.all { it == null || it.isExecuted }) {
                    val notNullArmor = armorSlots.filterNotNull()
                    if (notNullArmor.any { it.second.getDuraPercentage() >= targetDurability }) return@safeListener

                    val maxDura = notNullArmor.maxOfOrNull { it.second.getDuraPercentage() } ?: return@safeListener
                    val threshold = targetDurability * slowThreshold

                    if (maxDura >= threshold) {
                        val delay = Easing.OUT_CUBIC.inc(maxDura / targetDurability, minDelay.toFloat(), maxDelay.toFloat()).toInt()
                        if (timer.tickAndReset(delay.toLong())) {
                            HotbarSwitchManager.ghostSwitch(this, slot) {
                                connection.sendPacket(CPacketPlayerTryUseItem(EnumHand.MAIN_HAND))
                                throwAmount--
                            }
                        }
                    } else if (timer.tickAndReset(minDelay.toLong())) {
                        HotbarSwitchManager.ghostSwitch(this, slot) {
                            repeat(fastThrow) {
                                if (throwAmount > 0) {
                                    connection.sendPacket(CPacketPlayerTryUseItem(EnumHand.MAIN_HAND))
                                    throwAmount--
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun preCheck(event: SafeClientEvent): Boolean {
        if (CombatUtils.getScaledHealth(event.player as EntityLivingBase) <= minHealth) {
            NoSpamMessage.sendMessage(disableMessageID, "${getChatName()} Low health")
            disable()
            return false
        }
        if (!checkNearbyPlayers(event)) {
            NoSpamMessage.sendMessage(disableMessageID, "${getChatName()} Players nearby")
            disable()
            return false
        }
        val slot = findXp(event)
        if (slot == null) {
            NoSpamMessage.sendMessage(disableMessageID, "${getChatName()} No xp bottle found in hotbar")
            disable()
            return false
        }
        xpSlot = slot
        updateArmorSlots(event)
        if (checkFinished()) {
            NoSpamMessage.sendMessage(disableMessageID, "${getChatName()} Finished mending armors")
            disable()
            return false
        }
        return true
    }

    private fun checkNearbyPlayers(event: SafeClientEvent): Boolean {
        val box = AxisAlignedBB(event.player.posX - 0.5, event.player.posY - 0.5, event.player.posZ - 0.5, event.player.posX + 0.5, event.player.posY + 2.5, event.player.posZ + 0.5)
        return EntityManager.players.none { !EntityUtils.isFakeOrSelf(it) && it.entityBoundingBox.intersects(box) }
    }

    private fun updateArmorSlots(event: SafeClientEvent) {
        event.player.armorSlots.forEachIndexed { index, slot ->
            val stack = slot.stack
            if (!stack.isEmpty && isRepairable(stack)) {
                armorSlots[index] = slot to stack
            } else {
                armorSlots[index] = null
            }
        }
    }

    private fun isRepairable(stack: ItemStack): Boolean = stack.isItemDamaged && EnchantmentHelper.getEnchantmentLevel(Enchantments.MENDING, stack) > 0

    private fun findXp(event: SafeClientEvent): HotbarSlot? = event.player.hotbarSlots.firstItem(Items.EXPERIENCE_BOTTLE)

    private fun checkFinished(): Boolean = armorSlots.all { it == null || it.second.getDuraPercentage() >= targetDurability }

    private fun findAndMoveArmor(event: SafeClientEvent) {
        val count = armorSlots.filterNotNull().count()
        val repairable = armorSlots.filterNotNull().filter { it.second.getDuraPercentage() < targetDurability }.sortedBy { it.second.itemDamage }

        if (takeOffInHole && !inHole && repairable.isNotEmpty()) {
            NoSpamMessage.sendMessage(disableMessageID, "${getChatName()} Finished mending armors")
            disable()
        } else if (count > 1) {
            var emptySlots = event.player.inventorySlots.countEmpty()
            if (useCraftingSlot) emptySlots += event.player.craftingSlots.countEmpty()

            if (emptySlots >= repairable.size - 1) {
                for (pair in repairable) {
                    if (!moveArmor(event, pair.first)) return
                }
            } else {
                NoSpamMessage.sendMessage(disableMessageID, "${getChatName()} No empty slot for moving armor")
                disable()
            }
        }
    }

    private fun moveArmor(event: SafeClientEvent, slot: Slot): Boolean {
        val index = slot.slotIndex - 5
        if (lastTasks[index]?.isExecuted != false) {
            if (event.player.inventorySlots.hasEmpty()) {
                lastTasks[index] = inventoryTask {
                    priority(getModulePriority())
                    quickMove(slot)
                }.also { InventoryTaskManager.addTask(it) }
            } else if (useCraftingSlot) {
                val emptyCrafting = event.player.craftingSlots.firstEmpty()
                if (emptyCrafting != null) {
                    lastTasks[index] = inventoryTask {
                        priority(getModulePriority())
                        pickUp(slot)
                        pickUp(emptyCrafting)
                    }.also { InventoryTaskManager.addTask(it) }
                } else {
                    NoSpamMessage.sendMessage(disableMessageID, "${getChatName()} No empty slot for moving armor")
                    disable()
                    return false
                }
            } else {
                NoSpamMessage.sendMessage(disableMessageID, "${getChatName()} No empty slot for moving armor")
                disable()
                return false
            }
        }
        return true
    }

    private fun updateThrowAmount() {
        if (throwAmount <= 0 && confirmedAmount <= 0) {
            val pair = getLeastAndMostDamaged() ?: return
            val least = pair.first
            val most = pair.second

            val targetMax = (most.maxDamage * (1.0f - targetDurability.toFloat() / 100.0f)).toInt()
            val reqMin = least.itemDamage / 22
            val reqMax = MathUtilKt.ceilToInt((most.itemDamage - targetMax) / 22.0f)

            val minReq = Math.min(reqMin, reqMax)
            throwAmount = minReq
            confirmedAmount = minReq
            xpDiff = 0
            lastBottlePacket = System.currentTimeMillis()
        } else if (throwAmount <= 0 && System.currentTimeMillis() - lastBottlePacket > 100L) {
            throwAmount = 0
            confirmedAmount = 0
        }
    }

    private fun getLeastAndMostDamaged(): Pair<ItemStack, ItemStack>? {
        var least: ItemStack? = null
        var minDmg = Int.MAX_VALUE
        var most: ItemStack? = null
        var maxDmg = Int.MIN_VALUE

        for (pair in armorSlots) {
            val stack = pair?.second ?: continue
            if (stack.itemDamage < minDmg) {
                least = stack
                minDmg = stack.itemDamage
            }
            if (stack.itemDamage > maxDmg) {
                most = stack
                maxDmg = stack.itemDamage
            }
        }
        return if (least != null && most != null) least to most else null
    }

    private fun updateInHole(event: SafeClientEvent) {
        val holeInfo = HoleManager.getHoleInfo(event.player)
        inHole = holeInfo.isHole || (MovementUtils.isCentered(event.player, EntityUtils.getBetterPosition(event.player)) && event.world.getBlockState(EntityUtils.getBetterPosition(event.player)).getCollisionBoundingBox(event.world, EntityUtils.getBetterPosition(event.player)) != null)
    }
}
