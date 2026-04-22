package dev.wizard.meta.setting.settings.impl.number

import com.google.gson.JsonElement
import dev.wizard.meta.util.JsonUtilsKt
import kotlin.ranges.ClosedFloatingPointRange
import kotlin.reflect.KProperty

class DoubleSetting(
    name: CharSequence,
    value: Double,
    range: ClosedFloatingPointRange<Double>,
    step: Double,
    visibility: (() -> Boolean)? = null,
    consumer: (Double, Double) -> Double = { _, input -> input },
    description: CharSequence = "",
    fineStep: Double = step,
    override val isTransient: Boolean = false
) : NumberSetting<Double>(name, value, range, step, visibility, consumer, description, fineStep) {

    init {
        consumers.add(0) { _, it -> it.coerceIn(range) }
    }

    override fun read(jsonElement: JsonElement) {
        JsonUtilsKt.getAsDoubleOrNull(jsonElement)?.let {
            this.value = it
        }
    }

    override fun setValue(value: Double) {
        this.value = value
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Double {
        return value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Double) {
        this.value = value
    }
}
