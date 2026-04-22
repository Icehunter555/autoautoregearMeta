package dev.wizard.meta.setting.settings.impl.primitive

class StringSetting(
    name: CharSequence,
    value: String,
    visibility: (() -> Boolean)? = null,
    consumer: (String, String) -> String = { _, input -> input },
    description: CharSequence = "",
    override val isTransient: Boolean = false
) : AbstractCharSequenceSetting<String>(name, value, visibility, consumer, description) {

    override fun setValue(value: String) {
        this.value = value
    }
}
