package dev.wizard.meta.util

import dev.fastmc.common.collection.DynamicBitSet

class IDRegistry {
    private val bitSet = DynamicBitSet()

    fun register(): Int {
        var id = -1
        synchronized(bitSet) {
            val iterator = bitSet.iterator()
            while (iterator.hasNext()) {
                id = iterator.next()
            }
            bitSet.add(++id)
        }
        return id
    }

    fun unregister(id: Int) {
        synchronized(bitSet) {
            bitSet.remove(id)
        }
    }
}
