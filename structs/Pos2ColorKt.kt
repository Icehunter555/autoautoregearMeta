package dev.wizard.meta.structs

import dev.luna5ama.kmogus.MemoryStack
import kotlin.reflect.KMutableProperty1

fun MemoryStack.Pos2Color(): Pos2Color {
    return Pos2Color(calloc(12L))
}

fun MemoryStack.Pos2Color(pos: Vec2f32, color: Int): Pos2Color {
    return Pos2Color(malloc(12L), pos.address, color)
}

fun sizeof(dummy: Pos2Color.Companion): Long = 12L

fun sizeof(f: KMutableProperty1<Pos2Color, *>): Long = when (f.name) {
    "pos" -> 8L
    "color" -> 4L
    else -> throw IllegalArgumentException("Unknown field $f")
}

fun offsetof(f: KMutableProperty1<Pos2Color, *>): Long = when (f.name) {
    "pos" -> 0L
    "color" -> 8L
    else -> throw IllegalArgumentException("Unknown field $f")
}
