package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.util.threads.onMainThreadSafe

object GarbageCleanerCommand : ClientCommand("garbagecollector", arrayOf("gc", "garbagecleaner"), "call the memory cleaner") {

    init {
        execute {
            onMainThreadSafe {
                System.gc()
            }
        }
    }
}
