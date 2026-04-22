package dev.wizard.meta.util.delegate

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class ComputeFlag(private val block: () -> Boolean) : ReadOnlyProperty<Any?, Boolean> {
    private var value: Boolean = false

    override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean {
        if (!value) {
            value = block()
        }
        return value
    }
}
