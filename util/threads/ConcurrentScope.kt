package dev.wizard.meta.util.threads

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

object ConcurrentScope : CoroutineScope by CoroutineScope(concurrentContext) {
    val context = concurrentContext
}
