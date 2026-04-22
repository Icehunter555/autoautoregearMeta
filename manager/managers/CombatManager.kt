package dev.wizard.meta.manager.managers

import com.google.common.collect.MapMaker
import dev.fastmc.common.MathUtilKt
import dev.fastmc.common.TickTimer
import dev.fastmc.common.collection.FastObjectArrayList
import dev.fastmc.common.sort.ObjectIntrosort
import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.ListenerKt.parallelListener
import dev.wizard.meta.event.ListenerKt.safeListener
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.ConnectionEvent
import dev.wizard.meta.event.events.EntityEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.RunGameLoopEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.WorldEvent
import dev.wizard.meta.event.events.combat.CombatEvent
import dev.wizard.meta.event.events.combat.CrystalSetDeadEvent
import dev.wizard.meta.event.events.combat.CrystalSpawnEvent
import dev.wizard.meta.manager.Manager
import dev.wizard.meta.mixins.accessor.entity.AccessorEntityLivingBase
import dev.wizard.meta.module.AbstractModule
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.ModuleManager
import dev.wizard.meta.module.modules.client.CombatSetting
import dev.wizard.meta.util.EntityUtils.getEyePosition
import dev.wizard.meta.util.EntityUtils.getFlooredPosition
import dev.wizard.meta.util.EntityUtils.isFakeOrSelf
import dev.wizard.meta.util.accessor.NetworkKt.getEntityID
import dev.wizard.meta.util.combat.CalcContext
import dev.wizard.meta.util.combat.CrystalDamage
import dev.wizard.meta.util.combat.CrystalUtils
import dev.wizard.meta.util.combat.CrystalUtils.getBlockPos
import dev.wizard.meta.util.combat.CrystalUtils.canPlaceCrystal
import dev.wizard.meta.util.combat.DamageReduction
import dev.wizard.meta.util.combat.MotionTracker
import dev.wizard.meta.util.extension.MapKt.synchronized
import dev.wizard.meta.util.math.VectorUtils
import dev.wizard.meta.util.math.vector.ConversionKt.toVec3d
import dev.wizard.meta.util.math.vector.DistanceKt.distanceSqTo
import dev.wizard.meta.util.threads.BackgroundScope
import dev.wizard.meta.util.threads.CoroutineUtilsKt.isActiveOrFalse
import dev.wizard.meta.util.threads.DefaultScope
import dev.wizard.meta.util.threads.ThreadSafetyKt.onMainThreadSafe
import it.unimi.dsi.fastutil.ints.Int2FloatMaps
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap
import it.unimi.dsi.fastutil.ints.Int2LongMaps
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityEnderCrystal
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.SoundEvents
import net.minecraft.network.play.server.*
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import net.minecraft.world.World

object CombatManager : Manager() {
    private val combatModules: List<AbstractModule>
    private val playerAttackTimeMap: Map<EntityPlayer, Long> = WeakHashMap<EntityPlayer, Long>().synchronized()
    private val damageReductionTimer = TickTimer()
    private val damageReductions: ConcurrentMap<EntityLivingBase, DamageReduction> = MapMaker().weakKeys().makeMap()
    private val hurtTimeMap = Int2LongMaps.synchronize(Int2LongOpenHashMap()).apply { defaultReturnValue(-1L) }
    private val healthMap = Int2FloatMaps.synchronize(Int2FloatOpenHashMap()).apply { defaultReturnValue(Float.NaN) }

    var targetOverride: WeakReference<EntityLivingBase>? = null
    var target: EntityLivingBase? = null
        set(value) {
            if (field != value) {
                CombatEvent.UpdateTarget(field, value).post()
                field = value
            }
        }
        get() {
            if (field?.isEntityAlive == false) field = null
            return field
        }

    var targetList: Set<EntityLivingBase> = emptySet()
    var trackerSelf: MotionTracker? = null
        private set
    var trackerTarget: MotionTracker? = null

    var contextSelf: CalcContext? = null
        private set
    var contextTarget: CalcContext? = null
        private set

    private val crystalTimer = TickTimer()
    private val removeTimer = TickTimer()
    private var placeJob: Job? = null
    private var crystalJob: Job? = null
    private val placeMap0 = ConcurrentHashMap<BlockPos, CrystalDamage>()
    private val crystalMap0: ConcurrentMap<EntityEnderCrystal, CrystalDamage> = MapMaker().weakKeys().makeMap()

