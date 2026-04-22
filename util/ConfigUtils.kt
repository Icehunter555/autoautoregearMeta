package dev.wizard.meta.util

import dev.wizard.meta.MetaMod
import dev.wizard.meta.manager.managers.FriendManager
import dev.wizard.meta.manager.managers.MacroManager
import dev.wizard.meta.manager.managers.UUIDManager
import dev.wizard.meta.manager.managers.WaypointManager
import dev.wizard.meta.setting.ConfigManager
import java.io.File
import java.io.FileWriter
import java.io.IOException

object ConfigUtils {
    fun loadAll(): Boolean {
        var success = ConfigManager.loadAll()
        success = MacroManager.loadMacros() && success
        success = WaypointManager.loadWaypoints() && success
        success = FriendManager.loadFriends() && success
        success = UUIDManager.load() && success
        return success
    }

    fun saveAll(): Boolean {
        var success = ConfigManager.saveAll()
        success = MacroManager.saveMacros() && success
        success = WaypointManager.saveWaypoints() && success
        success = FriendManager.saveFriends() && success
        success = UUIDManager.save() && success
        return success
    }

    fun isPathValid(path: String): Boolean {
        return try {
            File(path).canonicalPath
            true
        } catch (e: Throwable) {
            false
        }
    }

    fun fixEmptyJson(file: File, isArray: Boolean = false) {
        var empty = false
        if (!file.exists()) {
            file.createNewFile()
            empty = true
        } else if (file.length() <= 8L) {
            val text = file.readText()
            if (text.isNotBlank()) {
                if (text.all { it == '[' || it == ']' || it == '{' || it == '}' || it == ' ' || it == '\n' || it == '\r' }) {
                    empty = true
                }
            } else {
                empty = true
            }
        }

        if (empty) {
            try {
                FileWriter(file, false).use {
                    it.write(if (isArray) "[]" else "{}")
                }
            } catch (e: IOException) {
                MetaMod.logger.warn("Failed fixing empty json", e)
            }
        }
    }
}
