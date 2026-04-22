package dev.wizard.meta.event.events.combat

import dev.wizard.meta.event.Event
import dev.wizard.meta.event.EventBus
import dev.wizard.meta.event.EventPosting
import net.minecraft.entity.item.EntityEnderCrystal

class CrystalSetDeadEvent(
    val x: Double,
    val y: Double,
    val z: Double,
    val crystals: List<EntityEnderCrystal>
) : Event, EventPosting by Companion {
    companion object : EventBus()
}
