package dev.wizard.meta.graphics.esp

import dev.wizard.meta.util.interfaces.Helper
import dev.wizard.meta.util.threads.onMainThread
import org.lwjgl.opengl.GL15

open class AbstractRenderer : Helper {
    protected var vaoID: Int = 0
    protected var vboID: Int = 0
    protected var posX: Double = 0.0
    protected var posY: Double = 0.0
    protected var posZ: Double = 0.0

    var size: Int = 0
        protected set

    protected open val initialized: Boolean
        get() = vaoID != 0 && vboID != 0

    open fun clear() {
        if (initialized) {
            onMainThread {
                if (initialized) {
                    GL15.glDeleteBuffers(vboID)
                    vboID = 0
                }
            }
        }
        size = 0
        posX = 0.0
        posY = 0.0
        posZ = 0.0
    }
}
