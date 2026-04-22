package dev.wizard.meta.util.threads

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

object BackgroundScope : CoroutineScope by CoroutineScope(backgroundContext) {
    val pool = backgroundPool
    val context = backgroundContext
}
