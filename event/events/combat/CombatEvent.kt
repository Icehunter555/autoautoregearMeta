package dev.wizard.meta.event.events.combat

import dev.wizard.meta.event.Event
import dev.wizard.meta.event.EventBus
import dev.wizard.meta.event.EventPosting
import net.minecraft.entity.EntityLivingBase

sealed class CombatEvent : Event {
    abstract val entity: EntityLivingBase?

    class UpdateTarget(val prevEntity: EntityLivingBase?, override val entity: EntityLivingBase?) : CombatEvent(), EventPosting by Companion {
        companion object : EventBus()
    }
}
