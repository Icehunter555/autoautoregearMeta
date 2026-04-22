package dev.wizard.meta.module.modules.misc

import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.graphics.ESPRenderer
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.inventory.operation.swapToBlock
import dev.wizard.meta.util.inventory.slot.countBlock
import dev.wizard.meta.util.inventory.slot.hotbarSlots
import dev.wizard.meta.util.math.RotationUtils
import dev.wizard.meta.util.math.vector.toVec3dCenter
import dev.wizard.meta.util.text.MessageSendUtils
import dev.wizard.meta.util.world.PlacementSearchOption
import dev.wizard.meta.util.world.getMaterial
import dev.wizard.meta.util.world.getPlacement
import dev.wizard.meta.util.world.placeBlock
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.abs

object AutoMudHut : Module(
    name = "AutoMudHut",
    category = Category.MISC,
    description = "Builds a small bunker around you. Needs 57 blocks."
) {
    private val blockType by setting("Block Type", BlockType.OBSIDIAN)
    private val delayTicks by setting("Delay Ticks", 4, 1..20, 1)
    private val rotateToPlace by setting("Rotate To Place", true)
    private val jumpLogic by setting("Jump Logic", true)

    private val template = arrayOf(
        intArrayOf(2, 0, 2), intArrayOf(-2, 0, 2), intArrayOf(2, 0, -2), intArrayOf(-2, 0, -2),
        intArrayOf(2, 1, 2), intArrayOf(-2, 1, 2), intArrayOf(2, 1, -2), intArrayOf(-2, 1, -2),
        intArrayOf(2, 2, 2), intArrayOf(-2, 2, 2), intArrayOf(2, 2, -2), intArrayOf(-2, 2, -2),
        intArrayOf(1, 2, 2), intArrayOf(0, 2, 2), intArrayOf(-1, 2, 2),
        intArrayOf(2, 2, 1), intArrayOf(2, 2, 0), intArrayOf(2, 2, -1),
        intArrayOf(-2, 2, 1), intArrayOf(-2, 2, 0), intArrayOf(-2, 2, -1),
        intArrayOf(1, 2, -2), intArrayOf(0, 2, -2), intArrayOf(-1, 2, -2),
        intArrayOf(1, 0, 2), intArrayOf(0, 0, 2), intArrayOf(-1, 0, 2),
        intArrayOf(2, 0, 1), intArrayOf(2, 0, 0), intArrayOf(2, 0, -1),
        intArrayOf(-2, 0, 1), intArrayOf(-2, 0, 0), intArrayOf(-2, 0, -1),
        intArrayOf(1, 0, -2), intArrayOf(0, 0, -2), intArrayOf(-1, 0, -2),
        intArrayOf(1, 1, 2), intArrayOf(0, 1, 2), intArrayOf(-1, 1, 2),
        intArrayOf(2, 1, 1), intArrayOf(2, 1, 0), intArrayOf(2, 1, -1),
        intArrayOf(-2, 1, 1), intArrayOf(-2, 1, 0), intArrayOf(-2, 1, -1),
        intArrayOf(1, 1, -2), intArrayOf(0, 1, -2), intArrayOf(-1, 1, -2),
        intArrayOf(1, 2, 1), intArrayOf(-1, 2, 1), intArrayOf(1, 2, -1), intArrayOf(-1, 2, -1),
        intArrayOf(0, 2, 1), intArrayOf(1, 2, 0), intArrayOf(-1, 2, 0), intArrayOf(0, 2, -1),
        intArrayOf(0, 2, 0)
    )

    private val positions = ArrayList<BlockPos>()
    private val renderer = ESPRenderer().apply {
        aFilled = 33
        aOutline = 233
    }
    private var blockIndex = 0
    private var building = false
    private var tickTimer = 0
    private var startPos = BlockPos.ORIGIN
    private var lastRotation: Vec3d? = null

    override fun getHudInfo(): String {
        return if (building) "§a$blockIndex§f/§c${positions.size}" else ""
    }

    init {
        onEnable {
            building = false
            blockIndex = 0
            tickTimer = 0
        }

        listener<TickEvent.Pre> {
            if (isEnabled && positions.isEmpty()) {
                if (!player.onGround) {
                    MessageSendUtils.sendChatMessage("§c$chatName You must be on the ground to use this!")
                    disable()
                    return@listener
                }

                val requiredBlocks = 57
                val availableBlocks = player.hotbarSlots.countBlock(blockType.block)

                if (availableBlocks < requiredBlocks) {
                    MessageSendUtils.sendChatMessage("§c$chatName Not enough blocks! Need $requiredBlocks, have $availableBlocks")
                    disable()
                    return@listener
                }

                calculatePositions()
                building = true
                blockIndex = 0
                tickTimer = 0
                MessageSendUtils.sendChatMessage("§a$chatName Started building bunker with ${blockType.displayName}")
                return@listener
            }

            if (building) {
                runBuilding()
            }
        }

        onDisable {
            building = false
            blockIndex = 0
            positions.clear()
            renderer.clear()
            lastRotation = null
        }

        listener<Render3DEvent> {
            if (building && blockIndex < positions.size) {
                renderer.clear()
                val pos = positions[blockIndex]
                renderer.add(pos, ColorRGB(0, 255, 0))
                for (i in blockIndex + 1 until positions.size) {
                    renderer.add(positions[i], ColorRGB(255, 255, 255, 50))
                }
                renderer.render(false)
            }
        }
    }

    private fun calculatePositions() {
        startPos = BlockPos(player.posX.toInt(), player.posY.toInt(), player.posZ.toInt())
        val facing = EnumFacing.fromAngle(player.rotationYaw.toDouble())
        val facing2 = facing.rotateY()

        positions.clear()
        for (pos in template) {
            val blockPos = startPos.up(pos[1]).offset(facing, pos[2]).offset(facing2, pos[0])
            positions.add(blockPos)
        }
    }

    private fun runBuilding() {
        if (++tickTimer < delayTicks) return
        tickTimer = 0

        if (blockIndex >= positions.size) {
            MessageSendUtils.sendChatMessage("§a$chatName Bunker completed!")
            disable()
            return
        }

        if (!swapToBlock(blockType.block)) {
            MessageSendUtils.sendChatMessage("§c$chatName No ${blockType.displayName} found in hotbar!")
            disable()
            return
        }

        val pos = positions[blockIndex]

        if (jumpLogic && shouldJump(pos)) {
            player.jump()
        }

        if (world.getMaterial(pos) == Material.AIR) {
            if (placeBlockAt(pos)) {
                blockIndex++
            }
        } else {
            blockIndex++
        }
    }

    private fun shouldJump(pos: BlockPos): Boolean {
        val playerPos = BlockPos(player)
        return player.onGround &&
                abs(pos.x - playerPos.x) == 2 &&
                pos.y - playerPos.y == 2 &&
                abs(pos.z - playerPos.z) == 2
    }

    private fun placeBlockAt(pos: BlockPos): Boolean {
        val placeInfo = getPlacement(pos, 3.0, PlacementSearchOption.range(5.0), PlacementSearchOption.ENTITY_COLLISION) ?: return false

        if (rotateToPlace) {
            val rotation = RotationUtils.getRotationTo(placeInfo.hitVec)
            lastRotation = Vec3d(rotation.x.toDouble(), rotation.y.toDouble(), 0.0)
            player.rotationYaw = rotation.x
            player.rotationPitch = rotation.y
        }

        if (!player.isSneaking) {
            player.connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.START_SNEAKING))
            placeBlock(placeInfo)
            player.connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.STOP_SNEAKING))
        } else {
            placeBlock(placeInfo)
        }

        return true
    }

    private enum class BlockType(val displayName: String, val block: Block) {
        COBBLESTONE("Cobblestone", Blocks.COBBLESTONE),
        STONE("Stone", Blocks.STONE),
        DIRT("Dirt", Blocks.DIRT),
        OBSIDIAN("Obsidian", Blocks.OBSIDIAN)
    }
}
