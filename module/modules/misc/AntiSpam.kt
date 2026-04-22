package dev.wizard.meta.module.modules.misc

import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import net.minecraft.network.play.server.SPacketChat
import net.minecraft.util.text.TextComponentString
import net.minecraft.util.text.TextFormatting

object AntiSpam : Module(
    name = "AntiSpam",
    category = Category.MISC,
    description = "Prevents chat spam by grouping duplicate messages"
) {
    private val showNumber by setting("Show Number", true)
    private val messageMap = HashMap<String, MessageData>()
    private var messageId = 2

    init {
        onDisable {
            messageMap.clear()
            messageId = 2
        }

        listener<PacketEvent.Receive>(-2932) {
            if (it.packet !is SPacketChat || it.isCancelled) return@listener
            val packet = it.packet as SPacketChat
            val component = packet.chatComponent
            val message = component.unformattedText

            it.cancel()

            if (messageMap.containsKey(message)) {
                val data = messageMap[message]!!
                data.count++
                val displayMessage = if (showNumber) {
                    TextComponentString("$message ${TextFormatting.GRAY}x${data.count}${TextFormatting.RESET}")
                } else {
                    component
                }
                mc.ingameGUI.chatGUI.printChatMessageWithOptionalDeletion(displayMessage, data.id)
            } else {
                messageId++
                mc.ingameGUI.chatGUI.printChatMessageWithOptionalDeletion(component, messageId)
                messageMap[message] = MessageData(messageId, 1)
            }
        }
    }

    private data class MessageData(val id: Int, var count: Int)
}
