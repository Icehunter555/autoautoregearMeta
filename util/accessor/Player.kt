package dev.wizard.meta.util.accessor

import dev.wizard.meta.mixins.accessor.player.AccessorPlayerControllerMP
import dev.wizard.meta.util.Wrapper
import net.minecraft.client.multiplayer.PlayerControllerMP

var PlayerControllerMP.blockHitDelay: Int
    get() = (this as AccessorPlayerControllerMP).blockHitDelay
    set(value) {
        (this as AccessorPlayerControllerMP).blockHitDelay = value
    }

val PlayerControllerMP.currentPlayerItem: Int
    get() = (this as AccessorPlayerControllerMP).currentPlayerItem

fun PlayerControllerMP.syncCurrentPlayItem() {
    if (Wrapper.player == null) return
    (this as AccessorPlayerControllerMP).trollInvokeSyncCurrentPlayItem()
}
