package dev.wizard.meta.module.modules.beta

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.player.PlayerMoveEvent
import dev.wizard.meta.mixins.accessor.network.AccessorSPacketPosLook
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.DoubleSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.interfaces.DisplayEnum
import net.minecraft.client.gui.GuiDisconnected
import net.minecraft.client.gui.GuiDownloadTerrain
import net.minecraft.client.gui.GuiMainMenu
import net.minecraft.client.gui.GuiMultiplayer
import net.minecraft.network.play.client.CPacketConfirmTeleport
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object PacketFly : Module(
    "PacketFly",
    category = Category.BETA,
    description = "Fly using packets",
    modulePriority = 9999
) {
    private val type by setting(this, EnumSetting(settingName("Mode"), Type.FAST))
    private val packetMode by setting(this, EnumSetting(settingName("Bounds Mode"), Mode.UP))
    private val strict by setting(this, BooleanSetting(settingName("Strict"), true))
    private val bounds by setting(this, BooleanSetting(settingName("Use Player Offset"), true))
    private val phase by setting(this, EnumSetting(settingName("Phase Mode"), Phase.NCP))
    private val multiAxis by setting(this, BooleanSetting(settingName("TripleAxis"), false))
    private val noPhaseSlow by setting(this, BooleanSetting(settingName("FastPhase"), false))
    private val speed by setting(this, DoubleSetting(settingName("Speed"), 1.0, 0.1..2.0, 0.1))
    private val factor by setting(this, DoubleSetting(settingName("Factor"), 1.0, 1.0..10.0, 0.1, { type == Type.FACTOR || type == Type.DESYNC }))
    private val antiKickMode by setting(this, EnumSetting(settingName("AntiKick"), AntiKick.NORMAL))
    private val limit by setting(this, EnumSetting(settingName("Limit"), Limit.NONE))
    private val constrict by setting(this, BooleanSetting(settingName("Extra Packet"), false))
    private val jitter by setting(this, BooleanSetting(settingName("Jitter"), false))

    private var teleportId = 0
    private var startingOutOfBoundsPos: CPacketPlayer.Position? = null
    private val packets = mutableSetOf<CPacketPlayer>()
    private val posLooks = ConcurrentHashMap<Int, TimeVec3d>()
    private var antiKickTicks = 0
    private var vDelay = 0
    private var hDelay = 0
    private var limitStrict = false
    private var limitTicks = 0
    private var jitterTicks = 0
    private var oddJitter = false
    private var speedX = 0.0
    private var speedY = 0.0
    private var speedZ = 0.0
    private var factorCounter = 0
    private var lastFactorTime = 0L

    init {
        onEnable {
            packets.clear()
            posLooks.clear()
            teleportId = 0
            vDelay = 0
            hDelay = 0
            antiKickTicks = 0
            limitTicks = 0
            jitterTicks = 0
            speedX = 0.0
            speedY = 0.0
            speedZ = 0.0
            oddJitter = false
            factorCounter = 0
            lastFactorTime = System.currentTimeMillis()
            startingOutOfBoundsPos = null

            val currentMc = mc
            if (currentMc.player != null) {
                startingOutOfBoundsPos = CPacketPlayer.Position(randomHorizontal(), 1.0, randomHorizontal(), currentMc.player.onGround).also {
                    packets.add(it)
                    currentMc.player.connection.sendPacket(it)
                }
            }
        }

        onDisable {
            packets.clear()
            posLooks.clear()
            teleportId = 0
            val currentMc = mc
            if (currentMc.player != null) {
                currentMc.player.motionX = 0.0
                currentMc.player.motionY = 0.0
                currentMc.player.motionZ = 0.0
            }
        }

        safeListener<PlayerMoveEvent.Pre> {
            if (mc.currentScreen is GuiDisconnected || mc.currentScreen is GuiMainMenu || mc.currentScreen is GuiMultiplayer || mc.currentScreen is GuiDownloadTerrain) {
                disable()
                return@safeListener
            }
            if (player.ticksExisted % 20 == 0) {
                cleanPosLooks()
            }
            player.motionX = 0.0
            player.motionY = 0.0
            player.motionZ = 0.0

            if (teleportId <= 0 && type != Type.SETBACK) {
                startingOutOfBoundsPos = CPacketPlayer.Position(randomHorizontal(), 1.0, randomHorizontal(), player.onGround).also {
                    packets.add(it)
                    connection.sendPacket(it)
                }
                return@safeListener
            }

            val phasing = checkCollisionBox()
            speedX = 0.0
            speedY = 0.0
            speedZ = 0.0

            if (mc.gameSettings.keyBindJump.isKeyDown && (hDelay < 1 || (multiAxis && phasing))) {
                speedY = if (player.ticksExisted % (if (type == Type.SETBACK || type == Type.SLOW || limit == Limit.STRICT) 10 else 20) == 0) {
                    if (antiKickMode != AntiKick.NONE) -0.032 else 0.062
                } else 0.062
                antiKickTicks = 0
                vDelay = 5
            } else if (mc.gameSettings.keyBindSneak.isKeyDown && (hDelay < 1 || (multiAxis && phasing))) {
                speedY = -0.062
                antiKickTicks = 0
                vDelay = 5
            }

            if ((multiAxis && phasing) || !mc.gameSettings.keyBindSneak.isKeyDown || !mc.gameSettings.keyBindJump.isKeyDown) {
                if (isPlayerMoving()) {
                    val moveSpeed = if (phasing && phase == Phase.NCP) {
                        if (noPhaseSlow) (if (multiAxis) 0.0465 else 0.062) else 0.031
                    } else 0.26
                    val dir = directionSpeed(moveSpeed * speed)
                    if ((dir[0] != 0.0 || dir[1] != 0.0) && (vDelay < 1 || (multiAxis && phasing))) {
                        speedX = dir[0]
                        speedZ = dir[1]
                        hDelay = 5
                    }
                }
                if (antiKickMode != AntiKick.NONE && (limit == Limit.NONE || limitTicks != 0)) {
                    if (antiKickTicks < (if (packetMode == Mode.BYPASS && !bounds) 1 else 3)) {
                        antiKickTicks++
                    } else {
                        antiKickTicks = 0
                        if (antiKickMode != AntiKick.LIMITED || !phasing) {
                            speedY = if (antiKickMode == AntiKick.STRICT) -0.08 else -0.04
                        }
                    }
                }
            }

            if (phasing && phase == Phase.NCP && (player.movementInput.moveForward != 0.0f || player.movementInput.moveStrafe != 0.0f) && speedY != 0.0) {
                speedY /= 2.5
            }

            if (limit != Limit.NONE) {
                if (limitTicks == 0) {
                    speedX = 0.0
                    speedY = 0.0
                    speedZ = 0.0
                } else if (limitTicks == 2 && jitter) {
                    if (oddJitter) {
                        speedX = 0.0
                        speedY = 0.0
                        speedZ = 0.0
                    }
                    oddJitter = !oddJitter
                }
            } else if (jitter && jitterTicks == 7) {
                speedX = 0.0
                speedY = 0.0
                speedZ = 0.0
            }

            when (type) {
                Type.FAST -> {
                    player.setVelocity(speedX, speedY, speedZ)
                    sendPackets(speedX, speedY, speedZ, packetMode, true, false)
                }
                Type.SETBACK -> {
                    sendPackets(speedX, speedY, speedZ, packetMode, true, false)
                }
                Type.SLOW -> {
                    player.setVelocity(speedX, speedY, speedZ)
                    sendPackets(speedX, speedY, speedZ, packetMode, false, false)
                }
                Type.FACTOR, Type.DESYNC -> {
                    val rawFactor = factor
                    var factorInt = Math.floor(rawFactor).toInt()
                    if (++factorCounter > (20.0 / ((rawFactor - factorInt) * 20.0)).toInt()) {
                        factorInt++
                        factorCounter = 0
                    }
                    for (i in 1..factorInt) {
                        player.setVelocity(speedX * i, speedY * i, speedZ * i)
                        sendPackets(speedX * i, speedY * i, speedZ * i, packetMode, true, false)
                    }
                    speedX = player.motionX
                    speedY = player.motionY
                    speedZ = player.motionZ
                }
            }

            vDelay--
            hDelay--

            if (constrict && (limit == Limit.NONE || limitTicks > 1)) {
                connection.sendPacket(CPacketPlayer.Position(player.posX, player.posY, player.posZ, false))
            }

            limitTicks++
            jitterTicks++
            if (limitTicks > (if (limit == Limit.STRICT) (if (limitStrict) 1 else 2) else 3)) {
                limitTicks = 0
                limitStrict = !limitStrict
            }
            if (jitterTicks > 7) jitterTicks = 0

            if (type != Type.SETBACK && teleportId <= 0) return@safeListener

            if (type != Type.SLOW) {
                it.x = speedX
                it.y = speedY
                it.z = speedZ
            }

            if (phase != Phase.NONE && (phase == Phase.VANILLA || checkCollisionBox())) {
                player.noClip = true
            }
        }

        safeListener<PacketEvent.Send> {
            val packet = it.packet
            if (packet is CPacketPlayer) {
                if (packet !is CPacketPlayer.Position && packet !is CPacketPlayer.PositionRotation) {
                    it.cancel()
                    return@safeListener
                }
                if (packets.contains(packet)) {
                    packets.remove(packet)
                } else {
                    it.cancel()
                }
            }
        }

        safeListener<PacketEvent.Receive> {
            val packet = it.packet
            if (packet is SPacketPlayerPosLook) {
                if (mc.currentScreen !is GuiDownloadTerrain) {
                    val p = packet as SPacketPlayerPosLook
                    if (player.isEntityAlive) {
                        if (teleportId <= 0) {
                            teleportId = p.teleportId
                        } else if (world.isBlockLoaded(BlockPos(player.posX, player.posY, player.posZ), false) && type != Type.SETBACK) {
                            if (type == Type.DESYNC) {
                                posLooks.remove(p.teleportId)
                                it.cancel()
                                if (type == Type.SLOW) player.setPosition(p.x, p.y, p.z)
                                return@safeListener
                            }
                            val vec = posLooks[p.teleportId]
                            if (vec != null && vec.x == p.x && vec.y == p.y && vec.z == p.z) {
                                posLooks.remove(p.teleportId)
                                it.cancel()
                                if (type == Type.SLOW) player.setPosition(p.x, p.y, p.z)
                                return@safeListener
                            }
                        }
                    }
                    (p as AccessorSPacketPosLook).apply {
                        yaw = player.rotationYaw
                        pitch = player.rotationPitch
                    }
                    p.flags.remove(SPacketPlayerPosLook.EnumFlags.X_ROT)
                    p.flags.remove(SPacketPlayerPosLook.EnumFlags.Y_ROT)
                    teleportId = p.teleportId
                } else {
                    teleportId = 0
                }
            }
        }
    }

    private fun SafeClientEvent.sendPackets(x: Double, y: Double, z: Double, mode: Mode, sendConfirmTeleport: Boolean, sendExtraCT: Boolean) {
        val nextPos = Vec3d(player.posX + x, player.posY + y, player.posZ + z)
        val boundsVec = getBoundsVec(x, y, z, mode)
        val nextPosPacket = CPacketPlayer.Position(nextPos.x, nextPos.y, nextPos.z, player.onGround)
        packets.add(nextPosPacket)
        connection.sendPacket(nextPosPacket)

        if (limit != Limit.NONE && limitTicks == 0) return

        val boundsPacket = CPacketPlayer.Position(boundsVec.x, boundsVec.y, boundsVec.z, player.onGround)
        packets.add(boundsPacket)
        connection.sendPacket(boundsPacket)

        if (sendConfirmTeleport) {
            teleportId++
            if (sendExtraCT) connection.sendPacket(CPacketConfirmTeleport(teleportId - 1))
            connection.sendPacket(CPacketConfirmTeleport(teleportId))
            posLooks[teleportId] = TimeVec3d(nextPos.x, nextPos.y, nextPos.z, System.currentTimeMillis())
            if (sendExtraCT) connection.sendPacket(CPacketConfirmTeleport(teleportId + 1))
        }
    }

    private fun SafeClientEvent.getBoundsVec(x: Double, y: Double, z: Double, mode: Mode): Vec3d {
        return when (mode) {
            Mode.UP -> Vec3d(player.posX + x, if (bounds) (if (strict) 255.0 else 256.0) else player.posY + 420.0, player.posZ + z)
            Mode.PRESERVE -> Vec3d(if (bounds) player.posX + randomHorizontal() else randomHorizontal(), if (strict) Math.max(player.posY, 2.0) else player.posY, if (bounds) player.posZ + randomHorizontal() else randomHorizontal())
            Mode.LIMITJITTER -> Vec3d(player.posX + (if (strict) x else randomLimitedHorizontal()), player.posY + randomLimitedVertical(), player.posZ + (if (strict) z else randomLimitedHorizontal()))
            Mode.BYPASS -> {
                if (bounds) {
                    val rawY = y * 510.0
                    Vec3d(player.posX + x, player.posY + (if (rawY > (if (player.dimension == -1) 127.0 else 255.0)) -rawY else if (rawY < 1.0) -rawY else rawY), player.posZ + z)
                } else {
                    Vec3d(player.posX + (if (x == 0.0) (if (Random.nextBoolean()) -10.0 else 10.0) else x * 38.0), player.posY + y, player.posZ + (if (z == 0.0) (if (Random.nextBoolean()) -10.0 else 10.0) else z * 38.0))
                }
            }
            Mode.OBSCURE -> Vec3d(player.posX + randomHorizontal(), Math.max(1.5, Math.min(player.posY + y, 253.5)), player.posZ + randomHorizontal())
            Mode.DOWN -> Vec3d(player.posX + x, if (bounds) (if (strict) 1.0 else 0.0) else player.posY - 1337.0, player.posZ + z)
        }
    }

    private fun SafeClientEvent.directionSpeed(speed: Double): DoubleArray {
        var forward = player.movementInput.moveForward
        var strafe = player.movementInput.moveStrafe
        var yaw = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * mc.renderPartialTicks
        if (forward != 0.0f) {
            if (strafe > 0.0f) {
                yaw += (if (forward > 0.0f) -45 else 45).toFloat()
            } else if (strafe < 0.0f) {
                yaw += (if (forward > 0.0f) 45 else -45).toFloat()
            }
            strafe = 0.0f
            if (forward > 0.0f) {
                forward = 1.0f
            } else if (forward < 0.0f) {
                forward = -1.0f
            }
        }
        val rad = Math.toRadians((yaw + 90.0f).toDouble())
        val sin = Math.sin(rad)
        val cos = Math.cos(rad)
        return doubleArrayOf(forward.toDouble() * speed * cos + strafe.toDouble() * speed * sin, forward.toDouble() * speed * sin - strafe.toDouble() * speed * cos)
    }

    private fun SafeClientEvent.isPlayerMoving(): Boolean = player.movementInput.moveForward != 0.0f || player.movementInput.moveStrafe != 0.0f

    private fun SafeClientEvent.checkCollisionBox(): Boolean {
        if (!world.getCollisionBoxes(player, player.entityBoundingBox.expand(0.0, 0.0, 0.0)).isEmpty()) return true
        return !world.getCollisionBoxes(player, player.entityBoundingBox.offset(0.0, 2.0, 0.0).contract(0.0, 1.99, 0.0)).isEmpty()
    }

    private fun cleanPosLooks() {
        posLooks.entries.removeIf { System.currentTimeMillis() - it.value.time > TimeUnit.SECONDS.toMillis(30) }
    }

    private fun randomHorizontal(): Double {
        val player = mc.player
        val n = if (bounds) 80 else if (packetMode == Mode.OBSCURE) (if (player != null && player.ticksExisted % 2 == 0) 480 else 100) else 29000000
        val randomValue = Random.nextInt(n) + (if (bounds) 5 else 500)
        return if (Random.nextBoolean()) randomValue.toDouble() else (-randomValue).toDouble()
    }

    private fun randomLimitedVertical(): Double {
        val randomValue = Random.nextInt(22) + 70
        return if (Random.nextBoolean()) randomValue.toDouble() else (-randomValue).toDouble()
    }

    private fun randomLimitedHorizontal(): Double {
        val randomValue = Random.nextInt(10)
        return if (Random.nextBoolean()) randomValue.toDouble() else (-randomValue).toDouble()
    }

    data class TimeVec3d(val x: Double, val y: Double, val z: Double, val time: Long)
    enum class AntiKick(override val displayName: CharSequence) : DisplayEnum { NONE("None"), NORMAL("Normal"), LIMITED("Limited"), STRICT("Strict") }
    enum class Limit(override val displayName: CharSequence) : DisplayEnum { NONE("None"), STRONG("Strong"), STRICT("Strict") }
    enum class Mode(override val displayName: CharSequence) : DisplayEnum { UP("Up"), PRESERVE("Preserve"), DOWN("Down"), LIMITJITTER("Limit Jitter"), BYPASS("Bypass"), OBSCURE("Obscure") }
    enum class Phase(override val displayName: CharSequence) : DisplayEnum { NONE("None"), VANILLA("Vanilla"), NCP("NCP") }
    enum class Type(override val displayName: CharSequence) : DisplayEnum { FACTOR("Factor"), SETBACK("Setback"), FAST("Fast"), SLOW("Slow"), DESYNC("Desync") }
}