    var placeList: List<CrystalDamage> = emptyList()
        private set
    var crystalList: List<Pair<EntityEnderCrystal, CrystalDamage>> = emptyList()
        private set

    private const val PLACE_RANGE = 6
    private const val PLACE_RANGE_SQ = 36.0
    private const val CRYSTAL_RANGE_SQ = 144.0
    private const val MAX_RANGE_SQ = 256.0

    @Retention(AnnotationRetention.RUNTIME)
    annotation class CombatModule

    init {
        val cacheList = ArrayList<AbstractModule>()
        for (module in ModuleManager.modules) {
            if (module.category == Category.COMBAT && module.javaClass.isAnnotationPresent(CombatModule::class.java)) {
                cacheList.add(module)
            }
        }
        combatModules = cacheList

        safeListener<PacketEvent.Receive>(priority = 114514) { event ->
            val packet = event.packet
            if (packet is SPacketSoundEffect) {
                if (packet.category == SoundCategory.BLOCKS && packet.sound == SoundEvents.ENTITY_GENERIC_EXPLODE) {
                    val list = crystalList.asSequence()
                        .map { it.first }
                        .filter { it.distanceSqTo(packet.x, packet.y, packet.z) <= CRYSTAL_RANGE_SQ }
                        .let { seq ->
                            if (CombatSetting.crystalSetDead) seq.onEach { it.setDead() } else seq
                        }
                        .toList()

                    if (list.isNotEmpty() && CombatSetting.crystalSetDead) {
                        onMainThreadSafe {
                            list.forEach {
                                world.removeEntityDangerously(it)
                                world.removeEntity(it)
                            }
                        }
                    }
                    CrystalSetDeadEvent(packet.x, packet.y, packet.z, list).post()
                }
            } else if (packet is SPacketSpawnObject) {
                if (packet.type == 51) {
                    val eyePos = player.getEyePosition()
                    val distSq = eyePos.distanceSqTo(packet.x, packet.y, packet.z)
                    if (distSq <= CRYSTAL_RANGE_SQ) {
                        val blockPos = BlockPos(MathUtilKt.floorToInt(packet.x), MathUtilKt.floorToInt(packet.y) - 1, MathUtilKt.floorToInt(packet.z))
                        getCrystalDamage(blockPos)?.let {
                            CrystalSpawnEvent(packet.entityId, it).post()
                        }
                    }
                }
            } else if (packet is SPacketAnimation) {
                if (packet.animationType == 1) {
                    hurtTimeMap[packet.entityId] = System.currentTimeMillis()
                }
            } else if (packet is SPacketCombatEvent) {
                if (packet.eventType == SPacketCombatEvent.Event.ENTITY_DIED && packet.entityId == player.entityId) {
                    EntityEvent.Death(player).post()
                }
            } else if (packet is SPacketEntityStatus) {
                when (packet.opCode) {
                    3.toByte() -> {
                        (packet.getEntity(world) as? EntityLivingBase)?.let {
                            EntityEvent.Death(it).post()
                        }
                        if (target?.let { packet.getEntityId() == it.entityId } == true) {
                            target = null
                        }
                    }
                    2.toByte(), 33.toByte(), 36.toByte(), 37.toByte() -> {
                        hurtTimeMap[packet.getEntityId()] = System.currentTimeMillis()
                    }
                }
            } else if (packet is SPacketEntityMetadata) {
                val dataManagerEntries = packet.dataManagerEntries ?: return@safeListener
                val entity = world.getEntityByID(packet.entityId) as? EntityLivingBase ?: return@safeListener
                
                val healthEntry = dataManagerEntries.find { entry ->
                    entry.isDirty && entry.key == runCatching { AccessorEntityLivingBase.trollGetHealthDataKey() }.getOrNull()
                } ?: return@safeListener

                val health = healthEntry.value as? Float ?: return@safeListener
                var prevHealth = healthMap.get(entity.entityId)
                if (prevHealth.isNaN()) {
                    prevHealth = entity.health
                }
                EntityEvent.UpdateHealth(entity, prevHealth, health).post()
                healthMap[entity.entityId] = health
                
                if (health <= 0.0f) {
                    EntityEvent.Death(entity).post()
                    if (target?.let { packet.entityId == it.entityId } == true) {
                        target = null
                    }
                }
            } else if (packet is SPacketDestroyEntities) {
                packet.entityIDs.forEach {
                    if (target?.let { target -> it == target.entityId } == true) {
                        target = null
                    }
                    hurtTimeMap.remove(it)
                    healthMap.remove(it)
                }
            }
        }

        listener<ConnectionEvent.Disconnect> {
            damageReductions.clear()
            hurtTimeMap.clear()
            healthMap.clear()
            target = null
            targetList = emptySet()
            trackerSelf = null
            trackerTarget = null
            contextSelf = null
            contextTarget = null
            placeMap0.clear()
            crystalMap0.clear()
            placeList = emptyList()
            crystalList = emptyList()
        }

        safeListener<WorldEvent.Entity.Add> { event ->
            val entity = event.entity
            if (entity is EntityPlayer) {
                damageReductions[entity] = DamageReduction(entity)
            } else if (entity is EntityEnderCrystal) {
                val distSq = entity.distanceSqTo(player.getEyePosition())
                if (distSq <= CRYSTAL_RANGE_SQ) {
                    val contextSelf = contextSelf ?: return@safeListener
                    val contextTarget = contextTarget
                    DefaultScope.launch {
                        val blockPos = entity.getBlockPos()
                        val mutableBlockPos = BlockPos.MutableBlockPos()
                        val crystalDamage = placeMap0.computeIfAbsent(blockPos) {
                            calculateDamage(contextSelf, contextTarget, mutableBlockPos, blockPos.toVec3d(0.5, 1.0, 0.5), blockPos, Math.sqrt(distSq))
                        }
                        crystalMap0[entity] = crystalDamage
                    }
                }
            }
        }

        listener<WorldEvent.Entity.Remove> { event ->
            val entity = event.entity
            if (entity is EntityLivingBase) {
                damageReductions.remove(entity)
            } else if (entity is EntityEnderCrystal) {
                crystalMap0.remove(entity)
            }
            if (entity == target) {
                target = null
            }
            hurtTimeMap.remove(entity.entityId)
            healthMap.remove(entity.entityId)
        }

        parallelListener<TickEvent.Post> {
            val safeClientEvent = SafeClientEvent.instance ?: return@parallelListener
            val player = safeClientEvent.player
            val currentTrackerSelf = trackerSelf
            val trackerSelf = if (currentTrackerSelf != null && currentTrackerSelf.entity == player) {
                currentTrackerSelf
            } else {
                MotionTracker(player)
            }
            trackerSelf.tick()
            CombatManager.trackerSelf = trackerSelf
            trackerTarget?.tick()

            val attacked = player.lastAttackedEntity
            if (attacked is EntityPlayer && attacked.isEntityAlive && !attacked.isFakeOrSelf()) {
                playerAttackTimeMap[attacked] = System.currentTimeMillis() - (player.ticksExisted - player.lastAttackedEntityTime) * 50L
            }
        }

        safeListener<RunGameLoopEvent.Tick>(priority = Int.MAX_VALUE) {
            val flag1 = damageReductionTimer.tickAndReset(CombatSetting.crystalUpdateDelay)
            val flag2 = crystalTimer.tickAndReset(CombatSetting.crystalUpdateDelay)
            if (flag1 || flag2) {
                playerController.updateController()
                if (flag1) {
                    EntityManager.players.forEach {
                        damageReductions[it] = DamageReduction(it)
                    }
                    target?.let {
                        damageReductions[it] = DamageReduction(it)
                    }
                }
                if (flag2) {
                    updateCrystalDamage(this)
                }
            }
        }
    }

