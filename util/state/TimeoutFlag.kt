package dev.wizard.meta.util.state

class TimeoutFlag<T> private constructor(
    val value: T,
    private val timeoutTime: Long
) {
    fun timeout(): Boolean {
        return System.currentTimeMillis() > timeoutTime
    }

    companion object {
        fun <T> relative(value: T, timeout: Long): TimeoutFlag<T> {
            return TimeoutFlag(value, System.currentTimeMillis() + timeout)
        }

        fun <T> absolute(value: T, timeout: Long): TimeoutFlag<T> {
            return TimeoutFlag(value, timeout)
        }
    }
}
