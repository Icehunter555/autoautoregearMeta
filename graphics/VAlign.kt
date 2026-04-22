package dev.wizard.meta.graphics

import dev.wizard.meta.util.interfaces.DisplayEnum

enum class VAlign(
    override val displayName: CharSequence,
    val multiplier: Float,
    val offset: Float
) : DisplayEnum {
    TOP("Top", 0.0f, -1.0f),
    CENTER("Center", 0.5f, 0.0f),
    BOTTOM("Bottom", 1.0f, 1.0f);

    companion object {
        @JvmField
        val VALUES = values().toList()
    }
}