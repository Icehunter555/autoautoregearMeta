package dev.wizard.meta.util.extension

import it.unimi.dsi.fastutil.longs.Long2LongMap
import it.unimi.dsi.fastutil.longs.Long2LongMaps
import java.util.*

fun <K, V> SortedMap<K, V>.firstKeyOrNull(): K? {
    return try {
        firstKey()
    } catch (e: NoSuchElementException) {
        null
    }
}

fun <K, V> NavigableMap<K, V>.lastValueOrNull(): V? {
    return lastEntry()?.value
}

fun <K, V> NavigableMap<K, V>.firstValueOrNull(): V? {
    return firstEntryOrNull()?.value
}

fun <K, V> NavigableMap<K, V>.firstEntryOrNull(): Map.Entry<K, V>? {
    return firstEntry()
}

fun <K, V> NavigableMap<K, V>.lastEntryOrNull(): Map.Entry<K, V>? {
    return lastEntry()
}

fun <K, V> Map<K, V>.synchronized(): Map<K, V> {
    return Collections.synchronizedMap(this)
}

fun <K, V> SortedMap<K, V>.synchronized(): SortedMap<K, V> {
    return Collections.synchronizedSortedMap(this)
}

fun <K, V> NavigableMap<K, V>.synchronized(): NavigableMap<K, V> {
    return Collections.synchronizedNavigableMap(this)
}

fun Long2LongMap.synchronized(): Long2LongMap {
    return Long2LongMaps.synchronize(this)
}
