package dev.wizard.meta.structs

import dev.luna5ama.kmogus.MemoryStack
import kotlin.reflect.KMutableProperty1

fun MemoryStack.Pos3Color(): Pos3Color {
    return Pos3Color(calloc(16L))
}

fun MemoryStack.Pos3Color(pos: Vec3f32, color: Int): Pos3Color {
    return Pos3Color(malloc(16L), pos.address, color)
}

fun sizeof(dummy: Pos3Color.Companion): Long = 16L

fun sizeof(f: KMutableProperty1<Pos3Color, *>): Long = when (f.name) {
    "pos" -> 12L
    "color" -> 4L
    else -> throw IllegalArgumentException("Unknown field $f")
}

fun offsetof(f: KMutableProperty1<Pos3Color, *>): Long = when (f.name) {
    "pos" -> 0L
    "color" -> 12L
    else -> throw IllegalArgumentException("Unknown field $f")
}
