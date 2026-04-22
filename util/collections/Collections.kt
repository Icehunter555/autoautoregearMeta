package dev.wizard.meta.util.collections

import it.unimi.dsi.fastutil.longs.LongCollection
import java.util.*

inline fun <T> compareFloatBy(crossinline block: (T) -> Float): Comparator<T> {
    return Comparator { a, b -> block(a).compareTo(block(b)) }
}

inline fun <T> compareFloatByDescending(crossinline block: (T) -> Float): Comparator<T> {
    return Comparator { a, b -> block(b).compareTo(block(a)) }
}

fun <T> List<T>.forEachFast(action: (T) -> Unit) {
    for (i in indices) {
        action(this[i])
    }
}

fun <T> List<T>.none(predicate: (T) -> Boolean): Boolean {
    if (isEmpty()) return true
    for (i in indices) {
        if (predicate(this[i])) return false
    }
    return true
}

fun <T> List<T>.asSequenceFast(): Sequence<T> = sequence {
    for (i in indices) {
        yield(this@asSequenceFast[i])
    }
}

@JvmName("averageOrZeroOfByte")
fun Iterable<Byte>.averageOrZero(): Double {
    var sum = 0.0
    var count = 0
    for (element in this) {
        sum += element.toDouble()
        count++
    }
    return if (count == 0) 0.0 else sum / count
}

@JvmName("averageOrZeroOfShort")
fun Iterable<Short>.averageOrZero(): Double {
    var sum = 0.0
    var count = 0
    for (element in this) {
        sum += element.toDouble()
        count++
    }
    return if (count == 0) 0.0 else sum / count
}

@JvmName("averageOrZeroOfInt")
fun Iterable<Int>.averageOrZero(): Double {
    var sum = 0.0
    var count = 0
    for (element in this) {
        sum += element.toDouble()
        count++
    }
    return if (count == 0) 0.0 else sum / count
}

@JvmName("averageOrZeroOfLong")
fun Iterable<Long>.averageOrZero(): Double {
    var sum = 0.0
    var count = 0
    for (element in this) {
        sum += element.toDouble()
        count++
    }
    return if (count == 0) 0.0 else sum / count
}

@JvmName("averageOrZeroOfFloat")
fun Iterable<Float>.averageOrZero(): Double {
    var sum = 0.0
    var count = 0
    for (element in this) {
        sum += element.toDouble()
        count++
    }
    return if (count == 0) 0.0 else sum / count
}

@JvmName("averageOrZeroOfeDouble")
fun Iterable<Double>.averageOrZero(): Double {
    var sum = 0.0
    var count = 0
    for (element in this) {
        sum += element
        count++
    }
    return if (count == 0) 0.0 else sum / count
}

fun LongCollection.removeBy(predicate: (Long) -> Boolean): Boolean {
    val it = iterator()
    var changed = false
    while (it.hasNext()) {
        if (predicate(it.nextLong())) {
            it.remove()
            changed = true
        }
    }
    return changed
}
