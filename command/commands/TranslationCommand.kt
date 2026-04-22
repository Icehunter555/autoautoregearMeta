package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.translation.TranslationManager
import dev.wizard.meta.util.text.NoSpamMessage

object TranslationCommand : ClientCommand("translation", arrayOf("i18n")) {

    init {
        literal("dump") {
            executeAsync("reloads the language system") {
                TranslationManager.dump()
                NoSpamMessage.sendMessage(this@TranslationCommand, "Dumped root lang to trollhack/lang")
            }
        }

        literal("reload") {
            executeAsync("Reload all configs") {
                TranslationManager.reload()
                NoSpamMessage.sendMessage(this@TranslationCommand, "Reloaded translations")
            }
        }

        literal("update") {
            string("language") {
                executeAsync {
                    TranslationManager.update()
                    NoSpamMessage.sendMessage(this@TranslationCommand, "Updated translation")
                }
            }
        }
    }
}
