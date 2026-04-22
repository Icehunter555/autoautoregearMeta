package dev.wizard.meta.event.events

import dev.wizard.meta.event.Event
import dev.wizard.meta.event.EventBus
import net.minecraft.block.Block
import net.minecraft.entity.Entity
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos

class AddCollisionBoxEvent(
    val entity: Entity?,
    val entityBox: AxisAlignedBB,
    val pos: BlockPos,
    val block: Block,
    val collidingBoxes: MutableList<AxisAlignedBB>
) : Event {
    override val eventBus: EventBus get() = Companion
    override fun post(event: Any) = Companion.post(event)

    companion object : EventBus()
}
