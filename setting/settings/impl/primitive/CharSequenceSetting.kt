package dev.wizard.meta.setting.settings.impl.primitive

class CharSequenceSetting(
    name: CharSequence,
    value: CharSequence,
    visibility: (() -> Boolean)? = null,
    consumer: (CharSequence, CharSequence) -> CharSequence = { _, input -> input },
    description: CharSequence = "",
    override val isTransient: Boolean = false
) : AbstractCharSequenceSetting<CharSequence>(name, value, visibility, consumer, description) {

    override fun setValue(value: String) {
        this.value = value
    }
}
