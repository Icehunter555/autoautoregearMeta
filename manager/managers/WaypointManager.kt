package dev.wizard.meta.manager.managers

import com.google.gson.*
import com.google.gson.annotations.SerializedName
import dev.wizard.meta.MetaMod
import dev.wizard.meta.event.events.WaypointUpdateEvent
import dev.wizard.meta.manager.Manager
import dev.wizard.meta.util.ConfigUtils
import dev.wizard.meta.util.Wrapper
import dev.wizard.meta.util.math.CoordinateConverter
import dev.wizard.meta.util.math.vector.toBlockPos
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentSkipListSet
import net.minecraft.util.math.BlockPos

object WaypointManager : Manager() {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val file = File("trollhack/waypoints.json")
    private val sdf = SimpleDateFormat("HH:mm:ss dd/MM/yyyy")
    val waypoints = ConcurrentSkipListSet<Waypoint> { a, b -> a.id.compareTo(b.id) }

    fun loadWaypoints(): Boolean {
        ConfigUtils.fixEmptyJson(file, true)
        val success = try {
            val cacheArray = gson.fromJson(file.readText(), Array<Waypoint>::class.java)
            waypoints.clear()
            if (cacheArray != null) {
                waypoints.addAll(cacheArray)
            }
            MetaMod.logger.info("Waypoint loaded")
            true
        } catch (e: Exception) {
            MetaMod.logger.warn("Failed loading waypoints", e)
            false
        }
        WaypointUpdateEvent(WaypointUpdateEvent.Type.CLEAR, null).post()
        return success
    }

    fun saveWaypoints(): Boolean {
        return try {
            file.bufferedWriter().use {
                gson.toJson(waypoints, it)
            }
            MetaMod.logger.info("Waypoint saved")
            true
        } catch (e: Exception) {
            MetaMod.logger.warn("Failed saving waypoint", e)
            false
        }
    }

    fun get(id: Int): Waypoint? {
        val waypoint = waypoints.find { it.id == id }
        WaypointUpdateEvent(WaypointUpdateEvent.Type.GET, waypoint).post()
        return waypoint
    }

    fun get(pos: BlockPos, currentDimension: Boolean = false): Waypoint? {
        val waypoint = waypoints.find { 
            (if (currentDimension) it.currentPos() else it.pos) == pos 
        }
        WaypointUpdateEvent(WaypointUpdateEvent.Type.GET, waypoint).post()
        return waypoint
    }

    fun add(locationName: String): Waypoint {
        val player = Wrapper.player
        val pos = player?.positionVector?.toBlockPos()
        return if (pos != null) {
            val waypoint = add(pos, locationName)
            WaypointUpdateEvent(WaypointUpdateEvent.Type.ADD, waypoint).post()
            waypoint
        } else {
            MetaMod.logger.error("Error during waypoint adding")
            dateFormatter(BlockPos(0, 0, 0), locationName)
        }
    }

    fun add(pos: BlockPos, locationName: String): Waypoint {
        val waypoint = dateFormatter(pos, locationName)
        waypoints.add(waypoint)
        WaypointUpdateEvent(WaypointUpdateEvent.Type.ADD, waypoint).post()
        return waypoint
    }

    fun remove(pos: BlockPos, currentDimension: Boolean = false): Boolean {
        val waypoint = get(pos, currentDimension)
        val removed = waypoints.remove(waypoint)
        WaypointUpdateEvent(WaypointUpdateEvent.Type.REMOVE, waypoint).post()
        return removed
    }

    fun remove(id: Int): Boolean {
        val waypoint = get(id) ?: return false
        val removed = waypoints.remove(waypoint)
        WaypointUpdateEvent(WaypointUpdateEvent.Type.REMOVE, waypoint).post()
        return removed
    }

    fun clear() {
        waypoints.clear()
        WaypointUpdateEvent(WaypointUpdateEvent.Type.CLEAR, null).post()
    }

    fun genServer(): String? {
        return Wrapper.minecraft.currentServerData?.serverIP ?: if (Wrapper.minecraft.isSingleplayer) "Singleplayer" else null
    }

    fun genDimension(): Int {
        return Wrapper.player?.dimension ?: -2
    }

    private fun dateFormatter(pos: BlockPos, locationName: String): Waypoint {
        return Waypoint(pos, locationName, sdf.format(Date()))
    }

    class Waypoint(
        @SerializedName("position") val pos: BlockPos,
        val name: String,
        @SerializedName("date", alternate = ["time"]) val date: String
    ) {
        val id: Int = genID()
        val server: String? = genServer()
        val dimension: Int = genDimension()

        fun currentPos(): BlockPos {
            return CoordinateConverter.toCurrent(dimension, pos)
        }

        private fun genID(): Int {
            return (waypoints.lastOrNull()?.id ?: -1) + 1
        }

        override fun toString(): String {
            return CoordinateConverter.asString(currentPos())
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Waypoint) return false
            return pos == other.pos && name == other.name && date == other.date && id == other.id && server == other.server && dimension == other.dimension
        }

        override fun hashCode(): Int {
            var result = pos.hashCode()
            result = 31 * result + name.hashCode()
            result = 31 * result + date.hashCode()
            result = 31 * result + id
            result = 31 * result + (server?.hashCode() ?: 0)
            result = 31 * result + dimension
            return result
        }
    }
}
