package dev.wizard.meta.manager.managers

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.MetaMod
import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.ListenerKt.concurrentListener
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.WorldEvent
import dev.wizard.meta.manager.Manager
import dev.wizard.meta.util.EntityUtils.getFlooredPosition
import dev.wizard.meta.util.EntityUtils.isFakeOrSelf
import dev.wizard.meta.util.math.CoordinateConverter
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.util.math.BlockPos
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object LogoutManager : Manager() {
    private val logoutFile = File("trollhack/logout.txt").apply {
        if (!exists()) {
            parentFile?.mkdirs()
            createNewFile()
            MetaMod.logger.info("Created logout.txt")
        }
    }

    val savedLogouts = ConcurrentHashMap<UUID, BlockPos>()
    val logoutTimes = ConcurrentHashMap<UUID, Instant>()
    val loggedPlayers = ConcurrentHashMap<UUID, BlockPos>()
    private val removed = Collections.synchronizedSet(HashSet<UUID>())
    private val logoutTimers = ConcurrentHashMap<UUID, TickTimer>()
    private const val logoutDelay = 3L
    private val timer = TickTimer(TimeUnit.SECONDS)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

    init {
        listener<WorldEvent.Unload> {
            clearAll()
        }

        listener<WorldEvent.Load> {
            loadFromFile()
        }

        listener<WorldEvent.Entity.Remove> { event ->
            val entity = event.entity
            if (entity is EntityOtherPlayerMP && !entity.isFakeOrSelf()) {
                removed.add(entity.uniqueID)
            }
        }

        concurrentListener<TickEvent.Post> {
            savedLogouts.keys.toList().forEach { uuid ->
                if (isOnline(uuid)) {
                    savedLogouts.remove(uuid)
                    logoutTimes.remove(uuid)
                    removeLogoutForPlayer(uuid)
                    logoutTimers.remove(uuid)
                }
            }

            EntityManager.players.forEach { player ->
                if (player is EntityOtherPlayerMP && !player.isFakeOrSelf()) {
                    loggedPlayers[player.uniqueID] = player.getFlooredPosition()
                }
            }

            removed.forEach { loggedPlayers.remove(it) }
            removed.clear()

            if (timer.tickAndReset(1L)) {
                val iterator = loggedPlayers.entries.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    val uuid = entry.key
                    val pos = entry.value
                    if (!isOnline(uuid)) {
                        val t = logoutTimers.computeIfAbsent(uuid) { TickTimer(TimeUnit.SECONDS) }
                        if (t.tickAndReset(logoutDelay)) {
                            val now = Instant.now()
                            savedLogouts[uuid] = pos
                            logoutTimes[uuid] = now
                            
                            val name = UUIDManager.getByUUID(uuid)?.name ?: uuid.toString()
                            writeLogout(name, pos, now)
                            
                            iterator.remove()
                            logoutTimers.remove(uuid)
                        }
                    } else {
                        logoutTimers.remove(uuid)
                    }
                }
            }
        }
    }

    private fun clearAll() {
        savedLogouts.clear()
        logoutTimes.clear()
        loggedPlayers.clear()
        removed.clear()
        logoutTimers.clear()
    }

    private fun loadFromFile() {
        if (!logoutFile.exists()) return
        logoutFile.forEachLine { line ->
            val parts = line.trim().split(" ")
            if (parts.size != 3) return@forEachLine
            
            val playerName = parts[0]
            val posParts = parts[1].split(",")
            if (posParts.size != 3) return@forEachLine
            
            val uuid = UUIDManager.getByName(playerName)?.uuid ?: return@forEachLine
            val coords = posParts.mapNotNull { it.toIntOrNull() }
            if (coords.size != 3) return@forEachLine
            
            savedLogouts[uuid] = BlockPos(coords[0], coords[1], coords[2])
            logoutTimes[uuid] = Instant.parse(parts[2])
        }
    }

    private fun writeLogout(name: String, pos: BlockPos, time: Instant) {
        logoutFile.appendText("$name ${CoordinateConverter.asString(pos)} $time${System.lineSeparator()}")
    }

    fun isOnline(uuid: UUID): Boolean {
        return connection?.getPlayerInfo(uuid) != null
    }

    private fun removeLogoutForPlayer(uuid: UUID) {
        val playerName = UUIDManager.getByUUID(uuid)?.name ?: return
        if (!logoutFile.exists()) return
        
        val lines = logoutFile.readLines()
        val updated = lines.filterNot { it.startsWith("$playerName ") }
        logoutFile.writeText(updated.joinToString(System.lineSeparator()) + System.lineSeparator())
    }
}
