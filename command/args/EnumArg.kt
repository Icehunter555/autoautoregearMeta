package dev.wizard.meta.command.args

import dev.wizard.meta.util.interfaces.DisplayEnum

class EnumArg<E : Enum<E>>(override val name: String, enumClass: Class<E>) : AbstractArg<E>(), AutoComplete by StaticPrefixMatch(Companion.getAllNames(enumClass)) {

    private val enumValues: Array<E> = enumClass.enumConstants

    override suspend fun convertToType(string: String?): E? {
        return enumValues.firstOrNull { it.name.equals(string, ignoreCase = true) }
    }

    companion object {
        fun <E : Enum<E>> getAllNames(clazz: Class<E>): List<String> {
            return ArrayList<String>().apply {
                clazz.enumConstants.forEach { enumValue ->
                    if (enumValue is DisplayEnum) {
                        add(enumValue.displayName.toString())
                    }
                    add(enumValue.name)
                }
            }
        }
    }
}
