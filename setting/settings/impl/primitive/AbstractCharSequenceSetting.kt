package dev.wizard.meta.setting.settings.impl.primitive

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import dev.wizard.meta.setting.settings.MutableNonPrimitive
import dev.wizard.meta.setting.settings.MutableSetting
import dev.wizard.meta.util.JsonUtilsKt

abstract class AbstractCharSequenceSetting<T : CharSequence>(
    name: CharSequence,
    value: T,
    visibility: (() -> Boolean)? = null,
    consumer: (T, T) -> T = { _, input -> input },
    description: CharSequence = ""
) : MutableSetting<T>(name, value, visibility, consumer, description), MutableNonPrimitive<T> {

    val stringValue: String
        get() = value.toString()

    override fun write(): JsonPrimitive {
        return JsonPrimitive(value.toString())
    }

    override fun read(jsonElement: JsonElement) {
        JsonUtilsKt.getAsStringOrNull(jsonElement)?.let {
            @Suppress("UNCHECKED_CAST")
            this.value = it as T
        }
    }
}
