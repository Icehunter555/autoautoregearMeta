package dev.wizard.meta.util.inventory

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.manager.managers.InventoryTaskManager
import dev.wizard.meta.module.AbstractModule

fun SafeClientEvent.inventoryTaskNow(block: InventoryTask.Builder.() -> Unit): InventoryTask {
    return InventoryTask.Builder().apply {
        priority(Int.MAX_VALUE)
        delay(0)
        block()
    }.build().also {
        InventoryTaskManager.runNow(this, it)
    }
}

fun AbstractModule.inventoryTask(block: InventoryTask.Builder.() -> Unit): InventoryTask {
    return InventoryTask.Builder().apply {
        priority(this@inventoryTask.modulePriority)
        block()
    }.build().also {
        InventoryTaskManager.addTask(it)
    }
}

val InventoryTask?.executedOrTrue: Boolean
    get() = this == null || this.executed

val InventoryTask?.confirmedOrTrue: Boolean
    get() = this == null || this.confirmed
