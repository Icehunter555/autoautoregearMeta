package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.util.ClipboardUtils
import dev.wizard.meta.util.text.NoSpamMessage

object ServerIpCommand : ClientCommand("serverip", arrayOf("ip", "server"), "get the connected server's ip") {

    init {
        executeSafe {
            if (mc.isSingleplayer) {
                NoSpamMessage.sendError("You are in singleplayer!")
                return@executeSafe
            }
            mc.currentServerData?.serverIP?.let { ip ->
                NoSpamMessage.sendMessage("You are connected to $ip!", false)
                try {
                    ClipboardUtils.copyToClipboard(ip)
                } catch (e: Exception) {
                    NoSpamMessage.sendError("Failed to copy server IP to clipboard: ${e.message}")
                }
            }
        }
    }
}
