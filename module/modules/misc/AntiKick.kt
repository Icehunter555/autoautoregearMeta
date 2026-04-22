package dev.wizard.meta.module.modules.misc

import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.text.NoSpamMessage

object AntiKick : Module(
    name = "AntiKick",
    category = Category.MISC,
    description = "Suppress network exceptions and prevent getting kicked"
) {
    @JvmStatic
    fun sendWarning(throwable: Throwable) {
        NoSpamMessage.sendWarning("$chatName Caught exception - \"$throwable\" check log for more info.")
        throwable.printStackTrace()
    }
}
