package dev.wizard.meta.gui.hudgui.elements.text

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.gui.hudgui.AbstractHudElement
import dev.wizard.meta.gui.hudgui.LabelHud
import dev.wizard.meta.util.math.MathUtils
import dev.wizard.meta.util.math.RotationUtils

object Rotation : LabelHud("Rotation", category = Category.TEXT, description = "Player rotation") {

    override fun updateText(event: SafeClientEvent) {
        val yaw = MathUtils.round(RotationUtils.normalizeAngle(event.player.rotationYaw), 1)
        val pitch = MathUtils.round(event.player.rotationPitch, 1)
        addText("Yaw ")
        addText(yaw.toString(), secondary = true)
        addText(" Pitch ")
        addText(pitch.toString(), secondary = true)
    }
}
