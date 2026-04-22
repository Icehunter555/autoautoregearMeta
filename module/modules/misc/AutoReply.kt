package dev.wizard.meta.module.modules.misc

import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.manager.managers.FriendManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.text.ChatTextUtils
import net.minecraft.network.play.server.SPacketChat

object AutoReply : Module(
    name = "AutoReply",
    category = Category.MISC,
    description = "Reply to sent messages"
) {
    private val defaultMode by setting("AutoReply Mode", DefaultMode.OFF)
    private val coordReplyMode by setting("Coords Reply Mode", CoordsMode.OFF)
    private val myMessage by setting("AutoReply Message", "I just sent this reply using meta client!") { defaultMode != DefaultMode.OFF }
    private val addAntiSpam by setting("Anti Spam Bypass", true) { defaultMode != DefaultMode.OFF }
    private val antiSpamAmount by setting("Anti Spam Amount", 2, 1..7, 1) { defaultMode != DefaultMode.OFF && addAntiSpam }

    private val triggerWords = setOf("coords", "!coords", "!COORDS", "wya", "where are you", "Coords", "position", "where you at")
    private val badTriggers = setOf("my coords", "my coordinates", "my position", "discord")

    init {
        listener<PacketEvent.Receive> { event ->
            if (event.packet is SPacketChat) {
                val message = event.packet.chatComponent.unformattedText
                var sent = false

                if (coordReplyMode != CoordsMode.OFF) {
                    val containsTrigger = triggerWords.any { message.contains(it, true) }
                    val badMessage = badTriggers.any { message.contains(it, true) }

                    if (message.contains(" whispers: ") && containsTrigger && !badMessage) {
                        val sender = message.substringBefore(" whispers:").trim()
                        if (FriendManager.isFriend(sender) || coordReplyMode == CoordsMode.ALL) {
                            player.sendChatMessage("/w $sender My coords are ${pos()} in the ${getDim()}")
                            sent = true
                        }
                    }
                }

                if (defaultMode != DefaultMode.OFF && message.contains(" whispers: ") && !sent) {
                    val sender = message.substringBefore(" whispers:").trim()
                    if (replyCheck(sender)) {
                        player.sendChatMessage("/w $sender $myMessage [${getAntiSpam()}]")
                    }
                }
            }
        }
    }

    private fun getDim(): String {
        return when (player.dimension) {
            0 -> "Overworld"
            -1 -> "Nether"
            1 -> "End"
            else -> "Unknown Dimension"
        }
    }

    private fun pos(): String {
        return "${player.posX.toInt()}, ${player.posY.toInt()}, ${player.posZ.toInt()}"
    }

    private fun replyCheck(name: String): Boolean {
        return when (defaultMode) {
            DefaultMode.ALL -> true
            DefaultMode.NOTFRIENDS -> !FriendManager.isFriend(name)
            DefaultMode.ONLYFRIENDS -> FriendManager.isFriend(name)
            DefaultMode.OFF -> false
        }
    }

    private fun getAntiSpam(): String {
        return if (addAntiSpam) ChatTextUtils.generateRandomSuffix(antiSpamAmount) else ""
    }

    private enum class CoordsMode(override val displayName: CharSequence) : DisplayEnum {
        OFF("Off"),
        ALL("All Players"),
        FRIENDS("Friends Only")
    }

    private enum class DefaultMode(override val displayName: CharSequence) : DisplayEnum {
        OFF("Off"),
        NOTFRIENDS("Not Friends"),
        ONLYFRIENDS("Only Friends"),
        ALL("All Players")
    }
}
