package dev.wizard.meta.util.threads

import kotlinx.coroutines.CoroutineScope

class BackgroundJob(
    val name: String,
    val delay: Long,
    val block: suspend CoroutineScope.() -> Unit
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BackgroundJob) return false
        return name == other.name && delay == other.delay
    }

    override fun hashCode(): Int {
        return 31 * name.hashCode() + delay.hashCode()
    }
}
