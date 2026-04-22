package dev.wizard.meta.setting.settings.impl.number

import com.google.gson.JsonPrimitive
import dev.wizard.meta.setting.settings.MutableSetting

abstract class NumberSetting<T>(
    name: CharSequence,
    value: T,
    val range: ClosedRange<T>,
    val step: T,
    visibility: (() -> Boolean)? = null,
    consumer: (T, T) -> T,
    description: CharSequence = "",
    val fineStep: T,
    override val isTransient: Boolean = false
) : MutableSetting<T>(name, value, visibility, consumer, description) where T : Number, T : Comparable<T> {

    override fun write(): JsonPrimitive {
        return JsonPrimitive(value)
    }

    final override fun setValue(value: String) {
        value.toDoubleOrNull()?.let {
            setValue(it)
        }
    }

    abstract fun setValue(value: Double)
}
