package dev.wizard.meta.util.math

object FPSCounter {
    var deltaTime: Double = 0.0
        private set
    private var lastRenderTime: Long = 0L

    fun tick() {
        val time = System.nanoTime()
        val prevRenderTime = lastRenderTime
        lastRenderTime = time
        if (prevRenderTime < 1L) return
        
        val delta = time - prevRenderTime
        deltaTime = delta.toDouble() * 1.0E-6 * 0.001
    }
}
