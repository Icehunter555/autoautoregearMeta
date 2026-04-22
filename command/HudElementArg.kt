package dev.wizard.meta.command

import dev.wizard.meta.command.args.AbstractArg
import dev.wizard.meta.command.args.AutoComplete
import dev.wizard.meta.command.args.DynamicPrefixMatch
import dev.wizard.meta.gui.GuiManager
import dev.wizard.meta.gui.hudgui.AbstractHudElement
import dev.wizard.meta.util.delegate.CachedValueN

class HudElementArg(override val name: String) : AbstractArg<AbstractHudElement>(), AutoComplete by DynamicPrefixMatch(Companion::allAlias) {

    override suspend fun convertToType(string: String?): AbstractHudElement? {
        return GuiManager.getHudElementOrNull(string)
    }

    companion object {
        val allAlias: List<String> by CachedValueN(5000L) {
            GuiManager.hudElements.asSequence()
                .flatMap { hud ->
                    sequence {
                        yield(hud.internalName)
                        hud.alias.forEach { yield(it.toString()) }
                    }
                }
                .sorted()
                .toList()
        }
    }
}
