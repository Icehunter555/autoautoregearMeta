package dev.wizard.meta.command.args

class BooleanArg(override val name: String) : AbstractArg<Boolean>() {

    override suspend fun convertToType(string: String?): Boolean? {
        return toTrueOrNull(string) ?: toFalseOrNull(string)
    }

    private fun toTrueOrNull(string: String?): Boolean? {
        return if (string.equals("true", ignoreCase = true) || string.equals("on", ignoreCase = true)) true else null
    }

    private fun toFalseOrNull(string: String?): Boolean? {
        return if (string.equals("false", ignoreCase = true) || string.equals("off", ignoreCase = true)) false else null
    }
}
