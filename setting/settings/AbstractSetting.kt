package dev.wizard.meta.setting.settings

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser

abstract class AbstractSetting<T : Any> : ISetting<T> {
    val listeners = ArrayList<() -> Unit>()
    val valueListeners = ArrayList<(T, T) -> Unit>()

    override val isVisible: Boolean
        get() = visibility?.invoke() ?: true

    override val isModified: Boolean
        get() = value != defaultValue

    override fun setValue(value: String) {
        val element = parser.parse(value)
        read(element)
    }

    override fun toString(): String {
        return value.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AbstractSetting<*>) return false
        return valueClass == other.valueClass && name == other.name && value == other.value
    }

    override fun hashCode(): Int {
        var result = valueClass.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + value.hashCode()
        return result
    }

    protected companion object {
        val gson: Gson = GsonBuilder().setPrettyPrinting().create()
        val parser = JsonParser()
    }
}
