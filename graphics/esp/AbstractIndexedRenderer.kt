package dev.wizard.meta.graphics.esp

import dev.wizard.meta.util.threads.onMainThread
import org.lwjgl.opengl.GL15

abstract class AbstractIndexedRenderer : AbstractRenderer() {
    protected var iboID: Int = 0

    override val initialized: Boolean
        get() = vaoID != 0 && vboID != 0 && iboID != 0

    override fun clear() {
        if (initialized) {
            onMainThread {
                if (initialized) {
                    GL15.glDeleteBuffers(vboID)
                    GL15.glDeleteBuffers(iboID)
                    vboID = 0
                    iboID = 0
                }
            }
        }
        size = 0
        posX = 0.0
        posY = 0.0
        posZ = 0.0
    }
}
