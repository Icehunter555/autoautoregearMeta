package dev.wizard.meta.util.threads

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

object DefaultScope : CoroutineScope by CoroutineScope(defaultContext) {
    val context = defaultContext
}
