package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.util.text.NoSpamMessage

object FontCommand : ClientCommand("fonts", description = "list fonts") {

    init {
        executeSafe {
            NoSpamMessage.sendMessage("Available fonts: Comic, Geo, Gidole, Orbitron, Queen, Underdog, WinkySans, Jetbrains, Monofur")
        }
    }
}
