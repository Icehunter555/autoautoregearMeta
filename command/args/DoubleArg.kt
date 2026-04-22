package dev.wizard.meta.command.args

class DoubleArg(override val name: String) : AbstractArg<Double>() {
    override suspend fun convertToType(string: String?): Double? {
        return string?.toDoubleOrNull()
    }
}
