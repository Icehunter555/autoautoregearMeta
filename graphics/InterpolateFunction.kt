package dev.wizard.meta.graphics

fun interface InterpolateFunction {
    fun invoke(time: Long, prev: Float, current: Float): Float
}