package dev.wizard.meta.structs

import dev.luna5ama.kmogus.MemoryStack
import kotlin.reflect.KMutableProperty1

fun MemoryStack.FontVertex(): FontVertex {
    return FontVertex(calloc(16L))
}

fun MemoryStack.FontVertex(position: Vec2f32, vertUV: Vec2i16, colorIndex: Byte, overrideColor: Byte, shadow: Byte): FontVertex {
    return FontVertex(malloc(16L), position.address, vertUV.address, colorIndex, overrideColor, shadow)
}

fun sizeof(dummy: FontVertex.Companion): Long = 16L

fun sizeof(f: KMutableProperty1<FontVertex, *>): Long = when (f.name) {
    "position" -> 8L
    "vertUV" -> 4L
    "colorIndex", "overrideColor", "shadow" -> 1L
    else -> throw IllegalArgumentException("Unknown field $f")
}

fun offsetof(f: KMutableProperty1<FontVertex, *>): Long = when (f.name) {
    "position" -> 0L
    "vertUV" -> 8L
    "colorIndex" -> 12L
    "overrideColor" -> 13L
    "shadow" -> 14L
    else -> throw IllegalArgumentException("Unknown field $f")
}
