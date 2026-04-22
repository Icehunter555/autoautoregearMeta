package dev.wizard.meta.util.extension

import it.unimi.dsi.fastutil.longs.LongCollection
import java.util.*

fun <E> MutableCollection<E>.add(e: E?) {
    if (e != null) {
        this.add(e)
    }
}

fun <T> Iterable<T>.sumOfFloat(selector: (T) -> Float): Float {
    var sum = 0.0f
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

inline fun <T> Sequence<T>.sumOfFloat(selector: (T) -> Float): Float {
    var sum = 0.0f
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

inline fun CharSequence.sumOfFloat(selector: (Char) -> Float): Float {
    var sum = 0.0f
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

fun <E> Collection<E>.synchronized(): Collection<E> {
    return Collections.synchronizedCollection(this)
}

fun <E> List<E>.synchronized(): List<E> {
    return Collections.synchronizedList(this)
}

fun <E> Set<E>.synchronized(): Set<E> {
    return Collections.synchronizedSet(this)
}

fun <E> SortedSet<E>.synchronized(): SortedSet<E> {
    return Collections.synchronizedSortedSet(this)
}

fun <E> NavigableSet<E>.synchronized(): NavigableSet<E> {
    return Collections.synchronizedNavigableSet(this)
}

inline fun LongCollection.forEach(action: (Long) -> Unit) {
    val it = iterator()
    while (it.hasNext()) {
        action(it.nextLong())
    }
}
