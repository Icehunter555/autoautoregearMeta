package dev.wizard.meta.command.args

import dev.wizard.meta.util.interfaces.Alias

open class LiteralArg(override val name: String, override val alias: Array<out String>) : AbstractArg<String>(), Alias, AutoComplete by StaticPrefixMatch(listOf(name, *alias)) {

    override suspend fun convertToType(string: String?): String? {
        if (name.equals(string, ignoreCase = true)) return string
        if (alias.any { it.equals(string, ignoreCase = false) }) return string
        return null
    }

    override fun toString(): String {
        return "[$name]"
    }
}
