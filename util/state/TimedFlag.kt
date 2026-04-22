package dev.wizard.meta.util.state

open class TimedFlag<T>(value: T) {
    var value: T = value
        set(newValue) {
            if (field != newValue) {
                lastUpdateTime = System.currentTimeMillis()
                field = newValue
            }
        }

    var lastUpdateTime: Long = System.currentTimeMillis()
        private set

    fun resetTime() {
        lastUpdateTime = System.currentTimeMillis()
    }
}
