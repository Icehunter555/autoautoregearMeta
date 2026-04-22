package dev.wizard.meta.module.modules.player

import dev.fastmc.common.TickTimer
import dev.fastmc.common.collection.CircularArray
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.InputEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.ProcessKeyBindEvent
import dev.wizard.meta.manager.managers.EntityManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.accessor.onItemUseFinish
import dev.wizard.meta.util.accessor.syncCurrentPlayItem
import dev.wizard.meta.util.math.vector.distanceSqTo
import dev.wizard.meta.util.threads.onMainThreadSafe
import net.minecraft.client.settings.KeyBinding
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Items
import net.minecraft.init.SoundEvents
import net.minecraft.item.ItemFood
import net.minecraft.network.play.client.CPacketPlayerDigging
import net.minecraft.network.play.client.CPacketPlayerTryUseItem
import net.minecraft.network.play.server.SPacketEntityStatus
import net.minecraft.network.play.server.SPacketSoundEffect
import net.minecraft.network.play.server.SPacketUpdateHealth
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.BlockPos

object BetterEat : Module(
    "BetterEat",
    category = Category.PLAYER,
    description = "Optimize eating"
) {
    private val toggleEat by setting(this, BooleanSetting(settingName("Toggle Eat"), false))
    private val displayEatDelay by setting(this, BooleanSetting(settingName("Display Eat Delay"), false))
    private val delay by setting(this, IntegerSetting(settingName("Delay"), 50, 0..500, 5))

    private val eatTimeArray = CircularArray<Int>(5)
    private val eatTimer = TickTimer()
    private val doubleClickTimer = TickTimer()
    private val spamTimer = TickTimer()

    private var toggled = false
    private var eating = false
    private var lastEatTime = 0L
    private var slot = -1

    override fun getHudInfo(): String = if (displayEatDelay) "${eatTimeArray.average().toInt()} ms" else ""

    @JvmStatic
    fun shouldCancelStopUsingItem(): Boolean {
        if (!INSTANCE.isEnabled) return false
        val player = mc.player ?: return false
        return isEating(player)
    }

    @JvmStatic
    private fun isEating(player: net.minecraft.client.entity.EntityPlayerSP): Boolean {
        if (!player.isHandActive) return false
        val item = player.activeItemStack.item
        return item is ItemFood || item == Items.POTIONITEM
    }

    private fun isValidItem(item: net.minecraft.item.Item): Boolean = item is ItemFood || item == Items.POTIONITEM

    init {
        onDisable {
            eatTimeArray.clear()
            eatTimer.reset(-69420L)
            toggled = false
            eating = false
            lastEatTime = 0L
            slot = -1
        }

        safeListener<PacketEvent.Receive> { event ->
            val packet = event.packet
            if (packet is SPacketEntityStatus) {
                if (eating && packet.opCode.toInt() == 9) {
                    if (packet.getEntity(world) == player) {
                        event.cancel()
                        onMainThreadSafe {
                            if (isEating(player)) {
                                val hand = player.activeHand
                                player.onItemUseFinish()
                                connection.sendPacket(CPacketPlayerTryUseItem(hand))
                            }
                        }
                    }
                }
            } else if (packet is SPacketUpdateHealth) {
                if (packet.health >= player.health && packet.foodLevel >= player.foodStats.foodLevel) {
                    eatTimer.reset()
                }
            } else if (packet is SPacketSoundEffect && packet.category == SoundCategory.PLAYERS && packet.sound == SoundEvents.ENTITY_PLAYER_BURP) {
                if (player.getDistanceSq(packet.x, packet.y, packet.z) <= 2.0 && (!eatTimer.tick(1L) || (!eatTimer.tick(25L) && checkPlayers(this)))) {
                    val current = System.currentTimeMillis()
                    eatTimeArray.add((current - lastEatTime).toInt())
                    lastEatTime = current
                    eatTimer.reset(-69420L)
                }
            }
        }

        safeListener<InputEvent.Mouse> {
            if (toggleEat && it.button == 1 && !it.state) {
                if (doubleClickTimer.tickAndReset(250L)) {
                    toggled = !toggled
                    if (toggled) slot = player.inventory.currentItem
                } else {
                    toggled = false
                    mc.playerController.syncCurrentPlayItem()
                    connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                    player.stopActiveHand()
                }
            }
        }

        safeListener<ProcessKeyBindEvent.Pre> {
            val flag = player.inventory.currentItem == slot && (!player.isHandActive || player.activeItemStack.item == Items.GOLDEN_APPLE) && EnumHand.values().any { isValidItem(player.getHeldItem(it).item) }

            if (toggleEat) {
                if (flag) {
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, toggled)
                } else if (toggled) {
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, false)
                    slot = -1
                }
            }
            toggled = toggled && flag
        }

        safeListener<ProcessKeyBindEvent.Post> {
            val playerEating = isEating(player)
            eating = mc.gameSettings.keyBindUseItem.isKeyDown && playerEating

            if (eating && spamTimer.tickAndReset(delay.toLong())) {
                connection.sendPacket(CPacketPlayerTryUseItem(player.activeHand))
            }

            if (!playerEating || player.activeItemStack.item !is ItemFood) {
                lastEatTime = System.currentTimeMillis()
            }
        }

        toggleEat.valueListeners.add { _, it -> if (!it) toggled = false }
    }

    private fun checkPlayers(event: SafeClientEvent): Boolean {
        return EntityManager.players.none { it.isEntityAlive && !EntityUtils.isFakeOrSelf(it) && it.entityBoundingBox.intersects(event.player.entityBoundingBox) }
    }
}