    fun getPlayerAttackTime(player: EntityPlayer): Long {
        return playerAttackTimeMap[player] ?: -1L
    }

    fun getCrystalDamage(crystal: EntityEnderCrystal): CrystalDamage? {
        return crystalMap0[crystal] ?: getCrystalDamage(crystal.getBlockPos())
    }

    fun getCrystalDamage(blockPos: BlockPos): CrystalDamage? {
        return contextSelf?.let { contextSelf ->
            placeMap0.computeIfAbsent(blockPos) {
                val crystalPos = blockPos.toVec3d(0.5, 1.0, 0.5)
                val dist = contextSelf.entity.getEyePosition().distanceTo(crystalPos)
                calculateDamage(contextSelf, contextTarget, BlockPos.MutableBlockPos(), crystalPos, it, dist)
            }
        }
    }

    fun getDamageReduction(entity: EntityLivingBase): DamageReduction? {
        return damageReductions[entity]
    }

    fun getHurtTime(entity: EntityLivingBase): Long {
        synchronized(hurtTimeMap) {
            var hurtTime = hurtTimeMap.get(entity.entityId)
            if (hurtTime == -1L && entity.hurtTime != 0) {
                hurtTime = System.currentTimeMillis() - entity.hurtTime * 50L
                hurtTimeMap[entity.entityId] = hurtTime
            }
            return hurtTime
        }
    }

