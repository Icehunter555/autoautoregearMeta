package dev.wizard.meta.gui.hudgui.elements.hud

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.graphics.GlStateUtils
import dev.wizard.meta.graphics.RenderUtils3D
import dev.wizard.meta.gui.hudgui.AbstractHudElement
import dev.wizard.meta.gui.hudgui.HudElement
import dev.wizard.meta.setting.settings.SettingRegister.Companion.setting
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.math.MathHelper
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20

object PlayerModel : HudElement("Player Model", category = Category.HUD, description = "Your player icon, or players you attacked") {

    private val emulatePitch by setting(this, "Emulate Pitch", true)
    private val emulateYaw by setting(this, "Emulate Yaw", false)
    private val background by setting(this, "BackGround", false)
    private val radius by setting(this, "Radius", 1.7f, 0.3f..2.9f, 0.1f, visibility = { background })
    private val paddingHorizontal by setting(this, "Horizontal Padding", 4.0f, 0.0f..100.0f, 1.0f, visibility = { background })
    private val paddingVertical by setting(this, "Vertical Padding", 2.0f, 0.0f..10.0f, 1.0f, visibility = { background })

    override val hudWidth = 50.0f
    override val hudHeight = 80.0f
    override val resizable = true

    override fun renderHud() {
        if (mc.renderManager.renderViewEntity == null) return
        super.renderHud()
        if (background) {
            renderFrame(paddingHorizontal, paddingVertical, 0.0f)
        }
        SafeClientEvent.instance?.let { event ->
            val yaw = if (emulateYaw) interpolateAndWrap(event.player.prevRotationYaw, event.player.rotationYaw) else 0.0f
            val pitch = if (emulatePitch) interpolateAndWrap(event.player.prevRotationPitch, event.player.rotationPitch) else 0.0f

            GlStateManager.pushMatrix()
            GlStateManager.translate(renderWidth / scale / 2.0f, renderHeight / scale - 8.0f, 0.0f)
            GlStateUtils.depth(true)
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
            GL20.glUseProgram(0)
            GuiInventory.drawEntityOnScreen(0, 0, 35, -yaw, -pitch, event.player)
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
            GlStateUtils.depth(false)
            GlStateUtils.texture2d(true)
            GlStateUtils.blend(true)
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 1)
            GlStateManager.enableAlpha()
            GlStateManager.popMatrix()
        }
    }

    private fun interpolateAndWrap(prev: Float, current: Float): Float {
        return MathHelper.wrapDegrees(prev + (current - prev) * RenderUtils3D.getPartialTicks())
    }
}
