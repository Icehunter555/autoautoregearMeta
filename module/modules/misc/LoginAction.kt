package dev.wizard.meta.module.modules.misc

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.command.commands.InfoCommand
import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.events.ConnectionEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.manager.managers.HotbarSwitchManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.combat.AutoRegear
import dev.wizard.meta.util.InfoCalculator
import dev.wizard.meta.util.MovementUtils
import dev.wizard.meta.util.TimeUtils
import dev.wizard.meta.util.text.MessageSendUtils
import dev.wizard.meta.util.text.NoSpamMessage
import net.minecraft.client.Minecraft
import net.minecraft.util.text.TextFormatting
import java.time.LocalTime

object LoginAction : Module(
    name = "LoginAction",
    category = Category.MISC,
    description = "Sends a given message(s) to public chat on login.",
    modulePriority = 150
) {
    private val sendLoginInfo by setting("Send Login Info", true)
    private val doLoginMessage by setting("Send Login message", true)
    private val loginMessage by setting("Message", "Hello World!") { doLoginMessage }
    private val sendAfterMoving by setting("Send After Moving", false) { doLoginMessage }
    private val reloadSound by setting("Reload Sound", false)
    private val autoRegear by setting("Auto Regear", false)
    private val actionDelay by setting("Action Delay", 3, 1..20, 1)

    private val actionTimer = TickTimer(TimeUnit.SECONDS)
    private var sentMessage = false
    private var sentInfo = false
    private var hasRegeared = false
    private var hasReloaded = false
    private var moved = false

    init {
        listener<ConnectionEvent.Disconnect> {
            sentMessage = false
            moved = false
            sentInfo = false
            hasReloaded = false
            hasRegeared = false
            actionTimer.reset()
        }

        listener<ConnectionEvent.Connect> {
            actionTimer.reset()
        }

        listener<TickEvent.Post> {
            if (!actionTimer.tick(actionDelay.toLong())) return@listener

            if (doLoginMessage && !sentMessage && (!sendAfterMoving || moved)) {
                MessageSendUtils.sendServerMessage(this, loginMessage.replace("/", ""))
                sentMessage = true
            }

            if (!moved) {
                moved = MovementUtils.isMoving(player)
            }

            if (sendLoginInfo && !sentInfo) {
                displayLoginInfo()
                sentInfo = true
            }

            if (reloadSound && !hasReloaded) {
                mc.soundHandler.resumeSounds()
                hasReloaded = true
            }

            if (autoRegear && !hasRegeared) {
                AutoRegear.placeShulker = true
                hasRegeared = true
            }
        }
    }

    private fun displayLoginInfo() {
        val name = player.gameProfile.name
        sendAndFormat(getTimeGreeting(name))
        sendAndFormat("${TextFormatting.WHITE}Welcome to ${TextFormatting.BOLD}${TextFormatting.LIGHT_PURPLE}${getServerInfo()}${TextFormatting.RESET}!")
        sendAndFormat("${TextFormatting.WHITE}There are ${TextFormatting.LIGHT_PURPLE}${getOnlinePlayers()}${TextFormatting.RESET}${TextFormatting.WHITE} players online! (${TextFormatting.GREEN}${InfoCalculator.onlineFriendCount()}${TextFormatting.RESET} friends, ${TextFormatting.RED}${getLoadedEnemies()}${TextFormatting.RESET} close)")
        sendAndFormat("${TextFormatting.WHITE}TPS: ${InfoCommand.getTpsWithColor()}   ${TextFormatting.WHITE}FPS: ${getFpsWithColor()}${TextFormatting.RESET}")
        sendAndFormat("${TextFormatting.GRAY}${TimeUtils.getTime(TimeUtils.TimeFormat.HHMMSS, TimeUtils.TimeUnit.H12)}${TextFormatting.RESET}")
        sendAndFormat("${TextFormatting.GRAY}${TimeUtils.getDate(TimeUtils.DateFormat.DDMMYY)}${TextFormatting.RESET}")
        sendAndFormat("${TextFormatting.DARK_GRAY}Meta Client 0.3B-10mq29")
    }

    private fun getLoadedEnemies(): Int {
        if (mc.isSingleplayer || mc.currentServerData == null) return 0
        return world.playerEntities.count {
            player.getDistance(it) < 120.0 && !dev.wizard.meta.util.EntityUtils.isFriend(it) && !dev.wizard.meta.util.EntityUtils.isSelf(it) && !dev.wizard.meta.util.EntityUtils.isNaked(it)
        }
    }

    private fun getFpsWithColor(): String {
        val fps = Minecraft.getDebugFPS()
        val color = when {
            fps >= 60 -> TextFormatting.GREEN
            fps >= 45 -> TextFormatting.YELLOW
            fps >= 30 -> TextFormatting.GOLD
            fps >= 15 -> TextFormatting.RED
            else -> TextFormatting.DARK_RED
        }
        return "$color$fps${TextFormatting.RESET}"
    }

    private fun getServerInfo(): String {
        return if (mc.isSingleplayer) "Singleplayer"
        else mc.currentServerData?.serverIP?.ifEmpty { "Limbo" } ?: "Limbo"
    }

    private fun getOnlinePlayers(): Int {
        return if (mc.isSingleplayer) 1 else mc.connection?.playerInfoMap?.size ?: 1
    }

    private fun getTimeGreeting(name: String): String {
        val hour = LocalTime.now().hour
        return when (hour) {
            in 6..11 -> "${TextFormatting.WHITE}Good Morning, ${TextFormatting.LIGHT_PURPLE}${TextFormatting.BOLD}$name${TextFormatting.RESET}${TextFormatting.WHITE}!${TextFormatting.RESET}"
            in 12..17 -> "${TextFormatting.WHITE}Good Afternoon, ${TextFormatting.LIGHT_PURPLE}${TextFormatting.BOLD}$name${TextFormatting.RESET}${TextFormatting.WHITE}!${TextFormatting.RESET}"
            in 18..23 -> "${TextFormatting.WHITE}Good Evening, ${TextFormatting.LIGHT_PURPLE}${TextFormatting.BOLD}$name${TextFormatting.RESET}${TextFormatting.WHITE}!${TextFormatting.RESET}"
            else -> "Late night ${TextFormatting.LIGHT_PURPLE}${TextFormatting.BOLD}$name${TextFormatting.RESET}${TextFormatting.WHITE}?${TextFormatting.RESET}"
        }
    }

    private fun sendAndFormat(message: String) {
        NoSpamMessage.sendRaw("  $message", false)
    }
}
