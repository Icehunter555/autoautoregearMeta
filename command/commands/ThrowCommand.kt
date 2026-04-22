package dev.wizard.meta.command.commands

import dev.wizard.meta.command.BlockArg
import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.command.ItemArg
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.manager.managers.InventoryTaskManager
import dev.wizard.meta.util.inventory.InventoryTask
import dev.wizard.meta.util.inventory.confirmedOrTrue
import dev.wizard.meta.util.inventory.operation.throwAll
import dev.wizard.meta.util.inventory.slot.armorSlots
import dev.wizard.meta.util.inventory.slot.inventorySlots
import dev.wizard.meta.util.inventory.slot.offhandSlot
import dev.wizard.meta.util.text.NoSpamMessage
import net.minecraft.init.Items
import net.minecraft.inventory.Slot
import net.minecraft.item.Item
import net.minecraft.item.ItemShulkerBox
import java.util.concurrent.ConcurrentLinkedQueue

object ThrowCommand : ClientCommand("throw", arrayOf("throwitem"), "Throws all of a specified item or performs metaswap") {

    private var lastTask: InventoryTask? = null
    private var worker: Thread? = null
    private const val DELAY_TICKS = 5L
    private val throwQueue = ConcurrentLinkedQueue<Slot>()
    @Volatile
    private var running = false

    private fun SafeClientEvent.addToThrowList(findSlots: SafeClientEvent.() -> List<Slot>) {
        val found = findSlots()
        if (found.isEmpty()) {
            NoSpamMessage.sendWarning("$chatName No matching items found.")
            return
        }
        throwQueue.addAll(found)
        if (!running) {
            startWorker()
        }
    }

    private fun startWorker() {
        running = true
        worker = Thread {
            while (running) {
                try {
                    if (mc.currentServerData == null || mc.world == null || mc.connection == null) {
                        stop()
                        break
                    }
                    if (throwQueue.isEmpty()) {
                        stop()
                        break
                    }
                    val safeEvent = SafeClientEvent.instance
                    if (safeEvent != null) {
                        if (lastTask.confirmedOrTrue && throwQueue.isNotEmpty()) {
                            val slot = throwQueue.poll()
                            if (slot != null) {
                                val task = InventoryTask.Builder().apply {
                                    priority(Int.MAX_VALUE)
                                    delay(0)
                                    throwAll(slot)
                                    postDelay(DELAY_TICKS)
                                    runInGui()
                                }.build()
                                InventoryTaskManager.runNow(safeEvent, task)
                                lastTask = task
                            }
                        }
                    }
                    Thread.sleep(4L)
                } catch (e: InterruptedException) {
                    break
                } catch (t: Throwable) {
                    NoSpamMessage.sendError("$chatName Error: ${t::class.java.simpleName}")
                }
            }
        }.apply {
            isDaemon = true
            name = "throwall-thread"
            start()
        }
    }

    fun stop() {
        running = false
        worker?.interrupt()
        worker = null
        throwQueue.clear()
    }

    private fun SafeClientEvent.findAllOfType(item: Item): List<Slot> {
        return player.inventorySlots.filter { slot ->
            slot != player.offhandSlot && !player.armorSlots.contains(slot) && slot.stack.item == item
        }
    }

    private fun SafeClientEvent.findAllShulkers(): List<Slot> {
        return player.inventorySlots.filter { slot ->
            slot != player.offhandSlot && !player.armorSlots.contains(slot) && slot.stack.item is ItemShulkerBox
        }
    }

    init {
        item("item") { itemArg ->
            executeSafe {
                addToThrowList { findAllOfType(getValue(itemArg)) }
            }
        }

        block("block") { blockArg ->
            executeSafe {
                val item = Item.getItemFromBlock(getValue(blockArg))
                addToThrowList { findAllOfType(item) }
            }
        }

        literal("Shulkers") {
            executeSafe {
                addToThrowList { findAllShulkers() }
            }
        }

        literal("bed", "beds") {
            executeSafe {
                addToThrowList { findAllOfType(Items.BED) }
            }
        }

        literal("crystal", "crystals") {
            executeSafe {
                addToThrowList { findAllOfType(Items.END_CRYSTAL) }
            }
        }

        literal("cart", "tntcart") {
            executeSafe {
                addToThrowList { findAllOfType(Items.TNT_MINECART) }
            }
        }

        literal("all") {
            executeSafe {
                addToThrowList { player.inventorySlots }
            }
        }
    }
}
