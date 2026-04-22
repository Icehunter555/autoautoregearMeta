package dev.wizard.meta.manager.managers

import dev.fastmc.common.TickTimer
import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.ListenerKt.concurrentListener
import dev.wizard.meta.event.ListenerKt.safeListener
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.RunGameLoopEvent
import dev.wizard.meta.event.events.WorldEvent
import dev.wizard.meta.manager.Manager
import dev.wizard.meta.util.EntityUtils.getBetterPosition
import dev.wizard.meta.util.EntityUtils.getFlooredPosition
import dev.wizard.meta.util.combat.HoleInfo
import dev.wizard.meta.util.combat.HoleUtils
import dev.wizard.meta.util.math.vector.DistanceKt.distanceSqTo
import dev.wizard.meta.util.threads.BackgroundScope
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSet
import it.unimi.dsi.fastutil.longs.LongSets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Predicate
import kotlinx.coroutines.launch
import net.minecraft.entity.Entity
import net.minecraft.util.math.BlockPos

object HoleManager : Manager() {
    private val holeMap0 = ConcurrentHashMap<BlockPos, HoleInfo>()
    private var holeSet: LongSet = LongSets.EMPTY_SET
    var holeInfos: List<HoleInfo> = emptyList()
        private set

    private val mainTimer = TickTimer()
    private val updateTimer = TickTimer()
    private val removeTimer = TickTimer()
    private val dirty = AtomicBoolean(false)

    private const val RANGE = 16
    private const val RANGE_SQ = 256
    private const val MAX_RANGE_SQ = 1024

    val holeMap: Map<BlockPos, HoleInfo> get() = holeMap0

    fun getHoleBelow(pos: BlockPos, yRange: Int): HoleInfo? {
        return getHoleBelow(pos, yRange) { true }
    }

    fun getHoleBelow(pos: BlockPos, yRange: Int, predicate: Predicate<HoleInfo>): HoleInfo? {
        for (yOffset in 0..yRange) {
            val offsetPos = pos.down(yOffset)
            val info = getHoleInfo(offsetPos)
            if (info.isHole && predicate.test(info)) {
                return info
            }
        }
        return null
    }

    fun getHoleInfo(entity: Entity): HoleInfo {
        return getHoleInfo(entity.getBetterPosition())
    }

    fun getHoleInfo(pos: BlockPos): HoleInfo {
        return holeMap0.computeIfAbsent(pos) {
            SafeClientEvent.instance?.let { safe ->
                HoleUtils.checkHoleM(safe, it)
            } ?: HoleInfo.empty(it)
        }
    }

    init {
        listener<WorldEvent.Unload> {
            holeMap0.clear()
            holeInfos = emptyList()
            dirty.set(false)
        }

        safeListener<WorldEvent.ClientBlockUpdate> { event ->
            BackgroundScope.launch {
                val playerPos = player.getFlooredPosition()
                val mutablePos = BlockPos.MutableBlockPos()
                val sequence = sequence {
                    for (x in event.pos.x + 2 downTo event.pos.x - 2) {
                        for (y in event.pos.y + 1 downTo event.pos.y - 2) {
                            for (z in event.pos.z + 2 downTo event.pos.z - 2) {
                                if (playerPos.distanceSqTo(x, y, z) <= RANGE_SQ) {
                                    yield(mutablePos.setPos(x, y, z))
                                }
                            }
                        }
                    }
                }
                updatePosSequence(this@safeListener, sequence)
            }
        }

        concurrentListener<RunGameLoopEvent.Render> {
            if (mainTimer.tickAndReset(100L)) {
                BackgroundScope.launch {
                    if (removeTimer.tickAndReset(500L)) {
                        removeInvalidPos(this@concurrentListener)
                    }
                    updatePos(this@concurrentListener, updateTimer.tickAndReset(1000L))
                }
            }
            if (dirty.getAndSet(false)) {
                updateHoleInfoList()
            }
        }
    }

    private fun removeInvalidPos(safeClientEvent: SafeClientEvent) {
        val playerPos = safeClientEvent.player.getFlooredPosition()
        var modified = false
        val iterator = holeMap0.keys.iterator()
        while (iterator.hasNext()) {
            val pos = iterator.next()
            if (playerPos.distanceSqTo(pos) > MAX_RANGE_SQ) {
                iterator.remove()
                modified = true
            }
        }
        if (modified) {
            dirty.set(true)
        }
    }

    private fun updatePos(safeClientEvent: SafeClientEvent, force: Boolean) {
        val playerPos = safeClientEvent.player.getFlooredPosition()
        val checked = LongOpenHashSet()
        val mutablePos = BlockPos.MutableBlockPos()
        var modified = false
        for (x in 16 downTo -16) {
            for (y in 16 downTo -16) {
                for (z in 16 downTo -16) {
                    mutablePos.setPos(playerPos.x + x, playerPos.y + y, playerPos.z + z)
                    if (mutablePos.y !in 0..255) continue
                    if (!force && holeSet.contains(mutablePos.toLong())) continue
                    modified = updatePos(safeClientEvent, playerPos, checked, mutablePos) || modified
                }
            }
        }
        if (modified) {
            dirty.set(true)
        }
    }

    private fun updatePosSequence(safeClientEvent: SafeClientEvent, sequence: Sequence<BlockPos.MutableBlockPos>) {
        val playerPos = safeClientEvent.player.getFlooredPosition()
        val checked = LongOpenHashSet()
        var modified = false
        sequence.forEach {
            modified = updatePos(safeClientEvent, playerPos, checked, it) || modified
        }
        if (modified) {
            dirty.set(true)
        }
    }

    private fun updatePos(safeClientEvent: SafeClientEvent, playerPos: BlockPos, checked: LongSet, pos: BlockPos.MutableBlockPos): Boolean {
        val l = pos.toLong()
        if (checked.contains(l)) return false
        if (pos.distanceSq(playerPos) > RANGE_SQ) return false
        
        val holeInfo = HoleUtils.checkHoleM(safeClientEvent, pos)
        return if (!holeInfo.isHole) {
            val prev = holeMap0.put(holeInfo.origin, holeInfo)
            checked.add(l)
            prev == null || prev.isHole
        } else {
            var modified = false
            for (holePos in holeInfo.holePos) {
                val prev = holeMap0.put(holePos, holeInfo)
                checked.add(holePos.toLong())
                modified = modified || prev == null || prev.type != holeInfo.type || prev.isTrapped != holeInfo.isTrapped || prev.isFullyTrapped != holeInfo.isFullyTrapped
            }
            modified
        }
    }

    private fun updateHoleInfoList() {
        val newHoleSet = LongOpenHashSet(holeMap0.size)
        holeMap0.keys.forEach { newHoleSet.add(it.toLong()) }
        holeSet = newHoleSet
        holeInfos = holeMap0.values.asSequence().filter { it.isHole }.distinct().toList()
    }
}
