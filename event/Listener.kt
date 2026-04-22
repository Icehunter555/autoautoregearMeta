package dev.wizard.meta.event

class Listener<T : Any>(
    owner: Any,
    eventID: Int,
    priority: Int,
    function: (T) -> Unit
) : AbstractListener<(T) -> Unit>(owner, eventID, priority, function)
