package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.module.modules.beta.MetaSwapper

object MetaSwapCommand : ClientCommand("metaswap", description = "swaps the meta for u") {

    init {
        executeSafe {
            MetaSwapper.enable()
        }
    }
}
