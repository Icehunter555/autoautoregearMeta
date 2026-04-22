package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import net.minecraft.world.GameType

object FakeGameMode : ClientCommand("fakegamemode", arrayOf("fgm", "gm"), "fake gamemode") {

    init {
        literal("creative", "c") {
            executeSafe("set your fake gamemode to creative") {
                playerController.setGameType(GameType.CREATIVE)
            }
        }

        literal("survival", "s") {
            executeSafe("set your fake gamemode to survival") {
                playerController.setGameType(GameType.SURVIVAL)
            }
        }

        literal("adventure", "a") {
            executeSafe("set your fake gamemode to adventure") {
                playerController.setGameType(GameType.ADVENTURE)
            }
        }

        literal("spectator", "sp") {
            executeSafe("set your fake gamemode to spectator") {
                playerController.setGameType(GameType.SPECTATOR)
            }
        }

        literal("reset", "r") {
            executeSafe("put u back in survival") {
                playerController.setGameType(GameType.SURVIVAL)
            }
        }
    }
}
