package dev.wizard.meta.event.events

import dev.wizard.meta.event.*
import dev.wizard.meta.util.interfaces.DisplayEnum
import net.minecraft.network.Packet

sealed class PacketEvent(val packet: Packet<*>) : Event {
    abstract val side: Side
    abstract val stage: Stage

    enum class Side(override val displayName: CharSequence) : DisplayEnum {
        CLIENT("Client"),
        SERVER("Server");

        override fun toString(): String = displayString
    }

    enum class Stage(override val displayName: CharSequence) : DisplayEnum {
        PRE("Pre"),
        POST("Post");

        override fun toString(): String = displayString
    }

    class Receive(packet: Packet<*>) : PacketEvent(packet), ICancellable by Cancellable(), EventPosting by Companion {
        override val side = Side.SERVER
        override val stage = Stage.PRE
        companion object : EventBus()
    }

    class PostReceive(packet: Packet<*>) : PacketEvent(packet), EventPosting by Companion {
        override val side = Side.SERVER
        override val stage = Stage.POST
        companion object : EventBus()
    }

    class Send(packet: Packet<*>) : PacketEvent(packet), ICancellable by Cancellable(), EventPosting by Companion {
        override val side = Side.CLIENT
        override val stage = Stage.PRE
        companion object : EventBus()
    }

    class PostSend(packet: Packet<*>) : PacketEvent(packet), EventPosting by Companion {
        override val side = Side.CLIENT
        override val stage = Stage.POST
        companion object : EventBus()
    }
}
