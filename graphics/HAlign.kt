package dev.wizard.meta.graphics

import dev.wizard.meta.util.interfaces.DisplayEnum

enum class HAlign(
    override val displayName: CharSequence,
    val multiplier: Float,
    val offset: Float
) : DisplayEnum {
    LEFT("Left", 0.0f, -1.0f),
    CENTER("Center", 0.5f, 0.0f),
    RIGHT("Right", 1.0f, 1.0f);

    companion object {
        @JvmField
        val VALUES = values().toList()
    }
}