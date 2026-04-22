package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.util.text.NoSpamMessage
import net.minecraft.util.text.TextFormatting
import java.util.*

object EmoteCommand : ClientCommand("emote", arrayOf("shrug", "sendemote"), "Send an ASCII emote or message") {

    private val emoteMap = mapOf(
        "shrug" to "¯\\_(ツ)_/¯",
        "tableflip" to "(╯°□°）╯︵ ┻━┻",
        "unflip" to "┬─┬ ノ( ゜-゜ノ)",
        "wot" to "(ಠ益ಠ)",
        "what" to "(ಠ益ಠ)",
        "cry" to "(ಥ﹏ಥ)",
        "lenny" to "( ͡° ͜ʖ ͡°)",
        "dealwithit" to "(⌐■_■)",
        "love" to "(♥‿♥)",
        "angry" to "ヽ(`Д´)ﾉ",
        "hmm" to "(¬_¬)",
        "yay" to "ヽ(・∀・)ﾉ",
        "wink" to "(>‿◕)",
        "sad" to "(つ﹏⊂)"
    )

    private fun SafeClientEvent.sendEmote(emote: String) {
        val key = emote.lowercase(Locale.ROOT)
        emoteMap[key]?.let {
            player.sendChatMessage(it)
        } ?: NoSpamMessage.sendError("Unknown emote: ${TextFormatting.WHITE}$key${TextFormatting.RESET}")
    }

    private fun SafeClientEvent.sendEmoteToPlayer(emote: String, tplayer: dev.wizard.meta.util.PlayerProfile) {
        val key = emote.lowercase(Locale.ROOT)
        emoteMap[key]?.let {
            player.sendChatMessage("/w ${tplayer.name} $it")
        } ?: NoSpamMessage.sendError("Unknown emote: ${TextFormatting.WHITE}$key${TextFormatting.RESET}")
    }

    init {
        emoteMap.forEach { (name, _) ->
            literal(name) {
                executeSafe {
                    sendEmote(name)
                }

                player("player") { playerArg ->
                    executeSafe {
                        sendEmoteToPlayer(name, getValue(playerArg))
                    }
                }
            }
        }
    }
}
