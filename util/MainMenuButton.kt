package dev.wizard.meta.util

import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.font.renderer.MainFontRenderer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui

class MainMenuButton(val text: String, val action: () -> Unit) {
    var x: Float = 0f
    var y: Float = 0f
    var width: Float = 150f
    var height: Float = 30f
    var hovered: Boolean = false

    fun setPosition(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    fun update(mouseX: Int, mouseY: Int) {
        hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
    }

    fun render() {
        val color = if (hovered) ColorRGB(215, 121, 39) else ColorRGB(183, 183, 183)
        Gui.drawRect(x.toInt(), y.toInt(), (x + width).toInt(), (y + height).toInt(), 0x60000000)
        MainFontRenderer.drawStringJava(text, x + 5f, y + 7f, color, 1.0f, true)
    }

    fun click() {
        if (hovered) {
            action()
        }
    }
}
