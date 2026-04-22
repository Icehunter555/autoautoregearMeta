package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.util.text.NoSpamMessage
import net.minecraft.crash.CrashReport

object CrashMeCommand : ClientCommand("crashme", description = "crash your game") {

    private var confirmTime = 0L

    init {
        execute("crash your came") {
            if (System.currentTimeMillis() - confirmTime > 15000L) {
                confirmTime = System.currentTimeMillis()
                NoSpamMessage.sendMessage("This will crash your game, run again to confirm")
            } else {
                confirmTime = 0L
                val report = CrashReport.makeCrashReport(RuntimeException("Manual crash"), "Crash command")
                mc.displayCrashReport(report)
            }
        }

        literal("confirm", "yes") {
            execute("crash") {
                val report = CrashReport.makeCrashReport(RuntimeException("Manual crash"), "Crash command")
                mc.displayCrashReport(report)
            }
        }
    }
}
