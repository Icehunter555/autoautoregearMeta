package dev.wizard.meta.setting.settings

abstract class ImmutableSetting<T : Any>(
    override val name: CharSequence,
    valueIn: T,
    override val visibility: (() -> Boolean)? = null,
    val consumer: (T, T) -> T,
    override val description: CharSequence = ""
) : AbstractSetting<T>() {
    override val value: T = valueIn
    override val valueClass: Class<T> = valueIn.javaClass
    override val defaultValue: T get() = value

    override fun setValue(value: String) {
        // Immutable, do nothing or throw
    }

    override fun resetValue() {
        // Immutable
    }
}
