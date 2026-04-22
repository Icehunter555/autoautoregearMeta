package dev.wizard.meta.setting.settings

import kotlin.reflect.KProperty

interface NonPrimitive<T : Any> : ISetting<T> {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
    }
}
