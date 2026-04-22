package dev.wizard.meta.module.modules.misc

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.ListenerKt.safeConcurrentListener
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.SafeClientEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.text.MessageDetection
import dev.wizard.meta.util.text.MessageSendUtils
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.text.getUnformatted
import dev.wizard.meta.util.threads.runSafeSuspend
import dev.wizard.meta.MetaMod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minecraft.init.Items
import net.minecraft.network.play.server.SPacketChat
import net.minecraft.network.play.server.SPacketUpdateHealth
import net.minecraft.util.EnumHand
import java.io.File

object AutoCope : Module(
    name = "AutoCope",
    category = Category.MISC,
    description = "Automatically sends excuses when you die"
) {
    private val mode = setting("Mode", Mode.INTERNAL)
    private val copeReply = setting("Cope Reply", "cope \$NAME")

    private const val NAME = "\$NAME"
    private const val CLIENT_NAME = "%CLIENT%"
    
    private val defaultExcuses = arrayOf(
        "Sorry, im using %CLIENT% client", "My ping is so bad", "I was changing my config :(",
        "Why did my AutoTotem break", "I was desynced", "Stupid hackers killed me",
        "Wow, so many try hards", "Lagggg", "I wasn't trying", "I'm not using %CLIENT% client",
        "Thers to much lag", "My dog ate my pc", "Sorry, %CLIENT% Client is really bad",
        "I was lagging", "He was cheating!", "Your hacking!", "Lol imagine actully trying",
        "I didn't move my mouse", "I was playing on easy mode(;", "My wifi went down",
        "I'm playing vanila", "My optifine didn't work", "The CPU cheated!"
    )
    private val file = File("trollhack/excuses.txt")
    private var loadedExcuses = defaultExcuses
    private val clients = arrayOf("Future", "Lambda", "EarthHack", "Salhack", "Pyro", "Impact")
    private val timer = TickTimer(TimeUnit.SECONDS)
    
    private fun createCopeReply(name: String, detect: (SafeClientEvent, String) -> Boolean): CopeReply {
        val setting = setting(name, false)
        return CopeReply(setting, detect)
    }

    private class CopeReply(val setting: BooleanSetting, val detect: (SafeClientEvent, String) -> Boolean)

    private val copeReplyList = listOf(
        createCopeReply("Face Place Cope") { _, message ->
            message.contains("faceplace", true) || message.contains("face place", true)
        },
        createCopeReply("Robot Cope") { _, message ->
            message.contains("ai", true) || message.contains("robot", true)
        },
        createCopeReply("Bed Cope") { _, message ->
            message.contains("bed", true)
        }
    )
    
    private val targetKeyWords = listOf(
        "fuck", "bro is", "iq", "skill", "brain", "fag", "lel", "bad", "suck", "ass",
        "retard", "imagine", "lol", "shit", "gay", "aid", "dont like", "don't like",
        "hate", "noob"
    )

    init {
        onEnable {
            if (mode.value == Mode.EXTERNAL) {
                if (file.exists()) {
                    val cacheList = ArrayList<String>()
                    try {
                        file.forEachLine { 
                            if (it.isNotBlank()) cacheList.add(it.trim()) 
                        }
                        NoSpamMessage.sendMessage(this, "Loaded spammer messages!")
                        loadedExcuses = cacheList.toTypedArray()
                    } catch (e: Exception) {
                        MetaMod.logger.error("Failed loading excuses", e)
                        loadedExcuses = defaultExcuses
                    }
                } else {
                    file.createNewFile()
                    NoSpamMessage.sendError(this, "Excuses file is empty!, please add them in the \u00a77excuses.txt\u00a7f under the \u00a77.minecraft/trollhack\u00a7f directory.")
                    loadedExcuses = defaultExcuses
                }
            } else {
                loadedExcuses = defaultExcuses
            }
        }

        safeConcurrentListener<PacketEvent.Receive> { event ->
            runSafeSuspend {
                val packet = event.packet
                if (packet is SPacketUpdateHealth) {
                    if (loadedExcuses.isEmpty()) return@runSafeSuspend
                    if (packet.health <= 0.0f && !isHoldingTotem(this) && timer.tickAndReset(3L)) {
                        MessageSendUtils.sendServerMessage(this@AutoCope, getExcuse())
                    }
                } else if (packet is SPacketChat) {
                    val message = getUnformatted(packet.chatComponent)
                    val playerName = MessageDetection.Message.OTHER.playerName(message) ?: return@runSafeSuspend
                    val messageWithoutName = message.replace(playerName, "", ignoreCase = true)
                    
                    if (copeReplyList.any { it.setting.value && it.detect(this, messageWithoutName) && targeting(this, messageWithoutName) }) {
                        MessageSendUtils.sendServerMessage(this@AutoCope, copeReply.value.replace(NAME, playerName, ignoreCase = true))
                    }
                }
            }
        }
    }

    private fun isHoldingTotem(event: SafeClientEvent): Boolean {
        return EnumHand.values().any { event.player.getHeldItem(it).item == Items.TOTEM_OF_UNDYING }
    }

    private fun getExcuse(): String {
        return loadedExcuses.random().replace(CLIENT_NAME, clients.random(), ignoreCase = true)
    }

    private fun targeting(event: SafeClientEvent, message: String): Boolean {
        if (message.contains(event.player.name, true)) return true
        if (targetKeyWords.isEmpty()) return false
        return targetKeyWords.any { message.contains(it, true) }
    }

    private enum class Mode(override val displayName: CharSequence) : DisplayEnum {
        INTERNAL("Internal"),
        EXTERNAL("External")
    }
}
