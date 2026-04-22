package dev.wizard.meta.module.modules.movement

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.player.InputUpdateEvent
import dev.wizard.meta.event.listener
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.EntityUtils
import net.minecraft.block.BlockVine
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.network.play.client.CPacketPlayerDigging
import net.minecraft.network.play.client.CPacketPlayerTryUseItem
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos

object NoSlowDown : Module(
    name = "NoSlowDown",
    category = Category.MOVEMENT,
    description = "Prevents being slowed down when using an item or going through cobwebs",
    priority = 1010
) {
    private val packet by setting("Packet", true)
    private val items by setting("Items", true)
    private val sneak by setting("Sneaking", false)
    val soulSand by setting("Soul Sand", false)
    val cobweb by setting("Cobweb", false)
    private val slime by setting("Slime", false)
    val vines by setting("Vines", false)
    val noSnow by setting("No Snow", false)

    private var isSneaking = false

    private fun SafeClientEvent.packetNoSlow(): Boolean {
        return packet && !player.isRiding && player.isHandActive && player.activeItemStack.item == Items.SHIELD
    }

    init {
        onDisable {
            isSneaking = false
            Blocks.SLIME_BLOCK.setDefaultSlipperiness(0.8f)
        }

        listener<InputUpdateEvent> { event ->
            if (player.isRiding) return@listener

            if (!player.isHandActive && isSneaking) {
                if (!player.isSneaking) {
                    connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.STOP_SNEAKING))
                }
                isSneaking = false
            }

            if (items && player.isHandActive) {
                event.movementInput.moveStrafe *= 5.0f
                event.movementInput.moveForward *= 5.0f
            }

            if (sneak && player.isSneaking) {
                event.movementInput.moveStrafe *= 3.3f
                event.movementInput.moveForward *= 3.3f
            }
        }

        listener<PacketEvent.Send> { event ->
            if (event.packet is CPacketPlayer && packetNoSlow()) {
                connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
            }
        }

        listener<PacketEvent.PostSend> { event ->
            if (event.packet is CPacketPlayer && packetNoSlow()) {
                connection.sendPacket(CPacketPlayerTryUseItem(player.activeHand))
            }
        }

        listener<TickEvent.Pre> {
            if (slime) {
                Blocks.SLIME_BLOCK.setDefaultSlipperiness(0.4945f)
            } else {
                Blocks.SLIME_BLOCK.setDefaultSlipperiness(0.8f)
            }

            if (vines) {
                val pos = EntityUtils.getBetterPosition(player)
                if (world.getBlockState(pos).block is BlockVine || world.getBlockState(pos.up()).block is BlockVine) {
                    player.motionY = Math.max(player.motionY, -0.1)
                }
            }
        }
    }
}
