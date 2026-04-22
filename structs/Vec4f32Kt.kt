package dev.wizard.meta.structs

import dev.luna5ama.kmogus.MemoryStack
import kotlin.reflect.KMutableProperty1

fun MemoryStack.Vec4f32(): Vec4f32 {
    return Vec4f32(calloc(16L))
}

fun MemoryStack.Vec4f32(x: Float, y: Float, z: Float, w: Float): Vec4f32 {
    return Vec4f32(malloc(16L), x, y, z, w)
}

fun sizeof(dummy: Vec4f32.Companion): Long = 16L

fun sizeof(f: KMutableProperty1<Vec4f32, *>): Long = when (f.name) {
    "x", "y", "z", "w" -> 4L
    else -> throw IllegalArgumentException("Unknown field $f")
}

fun offsetof(f: KMutableProperty1<Vec4f32, *>): Long = when (f.name) {
    "x" -> 0L
    "y" -> 4L
    "z" -> 8L
    "w" -> 12L
    else -> throw IllegalArgumentException("Unknown field $f")
}
