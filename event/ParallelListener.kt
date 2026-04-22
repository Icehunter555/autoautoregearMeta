package dev.wizard.meta.event

class ParallelListener<T : Any>(
    owner: Any,
    eventID: Int,
    function: suspend (T) -> Unit
) : AbstractListener<suspend (T) -> Unit>(owner, eventID, 0, function)
