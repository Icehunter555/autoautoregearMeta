package dev.wizard.meta.event.events

import dev.wizard.meta.event.Event
import dev.wizard.meta.event.EventBus
import net.minecraft.util.math.BlockPos

class BlockBreakEvent(
    val breakerID: Int,
    val position: BlockPos,
    val progress: Int
) : Event {
    override val eventBus: EventBus get() = Companion
    override fun post(event: Any) = Companion.post(event)

    companion object : EventBus()
}
