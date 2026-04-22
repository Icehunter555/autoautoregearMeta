package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.manager.managers.SpectateManager
import dev.wizard.meta.util.text.NoSpamMessage

object SpectateCommand : ClientCommand("spectate", arrayOf("spec"), "spectate a player") {

    init {
        literal("stop") {
            executeSafe {
                if (SpectateManager.isSpectating()) {
                    val specTarget = SpectateManager.spectateTarget ?: return@executeSafe
                    SpectateManager.disableSpectating()
                    NoSpamMessage.sendMessage("$chatName stopped spectating ${specTarget.name}")
                } else {
                    NoSpamMessage.sendError("$chatName you are not spectating anyone!")
                }
            }
        }

        player("target") { playerArg ->
            executeSafe {
                val targetProfile = getValue(playerArg)
                val entity = world.playerEntities.firstOrNull { it.uniqueID == targetProfile.uuid }

                if (entity == null) {
                    NoSpamMessage.sendError("$chatName could not find ${targetProfile.name} within render distance!")
                    return@executeSafe
                }

                if (SpectateManager.isSpectating()) {
                    SpectateManager.disableSpectating()
                }
                SpectateManager.spectatePlayer(entity)
                NoSpamMessage.sendMessage("$chatName started spectating ${entity.name}!")
            }
        }
    }
}
