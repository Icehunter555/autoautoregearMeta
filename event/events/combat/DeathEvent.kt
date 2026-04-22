package dev.wizard.meta.event.events.combat

import dev.wizard.meta.event.Event
import dev.wizard.meta.event.EventBus
import dev.wizard.meta.event.EventPosting
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.math.BlockPos

class DeathEvent(val entity: EntityLivingBase?, val pos: BlockPos) : Event, EventPosting by Companion {
    companion object : EventBus()
}
