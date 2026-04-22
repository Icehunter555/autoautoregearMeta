package dev.wizard.meta.setting.settings.impl.primitive

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import dev.wizard.meta.setting.settings.MutableSetting
import dev.wizard.meta.util.JsonUtilsKt
import kotlin.reflect.KProperty

class BooleanSetting(
    name: CharSequence,
    value: Boolean,
    visibility: (() -> Boolean)? = null,
    consumer: (Boolean, Boolean) -> Boolean = { _, input -> input },
    description: CharSequence = "",
    override val isTransient: Boolean = false
) : MutableSetting<Boolean>(name, value, visibility, consumer, description) {

    override fun write(): JsonElement {
        return JsonPrimitive(value)
    }

    override fun read(jsonElement: JsonElement) {
        JsonUtilsKt.getAsBooleanOrNull(jsonElement)?.let {
            this.value = it
        }
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Boolean {
        return value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
        this.value = value
    }
}