    private fun updateCrystalDamage(safeClientEvent: SafeClientEvent) {
        val flag1 = !placeJob.isActiveOrFalse
        val flag2 = !crystalJob.isActiveOrFalse
        if (flag1 || flag2) {
            val predictPosSelf = trackerSelf?.calcPosAhead(CombatSetting.predictTicksSelf) ?: safeClientEvent.player.positionVector
            val contextSelf = CalcContext(safeClientEvent, safeClientEvent.player, predictPosSelf)
            
            val contextTarget = target?.let { target ->
                val predictPos = trackerTarget?.calcPosAhead(CombatSetting.predictTicksTarget) ?: target.positionVector
                CalcContext(safeClientEvent, target, predictPos)
            }

            val remove = removeTimer.tickAndReset(100)
            CombatManager.contextSelf = contextSelf
            CombatManager.contextTarget = contextTarget

            if (flag1) {
                placeJob = BackgroundScope.launch {
                    updatePlaceMap(safeClientEvent, contextSelf, contextTarget, remove)
                    updatePlaceList()
                }
            }
            if (flag2) {
                crystalJob = BackgroundScope.launch {
                    updateCrystalMap(safeClientEvent, contextSelf, contextTarget, remove)
                    updateCrystalList()
                }
            }
        }
        damageReductionTimer.reset(CombatSetting.crystalUpdateDelay / -4)
    }

    private fun updatePlaceMap(safeClientEvent: SafeClientEvent, contextSelf: CalcContext, contextTarget: CalcContext?, remove: Boolean) {
        val eyePos = safeClientEvent.player.getEyePosition()
        val flooredPos = safeClientEvent.player.getFlooredPosition()
        val mutableBlockPos = BlockPos.MutableBlockPos()
        
        placeMap0.values.removeIf { crystalDamage ->
            remove && (crystalDamage.crystalPos.distanceSqTo(eyePos) > MAX_RANGE_SQ || 
                !safeClientEvent.canPlaceCrystal(crystalDamage.blockPos) ||
                (contextTarget != null && (crystalDamage.crystalPos.distanceSqTo(contextTarget.predictPos) > MAX_RANGE_SQ || !contextTarget.checkColliding(crystalDamage.crystalPos))))
        }

        placeMap0.replaceAll { blockPos, crystalDamage ->
            calculateDamage(contextSelf, contextTarget, mutableBlockPos, blockPos.toVec3d(0.5, 1.0, 0.5), blockPos, eyePos.distanceTo(crystalDamage.crystalPos))
        }

        val blockPos = BlockPos.MutableBlockPos()
        for (x in -PLACE_RANGE..PLACE_RANGE) {
            for (y in -PLACE_RANGE..PLACE_RANGE) {
                for (z in -PLACE_RANGE..PLACE_RANGE) {
                    VectorUtils.setAndAdd(blockPos, flooredPos, x, y, z)
                    if (blockPos.y !in 0..255) continue
                    
                    val crystalX = blockPos.x + 0.5
                    val crystalY = blockPos.y + 1.0
                    val crystalZ = blockPos.z + 0.5
                    val distSq = eyePos.distanceSqTo(crystalX, crystalY, crystalZ)
                    
                    if (distSq > PLACE_RANGE_SQ || placeMap0.containsKey(blockPos) || !safeClientEvent.canPlaceCrystal(blockPos)) continue
                    
                    val crystalPos = Vec3d(crystalX, crystalY, crystalZ)
                    if (contextTarget != null && (contextTarget.predictPos.distanceSqTo(crystalPos) > CRYSTAL_RANGE_SQ || !contextTarget.checkColliding(crystalPos))) continue
                    
                    val immutablePos = blockPos.toImmutable()
                    placeMap0[immutablePos] = calculateDamage(contextSelf, contextTarget, mutableBlockPos, crystalPos, immutablePos, Math.sqrt(distSq))
                }
            }
        }
    }

