package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.util.text.NoSpamMessage

object ClearChat : ClientCommand("clearchat", arrayOf("cc", "chatclear"), "clear the chat") {

    init {
        executeSafe {
            mc.ingameGUI.chatGUI.clearChatMessages(true)
            NoSpamMessage.sendMessage("$chatName cleared the chat")
        }
    }
}
