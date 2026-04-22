package dev.wizard.meta.event

import net.minecraftforge.fml.common.eventhandler.Event

fun Event.cancel() {
    this.isCanceled = true
}
