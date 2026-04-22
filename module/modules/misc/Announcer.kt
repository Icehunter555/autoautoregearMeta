package dev.wizard.meta.module.modules.misc

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.manager.managers.FriendManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.extension.remove
import dev.wizard.meta.util.math.vector.distanceTo
import dev.wizard.meta.util.text.ChatTextUtils
import dev.wizard.meta.util.text.MessageSendUtils
import net.minecraft.item.ItemAppleGold
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemFood
import net.minecraft.item.ItemPotion
import net.minecraft.network.play.client.CPacketPlayerDigging
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.network.play.client.CPacketUseEntity
import net.minecraft.network.play.server.SPacketChat
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import kotlin.random.Random

object Announcer : Module(
    name = "Announcer",
    category = Category.MISC,
    description = "annoying but fun"
) {
    private val greenText by setting("GreenText", false) {
        greeterMode != Greeter.NONE || movementSpam != MovementMode.NONE || placeAnnounce || eatAnnounce || breakAnnounce || attackAnnounce
    }
    private val bypassAntiSpam by setting("Bypass AntiSpam", false) {
        greeterMode != Greeter.NONE || movementSpam != MovementMode.NONE || placeAnnounce || eatAnnounce || breakAnnounce || attackAnnounce
    }
    private val antiSpamBypassAmount by setting("Bypass Amount", 3, 1..15, 1) {
        (greeterMode != Greeter.NONE || movementSpam != MovementMode.NONE || placeAnnounce || eatAnnounce || breakAnnounce || attackAnnounce) && bypassAntiSpam
    }
    private val worldTimeAnnounce by setting("World Time Announce", false)

    private val greeterMode by setting("Greeter Mode", Greeter.NONE)
    private val greeterJoinMessage by setting("Greeter Join Message", GreeterJMessage.WELCOME) { greeterMode == Greeter.JOIN || greeterMode == Greeter.BOTH }
    private val greeterLeaveMessage by setting("Greeter Leave Message", GreeterLMessage.GOODBYE) { greeterMode == Greeter.LEAVE || greeterMode == Greeter.BOTH }
    private val randomFinish by setting("Random Greeter Ending", false) { greeterMode != Greeter.NONE }
    private val greeterDelay by setting("Greeter Delay", 1, 1..180, 1) { greeterMode != Greeter.NONE }
    private val greeterTarget by setting("Join/leave target", Target.FRIENDS) { greeterMode != Greeter.NONE }

    private val movementSpam by setting("Movement Mode", MovementMode.NONE)
    private val movementDelay by setting("Movement Delay", 30, 1..180, 1) { movementSpam != MovementMode.NONE }
    private val movementThanksTo by setting("Movement Thanks To", ThanksTo.NONE) { movementSpam != MovementMode.NONE }

    private val placeAnnounce by setting("Place Announce", false)
    private val placeDelay by setting("Place Delay", 10, 1..50, 1) { placeAnnounce }
    private val placeThreshold by setting("Place Threshold", 10, 1..100, 1) { placeAnnounce }
    private val placeThanksTo by setting("Place Thanks To", ThanksTo.THANKS_TO) { placeAnnounce }

    private val eatAnnounce by setting("Eat Announce", false)
    private val eatDelay by setting("Eat Delay", 5, 1..50, 1) { eatAnnounce }
    private val eatThreshold by setting("Eat Threshold", 5, 1..50, 1) { eatAnnounce }
    private val eatThanksTo by setting("Eat Thanks To", ThanksTo.THANKS_TO) { eatAnnounce }

    private val breakAnnounce by setting("Break Announce", false)
    private val breakDelay by setting("Break Delay", 10, 1..50, 1) { breakAnnounce }
    private val breakThreshold by setting("Break Threshold", 10, 1..100, 1) { breakAnnounce }
    private val breakThanksTo by setting("Break Thanks To", ThanksTo.THANKS_TO) { breakAnnounce }

    private val attackAnnounce by setting("Attack Announce", false)
    private val attackDelay by setting("Attack Delay", 5, 1..50, 1) { attackAnnounce }
    private val attackThreshold by setting("Attack Threshold", 10, 1..100, 1) { attackAnnounce }
    private val attackThanksTo by setting("Attack Thanks To", ThanksTo.THANKS_TO) { attackAnnounce }

    private val greetTimer = TickTimer()
    private val moveTimer = TickTimer()
    private val placeTimer = TickTimer()
    private val eatTimer = TickTimer()
    private val breakTimer = TickTimer()
    private val attackTimer = TickTimer()

    private var lastPos: BlockPos? = null
    private var lastWorldTime = -1L
    private var wasEating = false
    private var eatingItem = ""
    private var blocks = 0
    private var eats = 0
    private var breaks = 0
    private var places = 0
    private var attacks = 0

    private val greeterAppend = listOf("cookies", "cake", "pizza", "ice cream")
    private val placeMessages = listOf("I just placed {amount} {name}", "I just built with {amount} {name}", "Just placed {amount} {name}")
    private val eatMessages = listOf("I just ate {amount} {name}", "Just consumed {amount} {name}", "I just snacked on {amount} {name}")
    private val breakMessages = listOf("I just mined {amount} {name}", "Just broke {amount} {name}", "Destroyed {amount} {name}")
    private val attackMessages = listOf("I just attacked {amount} entities", "Just hit {amount} entities", "Dealt damage to {amount} entities")
    private val worldTimeMessages = mapOf(
        0L to listOf("Good morning!", "The sun is rising!", "A new day begins!"),
        6000L to listOf("It's noon!", "The sun is at its peak!", "Midday has arrived!"),
        12000L to listOf("The sun is setting!", "Evening approaches!", "Sunset time!"),
        13000L to listOf("Time for bed!", "You can sleep now!", "Bedtime!"),
        18000L to listOf("It's midnight!", "The witching hour!", "Middle of the night!")
    )

    init {
        onEnable {
            blocks = 0
            eats = 0
            breaks = 0
            places = 0
            attacks = 0
            lastWorldTime = -1L
            wasEating = false
            eatingItem = ""
        }

        listener<TickEvent.Post> {
            if (movementSpam != MovementMode.NONE) runMoveSpam()
            if (worldTimeAnnounce) checkWorldTime()
            if (eatAnnounce) checkEating()
        }

        listener<PacketEvent.Send> { event ->
            if (placeAnnounce && event.packet is CPacketPlayerTryUseItemOnBlock) {
                val heldItem = player.getHeldItem(EnumHand.MAIN_HAND)
                if (heldItem.item is ItemBlock) {
                    places++
                    if (places >= placeThreshold && placeTimer.tickAndReset(placeDelay.toLong(), TimeUnit.SECONDS)) {
                        var msg = placeMessages.random().replace("{amount}", places.toString())
                        msg = msg.replace("{name}", heldItem.displayName)
                        MessageSendUtils.sendServerMessage(this, buildMessage(msg, placeThanksTo))
                        places = 0
                    }
                }
            }
            if (breakAnnounce && event.packet is CPacketPlayerDigging && (event.packet as CPacketPlayerDigging).action == CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK) {
                val pos = (event.packet as CPacketPlayerDigging).position
                val blockState = world.getBlockState(pos)
                breaks++
                if (breaks >= breakThreshold && breakTimer.tickAndReset(breakDelay.toLong(), TimeUnit.SECONDS)) {
                    var msg = breakMessages.random().replace("{amount}", breaks.toString())
                    msg = msg.replace("{name}", blockState.block.localizedName)
                    MessageSendUtils.sendServerMessage(this, buildMessage(msg, breakThanksTo))
                    breaks = 0
                }
            }
            if (attackAnnounce && event.packet is CPacketUseEntity && (event.packet as CPacketUseEntity).action == CPacketUseEntity.Action.ATTACK) {
                attacks++
                if (attacks >= attackThreshold && attackTimer.tickAndReset(attackDelay.toLong(), TimeUnit.SECONDS)) {
                    val msg = attackMessages.random().replace("{amount}", attacks.toString())
                    MessageSendUtils.sendServerMessage(this, buildMessage(msg, attackThanksTo))
                    attacks = 0
                }
            }
        }

        listener<PacketEvent.Receive> { event ->
            if (greeterMode == Greeter.NONE) return@listener
            if (event.packet is SPacketChat) {
                val message = (event.packet as SPacketChat).chatComponent.unformattedText
                val joinTrigger = " joined the game."
                val leaveTrigger = " left the game."

                if (message.contains(joinTrigger) && greeterMode != Greeter.LEAVE) {
                    val joiner = message.substringBefore(joinTrigger)
                    if (shouldGreet(joiner)) {
                         val msg = when (greeterJoinMessage) {
                            GreeterJMessage.HELLO -> "Hello, $joiner${getGreeterAppend()}"
                            GreeterJMessage.WELCOME -> "Welcome${getServerString()}$joiner${getGreeterAppend()}"
                            GreeterJMessage.GAMETIME -> "${getWorldTimeGreeting()} $joiner${getGreeterAppend()}"
                        }
                        if (greetTimer.tickAndReset(greeterDelay.toLong(), TimeUnit.SECONDS)) {
                             MessageSendUtils.sendServerMessage(this, buildMessage(msg, ThanksTo.NONE))
                        }
                    }
                } else if (message.contains(leaveTrigger) && greeterMode != Greeter.JOIN) {
                    val leaver = message.substringBefore(leaveTrigger)
                    if (shouldGreet(leaver)) {
                        val msg = when (greeterLeaveMessage) {
                            GreeterLMessage.CYA -> "Cya, $leaver"
                            GreeterLMessage.GOODBYE -> "Goodbye, $leaver"
                            GreeterLMessage.GAMETIME -> "Goodbye, $leaver, ${getWorldTimeGoodbye()}"
                        }
                        if (greetTimer.tickAndReset(greeterDelay.toLong(), TimeUnit.SECONDS)) {
                            MessageSendUtils.sendServerMessage(this, buildMessage(msg, ThanksTo.NONE))
                        }
                    }
                }
            }
        }
    }

    private fun checkEating() {
        val activeHand = player.activeHand
        val activeItem = if (activeHand != null) player.getHeldItem(activeHand) else null
        val isEating = activeHand != null && player.isHandActive

        if (isEating && activeItem != null) {
            val item = activeItem.item
            if (item is ItemFood || item is ItemAppleGold || item is ItemPotion) {
                if (!wasEating) {
                    eatingItem = activeItem.displayName
                }
                wasEating = true
            }
        } else if (wasEating) {
            eats++
            if (eats >= eatThreshold && eatTimer.tickAndReset(eatDelay.toLong(), TimeUnit.SECONDS)) {
                val msg = eatMessages.random().replace("{amount}", eats.toString()).replace("{name}", eatingItem)
                MessageSendUtils.sendServerMessage(this, buildMessage(msg, eatThanksTo))
                eats = 0
            }
            wasEating = false
            eatingItem = ""
        }
    }

    private fun checkWorldTime() {
        val currentTime = world.worldTime % 24000
        for ((threshold, messages) in worldTimeMessages) {
            if (lastWorldTime == -1L) continue
            val crossed = if (lastWorldTime + 1 <= threshold) threshold <= currentTime else currentTime < lastWorldTime
            if (crossed) {
                val msg = messages.random()
                MessageSendUtils.sendServerMessage(this, msg)
            }
        }
        lastWorldTime = currentTime
    }

    private fun runMoveSpam() {
        if (moveTimer.tickAndReset(movementDelay.toLong(), TimeUnit.SECONDS) && lastPos != null) {
            val dist = lastPos!!.distanceTo(player.posX, player.posY, player.posZ)
            val distTraveled = String.format("%.2f", dist)
            val msg = when (movementSpam) {
                MovementMode.TP -> "I just teleported $distTraveled blocks"
                MovementMode.WALK -> "I just walked $distTraveled blocks"
                MovementMode.FLIGHT -> "I just flew $distTraveled blocks"
                MovementMode.PHASE -> "I just phased $distTraveled blocks"
                MovementMode.NOCLIP -> "I just noclipped $distTraveled blocks"
                else -> ""
            }
            if (msg.isNotEmpty()) {
                MessageSendUtils.sendServerMessage(this, buildMessage(msg, movementThanksTo))
            }
            lastPos = EntityUtils.getBetterPosition(player)
        } else if (lastPos == null) {
            moveTimer.reset()
            lastPos = EntityUtils.getBetterPosition(player)
        }
    }

    private fun buildMessage(message: String, thanks: ThanksTo): String {
        val sb = StringBuilder(message)
        if (greenText) {
            sb.insert(0, "> ")
        }
        val thanksText = when (thanks) {
            ThanksTo.THANKS_TO -> " thanks to Meta Client!"
            ThanksTo.POWER_OF -> " with the power of Meta Client!"
            ThanksTo.NONE -> "!"
        }
        sb.append(thanksText)
        if (bypassAntiSpam && !MessageModifier.getAntiSpamBypass()) {
            sb.append(" [${ChatTextUtils.generateRandomSuffix(antiSpamBypassAmount)}]")
        }
        return sb.toString()
    }

    private fun getServerString(): String {
        if (mc.isIntegratedServerRunning) return ", "
        val serverData = mc.currentServerData ?: return ", "
        return " to ${serverData.serverIP.remove(".org", ".com", ".net")}, "
    }

    private fun getWorldTimeGreeting(): String {
        val time = world.worldTime % 24000
        return when (time) {
            in 0..11999 -> "Good Morning"
            in 12000..17999 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    private fun getWorldTimeGoodbye(): String {
        val time = world.worldTime % 24000
        return when (time) {
            in 0..11999 -> "have a good day"
            in 12000..17999 -> "have a good afternoon"
            else -> "have a good night"
        }
    }

    private fun getGreeterAppend(): String {
        return if (randomFinish) ", hope you brought ${greeterAppend.random()}" else ""
    }

    private fun shouldGreet(name: String): Boolean {
        return if (greeterTarget == Target.FRIENDS) {
            FriendManager.isFriend(name)
        } else {
            true
        }
    }

    private enum class Greeter { JOIN, LEAVE, BOTH, NONE }
    private enum class GreeterJMessage { WELCOME, HELLO, GAMETIME }
    private enum class GreeterLMessage { GOODBYE, CYA, GAMETIME }
    private enum class MovementMode { FLIGHT, WALK, TP, PHASE, NOCLIP, NONE }
    private enum class Target { FRIENDS, EVERYONE }
    private enum class ThanksTo { THANKS_TO, POWER_OF, NONE }
}
