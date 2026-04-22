package dev.wizard.meta.command.args

class GreedyStringArg(override val name: String) : AbstractArg<String>() {
    override suspend fun convertToType(string: String?): String? {
        return string
    }
}
