package dev.wizard.meta.event.events

import dev.wizard.meta.event.Event
import dev.wizard.meta.event.EventBus
import dev.wizard.meta.event.EventPosting
import net.minecraft.entity.EntityLivingBase

sealed class EntityEvent(val entity: EntityLivingBase) : Event {
    class Death(entity: EntityLivingBase) : EntityEvent(entity), EventPosting by Companion {
        companion object : EventBus()
    }

    class UpdateHealth(entity: EntityLivingBase, val prevHealth: Float, val health: Float) : EntityEvent(entity), EventPosting by Companion {
        companion object : EventBus()
    }
}
