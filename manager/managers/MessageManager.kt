package dev.wizard.meta.manager.managers

import dev.fastmc.common.TickTimer
import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.ListenerKt.concurrentListener
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.manager.Manager
import dev.wizard.meta.module.AbstractModule
import dev.wizard.meta.module.modules.client.Settings
import dev.wizard.meta.util.accessor.NetworkKt.setPacketMessage
import dev.wizard.meta.util.extension.CollectionKt.synchronized
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import net.minecraft.network.play.client.CPacketChatMessage

object MessageManager : Manager() {
    private val messageQueue: NavigableSet<QueuedMessage> = TreeSet<QueuedMessage>().synchronized()
    private val packetSet: MutableSet<CPacketChatMessage> = HashSet<CPacketChatMessage>().synchronized()
    private val timer = TickTimer()
    var lastPlayerMessage: String = ""
    private val activeModifiers: NavigableSet<MessageModifier> = TreeSet<MessageModifier>().synchronized()

    fun sendMessageDirect(message: String) {
        val packet = CPacketChatMessage(message)
        packetSet.add(packet)
        connection?.sendPacket(packet)
    }

    fun addMessageToQueue(message: String, source: Any, priority: Int = 0) {
        addMessageToQueue(CPacketChatMessage(message), source, priority)
    }

    fun addMessageToQueue(packet: CPacketChatMessage, source: Any, priority: Int = 0) {
        val queuedMessage = QueuedMessage(priority, source, packet)
        messageQueue.add(queuedMessage)
        packetSet.add(packet)
    }

    fun AbstractModule.newMessageModifier(filter: (QueuedMessage) -> Boolean = { true }, modifier: (QueuedMessage) -> String): MessageModifier {
        return MessageModifier(this.modulePriority, filter, modifier)
    }

    init {
        listener<PacketEvent.Send>(priority = 1) { event ->
            val packet = event.packet
            if (packet !is CPacketChatMessage || packetSet.remove(packet)) return@listener
            
            event.cancel()
            if (packet.message != lastPlayerMessage) {
                addMessageToQueue(packet, event, 0)
            } else {
                addMessageToQueue(packet, mc.player ?: event, 0x7FFFFFFE)
            }
        }

        concurrentListener<TickEvent.Post> {
            if (messageQueue.isEmpty()) {
                QueuedMessage.idCounter.set(Int.MIN_VALUE)
            } else {
                if (timer.tick(Settings.messageDelay)) {
                    messageQueue.pollFirst()?.let { queuedMessage ->
                        synchronized(activeModifiers) {
                            for (modifier in activeModifiers) {
                                modifier.apply(queuedMessage)
                            }
                        }
                        if (queuedMessage.packet.message.isNotBlank()) {
                            connection.sendPacket(queuedMessage.packet)
                            timer.reset()
                        }
                    }
                }
                while (messageQueue.size > Settings.maxMessageQueueSize) {
                    messageQueue.pollLast()
                }
            }
        }
    }

    class MessageModifier(
        private val priority: Int,
        private val filter: (QueuedMessage) -> Boolean,
        private val modifier: (QueuedMessage) -> String
    ) : Comparable<MessageModifier> {
        private val id = idCounter.getAndIncrement()

        fun enable() {
            activeModifiers.add(this)
        }

        fun disable() {
            activeModifiers.remove(this)
        }

        fun apply(queuedMessage: QueuedMessage) {
            if (filter(queuedMessage)) {
                queuedMessage.packet.setPacketMessage(modifier(queuedMessage))
            }
        }

        override fun compareTo(other: MessageModifier): Int {
            val result = -priority.compareTo(other.priority)
            return if (result != 0) result else id.compareTo(other.id)
        }

        companion object {
            private val idCounter = AtomicInteger(Int.MIN_VALUE)
        }
    }

    class QueuedMessage(
        val priority: Int,
        val source: Any,
        val packet: CPacketChatMessage
    ) : Comparable<QueuedMessage> {
        private val id = idCounter.getAndIncrement()

        override fun compareTo(other: QueuedMessage): Int {
            val result = -priority.compareTo(other.priority)
            return if (result != 0) result else id.compareTo(other.id)
        }

        companion object {
            val idCounter = AtomicInteger(Int.MIN_VALUE)
        }
    }
}
