package dev.wizard.meta.command.args

class FloatArg(override val name: String) : AbstractArg<Float>() {
    override suspend fun convertToType(string: String?): Float? {
        return string?.toFloatOrNull()
    }
}
