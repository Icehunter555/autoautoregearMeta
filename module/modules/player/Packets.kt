package dev.wizard.meta.module.modules.player

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.MetaMod
import dev.wizard.meta.event.events.ConnectionEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.manager.managers.TimerManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.text.MessageSendUtils
import dev.wizard.meta.util.threads.DefaultScope
import dev.wizard.meta.util.threads.ThreadSafetyKt
import kotlinx.coroutines.*
import net.minecraft.network.Packet
import net.minecraft.network.play.client.*
import net.minecraft.network.play.server.*
import net.minecraft.util.text.TextComponentString
import java.io.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object Packets : Module(
    "Packets",
    category = Category.PLAYER,
    description = "Various packet-related tweaks",
    modulePriority = 1000
) {
    private val page by setting(this, EnumSetting(settingName("Page"), Page.LOGGER))

    // Logger
    private val packetLoggerPage by setting(this, EnumSetting(settingName("Logger Settings"), LoggerSettings.GENERAL, { page == Page.LOGGER }))
    private val showClientTicks by setting(this, BooleanSetting(settingName("Show Client Ticks"), false, { page == Page.LOGGER && packetLoggerPage == LoggerSettings.GENERAL }))

    // Limiter
    private val limiterEnabled by setting(this, BooleanSetting(settingName("Limiter Enabled"), false, { page == Page.LIMITER }))
    private val maxPacketsLong by setting(this, FloatSetting(settingName("Max Packets Long"), 22.0f, 10.0f..40.0f, 0.25f, { page == Page.LIMITER && limiterEnabled }))
    private val longTermTicks by setting(this, IntegerSetting(settingName("Long Term Ticks"), 100, 20..250, 5, { page == Page.LIMITER && limiterEnabled }))
    private val maxPacketsShort by setting(this, FloatSetting(settingName("Max Packets Short"), 25.0f, 10.0f..40.0f, 0.25f, { page == Page.LIMITER && limiterEnabled }))
    private val shortTermTicks by setting(this, IntegerSetting(settingName("Short Term Ticks"), 20, 5..50, 1, { page == Page.LIMITER && limiterEnabled }))
    private val minTimer by setting(this, FloatSetting(settingName("Min Timer"), 0.6f, 0.1f..1.0f, 0.01f, { page == Page.LIMITER && limiterEnabled }))

    // Canceller
    private val cancellerEnabled by setting(this, BooleanSetting(settingName("Canceller Enabled"), false, { page == Page.CANCELLER }))
    private val cancelAll by setting(this, BooleanSetting(settingName("Cancel All"), false, { page == Page.CANCELLER && cancellerEnabled }))
    private val packetInput by setting(this, BooleanSetting(settingName("CPacket Input"), false, { page == Page.CANCELLER && cancellerEnabled && !cancelAll }))
    private val packetPlayer by setting(this, BooleanSetting(settingName("CPacket Player"), false, { page == Page.CANCELLER && cancellerEnabled && !cancelAll }))
    private val packetEntityAction by setting(this, BooleanSetting(settingName("CPacket Entity Action"), false, { page == Page.CANCELLER && cancellerEnabled && !cancelAll }))
    private val packetUseEntity by setting(this, BooleanSetting(settingName("CPacket Use Entity"), false, { page == Page.CANCELLER && cancellerEnabled && !cancelAll }))
    private val packetVehicleMove by setting(this, BooleanSetting(settingName("CPacket Vehicle Move"), false, { page == Page.CANCELLER && cancellerEnabled && !cancelAll }))

    private val clientSide: SideSetting
    private val serverSide: SideSetting
    private val fileTimeFormatter = DateTimeFormatter.ofPattern("HH-mm-ss_SSS")
    private var start = 0L
    private var last = 0L
    private var lastTick = 0L
    private val timer = TickTimer(TimeUnit.SECONDS)
    private var filename = ""
    private var lines = ArrayList<String>()
    private var lastPacketTime = -1L
    private val longPacketTime = ArrayDeque<Short>(100)
    private val shortPacketTime = ArrayDeque<Short>(20)
    private var longPacketSpeed = 20.0f
    private var shortPacketSpeed = 20.0f
    private var prevTimerSpeed = 1.0f
    private var numCancelledPackets = 0

    init {
        val clientBuilder = SideSetting.Builder(LoggerSettings.CLIENT)
        registerClient(clientBuilder)
        clientSide = clientBuilder.build()

        val serverBuilder = SideSetting.Builder(LoggerSettings.SERVER)
        registerServer(serverBuilder)
        serverSide = serverBuilder.build()

        onEnable {
            start = System.nanoTime()
            last = start
            lastTick = start
            filename = fileTimeFormatter.format(LocalTime.now()) + ".csv"
            synchronized(this@Packets) {
                lines.add("From,Stage,Packet Name,Time Since Start (ms),Time Since Last (ms),Data\n")
            }
        }

        onDisable {
            write()
            TimerManager.resetTimer(this)
            resetLimiter()
            numCancelledPackets = 0
        }

        safeParallelListener<TickEvent.Pre> {
            if (showClientTicks) {
                synchronized(this@Packets) {
                    val current = System.nanoTime()
                    lines.add("Tick Pulse,,${formattedTime(current - start)},${formattedTime(current - lastTick)}\n")
                    lastTick = current
                }
            }
            if (lines.size >= 500 || timer.tickAndReset(15L)) {
                write()
            }
        }

        listener<TickEvent.Post> {
            if (limiterEnabled) {
                val factorLong = if (longPacketSpeed > maxPacketsLong) maxPacketsLong / longPacketSpeed else null
                val factorShort = if (shortPacketSpeed > maxPacketsShort) maxPacketsShort / shortPacketSpeed else null

                val factor = factorLong ?: factorShort
                if (factor != null) {
                    prevTimerSpeed = Math.min(factor, prevTimerSpeed).coerceAtLeast(minTimer)
                    TimerManager.modifyTimer(this, 50.0f / prevTimerSpeed)
                } else {
                    prevTimerSpeed = 1.0f
                }
            }
        }

        listener<ConnectionEvent.Disconnect> {
            disable()
            resetLimiter()
        }

        listener<PacketEvent.Send> {
            if (cancellerEnabled && (cancelAll ||
                (it.packet is CPacketInput && packetInput) ||
                (it.packet is CPacketPlayer && packetPlayer) ||
                (it.packet is CPacketEntityAction && packetEntityAction) ||
                (it.packet is CPacketUseEntity && packetUseEntity) ||
                (it.packet is CPacketVehicleMove && packetVehicleMove))) {
                it.cancel()
                numCancelledPackets++
            }
            clientSide.handle(it)
            if (limiterEnabled && it.packet is CPacketPlayer) {
                if (lastPacketTime != -1L) {
                    val duration = (System.currentTimeMillis() - lastPacketTime).toShort()
                    synchronized(this@Packets) {
                        addAndTrim(longPacketTime, duration, longTermTicks)
                        addAndTrim(shortPacketTime, duration, shortTermTicks)
                        longPacketSpeed = 1000.0f / (longPacketTime.sum().toFloat() / longPacketTime.size)
                        shortPacketSpeed = 1000.0f / (shortPacketTime.sum().toFloat() / shortPacketTime.size)
                    }
                }
                lastPacketTime = System.currentTimeMillis()
            }
        }

        listener<PacketEvent.PostSend> { clientSide.handle(it) }
        listener<PacketEvent.Receive> { serverSide.handle(it) }
        listener<PacketEvent.PostReceive> { serverSide.handle(it) }
    }

    override fun getHudInfo(): String {
        return when (page) {
            Page.LOGGER -> "Logging"
            Page.LIMITER -> if (limiterEnabled) "Limiting" else "Disabled"
            Page.CANCELLER -> if (cancellerEnabled) "$numCancelledPackets cancelled" else "Disabled"
        }
    }

    private fun write() {
        val cache = synchronized(this) {
            val old = lines
            lines = ArrayList()
            old
        }

        DefaultScope.launch(Dispatchers.IO) {
            try {
                val dir = File("trollhack/packetLogs")
                if (!dir.exists()) dir.mkdir()
                BufferedWriter(FileWriter(File(dir, filename), true)).use { writer ->
                    cache.forEach { writer.write(it) }
                }
            } catch (e: Exception) {
                MetaMod.logger.warn("[$name] Failed saving packet log!", e)
            }
        }
    }

    private fun formattedTime(nano: Long): String = "%.2f".format(nano / 1000000.0)

    private fun resetLimiter() {
        lastPacketTime = -1L
        synchronized(this) {
            longPacketTime.clear()
            shortPacketTime.clear()
        }
        longPacketSpeed = 20.0f
        shortPacketSpeed = 20.0f
    }

    private fun addAndTrim(deque: ArrayDeque<Short>, value: Short, max: Int) {
        deque.addLast(value)
        while (deque.size > max.coerceAtLeast(0)) {
            deque.removeFirst()
        }
    }

    private fun registerClient(builder: SideSetting.Builder) {
        builder.handle<CPacketAnimation> { to("hand", packet.hand) }
        builder.handle<CPacketChatMessage> { to("message", packet.message) }
        builder.handle<CPacketClickWindow> {
            to("windowId", packet.windowId)
            to("slotId", packet.slotId)
            to("mouseButton", packet.usedButton)
            to("clickType", packet.clickType)
            to("transactionId", packet.actionNumber)
            to("clickedItem", packet.clickedItem)
        }
        builder.handle<CPacketConfirmTeleport> { to("teleportId", packet.teleportId) }
        builder.handle<CPacketEntityAction> {
            to("action", packet.action.name)
            to("auxData", packet.auxData)
        }
        builder.handle<CPacketHeldItemChange> { to("slotId", packet.slotId) }
        builder.handle<CPacketKeepAlive> { to("key", packet.key) }
        builder.handle<CPacketPlayer> {
            if (packet.moving) {
                to("x", packet.getX(0.0))
                to("y", packet.getY(0.0))
                to("z", packet.getZ(0.0))
            }
            if (packet.rotating) {
                to("yaw", packet.getYaw(0.0f))
                to("pitch", packet.getPitch(0.0f))
            }
            to("onGround", packet.isOnGround)
        }
        builder.handle<CPacketPlayerDigging> {
            to("x", packet.position.x)
            to("y", packet.position.y)
            to("z", packet.position.z)
            to("facing", packet.facing)
            to("action", packet.action)
        }
        builder.handle<CPacketPlayerTryUseItem> { to("hand", packet.hand) }
        builder.handle<CPacketPlayerTryUseItemOnBlock> {
            to("x", packet.pos.x)
            to("y", packet.pos.y)
            to("z", packet.pos.z)
            to("side", packet.direction)
            to("hitVecX", packet.facingX)
            to("hitVecY", packet.facingY)
            to("hitVecZ", packet.facingZ)
        }
        builder.handle<CPacketUseEntity> {
            to("action", packet.action)
            to("hand", packet.hand)
            to("hitVecX", packet.hitVec?.x)
            to("hitVecY", packet.hitVec?.y)
            to("hitVecZ", packet.hitVec?.z)
        }
        builder.handle<CPacketConfirmTransaction> {
            to("windowId", packet.windowId)
            to("uid", packet.uid)
        }
    }

    private fun registerServer(builder: SideSetting.Builder) {
        builder.handle<SPacketBlockChange> {
            to("x", packet.blockPosition.x)
            to("y", packet.blockPosition.y)
            to("z", packet.blockPosition.z)
            to("block", packet.blockState.block.toString())
        }
        builder.handle<SPacketChat> {
            to("unformattedText", packet.chatComponent.unformattedText)
            to("type", packet.type)
            to("isSystem", packet.isSystem)
        }
        builder.handle<SPacketChunkData> {
            to("chunkX", packet.chunkX)
            to("chunkZ", packet.chunkZ)
            to("extractedSize", packet.extractedSize)
        }
        builder.handle<SPacketConfirmTransaction> {
            to("window", packet.windowId)
            to("id", packet.actionNumber)
            to("accepted", packet.wasAccepted())
        }
        builder.handle<SPacketDestroyEntities> {
            to("entityIDs") {
                packet.entityIDs.forEach { append("> $it ") }
            }
        }
        builder.handle<SPacketEntityMetadata> {
            to("dataEntries") {
                packet.dataManagerEntries?.forEach { append("> isDirty: ${it.isDirty} key: ${it.key} value: ${it.value} ") } ?: append("null")
            }
        }
        builder.handle<SPacketEntityProperties> { to("entityID", packet.entityId) }
        builder.handle<SPacketEntityStatus> {
            to("entityID", packet.entityId)
            to("opCode", packet.opCode)
        }
        builder.handle<SPacketEntityTeleport> {
            to("x", packet.x)
            to("y", packet.y)
            to("z", packet.z)
            to("yaw", packet.yaw)
            to("pitch", packet.pitch)
            to("entityID", packet.entityId)
        }
        builder.handle<SPacketKeepAlive> { to("id", packet.id) }
        builder.handle<SPacketMultiBlockChange> {
            to("changedBlocks") {
                packet.changedBlocks.forEach { append("> x: ${it.pos.x} y: ${it.pos.y} z: ${it.pos.z} ") }
            }
        }
        builder.handle<SPacketPlayerPosLook> {
            to("x", packet.x)
            to("y", packet.y)
            to("z", packet.z)
            to("yaw", packet.yaw)
            to("pitch", packet.pitch)
            to("teleportId", packet.teleportId)
            to("flags") {
                packet.flags.forEach { append("> ${it.name} ") }
            }
        }
        builder.handle<SPacketSoundEffect> {
            to("sound", packet.sound.soundName)
            to("category", packet.category)
            to("posX", packet.x)
            to("posY", packet.y)
            to("posZ", packet.z)
            to("volume", packet.volume)
            to("pitch", packet.pitch)
        }
        builder.handle<SPacketSpawnObject> {
            to("entityID", packet.entityID)
            to("data", packet.data)
        }
        builder.handle<SPacketTeams> {
            to("action", packet.action)
            to("displayName", packet.displayName)
            to("color", packet.color)
        }
        builder.handle<SPacketTimeUpdate> {
            to("totalWorldTime", packet.totalWorldTime)
            to("worldTime", packet.worldTime)
        }
        builder.handle<SPacketUnloadChunk> {
            to("x", packet.x)
            to("z", packet.z)
        }
        builder.handle<SPacketUpdateHealth> {
            to("foodLevel", packet.foodLevel)
            to("health", packet.health)
        }
        builder.handle<SPacketUpdateTileEntity> {
            to("x", packet.pos.x)
            to("y", packet.pos.y)
            to("z", packet.pos.z)
        }
        builder.handle<SPacketHeldItemChange> { to("hotbar", packet.heldItemHotbarIndex) }
        builder.handle<SPacketSetSlot> {
            to("windowId", packet.windowId)
            to("slot", packet.slot)
            to("item", packet.stack)
        }
    }

    private enum class LogStage(override val displayName: CharSequence, val predicate: (PacketEvent.Stage) -> Boolean) : DisplayEnum {
        PRE("Pre", { it == PacketEvent.Stage.PRE }),
        POST("Post", { it == PacketEvent.Stage.POST }),
        BOTH("Both", { true })
    }

    private enum class LoggerSettings(override val displayName: CharSequence) : DisplayEnum {
        GENERAL("General"), CLIENT("Client"), SERVER("Server");
        override fun toString(): String = displayName.toString()
    }

    private enum class Page(override val displayName: CharSequence) : DisplayEnum {
        LOGGER("Packet Logger"), LIMITER("Packet Limiter"), CANCELLER("Packet Canceller")
    }

    private class SideSetting(
        val side: LoggerSettings,
        handleFunc: List<Pair<Class<out Packet<*>>, PacketLogBuilder<*>.() -> Unit>>
    ) {
        val sideEnabled by setting(INSTANCE, BooleanSetting(INSTANCE.settingName("${side} Enabled"), false, { INSTANCE.packetLoggerPage == side }))
        val sideStage by setting(INSTANCE, EnumSetting(INSTANCE.settingName("${side} Stage"), LogStage.PRE, { INSTANCE.packetLoggerPage == side && sideEnabled }))
        val logInChat by setting(INSTANCE, BooleanSetting(INSTANCE.settingName("${side} Log In Chat"), false, { INSTANCE.packetLoggerPage == side && sideEnabled }))
        val logAll by setting(INSTANCE, BooleanSetting(INSTANCE.settingName("${side} Log All"), false, { INSTANCE.packetLoggerPage == side && sideEnabled }))
        val logCancelled by setting(INSTANCE, BooleanSetting(INSTANCE.settingName("${side} Log Cancelled"), false, { INSTANCE.packetLoggerPage == side && sideEnabled && !logAll }))

        val handlers = LinkedHashMap<Class<out Packet<*>>, Handler>()
        val unknownHandler = Handler(setting(INSTANCE, BooleanSetting(INSTANCE.settingName("${side} Log Unknown"), false, { INSTANCE.packetLoggerPage == side && sideEnabled && !logAll }))) {
            unaryPlus("Unknown")
        }

        init {
            handleFunc.forEach { (clazz, func) ->
                val s = setting(INSTANCE, BooleanSetting(INSTANCE.settingName(clazz.simpleName ?: "Unknown"), false, { INSTANCE.packetLoggerPage == side && sideEnabled && !logAll }))
                handlers[clazz] = Handler(s, func as PacketLogBuilder<out Packet<*>>.() -> Unit)
            }
        }

        fun handle(event: PacketEvent) {
            if (!sideEnabled || !sideStage.predicate(event.stage)) return
            if (!logCancelled && event is PacketEvent.Send && event.cancelled) return

            val handler = handlers[event.packet::class.java] ?: handlers.values.firstOrNull { it.setting.value && it.setting.internalName == event.packet::class.java.simpleName } ?: unknownHandler
            handler.handle(event)
        }

        inner class Handler(val setting: BooleanSetting, val handleFunc: PacketLogBuilder<out Packet<*>>.() -> Unit) {
            fun handle(event: PacketEvent) {
                if (!logAll && !setting.value) return
                val builder = PacketLogBuilder(event, event.packet)
                (handleFunc as PacketLogBuilder<Packet<*>>.() -> Unit)(builder as PacketLogBuilder<Packet<*>>)
                builder.build(logInChat)
            }
        }

        class Builder(val side: LoggerSettings) {
            val handlers = HashMap<Class<out Packet<*>>, PacketLogBuilder<*>.() -> Unit>()
            inline fun <reified T : Packet<*>> handle(noinline block: PacketLogBuilder<T>.() -> Unit) {
                handlers[T::class.java] = block as PacketLogBuilder<*>.() -> Unit
            }
            fun build() = SideSetting(side, handlers.toList().sortedBy { it.first.simpleName })
        }
    }

    private class PacketLogBuilder<T : Packet<*>>(val event: PacketEvent, val packet: T) {
        val stringBuilder = StringBuilder().apply {
            append("${event.side},${event.stage},${packet::class.java.simpleName},${INSTANCE.formattedTime(System.nanoTime() - start)},${INSTANCE.formattedTime(System.nanoTime() - last)},")
        }

        fun unaryPlus(string: String) { stringBuilder.append(string) }
        fun to(key: String, value: Any?) { if (value != null) add(key, value.toString()) }
        fun to(key: String, block: StringBuilder.() -> Unit) {
            val sb = StringBuilder()
            block(sb)
            add(key, sb.toString())
        }
        fun add(key: String, value: String) { stringBuilder.append("$key: $value ") }

        fun build(logInChat: Boolean) {
            stringBuilder.append("\n")
            val line = stringBuilder.toString()
            synchronized(INSTANCE) {
                lines.add(line)
                last = System.nanoTime()
            }
            if (logInChat) MessageSendUtils.sendChatMessage(line)
        }
    }
}
