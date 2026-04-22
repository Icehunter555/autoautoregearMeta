package dev.wizard.meta.module.modules.client

import club.minnced.discord.rpc.DiscordEventHandlers
import club.minnced.discord.rpc.DiscordRichPresence
import club.minnced.discord.rpc.DiscordUser
import dev.wizard.meta.MetaMod
import dev.wizard.meta.gui.hudgui.elements.text.TPS
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.misc.AltProtect
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.InfoCalculator
import net.minecraft.client.Minecraft

object DiscordRPC : Module(
    "DiscordRPC",
    category = Category.CLIENT,
    description = "Configure the discord rpc system",
    alwaysEnabled = true
) {
    private val line1Left by setting(this, EnumSetting(settingName("Line 1 Left"), LineInfo.VERSION))
    private val line1Right by setting(this, EnumSetting(settingName("Line 1 Right"), LineInfo.USERNAME))
    private val line2Left by setting(this, EnumSetting(settingName("Line 2 Left"), LineInfo.SERVER_IP))
    private val line2Right by setting(this, EnumSetting(settingName("Line 2 Right"), LineInfo.HEALTH))
    private val hideWhenAltProtect by setting(this, BooleanSetting(settingName("Hide When AltProtect"), false, { AltProtect.isEnabled }))

    private val presence = DiscordRichPresence()
    private val lib = club.minnced.discord.rpc.DiscordRPC.INSTANCE
    private var connected = false
    private var lastUpdate = 0L
    @Volatile
    private var thread: Thread? = null
    @Volatile
    private var running = false
    private const val ID = "1446991835913523324"
    private const val INTERVAL = 8000L

    init {
        onEnable { start() }
        onDisable { end() }
    }

    @Synchronized
    private fun start() {
        if (running) {
            MetaMod.logger.info("[DiscordRPC] Already running, stopping first...")
            end()
            Thread.sleep(1000L)
        }
        MetaMod.logger.info("[DiscordRPC] Initializing Discord RPC")
        try {
            val handlers = DiscordEventHandlers().apply {
                ready = DiscordEventHandlers.OnReady { user ->
                    MetaMod.logger.info("[DiscordRPC] Ready! (User: ${user.username})")
                }
                disconnected = DiscordEventHandlers.OnStatus { errorCode, message ->
                    MetaMod.logger.warn("[DiscordRPC] Disconnected: $errorCode - $message")
                }
                errored = DiscordEventHandlers.OnStatus { errorCode, message ->
                    MetaMod.logger.error("[DiscordRPC] Error: $errorCode - $message")
                }
            }
            lib.Discord_Initialize(ID, handlers, true, "")
            connected = true
            presence.startTimestamp = System.currentTimeMillis() / 1000L
            presence.largeImageKey = "meta-logo"
            presence.largeImageText = "Meta 0.3B-10mq29"
            updatePresence()
            lib.Discord_UpdatePresence(presence)
            lastUpdate = System.currentTimeMillis()
            running = true
            thread = Thread({
                while (running && !Thread.currentThread().isInterrupted) {
                    try {
                        lib.Discord_RunCallbacks()
                        if (System.currentTimeMillis() - lastUpdate >= INTERVAL) {
                            updatePresence()
                            lib.Discord_UpdatePresence(presence)
                            lastUpdate = System.currentTimeMillis()
                        }
                        Thread.sleep(2000L)
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    } catch (e: Exception) {
                        MetaMod.logger.error("[DiscordRPC] Error in thread: ${e.message}")
                    }
                }
            }, "Discord-RPC-Callback-Handler").apply {
                isDaemon = true
                start()
            }
            MetaMod.logger.info("[DiscordRPC] Started successfully")
        } catch (e: Exception) {
            MetaMod.logger.error("[DiscordRPC] Failed to initialize: ${e.message}")
            running = false
            connected = false
            disable()
        }
    }

    @Synchronized
    private fun end() {
        if (!connected) return
        MetaMod.logger.info("[DiscordRPC] Shutting down")
        running = false
        thread?.let {
            if (!it.isInterrupted) it.interrupt()
            try {
                it.join(3000L)
            } catch (e: InterruptedException) {
                MetaMod.logger.warn("[DiscordRPC] Interrupted while waiting for thread")
            }
        }
        thread = null
        try {
            lib.Discord_ClearPresence()
            lib.Discord_Shutdown()
            connected = false
        } catch (e: Exception) {
            MetaMod.logger.warn("[DiscordRPC] Error during shutdown: ${e.message}")
        }
    }

    private fun updatePresence() {
        val left1 = getLine(line1Left)
        val right1 = getLine(line1Right)
        val left2 = getLine(line2Left)
        val right2 = getLine(line2Right)
        presence.details = left1 + getSeparator(0) + right1
        presence.state = left2 + getSeparator(1) + right2
    }

    private fun shouldHideInfo(): Boolean = AltProtect.isEnabled && hideWhenAltProtect

    private fun getLine(line: LineInfo): String {
        if (shouldHideInfo()) {
            return when (line) {
                LineInfo.VERSION -> "Meta 0.3B-10mq29"
                LineInfo.WORLD -> "Main Menu"
                LineInfo.USERNAME -> AltProtect.initialName ?: "Err"
                LineInfo.FPS -> "${Minecraft.getDebugFPS()} FPS"
                LineInfo.SERVER_IP -> "Menu"
                else -> "Unknown"
            }
        }
        return when (line) {
            LineInfo.VERSION -> "Meta 0.3B-10mq29"
            LineInfo.WORLD -> if (mc.isSingleplayer) "Singleplayer" else if (mc.currentServerData != null) "Multiplayer" else "Main Menu"
            LineInfo.DIMENSION -> InfoCalculator.dimension()
            LineInfo.USERNAME -> if (AltProtect.isEnabled && AltProtect.nameProtect) "[Hidden]" else (mc.player?.name ?: mc.session.username)
            LineInfo.HEALTH -> mc.player?.let { "${it.health.toInt()} HP" } ?: "Unknown"
            LineInfo.SERVER_IP -> {
                if (mc.player == null) "Menu"
                else if (mc.isSingleplayer) "Singleplayer"
                else mc.currentServerData?.serverIP ?: "Limbo"
            }
            LineInfo.COORDS -> mc.player?.let { val pos = it.position; "(${pos.x}, ${pos.y}, ${pos.z})" } ?: "Unknown"
            LineInfo.SPEED -> mc.player?.let { "%.1f m/s".format(InfoCalculator.speed()) } ?: "Unknown"
            LineInfo.FPS -> "${Minecraft.getDebugFPS()} FPS"
            LineInfo.TPS -> mc.player?.let { "%.1f TPS".format(TPS.tpsBuffer.average()) } ?: "Unknown"
            LineInfo.PING -> if (InfoCalculator.ping() <= 1) "Unknown" else InfoCalculator.ping().toString()
            LineInfo.NONE -> " "
        }
    }

    private fun getSeparator(line: Int): String {
        return if (line == 0) {
            if (line1Left == LineInfo.NONE || line1Right == LineInfo.NONE) " " else " | "
        } else {
            if (line2Left == LineInfo.NONE || line2Right == LineInfo.NONE) " " else " | "
        }
    }

    private enum class LineInfo { VERSION, WORLD, DIMENSION, USERNAME, HEALTH, SERVER_IP, COORDS, SPEED, FPS, TPS, PING, NONE }
}
