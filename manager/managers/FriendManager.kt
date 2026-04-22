package dev.wizard.meta.manager.managers

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import dev.wizard.meta.MetaMod
import dev.wizard.meta.manager.Manager
import dev.wizard.meta.module.modules.client.Friends
import dev.wizard.meta.util.ConfigUtils
import dev.wizard.meta.util.PlayerProfile
import dev.wizard.meta.util.extension.CollectionKt.synchronized
import dev.wizard.meta.util.extension.MapKt.synchronized
import java.io.File
import java.util.*

object FriendManager : Manager() {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val file = File("trollhack/friends.json")
    private var friendFile = FriendFile()
    val friends: MutableMap<String, PlayerProfile> = HashMap<String, PlayerProfile>().synchronized()

    var enabled: Boolean
        get() = friendFile.enabled
        set(value) {
            friendFile.enabled = value
        }

    val empty: Boolean
        get() = friends.isEmpty()

    fun isFriend(name: String): Boolean {
        if (!enabled) return false
        return friends.containsKey(name.lowercase(Locale.ROOT))
    }

    fun addFriend(name: String): Boolean {
        val playerProfile = UUIDManager.getByName(name)
        return if (playerProfile != null) {
            friendFile.friends.add(playerProfile)
            friends[playerProfile.name.lowercase(Locale.ROOT)] = playerProfile
            Friends.sendFriendsMessage(playerProfile)
            true
        } else {
            false
        }
    }

    fun removeFriend(name: String): Boolean {
        val string = name.lowercase(Locale.ROOT)
        val playerProfile = friends.remove(string)
        return friendFile.friends.remove(playerProfile)
    }

    fun clearFriend() {
        friends.clear()
        friendFile.friends.clear()
    }

    fun loadFriends(): Boolean {
        ConfigUtils.fixEmptyJson(file)
        return try {
            val text = file.readText()
            val type = object : TypeToken<FriendFile>() {}.type
            friendFile = gson.fromJson(text, type)
            friends.clear()
            val map = friendFile.friends.associateBy { it.name.lowercase(Locale.ROOT) }
            friends.putAll(map)
            MetaMod.logger.info("Friend loaded")
            true
        } catch (e: Exception) {
            MetaMod.logger.warn("Failed loading friends", e)
            false
        }
    }

    fun saveFriends(): Boolean {
        return try {
            file.bufferedWriter().use {
                gson.toJson(friendFile, it)
            }
            MetaMod.logger.info("Friends saved")
            true
        } catch (e: Exception) {
            MetaMod.logger.warn("Failed saving friends", e)
            false
        }
    }

    data class FriendFile(
        var enabled: Boolean = true,
        val friends: MutableSet<PlayerProfile> = LinkedHashSet<PlayerProfile>().synchronized()
    )
}
