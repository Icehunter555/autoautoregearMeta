package dev.wizard.meta.module.modules.client

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.interfaces.DisplayEnum
import net.minecraft.client.gui.GuiChat
import net.minecraft.network.play.client.CPacketClientStatus
import net.minecraft.network.play.client.CPacketTabComplete
import net.minecraft.network.play.server.SPacketStatistics
import net.minecraft.network.play.server.SPacketTabComplete

object FastLatency : Module(
    "FastLatency",
    category = Category.CLIENT,
    description = "Calculates your ping faster"
) {
    private val mode by setting(this, EnumSetting(settingName("Mode"), Mode.STATISTICS))
    private val sendDelay by setting(this, IntegerSetting(settingName("Send Delay"), 2000, 100..10000, 10))
    private val timeout by setting(this, IntegerSetting(settingName("Timeout"), 5000, 1000..30000, 100))

    private val delayTimer = TickTimer(TimeUnit.MILLISECONDS)
    private var lastSentId = 0L
    private var hasReceived = true
    private var lastSentTime = 0L
    var lastPacketPing = 0L
        private set

    init {
        safeListener<TickEvent.Post>(priority = 1000) {
            val currentTime = System.currentTimeMillis()
            val shouldSendFromTimer = delayTimer.tickAndReset(sendDelay.toLong()) && hasReceived
            val shouldSendFromTimeout = !hasReceived && currentTime - lastSentTime >= timeout.toLong()

            if (shouldSendFromTimer || shouldSendFromTimeout) {
                if (mode == Mode.COMPLETION && mc.currentScreen is GuiChat) return@safeListener

                lastSentTime = System.currentTimeMillis()
                when (mode) {
                    Mode.STATISTICS -> connection.sendPacket(CPacketClientStatus(CPacketClientStatus.State.REQUEST_STATS))
                    Mode.COMPLETION -> connection.sendPacket(CPacketTabComplete("/", null, false))
                }
                hasReceived = false
            }
        }

        safeListener<PacketEvent.Receive>(priority = 1000) {
            val receiveTime = System.currentTimeMillis()
            val packet = it.packet
            when (mode) {
                Mode.STATISTICS -> {
                    if (packet is SPacketStatistics) {
                        lastPacketPing = receiveTime - lastSentTime
                        hasReceived = true
                    }
                }
                Mode.COMPLETION -> {
                    if (packet is SPacketTabComplete) {
                        lastPacketPing = receiveTime - lastSentTime
                        hasReceived = true
                    }
                }
            }
        }
    }

    override fun getHudInfo(): String = "%.2f ms".format(lastPacketPing.toDouble())

    private enum class Mode(override val displayName: String) : DisplayEnum {
        STATISTICS("Stats"),
        COMPLETION("Completion")
    }
}