    private fun updateCrystalMap(safeClientEvent: SafeClientEvent, contextSelf: CalcContext, contextTarget: CalcContext?, remove: Boolean) {
        val eyePos = safeClientEvent.player.getEyePosition()
        val mutableBlockPos = BlockPos.MutableBlockPos()
        
        if (remove) {
            crystalMap0.keys.removeIf { it.distanceSqTo(eyePos) > MAX_RANGE_SQ }
        }

        crystalMap0.replaceAll { crystal, crystalDamage ->
            placeMap0.computeIfAbsent(crystalDamage.blockPos) {
                calculateDamage(contextSelf, contextTarget, mutableBlockPos, it.toVec3d(0.5, 1.0, 0.5), it, eyePos.distanceTo(crystalDamage.crystalPos))
            }
        }

        for (entity in EntityManager.entity) {
            if (!entity.isEntityAlive || !entity.isAddedToWorld || entity !is EntityEnderCrystal) continue
            val distSq = entity.distanceSqTo(eyePos)
            if (distSq > CRYSTAL_RANGE_SQ) continue
            
            crystalMap0.computeIfAbsent(entity) {
                val crystalBlockPos = entity.getBlockPos()
                placeMap0.computeIfAbsent(crystalBlockPos) {
                    calculateDamage(contextSelf, contextTarget, mutableBlockPos, it.toVec3d(0.5, 1.0, 0.5), it, Math.sqrt(distSq))
                }
            }
        }
    }

    private fun calculateDamage(contextSelf: CalcContext, contextTarget: CalcContext?, mutableBlockPos: BlockPos.MutableBlockPos, crystalPos: Vec3d, blockPos: BlockPos, distance: Double): CrystalDamage {
        val selfDamage = Math.max(contextSelf.calcDamage(crystalPos, true, mutableBlockPos), contextSelf.calcDamage(crystalPos, false, mutableBlockPos))
        val targetDamage = contextTarget?.calcDamage(crystalPos, true, mutableBlockPos) ?: 0.0f
        return CrystalDamage(crystalPos, blockPos, selfDamage, targetDamage, distance, contextSelf.currentPos.distanceTo(crystalPos))
    }

    private fun updatePlaceList() {
        val collection = placeMap0.values
        val array = collection.toTypedArray()
        val list = FastObjectArrayList.wrap(array)
        ObjectIntrosort.sort(list.elements(), 0, list.size) { a, b ->
            b.targetDamage.compareTo(a.targetDamage)
        }
        placeList = list
    }

    private fun updateCrystalList() {
        val entries = crystalMap0.entries
        val list = FastObjectArrayList.wrap(Array(entries.size) { Pair(null as EntityEnderCrystal?, null as CrystalDamage?) }, 0)
        for (entry in entries) {
            list.add(Pair(entry.key, entry.value))
        }
        ObjectIntrosort.sort(list.elements(), 0, list.size) { a, b ->
            b.second!!.targetDamage.compareTo(a.second!!.targetDamage)
        }
        crystalList = list as List<Pair<EntityEnderCrystal, CrystalDamage>>
    }

    fun isActiveAndTopPriority(module: AbstractModule): Boolean {
        return module.isActive && isOnTopPriority(module)
    }

    fun isOnTopPriority(module: AbstractModule): Boolean {
        return topPriority <= module.modulePriority
    }

    val topPriority: Int
        get() = topModule?.modulePriority ?: -1

    val topModule: AbstractModule?
        get() {
            var topModule: AbstractModule? = null
            for (module in combatModules) {
                if (!module.isActive) continue
                if (module.modulePriority >= (topModule?.modulePriority ?: 0)) {
                    topModule = module
                }
            }
            return topModule
        }
}
