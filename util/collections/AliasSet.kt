package dev.wizard.meta.util.collections

import dev.wizard.meta.util.interfaces.Alias
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class AliasSet<T : Alias>(
    map: MutableMap<String, T> = ConcurrentHashMap()
) : NameableSet<T>(map) {

    override fun add(element: T): Boolean {
        var modified = super.add(element)
        element.alias.forEach { alias ->
            val key = alias.toString().lowercase(Locale.ROOT)
            val prevValue = map.put(key, element)
            if (prevValue != null && prevValue != element) {
                remove(prevValue)
            }
            modified = prevValue != element || modified
        }
        return modified
    }

    override fun remove(element: T): Boolean {
        var modified = super.remove(element)
        element.alias.forEach { alias ->
            val key = alias.toString().lowercase(Locale.ROOT)
            modified = map.remove(key) != null || modified
        }
        return modified
    }
}
