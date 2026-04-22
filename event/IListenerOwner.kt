package dev.wizard.meta.event

interface IListenerOwner {
    fun register(listener: Listener<*>)
    fun register(listener: ParallelListener<*>)
    fun subscribe()
    fun unsubscribe()
}
