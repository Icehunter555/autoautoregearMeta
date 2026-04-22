package dev.wizard.meta.event.events.combat

import dev.wizard.meta.event.Event
import dev.wizard.meta.event.EventBus
import dev.wizard.meta.event.EventPosting
import dev.wizard.meta.util.combat.CrystalDamage

class CrystalSpawnEvent(val entityID: Int, val crystalDamage: CrystalDamage) : Event, EventPosting by Companion {
    companion object : EventBus()
}
