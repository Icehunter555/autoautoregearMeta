package dev.wizard.meta.util.collections

import dev.wizard.meta.util.interfaces.Nameable
import java.util.*
import java.util.concurrent.ConcurrentHashMap

open class NameableSet<T : Nameable>(
    protected val map: MutableMap<String, T> = ConcurrentHashMap()
) : AbstractMutableSet<T>() {

    override val size: Int
        get() = map.size

    fun containsName(name: String): Boolean {
        return map.containsKey(name.lowercase(Locale.ROOT))
    }

    fun containsNames(names: Iterable<String>): Boolean {
        return names.all { containsName(it) }
    }

    fun containsNames(names: Array<String>): Boolean {
        return names.all { containsName(it) }
    }

    override fun contains(element: T): Boolean {
        return map.containsKey(element.name.toString().lowercase(Locale.ROOT))
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        return elements.all { contains(it) }
    }

    override fun iterator(): MutableIterator<T> {
        return map.values.iterator()
    }

    operator fun get(name: String): T? {
        return map[name.lowercase(Locale.ROOT)]
    }

    fun getOrPut(name: String, value: () -> T): T {
        return get(name) ?: value().also { add(it) }
    }

    override fun add(element: T): Boolean {
        return map.put(element.name.toString().lowercase(Locale.ROOT), element) == null
    }

    override fun addAll(elements: Collection<T>): Boolean {
        var modified = false
        for (element in elements) {
            if (add(element)) modified = true
        }
        return modified
    }

    override fun remove(element: T): Boolean {
        return map.remove(element.name.toString().lowercase(Locale.ROOT)) != null
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        var modified = false
        for (element in elements) {
            if (remove(element)) modified = true
        }
        return modified
    }

    override fun clear() {
        map.clear()
    }
}
