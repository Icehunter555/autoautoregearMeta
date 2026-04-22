package dev.wizard.meta.module.modules.misc

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.ChatReceiveEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.manager.managers.FriendManager
import dev.wizard.meta.mixins.PatchedITextComponent
import dev.wizard.meta.module.AbstractModule
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.TimeUtils
import dev.wizard.meta.util.accessor.textComponent
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.text.EnumTextColor
import dev.wizard.meta.util.threads.onMainThread
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.client.gui.ChatLine
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.init.SoundEvents
import net.minecraft.network.play.server.SPacketChat
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.TextComponentString
import net.minecraft.util.text.TextFormatting
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

object ChatTweaks : Module("ChatTweaks", Category.MISC, "tweaks for minecraft chat") {
    val extraChatHistory by setting("Extra Chat History", false)
    val maxMessages by setting("Max Message", 1000, 100..5000, 100, { extraChatHistory })
    val cleanChat by setting("Clean Chat", false)
    private val doChatColor by setting("Color Chat Background", false, { !cleanChat })
    private val chatColor by setting("Chat Color", ColorRGB(255, 255, 255), false, { !cleanChat && doChatColor })
    
    private val chatTimeStamps by setting("Chat TimeStamps", TimeStampMode.EVENT)
    private val timeStampColor by setting("TimeStamp Color", EnumTextColor.GRAY, { chatTimeStamps != TimeStampMode.NONE })
    private val timeStampSeparator by setting("Timestamp Separator", Separator.ARROWS, { chatTimeStamps != TimeStampMode.NONE })
    private val timeStampFormat by setting("TimeStamp Format", TimeUtils.TimeFormat.HHMM, { chatTimeStamps != TimeStampMode.NONE })
    private val timeStampUnit by setting("TimeStamp Unit", TimeUtils.TimeUnit.H12, { chatTimeStamps != TimeStampMode.NONE })
    
    val chatAnimation by setting("Chat Animation", false)
    
    private val friendHighlightChat by setting("Friend Highlight Chat", true)
    private val friendMessageSound by setting("Friend Message Sound", false)
    private val friendHighlightTab by setting("Friend Highlight Tab", true)
    private val friendPrefix by setting("Friend Prefix", true)
    private val friendBold by setting("Friend Bold", false)
    private val friendColor by setting("Friend Color", EnumTextColor.GREEN)
    
    private val selfHighlightChat by setting("Self Highlight Chat", false)
    private val selfHighlightTab by setting("Self Highlight Tab", false)
    private val selfBold by setting("Self Bold", false)
    private val selfColor by setting("Self Color", EnumTextColor.LIGHT_PURPLE)
    
    private val whisperSound by setting("Whisper Sound", false)
    
    private val playerNameRegex = Regex("<(.+?)>")

