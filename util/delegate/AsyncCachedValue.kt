package dev.wizard.meta.util.delegate

import dev.fastmc.common.TimeUnit
import dev.wizard.meta.util.threads.ConcurrentScope
import dev.wizard.meta.util.threads.DefaultScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class AsyncCachedValue<T>(
    updateTime: Long,
    timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
    private val context: CoroutineContext = DefaultScope.context,
    block: () -> T
) : CachedValue<T>(updateTime, timeUnit, block), ReadWriteProperty<Any?, T> {

    override fun get(): T {
        val cached = value
        return if (cached == null) {
            block().also { value = it }
        } else {
            if (timer.tickAndReset(this.updateTime)) {
                ConcurrentScope.launch(context) {
                    value = block()
                }
            }
            cached
        }
    }

    override fun update() {
        timer.reset()
        ConcurrentScope.launch(context) {
            value = block()
        }
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = get()

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
}
