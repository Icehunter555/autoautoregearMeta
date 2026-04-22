package dev.wizard.meta.command.args

class StaticPrefixMatch(private val matchList: Collection<String>) : AutoComplete {

    override fun completeForInput(string: String): String? {
        if (string.isBlank()) return null
        return matchList.firstOrNull { it.startsWith(string, ignoreCase = true) }
    }
}