    init {
        listener<PacketEvent.Receive>(-100) {
            val packet = it.packet
            if (packet is SPacketChat) {
                val messageText = packet.chatComponent.unformattedText
                if (whisperSound && isWhisperMessage(messageText)) {
                    onMainThread {
                        mc.soundHandler.playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f))
                    }
                }
                
                if (friendHighlightChat && FriendManager.enabled && replaceFriendName(packet) && friendMessageSound) {
                    onMainThread {
                        mc.soundHandler.playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f))
                    }
                }
                
                if (selfHighlightChat) {
                    replaceSelfName(packet)
                }
            }
        }

        listener<PacketEvent.Receive>(-69696420) {
            val packet = it.packet
            if (packet is SPacketChat && chatTimeStamps == TimeStampMode.PACKET && !it.cancelled) {
                val timestamp = TextComponentString(formattedTime)
                timestamp.appendSibling(packet.textComponent)
                packet.textComponent = timestamp
            }
        }

        listener<ChatReceiveEvent>(-69696420) {
            if (chatTimeStamps == TimeStampMode.EVENT) {
                val timestamp = TextComponentString(formattedTime)
                timestamp.appendSibling(it.message)
                it.message = timestamp
            }
        }
    }

    val formattedTime: String
        get() = "${timeStampSeparator.left}${TextFormatting.fromColorIndex(timeStampColor.index)}${TimeUtils.getTime(timeStampFormat, timeStampUnit)}${timeStampSeparator.right} "

    val time: String
        get() = "${timeStampSeparator.left}${TimeUtils.getTime(timeStampFormat, timeStampUnit)}${timeStampSeparator.right} "

    private fun isWhisperMessage(message: String): Boolean {
        return message.contains(" whispers: ", ignoreCase = true)
    }

    private fun replaceSelfName(packet: SPacketChat): Boolean {
        val selfName = mc.session.username
        val playerName = playerNameRegex.findAll(packet.textComponent.unformattedText)
            .map { it.groupValues[1] }
            .firstOrNull { it.equals(selfName, true) }

        if (playerName != null) {
            val textComponent = packet.textComponent as? PatchedITextComponent ?: return false
            val siblings = textComponent.inplaceIterator.asSequence().toList()

            if (siblings.size == 1) {
                val replaced = replaceComponentSelf(packet.textComponent, playerName)
                if (replaced != null) {
                    packet.textComponent = replaced
                }
            } else {
                val selfNameComponent = siblings.firstOrNull { it.unformattedText == playerName }
                if (selfNameComponent != null) {
                    selfNameComponent.style.color = selfColor.textFormatting
                } else {
                    replaceSiblingSelf(packet.textComponent, playerName)
                }
            }
            return true
        }
        return false
    }

    private fun replaceSiblingSelf(component: ITextComponent, playerName: String): Boolean {
        val siblings = component.siblings
        for (i in siblings.indices) {
            val replaced = replaceComponentSelf(siblings[i], playerName)
            if (replaced != null) {
                siblings[i] = replaced
                return true
            }
        }
        
        for (sibling in siblings) {
            if (replaceSiblingSelf(sibling, playerName)) return true
        }
        return false
    }

    private fun replaceComponentSelf(component: ITextComponent, playerName: String): TextComponentString? {
        var found = false
        val newText = playerNameRegex.replace(component.formattedText) {
            val originalName = it.groupValues[1]
            if (originalName.contains(playerName, true)) {
                found = true
                "<${getSelfReplacement(playerName)}>"
            } else {
                it.value
            }
        }
        
        return if (found) {
            val text = TextComponentString(newText)
            text.style = component.style
            text
        } else null
    }

    private fun getSelfReplacement(name: String): String {
        return buildString {
            append(TextFormatting.RESET)
            append(selfColor.textFormatting)
            if (selfBold) append(TextFormatting.BOLD)
            append(name)
            append(TextFormatting.RESET)
        }
    }

    private fun replaceFriendName(packet: SPacketChat): Boolean {
        val playerName = playerNameRegex.findAll(packet.textComponent.unformattedText)
            .map { it.groupValues[1] }
            .firstOrNull { FriendManager.isFriend(it) }

        if (playerName != null) {
            val textComponent = packet.textComponent as? PatchedITextComponent ?: return false
            val siblings = textComponent.inplaceIterator.asSequence().toList()

            if (siblings.size == 1) {
                val replaced = replaceComponent(packet.textComponent, playerName)
                if (replaced != null) {
                    packet.textComponent = replaced
                }
            } else {
                val friendNameComponent = siblings.firstOrNull { it.unformattedText == playerName }
                if (friendNameComponent != null) {
                    friendNameComponent.style.color = friendColor.textFormatting
                } else {
                    replaceSibling(packet.textComponent, playerName)
                }
            }
            return true
        }
        return false
    }

    private fun replaceSibling(component: ITextComponent, playerName: String): Boolean {
        val siblings = component.siblings
        for (i in siblings.indices) {
            val replaced = replaceComponent(siblings[i], playerName)
            if (replaced != null) {
                siblings[i] = replaced
                return true
            }
        }
        
        for (sibling in siblings) {
            if (replaceSibling(sibling, playerName)) return true
        }
        return false
    }

    private fun replaceComponent(component: ITextComponent, playerName: String): TextComponentString? {
        var found = false
        val newText = playerNameRegex.replace(component.formattedText) {
            val originalName = it.groupValues[1]
            if (originalName.contains(playerName, true)) {
                found = true
                "${if (!friendPrefix) "<" else ""}${getFriendReplacement(playerName)}${if (!friendPrefix) ">" else ""}"
            } else {
                it.value
            }
        }
        
        return if (found) {
            val text = TextComponentString(newText)
            text.style = component.style
            text
        } else null
    }

    private fun getFriendReplacement(name: String): String {
        return if (FriendManager.isFriend(name)) {
            buildString {
                if (friendPrefix) {
                    append("[")
                    append(TextFormatting.RESET)
                    append(friendColor.textFormatting)
                    append("F")
                    append(TextFormatting.RESET)
                    append("] ")
                }
                append(TextFormatting.RESET)
                append(friendColor.textFormatting)
                if (friendBold) append(TextFormatting.BOLD)
                append(name)
                append(TextFormatting.RESET)
            }
        } else {
            name
        }
    }

    @JvmStatic
    fun getChatColor(): Int {
        return ColorRGB(chatColor.r, chatColor.g, chatColor.b, 47).rgba
    }

    // Mixin callbacks need to be handled carefully if they are external
    // Assuming these are called from Mixins
    @JvmStatic
    fun handleSetChatLine(drawnChatLines: MutableList<ChatLine>, chatLines: MutableList<ChatLine>, chatComponent: ITextComponent, chatLineId: Int, updateCounter: Int, displayOnly: Boolean, ci: CallbackInfo) {
        if (!extraChatHistory || !isEnabled) return

        while (drawnChatLines.isNotEmpty() && drawnChatLines.size > maxMessages) {
            drawnChatLines.removeAt(drawnChatLines.lastIndex)
        }

        if (!displayOnly) {
            chatLines.add(0, ChatLine(updateCounter, chatComponent, chatLineId))
            while (chatLines.isNotEmpty() && chatLines.size > maxMessages) {
                chatLines.removeAt(chatLines.lastIndex)
            }
        }
        ci.cancel()
    }

    @JvmStatic
    fun getPlayerName(info: NetworkPlayerInfo, cir: CallbackInfoReturnable<String>) {
        if (isDisabled) return
        val name = info.gameProfile.name
        
        if (selfHighlightTab && mc.player?.name.equals(name, true)) {
            cir.returnValue = getSelfReplacement(name)
            return
        }
        
        if (friendHighlightTab && FriendManager.isFriend(name)) {
            cir.returnValue = getFriendReplacement(name)
        }
    }

    enum class TimeStampMode(override val displayName: String) : DisplayEnum {
        EVENT("All"),
        PACKET("Packet"),
        NONE("None")
    }

    enum class Separator(override val displayName: String, val left: String, val right: String) : DisplayEnum {
        ARROWS("< >", "<", ">"),
        SQUARE_BRACKETS("[ ]", "[", "]"),
        CURLY_BRACKETS("{ }", "{", "}"),
        ROUND_BRACKETS("( )", "(", ")"),
        NONE("None", "", "")
    }
}
