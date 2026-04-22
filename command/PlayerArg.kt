package dev.wizard.meta.command

import dev.wizard.meta.command.args.AbstractArg
import dev.wizard.meta.command.args.AutoComplete
import dev.wizard.meta.command.args.DynamicPrefixMatch
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.manager.managers.UUIDManager
import dev.wizard.meta.util.PlayerProfile
import dev.wizard.meta.util.delegate.CachedValueN

class PlayerArg(override val name: String) : AbstractArg<PlayerProfile>(), AutoComplete by DynamicPrefixMatch(Companion::playerInfoMap) {

    override suspend fun checkType(string: String?): Boolean {
        return !string.isNullOrBlank()
    }

    override suspend fun convertToType(string: String?): PlayerProfile? {
        return string?.let { UUIDManager.getByString(it) }
    }

    companion object {
        val playerInfoMap: List<String>? by CachedValueN(3000L) {
            SafeClientEvent.instance?.let {
                it.connection.playerInfoMap
                    .map { info -> info.gameProfile.name }
                    .sorted()
            }
        }
    }
}
