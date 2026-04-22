package dev.wizard.meta.module.modules.misc

import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityEnderCrystal
import net.minecraft.network.play.client.CPacketPlayerDigging
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos

object NoBreakAnimation : Module(
    name = "NoBreakAnimation",
    category = Category.MISC,
    description = "Prevents block break animation server side"
) {
    private var isMining = false
    private var lastPos: BlockPos? = null
    private var lastFacing: EnumFacing? = null

    init {
        onDisable {
            resetMining()
        }

        listener<PacketEvent.Send> {
            if (it.packet !is CPacketPlayerDigging) return@listener
            val packet = it.packet as CPacketPlayerDigging

            val entities = world.getEntitiesWithinAABBExcludingEntity(null, AxisAlignedBB(packet.position))
            for (entity in entities) {
                if (entity is EntityEnderCrystal || entity is EntityLivingBase) {
                    resetMining()
                    return@listener
                }
            }

            when (packet.action) {
                CPacketPlayerDigging.Action.START_DESTROY_BLOCK -> {
                    isMining = true
                    lastPos = packet.position
                    lastFacing = packet.facing
                }
                CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK -> {
                    resetMining()
                }
                else -> {}
            }
        }

        listener<TickEvent.Pre> {
            if (!mc.gameSettings.keyBindAttack.isKeyDown) {
                resetMining()
                return@listener
            }

            if (isMining && lastPos != null && lastFacing != null) {
                connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, lastPos!!, lastFacing!!))
            }
        }
    }

    private fun resetMining() {
        isMining = false
        lastPos = null
        lastFacing = null
    }
}