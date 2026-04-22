package dev.wizard.meta.util.inventory

import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.util.delegate.ComputeFlag
import dev.wizard.meta.util.interfaces.Helper
import dev.wizard.meta.util.threads.onMainThreadSafe
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class InventoryTask private constructor(
    private val id: Int,
    val priority: Int,
    val delay: Long,
    val postDelay: Long,
    val timeout: Long,
    val runInGui: Boolean,
    private val clicks: Array<Step>
) : Comparable<InventoryTask>, Helper {

    val finished: Boolean by ComputeFlag { cancelled || finishTime != -1L }
    val executed: Boolean by ComputeFlag { cancelled || (finished && System.currentTimeMillis() - finishTime > postDelay) }
    val confirmed: Boolean by ComputeFlag {
        if (cancelled) true
        else if (!executed) false
        else futures.all { it?.timeout(timeout) ?: true }
    }

    private val futures = arrayOfNulls<StepFuture>(clicks.size)
    private var finishTime = -1L
    private var index = 0
    private var cancelled = false

    fun runTask(event: SafeClientEvent): StepFuture? {
        if (cancelled || index >= clicks.size) return null
        val currentIndex = index++
        val future = clicks[currentIndex].run(event)
        futures[currentIndex] = future
        if (index >= clicks.size && finishTime == -1L) {
            onMainThreadSafe { playerController?.updateController() }
            finishTime = System.currentTimeMillis()
        }
        return future
    }

    fun cancel() {
        cancelled = true
    }

    override fun compareTo(other: InventoryTask): Int {
        val result = other.priority.compareTo(this.priority)
        return if (result != 0) result else this.id.compareTo(other.id)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InventoryTask) return false
        return id == other.id
    }

    override fun hashCode(): Int = id

    class Builder {
        private val clicks = mutableListOf<Step>()
        private var priority = 0
        private var delay = 0L
        private var postDelay = 50L
        private var timeout = 3000L
        private var runInGui = false

        fun priority(value: Int) { this.priority = value }

        fun delay(value: Int, timeUnit: TimeUnit = TimeUnit.MILLISECONDS) { this.delay = value.toLong() * timeUnit.multiplier }
        fun delay(value: Long, timeUnit: TimeUnit = TimeUnit.MILLISECONDS) { this.delay = value * timeUnit.multiplier }

        fun postDelay(value: Int, timeUnit: TimeUnit = TimeUnit.MILLISECONDS) { this.postDelay = value.toLong() * timeUnit.multiplier }
        fun postDelay(value: Long, timeUnit: TimeUnit = TimeUnit.MILLISECONDS) { this.postDelay = value * timeUnit.multiplier }

        fun timeout(value: Int, timeUnit: TimeUnit = TimeUnit.MILLISECONDS) { this.timeout = value.toLong() * timeUnit.multiplier }
        fun timeout(value: Long, timeUnit: TimeUnit = TimeUnit.MILLISECONDS) { this.timeout = value * timeUnit.multiplier }

        fun runInGui() { this.runInGui = true }

        operator fun Step.unaryPlus() { clicks.add(this) }

        fun build(): InventoryTask {
            return InventoryTask(idCounter.getAndIncrement(), priority, delay, postDelay, timeout, runInGui, clicks.toTypedArray())
        }
    }

    companion object {
        private val idCounter = AtomicInteger(Int.MIN_VALUE)

        fun resetIdCounter() {
            idCounter.set(Int.MIN_VALUE)
        }
    }
}
