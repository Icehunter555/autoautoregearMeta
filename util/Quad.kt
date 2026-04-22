package dev.wizard.meta.util

import java.io.Serializable

data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
) : Serializable {
    override fun toString(): String {
        return "($first, $second, $third, $fourth)"
    }
}
