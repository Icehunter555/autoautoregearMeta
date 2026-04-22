package dev.wizard.meta.setting.settings

import kotlin.reflect.KProperty

interface MutableNonPrimitive<T : Any> : IMutableSetting<T>, NonPrimitive<T> {
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
}
