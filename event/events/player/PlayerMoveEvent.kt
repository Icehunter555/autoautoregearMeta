package dev.wizard.meta.event.events.player

import dev.wizard.meta.event.*
import net.minecraft.client.entity.EntityPlayerSP

sealed class PlayerMoveEvent : Event {
    class Pre(private val player: EntityPlayerSP) : PlayerMoveEvent(), ICancellable by Cancellable(), EventPosting by Companion {
        private val prevX = player.motionX
        private val prevY = player.motionY
        private val prevZ = player.motionZ
        private var _x = Double.NaN
        private var _y = Double.NaN
        private var _z = Double.NaN

        val isModified: Boolean get() = x != prevX || y != prevY || z != prevZ

        var x: Double
            get() = get(_x, player.motionX)
            set(value) { _x = value }

        var y: Double
            get() = get(_y, player.motionY)
            set(value) { _y = value }

        var z: Double
            get() = get(_z, player.motionZ)
            set(value) { _z = value }

        private fun get(x: Double, y: Double): Double {
            return if (cancelled) 0.0 else if (!x.isNaN()) x else y
        }

        companion object : NamedProfilerEventBus("trollPlayerMove")
    }

    object Post : PlayerMoveEvent(), EventPosting by EventBus()
}
