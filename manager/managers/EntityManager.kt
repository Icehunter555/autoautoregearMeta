package dev.wizard.meta.manager.managers

import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.ListenerKt.parallelListener
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.ConnectionEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.WorldEvent
import dev.wizard.meta.manager.Manager
import dev.wizard.meta.util.math.BoundingBoxUtilsKt.intersectsBlock
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos

object EntityManager : Manager() {
    var entity: List<Entity> = emptyList()
        private set
    var livingBase: List<EntityLivingBase> = emptyList()
        private set
    var players: List<EntityPlayer> = emptyList()
        private set

    private var entityByID: Int2ObjectMap<Entity> = Int2ObjectMaps.emptyMap()
    private var livingBaseByID: Int2ObjectMap<EntityLivingBase> = Int2ObjectMaps.emptyMap()
    private var playersByID: Int2ObjectMap<EntityPlayer> = Int2ObjectMaps.emptyMap()

    init {
        listener<ConnectionEvent.Disconnect>(priority = Int.MAX_VALUE, alwaysListening = true) {
            entity = emptyList()
            livingBase = emptyList()
            players = emptyList()
            entityByID = Int2ObjectMaps.emptyMap()
            livingBaseByID = Int2ObjectMaps.emptyMap()
            playersByID = Int2ObjectMaps.emptyMap()
        }

        listener<WorldEvent.Entity.Add>(priority = Int.MAX_VALUE, alwaysListening = true) { event ->
            val e = event.entity
            entity = entity + e
            entityByID = Int2ObjectOpenHashMap(entityByID).apply { remove(e.entityId) }
            if (e is EntityLivingBase) {
                livingBase = livingBase + e
                livingBaseByID = Int2ObjectOpenHashMap(livingBaseByID).apply { remove(e.entityId) }
                if (e is EntityPlayer) {
                    players = players + e
                    playersByID = Int2ObjectOpenHashMap(playersByID).apply { remove(e.entityId) }
                }
            }
        }

        listener<WorldEvent.Entity.Remove>(priority = Int.MAX_VALUE, alwaysListening = true) { event ->
            val e = event.entity
            entity = entity - e
            entityByID = Int2ObjectOpenHashMap(entityByID).apply { remove(e.entityId) }
            if (e is EntityLivingBase) {
                livingBase = livingBase - e
                livingBaseByID = Int2ObjectOpenHashMap(livingBaseByID).apply { remove(e.entityId) }
                if (e is EntityPlayer) {
                    players = players - e
                    playersByID = Int2ObjectOpenHashMap(playersByID).apply { remove(e.entityId) }
                }
            }
        }

        parallelListener<TickEvent.Post> {
            val world = world
            
            val loadedEntities = world.loadedEntityList
            entity = loadedEntities.toList()
            livingBase = loadedEntities.filterIsInstance<EntityLivingBase>()
            players = world.playerEntities.toList()
            
            entityByID = mapFromList(entity)
            livingBaseByID = mapFromList(livingBase)
            playersByID = mapFromList(players)
        }
    }

    private fun <T : Entity> mapFromList(list: List<T>): Int2ObjectMap<T> {
        val map = Int2ObjectOpenHashMap<T>(list.size)
        for (e in list) {
            map[e.entityId] = e
        }
        return map
    }

    fun getEntityByID(id: Int): Entity? = entityByID.get(id)
    fun getLivingBaseByID(id: Int): EntityLivingBase? = livingBaseByID.get(id)
    fun getPlayerByID(id: Int): EntityPlayer? = playersByID.get(id)

    fun checkNoEntityCollision(box: AxisAlignedBB, predicate: (Entity) -> Boolean): Boolean {
        return entity.asSequence()
            .filter { it.isEntityAlive }
            .filter { it.canBePushed }
            .filter { it.entityBoundingBox.intersects(box) }
            .none(predicate)
    }

    fun checkNoEntityCollision(box: AxisAlignedBB, ignoreEntity: Entity?): Boolean {
        if (ignoreEntity == null) return checkNoEntityCollision(box)
        return entity.asSequence()
            .filter { it.isEntityAlive }
            .filter { it.canBePushed }
            .filter { it != ignoreEntity || it.isRidingSameEntity(ignoreEntity) }
            .filter { it.entityBoundingBox.intersects(box) }
            .none()
    }

    fun checkNoEntityCollision(pos: BlockPos, ignoreEntity: Entity?): Boolean {
        if (ignoreEntity == null) return checkNoEntityCollision(pos)
        return entity.asSequence()
            .filter { it.isEntityAlive }
            .filter { it.canBePushed }
            .filter { it != ignoreEntity || it.isRidingSameEntity(ignoreEntity) }
            .filter { it.entityBoundingBox.intersectsBlock(pos) }
            .none()
    }

    fun checkNoEntityCollision(box: AxisAlignedBB): Boolean {
        return entity.asSequence()
            .filter { it.isEntityAlive }
            .filter { it.canBePushed }
            .filter { it.entityBoundingBox.intersects(box) }
            .none()
    }

    fun checkNoEntityCollision(pos: BlockPos): Boolean {
        return entity.asSequence()
            .filter { it.isEntityAlive }
            .filter { it.canBePushed }
            .filter { it.entityBoundingBox.intersectsBlock(pos) }
            .none()
    }

    fun checkNoEntity(box: AxisAlignedBB, predicate: (Entity) -> Boolean): Boolean {
        return entity.asSequence()
            .filter { it.isEntityAlive }
            .filter { it.entityBoundingBox.intersects(box) }
            .none(predicate)
    }

    fun checkNoEntity(box: AxisAlignedBB, ignoreEntity: Entity): Boolean {
        return entity.asSequence()
            .filter { it.isEntityAlive }
            .filter { it != ignoreEntity }
            .filter { it.entityBoundingBox.intersects(box) }
            .none()
    }

    fun checkNoEntity(box: AxisAlignedBB): Boolean {
        return entity.asSequence()
            .filter { it.isEntityAlive }
            .filter { it.entityBoundingBox.intersects(box) }
            .none()
    }

    fun checkNoEntity(pos: BlockPos): Boolean {
        return entity.asSequence()
            .filter { it.isEntityAlive }
            .filter { it.entityBoundingBox.intersectsBlock(pos) }
            .none()
    }
}
