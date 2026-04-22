package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.util.text.MessageSendUtils

object ReconnectCommand : ClientCommand("relog", description = "log out and back in") {

    init {
        executeSafe {
            MessageSendUtils.sendServerMessage("/server queue")
        }
    }
}
