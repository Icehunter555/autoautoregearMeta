package dev.wizard.meta.command.args

class IntArg(override val name: String) : AbstractArg<Int>() {
    override suspend fun convertToType(string: String?): Int? {
        return string?.toIntOrNull()
    }
}
