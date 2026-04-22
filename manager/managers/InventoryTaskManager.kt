package dev.wizard.meta.manager.managers

import dev.fastmc.common.TickTimer
import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.RunGameLoopEvent
import dev.wizard.meta.event.events.WorldEvent
import dev.wizard.meta.manager.Manager
import dev.wizard.meta.util.TpsCalculator
import dev.wizard.meta.util.inventory.ClickFuture
import dev.wizard.meta.util.inventory.InventoryTask
import dev.wizard.meta.util.inventory.StepFuture
import dev.wizard.meta.util.inventory.UtilsKt.removeHoldingItem
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.network.play.server.SPacketConfirmTransaction
import java.util.*

object InventoryTaskManager : Manager() {
    private val confirmMap = Short2ObjectOpenHashMap<ClickFuture>()
    private val taskQueue = PriorityQueue<InventoryTask>()
    private val timer = TickTimer()
    private var lastTask: InventoryTask? = null
    private val queueLock = Any()

    fun addTask(task: InventoryTask) {
        synchronized(queueLock) {
            taskQueue.add(task)
        }
    }

    fun runNow(event: SafeClientEvent, task: InventoryTask) {
        synchronized(this) {
            if (!event.player.inventory.itemStack.isEmpty) {
                event.removeHoldingItem()
            }
            while (!task.finished) {
                task.runTask(event)?.let {
                    handleFuture(it)
                }
            }
            timer.reset((task.postDelay * TpsCalculator.multiplier).toLong())
        }
    }

    private fun lastTaskOrNext(safeClientEvent: SafeClientEvent): InventoryTask? {
        var task = lastTask
        if (task == null) {
            val newTask = synchronized(queueLock) {
                taskQueue.poll()?.also {
                    lastTask = it
                }
            } ?: return null
            
            if (!safeClientEvent.player.inventory.itemStack.isEmpty) {
                safeClientEvent.removeHoldingItem()
                return null
            }
            task = newTask
        }
        return task
    }

    private fun runTask(safeClientEvent: SafeClientEvent, task: InventoryTask) {
        synchronized(this) {
            if (safeClientEvent.minecraft.currentScreen is GuiContainer && !task.runInGui && !safeClientEvent.player.inventory.itemStack.isEmpty) {
                timer.reset(500L)
                return
            }
            if (task.delay == 0L) {
                runNow(safeClientEvent, task)
            } else {
                task.runTask(safeClientEvent)?.let {
                    handleFuture(it)
                    timer.reset((task.delay * TpsCalculator.multiplier).toLong())
                }
            }
            if (task.finished) {
                timer.reset((task.postDelay * TpsCalculator.multiplier).toLong())
                lastTask = null
            }
        }
    }

    private fun handleFuture(future: StepFuture) {
        if (future is ClickFuture) {
            synchronized(queueLock) {
                confirmMap[future.id] = future
            }
        }
    }

    private fun reset() {
        synchronized(queueLock) {
            timer.time = 0L
            confirmMap.clear()
            lastTask?.cancel()
            lastTask = null
            taskQueue.forEach { it.cancel() }
            taskQueue.clear()
        }
    }

    init {
        listener<PacketEvent.Receive> { event ->
            val packet = event.packet
            if (packet is SPacketConfirmTransaction) {
                synchronized(queueLock) {
                    confirmMap.remove(packet.actionNumber)?.confirm()
                }
            }
        }

        listener<RunGameLoopEvent.Render> {
            val safeClientEvent = SafeClientEvent.instance ?: return@listener
            if (lastTask == null && taskQueue.isEmpty()) {
                InventoryTask.resetIdCounter()
                return@listener
            }
            if (!timer.tick(0L)) return@listener
            
            lastTaskOrNext(safeClientEvent)?.let {
                runTask(safeClientEvent, it)
            }
        }

        listener<WorldEvent.Unload> {
            reset()
        }
    }
}
