package dev.wizard.meta.module.modules.player

import dev.fastmc.common.TickTimer
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.util.accessor.setClickedItem
import dev.wizard.meta.util.extension.synchronized
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.inventory.ClickType
import net.minecraft.inventory.ContainerPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketClickWindow
import net.minecraft.network.play.client.CPacketConfirmTransaction
import net.minecraft.network.play.server.SPacketConfirmTransaction
import java.util.*
import kotlin.random.Random

object Sync : Module(
    "Sync",
    category = Category.PLAYER,
    description = "Fix inventory desyncs",
    enabledByDefault = true
) {
    private val startDelay by setting(this, IntegerSetting(settingName("Start Delay"), 200, 50..1000, 50))
    private val endTimeout by setting(this, IntegerSetting(settingName("End Timeout"), 300, 50..1000, 50))
    private val interval by setting(this, IntegerSetting(settingName("Interval"), 1500, 50..3000, 50))
    private val forceConfirm by setting(this, BooleanSetting(settingName("Force Confirm"), true))
    private val cancelExtraConfirm by setting(this, BooleanSetting(settingName("Cancel Extra Confirm"), false))

    private val firstPacketTimer = TickTimer()
    private val packetTimer = TickTimer()
    private val sendTimer = TickTimer()
    private var sent = true

    private val craftingItems = Array<Item>(5) { Items.AIR }
    private val illegalStack = ItemStack(Item.getItemFromBlock(Blocks.BARRIER))
    private val randomActionID = TreeSet<Short>().synchronized()

    init {
        onEnable {
            firstPacketTimer.reset()
            packetTimer.reset()
            sendTimer.reset()
            sent = true
        }

        onDisable {
            randomActionID.clear()
            craftingItems.fill(Items.AIR)
        }

        listener<PacketEvent.PostSend> {
            val packet = it.packet
            if (packet is CPacketClickWindow) {
                if (packet.clickedItem == illegalStack) return@listener
                if (sent && packetTimer.tick(endTimeout.toLong())) {
                    firstPacketTimer.reset()
                }
                packetTimer.reset()
                sent = false
            }
        }

        listener<PacketEvent.Receive> {
            val packet = it.packet
            if (forceConfirm && cancelExtraConfirm && packet is SPacketConfirmTransaction && packet.windowId == 0 && !packet.wasAccepted()) {
                if (randomActionID.remove(packet.actionNumber)) {
                    it.cancel()
                }
            }
        }

        safeParallelListener<TickEvent.Post> {
            for (i in 1 until craftingItems.size) {
                val prev = craftingItems[i]
                val curr = player.inventoryContainer.getSlot(i).stack.item
                if (prev != curr) {
                    craftingItems[0] = Items.AIR
                }
                craftingItems[i] = curr
            }

            if (mc.currentScreen is GuiContainer && player.openContainer is ContainerPlayer) {
                val craftOutput = player.openContainer.getSlot(0).stack.item
                if (craftOutput != Items.AIR) {
                    craftingItems[0] = craftOutput
                }
            }

            if (craftingItems[0] != Items.AIR) return@safeParallelListener

            if (!firstPacketTimer.tick(startDelay.toLong())) {
                if (forceConfirm && cancelExtraConfirm) randomActionID.clear()
                return@safeParallelListener
            }

            if (sent && packetTimer.tick(endTimeout.toLong())) return@safeParallelListener
            if (!sendTimer.tickAndReset(interval.toLong())) return@safeParallelListener

            var random = Random.nextInt().toShort()
            val potentialNumber = player.inventoryContainer.getNextTransactionID(player.inventory)
            if (Math.abs(random - potentialNumber) < 1000) {
                random = if (potentialNumber > random) (potentialNumber + 1337).toShort() else (potentialNumber - 1337).toShort()
            }

            val packet = CPacketClickWindow(0, 0, 0, ClickType.PICKUP, ItemStack.EMPTY, random)
            (packet as AccessorCPacketClickWindow).trollSetClickedItem(illegalStack)
            connection.sendPacket(packet)
            sent = true

            if (forceConfirm) {
                connection.sendPacket(CPacketConfirmTransaction(0, random, true))
                if (cancelExtraConfirm) randomActionID.add(random)
            }
        }
    }
}
