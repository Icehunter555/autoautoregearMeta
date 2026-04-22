package dev.wizard.meta.gui.hudgui.elements.text

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.graphics.font.TextComponent
import dev.wizard.meta.gui.hudgui.AbstractHudElement
import dev.wizard.meta.gui.hudgui.LabelHud
import dev.wizard.meta.module.modules.client.ClickGUI
import dev.wizard.meta.setting.settings.SettingRegister.Companion.setting
import dev.wizard.meta.util.math.times
import net.minecraft.util.math.Vec3d

object Coords : LabelHud("Coords", category = Category.TEXT, description = "display ur position") {

    private val showX by setting(this, "Show X", true)
    private val showY by setting(this, "Show Y", true)
    private val showZ by setting(this, "Show Z", true)
    private val showNetherOverworld by setting(this, "Show Nether/Overworld", true)
    private val decimalPlaces by setting(this, "Decimal Places", 1, 0..4, 1)
    private val thousandsSeparator by setting(this, "Thousands Separator", false)

    private val netherToOverworld = Vec3d(8.0, 1.0, 8.0)
    private val overworldToNether = Vec3d(0.125, 1.0, 0.125)

    override fun updateText(event: SafeClientEvent) {
        val entity = event.mc.renderViewEntity ?: event.player
        addText("XYZ")
        addTextLine(getFormattedCoords(entity.positionVector).text, secondary = true)

        if (showNetherOverworld) {
            when (entity.dimension) {
                -1 -> {
                    addText("Overworld")
                    addTextLine(getFormattedCoords(entity.positionVector * netherToOverworld).text, secondary = true)
                }
                0 -> {
                    addText("Nether")
                    addTextLine(getFormattedCoords(entity.positionVector * overworldToNether).text, secondary = true)
                }
            }
        }
    }

    private fun getFormattedCoords(pos: Vec3d): TextComponent.TextElement {
        val x = roundOrInt(pos.x)
        val y = roundOrInt(pos.y)
        val z = roundOrInt(pos.z)
        val sb = StringBuilder()
        if (showX) sb.append(x)
        if (showY) appendWithComma(sb, y)
        if (showZ) appendWithComma(sb, z)
        return TextComponent.TextElement(sb.toString(), ClickGUI.text)
    }

    private fun roundOrInt(input: Double): String {
        val separatorFormat = if (thousandsSeparator) "," else ""
        return "%,$separatorFormat.${decimalPlaces}f".format(input)
    }

    private fun appendWithComma(sb: StringBuilder, string: String) {
        if (sb.isNotEmpty()) sb.append(", ")
        sb.append(string)
    }
}
