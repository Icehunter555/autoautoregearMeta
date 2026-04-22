package dev.wizard.meta.event

interface AlwaysListening : IListenerOwner {
    override fun register(listener: Listener<*>) {
        EventBus.get(listener.eventID).subscribe(listener)
    }

    override fun register(listener: ParallelListener<*>) {
        EventBus.get(listener.eventID).subscribe(listener)
    }

    override fun subscribe() {
    }

    override fun unsubscribe() {
    }
}
