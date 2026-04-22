package dev.wizard.meta.manager.managers

import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.ListenerKt.safeListener
import dev.wizard.meta.event.events.EntityEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.WorldEvent
import dev.wizard.meta.event.events.combat.TotemPopEvent
import dev.wizard.meta.manager.Manager
import dev.wizard.meta.mixins.accessor.entity.AccessorEntityLivingBase
import dev.wizard.meta.util.accessor.NetworkKt.getEntityID
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.server.SPacketAnimation
import net.minecraft.network.play.server.SPacketEntityMetadata
import net.minecraft.network.play.server.SPacketEntityStatus

object HealthManager : Manager() {
    private val trackerMap: Int2ObjectMap<Tracker> = Int2ObjectMaps.synchronize(Int2ObjectOpenHashMap<Tracker>())

    fun getTracker(entity: EntityPlayer): Tracker {
        return trackerMap.computeIfAbsent(entity.entityId) {
            Tracker(entity)
        }
    }

    init {
        listener<WorldEvent.Entity.Remove>(alwaysListening = true) {
            trackerMap.remove(it.entity.entityId)
        }

        listener<TotemPopEvent.Pop>(alwaysListening = true) {
            val tracker = getTracker(it.entity)
            tracker.lastDamage += tracker.health
        }

        safeListener<PacketEvent.Receive>(alwaysListening = true) { event ->
            val packet = event.packet
            if (packet is SPacketAnimation) {
                if (packet.animationType == 1) {
                    trackerMap.get(packet.entityID)?.let {
                        it.hurtTime = System.currentTimeMillis()
                    }
                }
            } else if (packet is SPacketEntityStatus) {
                when (packet.opCode) {
                    2.toByte(), 33.toByte(), 36.toByte(), 37.toByte() -> {
                        trackerMap.get(packet.getEntityId())?.let {
                            it.hurtTime = System.currentTimeMillis()
                        }
                    }
                }
            } else if (packet is SPacketEntityMetadata) {
                val dataManagerEntries = packet.dataManagerEntries ?: return@safeListener
                val entity = world.getEntityByID(packet.entityId) as? EntityPlayer ?: return@safeListener
                val tracker = getTracker(entity)
                
                val healthEntry = dataManagerEntries.find { entry ->
                    entry.isDirty && entry.key == runCatching { AccessorEntityLivingBase.trollGetHealthDataKey() }.getOrNull()
                } ?: return@safeListener

                val health = healthEntry.value as? Float ?: return@safeListener
                val prevHealth = tracker.health
                val diff = prevHealth - health
                
                if (diff > 0.0f) {
                    if (diff > 2.0f || TotemPopManager.getTracker(entity)?.let { System.currentTimeMillis() - it.popTime > 10L } != false) {
                        tracker.lastDamage += diff
                    }
                }
                
                EntityEvent.UpdateHealth(entity, prevHealth, health).post()
                tracker.health = health
            }
        }
    }

    class Tracker(val entity: EntityPlayer) {
        var health: Float = entity.health
        var lastDamage: Float = 0.0f
        var hurtTime: Long = 0L
            set(value) {
                lastDamage = 0.0f
                field = value
            }

        init {
            if (entity.hurtTime != 0) {
                hurtTime = System.currentTimeMillis() - entity.hurtTime * 50L
            }
        }
    }
}
