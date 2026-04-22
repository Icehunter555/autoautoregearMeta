package dev.wizard.meta.manager.managers

import com.google.gson.*
import dev.wizard.meta.MetaMod
import dev.wizard.meta.manager.Manager
import dev.wizard.meta.util.ConfigUtils
import dev.wizard.meta.util.PlayerProfile
import dev.wizard.meta.util.Wrapper
import dev.wizard.meta.util.extension.MapKt.synchronized
import java.io.*
import java.net.URL
import java.util.*

object UUIDManager : Manager() {
    private val file = File("trollhack/uuid_cache.txt").apply {
        if (!exists()) {
            createNewFile()
            MetaMod.logger.info("Created uuid_cache.txt")
        }
    }
    private val parser = JsonParser()
    private val nameProfileMap: MutableMap<String, PlayerProfile> = LinkedHashMap<String, PlayerProfile>().synchronized()
    private val uuidNameMap: MutableMap<UUID, PlayerProfile> = LinkedHashMap<UUID, PlayerProfile>().synchronized()
    private val uuidRegex = Regex("[a-z0-9].{7}-[a-z0-9].{3}-[a-z0-9].{3}-[a-z0-9].{3}-[a-z0-9].{11}")

    fun getByString(stringIn: String): PlayerProfile? {
        fixUUID(stringIn)?.let {
            getByUUID(it)?.let { profile -> return profile }
        }
        return getByName(stringIn)
    }

    fun getByUUID(uuid: UUID, forceOnline: Boolean = false): PlayerProfile? {
        val result = uuidNameMap.getOrPut(uuid) {
            requestProfile(uuid, forceOnline)?.also { profile ->
                nameProfileMap[profile.name]?.let { uuidNameMap.remove(it.uuid) }
                nameProfileMap[profile.name] = profile
            }
        }
        trimMaps()
        return if (result != null && !result.isInvalid) result else null
    }

    fun getByName(name: String, forceOnline: Boolean = false): PlayerProfile? {
        val key = name.lowercase(Locale.ROOT)
        val result = nameProfileMap.getOrPut(key) {
            requestProfile(name, forceOnline)?.also { profile ->
                uuidNameMap[profile.uuid]?.let { nameProfileMap.remove(it.name) }
                uuidNameMap[profile.uuid] = profile
            }
        }
        trimMaps()
        return if (result != null && !result.isInvalid) result else null
    }

    private fun trimMaps() {
        while (nameProfileMap.size > 1000) {
            nameProfileMap.remove(nameProfileMap.keys.first())?.let {
                uuidNameMap.remove(it.uuid)
            }
        }
    }

    private fun requestProfile(name: String, forceOnline: Boolean): PlayerProfile? {
        if (!forceOnline) {
            Wrapper.minecraft.connection?.playerInfoMap?.find { it.gameProfile.name.equals(name, true) }?.let {
                return PlayerProfile(it.gameProfile.id, it.gameProfile.name)
            }
        }
        val response = try {
            URL("https://api.mojang.com/users/profiles/minecraft/$name").readText()
        } catch (e: FileNotFoundException) {
            return PlayerProfile.INVALID
        } catch (e: Exception) {
            MetaMod.logger.error("Failed requesting profile", e)
            return null
        }
        if (response.isBlank()) {
            MetaMod.logger.error("Response is null or blank, internet might be down")
            return null
        }
        return try {
            val json = parser.parse(response).asJsonObject
            val id = json["id"].asString
            val realName = json["name"].asString
            PlayerProfile(fixUUID(id)!!, realName)
        } catch (e: Exception) {
            MetaMod.logger.error("Failed parsing profile", e)
            null
        }
    }

    private fun requestProfile(uuid: UUID, forceOnline: Boolean): PlayerProfile? {
        if (forceOnline) {
            Wrapper.minecraft.connection?.getPlayerInfo(uuid)?.let {
                return PlayerProfile(it.gameProfile.id, it.gameProfile.name)
            }
        }
        val response = try {
            URL("https://api.mojang.com/user/profiles/${removeDashes(uuid.toString())}/names").readText()
        } catch (e: FileNotFoundException) {
            return PlayerProfile.INVALID
        } catch (e: Exception) {
            MetaMod.logger.error("Failed requesting profile", e)
            return null
        }
        if (response.isBlank()) {
            MetaMod.logger.error("Response is null or blank, internet might be down")
            return null
        }
        return try {
            val json = parser.parse(response).asJsonArray
            val name = json.last().asJsonObject["name"].asString
            PlayerProfile(uuid, name)
        } catch (e: Exception) {
            MetaMod.logger.error("Failed parsing profile", e)
            null
        }
    }

    fun getNameHistory(uuid: UUID): List<String>? {
        val response = try {
            URL("https://api.mojang.com/user/profiles/${removeDashes(uuid.toString())}/names").readText()
        } catch (e: FileNotFoundException) {
            return null
        } catch (e: Exception) {
            MetaMod.logger.error("Failed requesting name history for $uuid", e)
            return null
        }
        if (response.isBlank()) {
            MetaMod.logger.error("Response blank, internet might be down")
            return null
        }
        return try {
            val json = parser.parse(response).asJsonArray
            json.map { it.asJsonObject["name"].asString }
        } catch (e: Exception) {
            MetaMod.logger.error("Failed parsing name history for $uuid", e)
            null
        }
    }

    fun load(): Boolean {
        ConfigUtils.fixEmptyJson(file)
        return try {
            val profileList = ArrayList<PlayerProfile>()
            if (file.exists()) {
                file.forEachLine {
                    runCatching {
                        val split = it.split(':')
                        profileList.add(PlayerProfile(UUID.fromString(split[1]), split[0]))
                    }
                }
            }
            uuidNameMap.clear()
            nameProfileMap.clear()
            uuidNameMap.putAll(profileList.associateBy { it.uuid })
            nameProfileMap.putAll(profileList.associateBy { it.name })
            MetaMod.logger.info("UUID cache loaded")
            true
        } catch (e: Exception) {
            MetaMod.logger.warn("Failed loading UUID cache", e)
            false
        }
    }

    fun save(): Boolean {
        return try {
            file.bufferedWriter().use { writer ->
                uuidNameMap.values.filter { !it.isInvalid }.forEach {
                    writer.write("${it.name}:${it.uuid}")
                    writer.newLine()
                }
            }
            MetaMod.logger.info("UUID cache saved")
            true
        } catch (e: Exception) {
            MetaMod.logger.warn("Failed saving UUID cache", e)
            false
        }
    }

    private fun fixUUID(string: String): UUID? {
        if (isUUID(string)) return UUID.fromString(string)
        if (string.length < 32) return null
        val fixed = insertDashes(string)
        return if (isUUID(fixed)) UUID.fromString(fixed) else null
    }

    private fun isUUID(string: String): Boolean = uuidRegex.matches(string)

    private fun removeDashes(string: String): String = string.replace("-", "")

    private fun insertDashes(string: String): String {
        return StringBuilder(string)
            .insert(8, '-')
            .insert(13, '-')
            .insert(18, '-')
            .insert(23, '-')
            .toString()
    }
}
