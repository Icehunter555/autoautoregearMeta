package dev.wizard.meta.command.args

class LongArg(override val name: String) : AbstractArg<Long>() {
    override suspend fun convertToType(string: String?): Long? {
        return string?.toLongOrNull()
    }
}
