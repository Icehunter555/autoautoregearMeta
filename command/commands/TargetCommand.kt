package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.manager.managers.CombatManager
import dev.wizard.meta.util.text.NoSpamMessage
import java.lang.ref.WeakReference

object TargetCommand : ClientCommand("target", arrayOf("enemy"), "Override combat target") {

    init {
        player("player") { playerArg ->
            executeSafe {
                val targetPlayer = getValue(playerArg)
                if (targetPlayer.name == player.name) {
                    NoSpamMessage.sendError(this@TargetCommand, "You can't target yourself!")
                    return@executeSafe
                }
                val target = world.getPlayerEntityByName(targetPlayer.name)
                if (target == null) {
                    NoSpamMessage.sendError(this@TargetCommand, "Player ${targetPlayer.name} not found!")
                    return@executeSafe
                }
                CombatManager.targetOverride = WeakReference(target)
                NoSpamMessage.sendMessage("Targeting ${targetPlayer.name}")
            }
        }

        executeSafe {
            CombatManager.targetOverride = null
            NoSpamMessage.sendMessage("Target override cleared")
        }
    }
}
