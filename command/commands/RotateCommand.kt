package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand

object RotateCommand : ClientCommand("rotate", description = "Rotate view to specific value.") {

    init {
        literal("yaw") {
            float("yaw") { yawArg ->
                executeSafe {
                    player.rotationYaw = getValue(yawArg)
                }
            }
        }

        literal("pitch") {
            float("pitch") { pitchArg ->
                executeSafe {
                    player.rotationPitch = getValue(pitchArg).coerceIn(-90.0f, 90.0f)
                }
            }
        }

        float("yaw") { yawArg ->
            float("pitch") { pitchArg ->
                executeSafe {
                    player.rotationYaw = getValue(yawArg)
                    player.rotationPitch = getValue(pitchArg).coerceIn(-90.0f, 90.0f)
                }
            }
        }
    }
}
