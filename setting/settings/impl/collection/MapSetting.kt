package dev.wizard.meta.setting.settings.impl.collection

import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import dev.wizard.meta.setting.settings.ImmutableSetting
import java.util.*

class MapSetting<K, V, T : MutableMap<K, V>>(
    name: CharSequence,
    override val value: T,
    val typeToken: TypeToken<T>,
    override val visibility: (() -> Boolean)? = null,
    override val description: CharSequence = "",
    override val isTransient: Boolean = false
) : ImmutableSetting<T>(name, value, visibility, { _, input -> input }, description) {

    @Suppress("UNCHECKED_CAST")
    override val defaultValue: T = (value.javaClass.newInstance() as T).apply {
        putAll(value)
    }

    override fun resetValue() {
        value.clear()
        value.putAll(defaultValue)
    }

    override fun write(): JsonElement {
        return gson.toJsonTree(value)
    }

    override fun read(jsonElement: JsonElement) {
        val cacheMap: Map<K, V> = gson.fromJson(jsonElement, typeToken.type)
        value.clear()
        value.putAll(cacheMap)
    }

    override fun toString(): String {
        return value.entries.joinToString { "${it.key} to ${it.value}" }
    }

    companion object {
        inline operator fun <reified K : Any, reified V : Any, T : MutableMap<K, V>> invoke(
            name: CharSequence,
            value: T,
            noinline visibility: (() -> Boolean)? = null,
            description: CharSequence = ""
        ): MapSetting<K, V, T> {
            return MapSetting(name, value, object : TypeToken<T>() {}, visibility, description)
        }
    }
}
