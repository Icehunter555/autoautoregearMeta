package dev.wizard.meta.event

open class ListenerOwner : IListenerOwner {
    private val listeners = ArrayList<Listener<*>>()
    private val parallelListeners = ArrayList<ParallelListener<*>>()

    override fun register(listener: Listener<*>) {
        listeners.add(listener)
    }

    override fun register(listener: ParallelListener<*>) {
        parallelListeners.add(listener)
    }

    override fun subscribe() {
        listeners.forEach { EventBus[it.eventID].subscribe(it) }
        parallelListeners.forEach { EventBus[it.eventID].subscribe(it) }
    }

    override fun unsubscribe() {
        listeners.forEach { EventBus[it.eventID].unsubscribe(it) }
        parallelListeners.forEach { EventBus[it.eventID].unsubscribe(it) }
    }
}
