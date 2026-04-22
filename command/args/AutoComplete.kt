package dev.wizard.meta.command.args

interface AutoComplete {
    fun completeForInput(string: String): String?
}
