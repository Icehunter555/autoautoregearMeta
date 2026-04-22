package dev.wizard.meta.graphics.shaders

import dev.wizard.meta.event.AlwaysListening
import dev.wizard.meta.event.listener
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.graphics.RenderUtils3D
import org.lwjgl.input.Mouse

object ParticleShader : GLSLSandbox("/assets/meta/shaders/gui/Particle.fsh"), AlwaysListening {
    private val initTime = System.currentTimeMillis()
    private var prevMouseX = 0f
    private var prevMouseY = 0f
    private var mouseX = 0f
    private var mouseY = 0f

    init {
        listener<TickEvent.Post>(alwaysListening = true) {
            prevMouseX = mouseX
            prevMouseY = mouseY
            mouseX = Mouse.getX().toFloat() - 1.0f
            mouseY = (mc.field_71440_d - Mouse.getY()).toFloat() - 1.0f
        }
    }

    fun render() {
        val deltaTicks = RenderUtils3D.getPartialTicks()
        val width = mc.field_71443_c.toFloat()
        val height = mc.field_71440_d.toFloat()
        val currentMouseX = prevMouseX + (mouseX - prevMouseX) * deltaTicks
        val currentMouseY = prevMouseY + (mouseY - prevMouseY) * deltaTicks
        render(width, height, currentMouseX, currentMouseY, initTime)
    }
}
