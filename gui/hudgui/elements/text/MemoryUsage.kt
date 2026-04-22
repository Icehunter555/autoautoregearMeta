package dev.wizard.meta.gui.hudgui.elements.text

import dev.wizard.meta.event.*
import dev.wizard.meta.event.events.RunGameLoopEvent
import dev.wizard.meta.gui.hudgui.AbstractHudElement
import dev.wizard.meta.gui.hudgui.LabelHud
import dev.wizard.meta.setting.settings.SettingRegister.Companion.setting
import java.util.*

object MemoryUsage : LabelHud("Memory Usage", category = Category.TEXT, description = "Display the used, allocated and max memory") {

    private val showAllocated by setting(this, "Show Allocated", false)
    private val showMax by setting(this, "Show Max", false)
    private val showAllocations by setting(this, "Show Allocations", false)

    private const val BYTE_TO_MB = 1048576L
    private val allocations = ArrayDeque<AllocationRecord>()
    private var lastUsed = getUsed()

    init {
        listener<RunGameLoopEvent.Start> {
            if (!showAllocations) {
                allocations.clear()
            }
            val current = getUsed()
            if (lastUsed != 0L) {
                val diff = current - lastUsed
                if (diff > 0L) {
                    allocations.add(AllocationRecord(System.nanoTime() + 3000000000L, diff.toFloat() / 1024.0f))
                }
            }
            lastUsed = current
        }
    }

    override fun updateText(event: SafeClientEvent) {
        addText(getUsedMB().toString(), secondary = true)
        if (showAllocated) {
            val allocatedMemory = Runtime.getRuntime().totalMemory() / BYTE_TO_MB
            addText("/ $allocatedMemory", secondary = true)
        }
        if (showAllocations) {
            addTextLine(getAllocationText(), secondary = true)
        }
        if (showMax) {
            val maxMemory = Runtime.getRuntime().maxMemory() / BYTE_TO_MB
            addText(maxMemory.toString(), secondary = true)
        }
        addText("MB")
    }

    private fun getAllocationText(): String {
        val current = System.nanoTime()
        while (allocations.isNotEmpty() && allocations.peek().time < current) {
            allocations.poll()
        }
        if (allocations.isEmpty()) return "(0.0 MB/s)"

        var totalAllocation = 0.0f
        allocations.forEach { totalAllocation += it.allocation }
        val timeLength = (allocations.last.time - allocations.first.time).toFloat() / 1.0E9f
        return "(%.2f MB/s)".format(totalAllocation / Math.max(timeLength, 0.1f) / 1024.0f)
    }

    private fun getUsedMB(): Int = (getUsed() / BYTE_TO_MB).toInt()

    private fun getUsed(): Long = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

    private class AllocationRecord(val time: long, val allocation: Float)
}
