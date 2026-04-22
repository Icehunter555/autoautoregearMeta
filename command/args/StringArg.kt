package dev.wizard.meta.command.args

class StringArg(override val name: String) : AbstractArg<String>() {
    override suspend fun convertToType(string: String?): String? {
        return string
    }
}
