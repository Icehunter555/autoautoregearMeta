package dev.wizard.meta.event

import dev.wizard.meta.util.ClassUtils
import dev.wizard.meta.util.threads.ConcurrentScope
import kotlinx.coroutines.launch

const val DEFAULT_LISTENER_PRIORITY = 0

inline fun <reified E : Event> IListenerOwner.safeListener(
    priority: Int = DEFAULT_LISTENER_PRIORITY,
    alwaysListening: Boolean = false,
    crossinline function: SafeClientEvent.(E) -> Unit
) {
    listener<E>(priority, alwaysListening) { event ->
        SafeClientEvent.instance?.let { it.function(event) }
    }
}

inline fun <reified E : Event> IListenerOwner.safeParallelListener(
    alwaysListening: Boolean = false,
    crossinline function: suspend SafeClientEvent.(E) -> Unit
) {
    parallelListener<E>(alwaysListening) { event ->
        SafeClientEvent.instance?.let { it.function(event) }
    }
}

inline fun <reified E : Event> IListenerOwner.safeConcurrentListener(
    alwaysListening: Boolean = false,
    crossinline function: suspend SafeClientEvent.(E) -> Unit
) {
    concurrentListener<E>(alwaysListening) { event ->
        SafeClientEvent.instance?.let { it.function(event) }
    }
}

inline fun <reified E : Event> IListenerOwner.listener(
    priority: Int = DEFAULT_LISTENER_PRIORITY,
    alwaysListening: Boolean = false,
    noinline function: (E) -> Unit
) {
    listener(this, E::class.java, priority, alwaysListening, function)
}

inline fun <reified E : Event> IListenerOwner.parallelListener(
    alwaysListening: Boolean = false,
    noinline function: suspend (E) -> Unit
) {
    parallelListener(this, E::class.java, alwaysListening, function)
}

inline fun <reified E : Event> IListenerOwner.concurrentListener(
    alwaysListening: Boolean = false,
    noinline function: suspend (E) -> Unit
) {
    concurrentListener(this, E::class.java, alwaysListening, function)
}

@Suppress("UNCHECKED_CAST")
fun <E : Event> listener(
    owner: IListenerOwner,
    eventClass: Class<E>,
    priority: Int,
    alwaysListening: Boolean,
    function: (E) -> Unit
) {
    val eventBus = getEventBus(eventClass)
    val listener = Listener(owner, eventBus.busID, priority, function as (Any) -> Unit)
    if (alwaysListening) {
        eventBus.subscribe(listener)
    } else {
        owner.register(listener)
    }
}

@Suppress("UNCHECKED_CAST")
fun <E : Event> parallelListener(
    owner: IListenerOwner,
    eventClass: Class<E>,
    alwaysListening: Boolean,
    function: suspend (E) -> Unit
) {
    val eventBus = getEventBus(eventClass)
    val listener = ParallelListener(owner, eventBus.busID, function as suspend (Any) -> Unit)
    if (alwaysListening) {
        eventBus.subscribe(listener)
    } else {
        owner.register(listener)
    }
}

@Suppress("UNCHECKED_CAST")
fun <E : Event> concurrentListener(
    owner: IListenerOwner,
    eventClass: Class<E>,
    alwaysListening: Boolean,
    function: suspend (E) -> Unit
) {
    val eventBus = getEventBus(eventClass)
    val listener = Listener(owner, eventBus.busID, Int.MAX_VALUE) { event ->
        ConcurrentScope.launch {
            function(event as E)
        }
    }
    if (alwaysListening) {
        eventBus.subscribe(listener)
    } else {
        owner.register(listener)
    }
}

private fun getEventBus(eventClass: Class<out Event>): EventBus {
    return try {
        (ClassUtils.getInstance(eventClass) as EventPosting).eventBus
    } catch (e: Exception) {
        (eventClass.getDeclaredField("Companion").get(null) as EventPosting).eventBus
    }
}
