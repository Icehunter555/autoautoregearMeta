package dev.wizard.meta.graphics

import dev.wizard.meta.event.AlwaysListening
import dev.wizard.meta.module.modules.client.ClickGUI
import dev.wizard.meta.util.interfaces.Helper
import kotlin.math.ceil

object Resolution : AlwaysListening, Helper {
    val widthI: Int get() = mc.displayWidth
    val heightI: Int get() = mc.displayHeight
    val widthF: Float get() = mc.displayWidth.toFloat()
    val heightF: Float get() = mc.displayHeight.toFloat()

    val trollWidthF: Float get() = widthF / ClickGUI.scaleFactor
    val trollHeightF: Float get() = heightF / ClickGUI.scaleFactor

    val trollWidthI: Int get() = ceil(trollWidthF).toInt()
    val trollHeightI: Int get() = ceil(trollHeightF).toInt()
}