package dev.wizard.meta.setting.settings.impl.primitive

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import dev.wizard.meta.setting.settings.MutableNonPrimitive
import dev.wizard.meta.setting.settings.MutableSetting
import dev.wizard.meta.util.JsonUtilsKt
import dev.wizard.meta.util.extension.next
import java.util.*

class EnumSetting<T : Enum<T>>(
    name: CharSequence,
    value: T,
    visibility: (() -> Boolean)? = null,
    consumer: (T, T) -> T = { _, input -> input },
    description: CharSequence = "",
    override val isTransient: Boolean = false
) : MutableSetting<T>(name, value, visibility, consumer, description), MutableNonPrimitive<T> {

    val enumClass: Class<T> = value.declaringClass
    val enumValues: Array<T> = enumClass.enumConstants

    fun nextValue() {
        value = value.next()
    }

    override fun setValue(value: String) {
        val formatted = value.uppercase(Locale.ROOT).replace(' ', '_')
        val newValue = enumValues.firstOrNull { it.name == formatted }
        if (newValue != null) {
            this.value = newValue
        }
    }

    override fun write(): JsonElement {
        return JsonPrimitive(value.name)
    }

    override fun read(jsonElement: JsonElement) {
        JsonUtilsKt.getAsStringOrNull(jsonElement)?.let { element ->
            enumValues.firstOrNull { it.name.equals(element, ignoreCase = true) }?.let {
                this.value = it
            }
        }
    }
}
