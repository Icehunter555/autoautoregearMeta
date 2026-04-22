package dev.wizard.meta.setting.settings

import com.google.gson.JsonElement

abstract class MutableSetting<T : Any>(
    override val name: CharSequence,
    valueIn: T,
    override val visibility: (() -> Boolean)? = null,
    consumer: (T, T) -> T,
    override val description: CharSequence = ""
) : AbstractSetting<T>(), IMutableSetting<T> {

    override val defaultValue: T = valueIn
    override var value: T = valueIn
        set(newValue) {
            if (field != newValue) {
                val prev = field
                var resultValue = newValue
                for (index in consumers.indices.reversed()) {
                    resultValue = consumers[index].invoke(prev, resultValue)
                }
                field = resultValue
                valueListeners.forEach { it.invoke(prev, field) }
                listeners.forEach { it.invoke() }
            }
        }

    override val valueClass: Class<T> = valueIn.javaClass
    val consumers = arrayListOf(consumer)

    override fun resetValue() {
        value = defaultValue
    }

    override fun write(): JsonElement {
        return gson.toJsonTree(value)
    }

    override fun read(element: JsonElement) {
        val newValue = gson.fromJson(element, valueClass)
        value = newValue
    }
}
