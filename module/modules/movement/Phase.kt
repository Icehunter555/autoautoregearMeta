package dev.wizard.meta.module.modules.movement

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.listener
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.math.vector.toBlockPos
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.util.math.Vec3d

object Phase : Module(
    name = "Phase",
    category = Category.MOVEMENT,
    description = "Phase through blocks",
    priority = 1010
) {
    private val mode by setting("Mode", Mode.STUCK)
    private val onlyBurrow by setting("OnlyBurrow", true, { mode == Mode.SKIP })
    private val onBurrow by setting("OnBurrow", false, { !onlyBurrow || mode == Mode.STUCK })
    private val speed by setting("Speed", 0.6, 0.1..1.0, 0.01, { mode == Mode.SKIP })
    private val delay by setting("Delay", 1, 0..20, 1, { mode == Mode.SKIP })
    private val offset by setting("Offset", 1, 0..10, 1, { mode == Mode.STUCK })
    private val autoToggle by setting("AutoToggle", false, { mode == Mode.STUCK })

    private var tickTimer = 0

    private fun SafeClientEvent.stuckPhase() {
        val x = Math.abs(player.posX) - Math.floor(Math.abs(player.posX))
        val z = Math.abs(player.posZ) - Math.floor(Math.abs(player.posZ))
        if (x == 0.7 || x == 0.3 || z == 0.7 || z == 0.3) return

        if (!onBurrow && world.getBlockState(EntityUtils.getFlooredPosition(player)).block == Blocks.AIR) return

        val playerVec = player.positionVector
        val offsetValue = offset.toDouble() / 100.0

        val vecXPos = playerVec.add(0.3 + offsetValue, 0.2, 0.0)
        if (world.getBlockState(vecXPos.toBlockPos()).block != Blocks.AIR) {
            player.setPosition(player.posX + offsetValue, player.posY, player.posZ)
            if (autoToggle) disable()
            return
        }

        val vecXNeg = playerVec.add(-0.3 - offsetValue, 0.2, 0.0)
        if (world.getBlockState(vecXNeg.toBlockPos()).block != Blocks.AIR) {
            player.setPosition(player.posX - offsetValue, player.posY, player.posZ)
            if (autoToggle) disable()
            return
        }

        val vecZPos = playerVec.add(0.0, 0.2, 0.3 + offsetValue)
        if (world.getBlockState(vecZPos.toBlockPos()).block != Blocks.AIR) {
            player.setPosition(player.posX, player.posY, player.posZ + offsetValue)
            if (autoToggle) disable()
            return
        }

        val vecZNeg = playerVec.add(0.0, 0.2, -0.3 - offsetValue)
        if (world.getBlockState(vecZNeg.toBlockPos()).block != Blocks.AIR) {
            player.setPosition(player.posX, player.posY, player.posZ - offsetValue)
            if (autoToggle) disable()
        }
    }

    private fun SafeClientEvent.skipPhase() {
        tickTimer++
        val isInsideBlock = collideBlockIntersects(player.entityBoundingBox) { it != Blocks.AIR }
        if (!player.onGround || tickTimer < delay || !player.collidedHorizontally) return

        if (isInsideBlock && !player.isSneaking && !onBurrow && !onlyBurrow) return

        val playerPos = EntityUtils.getFlooredPosition(player)
        if (onlyBurrow && !isInsideBlock && world.isAirBlock(playerPos.up()) && world.getBlockState(playerPos).block != Blocks.OBSIDIAN) return

        val direction = getDirection()
        val posX = -Math.sin(direction) * speed
        val posZ = Math.cos(direction) * speed

        for (i in 0 until 3) {
            connection.sendPacket(CPacketPlayer.Position(player.posX, player.posY + 0.06, player.posZ, true))
            connection.sendPacket(CPacketPlayer.Position(player.posX + posX * i, player.posY, player.posZ + posZ * i, true))
        }
        player.setEntityBoundingBox(player.entityBoundingBox.offset(posX, 0.0, posZ))
        player.setPositionAndUpdate(player.posX + posX, player.posY, player.posZ + posZ)
        tickTimer = 0
    }

    private fun SafeClientEvent.getDirection(): Double {
        var rotationYaw = player.rotationYaw
        if (player.movementInput.moveForward < 0.0f) {
            rotationYaw += 180.0f
        }
        var forward = 1.0f
        if (player.movementInput.moveForward < 0.0f) {
            forward = -0.5f
        } else if (player.movementInput.moveForward > 0.0f) {
            forward = 0.5f
        }
        if (player.movementInput.moveStrafe > 0.0f) {
            rotationYaw -= 90.0f * forward
        }
        if (player.movementInput.moveStrafe < 0.0f) {
            rotationYaw += 90.0f * forward
        }
        return Math.toRadians(rotationYaw.toDouble())
    }

    private fun SafeClientEvent.collideBlockIntersects(axisAlignedBB: net.minecraft.util.math.AxisAlignedBB, collide: (net.minecraft.block.Block) -> Boolean): Boolean {
        val minX = Math.floor(player.entityBoundingBox.minX).toInt()
        val maxX = Math.floor(player.entityBoundingBox.maxX).toInt() + 1
        val minZ = Math.floor(player.entityBoundingBox.minZ).toInt()
        val maxZ = Math.floor(player.entityBoundingBox.maxZ).toInt() + 1
        for (x in minX until maxX) {
            for (z in minZ until maxZ) {
                val blockPos = net.minecraft.util.math.BlockPos(x, axisAlignedBB.minY.toInt(), z)
                val block = world.getBlockState(blockPos).block
                if (collide(block)) {
                    val blockState = world.getBlockState(blockPos)
                    val boundingBox = block.getCollisionBoundingBox(blockState, world, blockPos)
                    if (boundingBox != null && player.entityBoundingBox.intersects(boundingBox)) return true
                }
            }
        }
        return false
    }

    init {
        listener<TickEvent.Pre> {
            when (mode) {
                Mode.STUCK -> stuckPhase()
                Mode.SKIP -> skipPhase()
            }
        }
    }

    private enum class Mode(override val displayName: CharSequence) : DisplayEnum {
        STUCK("Stuck"), SKIP("Skip")
    }
}
