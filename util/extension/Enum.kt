package dev.wizard.meta.util.extension

import dev.wizard.meta.util.interfaces.DisplayEnum
import java.util.*

fun <E : Enum<E>> E.next(): E {
    val constants = declaringClass.enumConstants
    @Suppress("UNCHECKED_CAST")
    return constants[(ordinal + 1) % constants.size] as E
}

fun Enum<*>.readableName(): CharSequence {
    return (this as? DisplayEnum)?.displayName ?: name.split('_').joinToString(" ") { 
        it.lowercase(Locale.ROOT).capitalize() 
    }
}
