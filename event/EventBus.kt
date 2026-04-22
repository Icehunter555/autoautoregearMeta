package dev.wizard.meta.event

import dev.fastmc.common.collection.FastIntMap
import dev.wizard.meta.util.threads.DefaultScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

open class EventBus : EventPosting {
    val busID = id.getAndIncrement()
    protected val listeners = CopyOnWriteArrayList<Listener<*>>()
    private val parallelListeners = CopyOnWriteArrayList<ParallelListener<*>>()

    init {
        eventBusMap.set(busID, this)
    }

    override val eventBus: EventBus get() = this

    @Suppress("UNCHECKED_CAST")
    override fun post(event: Any) {
        listeners.forEach {
            (it.function as (Any) -> Unit)(event)
        }
        invokeParallel(event)
    }

    @Suppress("UNCHECKED_CAST")
    protected fun invokeParallel(event: Any) {
        if (parallelListeners.isNotEmpty()) {
            runBlocking {
                parallelListeners.forEach { listener ->
                    launch(DefaultScope.context) {
                        (listener.function as suspend (Any) -> Unit)(event)
                    }
                }
            }
        }
    }

    fun subscribe(listener: Listener<*>) {
        val size = listeners.size
        for (i in 0 until size) {
            val other = listeners[i]
            if (listener == other) return
            if (listener.priority > other.priority) {
                listeners.add(i, listener)
                return
            }
        }
        listeners.add(listener)
    }

    fun subscribe(listener: ParallelListener<*>) {
        val size = parallelListeners.size
        for (i in 0 until size) {
            val other = parallelListeners[i]
            if (listener == other) return
            if (listener.priority > other.priority) {
                parallelListeners.add(i, listener)
                return
            }
        }
        parallelListeners.add(listener)
    }

    fun unsubscribe(listener: Listener<*>) {
        listeners.removeIf { it == listener }
    }

    fun unsubscribe(listener: ParallelListener<*>) {
        parallelListeners.removeIf { it == listener }
    }

    companion object {
        private val id = AtomicInteger()
        private val eventBusMap = FastIntMap<EventBus>()

        operator fun get(busID: Int): EventBus {
            return eventBusMap.get(busID)!!
        }
    }
}
