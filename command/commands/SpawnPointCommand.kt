package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.util.ClipboardUtils
import dev.wizard.meta.util.text.NoSpamMessage

object SpawnPointCommand : ClientCommand("spawnpoint", description = "get the coord of ur spawnpoint") {

    init {
        executeSafe {
            val blockPos = player.bedLocation
            if (blockPos == null) {
                NoSpamMessage.sendError("$chatName could not find bed coords!")
                return@executeSafe
            }
            try {
                ClipboardUtils.copyToClipboard(blockPos.toString())
                NoSpamMessage.sendMessage("Your spawnpoint is at $blockPos!")
            } catch (e: Exception) {
                NoSpamMessage.sendError("Failed to copy coordinates to clipboard: ${e.message}")
            }
        }
    }
}
