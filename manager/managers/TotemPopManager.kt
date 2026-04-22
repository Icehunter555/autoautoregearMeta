package dev.wizard.meta.manager.managers

import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.ListenerKt.parallelListener
import dev.wizard.meta.event.ListenerKt.safeListener
import dev.wizard.meta.event.events.ConnectionEvent
import dev.wizard.meta.event.events.EntityEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.combat.TotemPopEvent
import dev.wizard.meta.manager.Manager
import dev.wizard.meta.util.accessor.NetworkKt.getEntityID
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.server.SPacketEntityStatus

object TotemPopManager : Manager() {
    private val trackerMap: Int2ObjectMap<Tracker> = Int2ObjectMaps.synchronize(Int2ObjectOpenHashMap<Tracker>())

    fun getPopCount(entity: Entity): Int {
        return trackerMap.get(entity.entityId)?.count ?: 0
    }

    fun getTracker(entity: Entity): Tracker? {
        return trackerMap.get(entity.entityId)
    }

    init {
        listener<ConnectionEvent.Disconnect>(alwaysListening = true) {
            trackerMap.clear()
        }

        listener<EntityEvent.Death>(priority = 5000) { event ->
            val entity = event.entity
            if (entity !is EntityPlayer) return@listener
            
            if (entity == mc.player) {
                trackerMap.clear()
            } else {
                trackerMap.remove(entity.entityId)?.let {
                    TotemPopEvent.Death(entity, it.count).post()
                }
            }
        }

        safeListener<PacketEvent.Receive>(alwaysListening = true) { event ->
            val packet = event.packet
            if (packet is SPacketEntityStatus && packet.opCode == 35.toByte()) {
                val entity = packet.getEntity(world) as? EntityPlayer ?: return@safeListener
                val tracker = trackerMap.computeIfAbsent(packet.getEntityId()) {
                    Tracker(entity.entityId, entity.name)
                }
                tracker.updateCount()
                TotemPopEvent.Pop(entity, tracker.count).post()
            }
        }

        parallelListener<TickEvent.Post>(alwaysListening = true) {
            EntityManager.players.forEach { player ->
                trackerMap.get(player.entityId)?.update()
            }
            
            val removeTime = System.currentTimeMillis()
            val iterator = trackerMap.values.iterator()
            while (iterator.hasNext()) {
                val tracker = iterator.next()
                if (tracker.timeout < removeTime) {
                    TotemPopEvent.Clear(tracker.name, tracker.count).post()
                    iterator.remove()
                }
            }
        }
    }

    class Tracker(val entityID: Int, val name: String) {
        var count: Int = 0
            private set
        var timeout: Long = System.currentTimeMillis() + 15000L
            private set
        var popTime: Long = 0L
            private set

        fun update() {
            timeout = System.currentTimeMillis() + 15000L
        }

        fun updateCount() {
            synchronized(this) {
                count++
                timeout = System.currentTimeMillis() + 15000L
                popTime = System.currentTimeMillis()
            }
        }
    }
}
