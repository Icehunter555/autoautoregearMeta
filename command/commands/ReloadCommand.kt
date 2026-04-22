package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.graphics.font.renderer.MainFontRenderer

object ReloadCommand : ClientCommand("reload", description = "reload parts of the game") {

    init {
        literal("lang", "language") {
            execute("reloads the language system") {
                mc.languageManager.onResourceManagerReload(mc.resourceManager)
            }
        }

        literal("glyph", "font") {
            execute("Reload the glyph chache") {
                MainFontRenderer.reloadFonts()
            }
        }

        literal("sound", "soundsystem") {
            executeSafe("Reload the sound system") {
                // Nothing in the decompiled logic
            }
        }

        literal("chunks", "render") {
            executeSafe("Reload Chunks") {
                mc.renderGlobal.loadRenderers()
            }
        }
    }
}
