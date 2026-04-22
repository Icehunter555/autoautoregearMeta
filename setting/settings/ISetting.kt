package dev.wizard.meta.setting.settings

import com.google.gson.JsonElement
import dev.wizard.meta.util.interfaces.Nameable

interface ISetting<T : Any> : Nameable {
    val value: T
    val defaultValue: T
    val valueClass: Class<T>
    val visibility: (() -> Boolean)?
    val description: CharSequence
    val isVisible: Boolean
    val isModified: Boolean
    val isTransient: Boolean

    fun setValue(value: String)
    fun resetValue()
    fun write(): JsonElement
    fun read(element: JsonElement)
}
