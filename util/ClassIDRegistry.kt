package dev.wizard.meta.util

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap

class ClassIDRegistry<T> {
    private val map: Object2IntOpenHashMap<Class<out T>> = Object2IntOpenHashMap<Class<out T>>().apply {
        defaultReturnValue(-1)
    }
    private val registry: IDRegistry = IDRegistry()

    fun get(clazz: Class<out T>): Int {
        var id = map.getInt(clazz)
        if (id == -1) {
            synchronized(registry) {
                id = registry.register()
                map.put(clazz, id)
            }
        }
        return id
    }
}
