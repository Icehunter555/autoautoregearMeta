package dev.wizard.meta.module.modules.misc

import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.text.NoSpamMessage
import net.minecraft.network.play.server.SPacketChat

object AntiLog4J : Module(
    name = "AntiLog4J",
    category = Category.MISC,
    description = "stops extremely retarded people from using log4j",
    modulePriority = 9999
) {
    private val sendMessage by setting("Send Message", false)

    init {
        listener<PacketEvent.Receive>(999) {
            if (it.packet is SPacketChat) {
                val message = it.packet.chatComponent.unformattedText
                if (message.contains("jndi", ignoreCase = true)) {
                    it.cancel()
                    if (sendMessage) {
                        NoSpamMessage.sendMessage("Blocked an attempted log4j message")
                    }
                }
            }
        }
    }
}
