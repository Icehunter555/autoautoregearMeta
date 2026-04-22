package dev.wizard.meta.util.inventory

import dev.wizard.meta.event.SafeClientEvent
import net.minecraft.inventory.ClickType
import net.minecraft.inventory.Slot

class Click(
    private val windowID: Int,
    private val slotProvider: (SafeClientEvent) -> Slot?,
    private val mouseButton: (SafeClientEvent) -> Int?,
    private val type: ClickType
) : Step {

    constructor(windowID: Int, slotProvider: (SafeClientEvent) -> Slot?, mouseButton: Int, type: ClickType) :
            this(windowID, slotProvider, { mouseButton }, type)

    constructor(windowID: Int, slot: Slot, mouseButton: Int, type: ClickType) :
            this(windowID, { slot }, { mouseButton }, type)

    override fun run(event: SafeClientEvent): StepFuture {
        val slot = slotProvider(event)
        val button = mouseButton(event)
        return if (slot != null && button != null) {
            ClickFuture(event.clickSlot(windowID, slot, button, type))
        } else {
            InstantFuture
        }
    }
}
