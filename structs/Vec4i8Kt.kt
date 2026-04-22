package dev.wizard.meta.structs

import dev.luna5ama.kmogus.MemoryStack
import kotlin.reflect.KMutableProperty1

fun MemoryStack.Vec4i8(): Vec4i8 {
    return Vec4i8(calloc(4L))
}

fun MemoryStack.Vec4i8(x: Byte, y: Byte, z: Byte, w: Byte): Vec4i8 {
    return Vec4i8(malloc(4L), x, y, z, w)
}

fun sizeof(dummy: Vec4i8.Companion): Long = 4L

fun sizeof(f: KMutableProperty1<Vec4i8, *>): Long = when (f.name) {
    "x", "y", "z", "w" -> 1L
    else -> throw IllegalArgumentException("Unknown field $f")
}

fun offsetof(f: KMutableProperty1<Vec4i8, *>): Long = when (f.name) {
    "x" -> 0L
    "y" -> 1L
    "z" -> 2L
    "w" -> 3L
    else -> throw IllegalArgumentException("Unknown field $f")
}
