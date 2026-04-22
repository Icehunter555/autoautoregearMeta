package dev.wizard.meta.setting.settings.impl.collection

import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import dev.wizard.meta.setting.settings.ImmutableSetting
import dev.wizard.meta.util.JsonUtilsKt
import java.util.*

class CollectionSetting<E, T : MutableCollection<E>>(
    name: CharSequence,
    override val value: T,
    val typeToken: TypeToken<*>,
    override val visibility: (() -> Boolean)? = null,
    override val description: CharSequence = "",
    override val isTransient: Boolean = false
) : ImmutableSetting<T>(name, value, visibility, { _, input -> input }, description), MutableCollection<E> {

    @Suppress("UNCHECKED_CAST")
    override val defaultValue: T = (value.javaClass.newInstance() as T).apply {
        addAll(value)
    }

    private val lockObject = Any()
    val editListeners = ArrayList<(T) -> Unit>()

    override val size: Int get() = value.size
    override fun contains(element: E): Boolean = value.contains(element)
    override fun containsAll(elements: Collection<E>): Boolean = value.containsAll(elements)
    override fun isEmpty(): Boolean = value.isEmpty()
    override fun iterator(): MutableIterator<E> = value.iterator()
    override fun add(element: E): Boolean = value.add(element)
    override fun addAll(elements: Collection<E>): Boolean = value.addAll(elements)
    override fun clear() { value.clear() }
    override fun remove(element: E): Boolean = value.remove(element)
    override fun removeAll(elements: Collection<E>): Boolean = value.removeAll(elements)
    override fun retainAll(elements: Collection<E>): Boolean = value.retainAll(elements)

    fun editValue(block: CollectionSetting<E, T>.() -> Unit) {
        block()
        editListeners.forEach { it.invoke(value) }
    }

    override fun resetValue() {
        synchronized(lockObject) {
            editValue {
                value.clear()
                value.addAll(defaultValue)
            }
        }
    }

    override fun write(): JsonElement {
        return gson.toJsonTree(value)
    }

    override fun read(jsonElement: JsonElement) {
        JsonUtilsKt.getAsJsonArrayOrNull(jsonElement)?.let {
            val cacheArray = gson.fromJson<Array<E>>(it, typeToken.type)
            synchronized(lockObject) {
                editValue {
                    value.clear()
                    value.addAll(cacheArray)
                }
            }
        }
    }

    override fun toString(): String {
        return value.joinToString()
    }

    companion object {
        inline operator fun <reified E : Any, T : MutableCollection<E>> invoke(
            name: CharSequence,
            value: T,
            noinline visibility: (() -> Boolean)? = null,
            description: CharSequence = ""
        ): CollectionSetting<E, T> {
            return CollectionSetting(name, value, object : TypeToken<Array<E>>() {}, visibility, description)
        }
    }
}
