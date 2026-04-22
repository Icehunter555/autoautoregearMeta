package dev.wizard.meta.module.modules.movement

import dev.fastmc.common.MathUtilKt
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.AddCollisionBoxEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.player.PlayerTravelEvent
import dev.wizard.meta.event.listener
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.BaritoneUtils
import dev.wizard.meta.util.accessor.getMoving
import dev.wizard.meta.util.accessor.getY
import dev.wizard.meta.util.accessor.setY
import net.minecraft.block.BlockLiquid
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityBoat
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos

object WaterWalk : Module(
    name = "WaterWalk",
    category = Category.MOVEMENT,
    description = "Allows you to walk on water",
    priority = 1010
) {
    private val mode by setting("Mode", Mode.SOLID)
    private val waterWalkBox = AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.99, 1.0)

    private fun SafeClientEvent.isInWater(entity: Entity): Boolean {
        val box = entity.entityBoundingBox
        val y = MathUtilKt.floorToInt(box.minY + 0.01)
        val pos = BlockPos.PooledMutableBlockPos.retain()
        for (x in MathUtilKt.floorToInt(box.minX)..MathUtilKt.floorToInt(box.maxX)) {
            for (z in MathUtilKt.floorToInt(box.minZ)..MathUtilKt.floorToInt(box.maxZ)) {
                if (world.getBlockState(pos.setPos(x, y, z)).block is BlockLiquid) {
                    pos.release()
                    return true
                }
            }
        }
        pos.release()
        return false
    }

    private fun SafeClientEvent.isAboveLiquid(entity: Entity, box: AxisAlignedBB, packet: Boolean): Boolean {
        val offset = if (packet) 0.03 else if (entity is EntityPlayer) 0.2 else 0.5
        val y = MathUtilKt.floorToInt(box.minY - offset)
        val pos = BlockPos.PooledMutableBlockPos.retain()
        for (x in MathUtilKt.floorToInt(box.minX)..MathUtilKt.floorToInt(box.maxX)) {
            for (z in MathUtilKt.floorToInt(box.minZ)..MathUtilKt.floorToInt(box.maxZ)) {
                if (world.getBlockState(pos.setPos(x, y, z)).block is BlockLiquid) {
                    pos.release()
                    return true
                }
            }
        }
        pos.release()
        return false
    }

    init {
        onToggle {
            BaritoneUtils.settings?.assumeWalkOnWater?.value = it
        }

        listener<PlayerTravelEvent> {
            if (mc.gameSettings.keyBindSneak.isKeyDown || player.fallDistance > 3.0f || !isInWater(player)) {
                return@listener
            }
            if (mode == Mode.DOLPHIN) {
                player.motionY += 0.04
            } else {
                player.motionY = 0.1
                player.ridingEntity?.let {
                    if (it !is EntityBoat) {
                        it.motionY = 0.3
                    }
                }
            }
        }

        listener<PacketEvent.Send> { event ->
            val packet = event.packet
            if (packet !is CPacketPlayer || !packet.getMoving()) return@listener
            if (mc.gameSettings.keyBindSneak.isKeyDown || MathUtilKt.isEven(player.ticksExisted)) return@listener

            val entity = player.ridingEntity ?: player
            if (isAboveLiquid(entity, entity.entityBoundingBox, true) && !isInWater(entity)) {
                packet.setY(packet.getY() + 0.02)
            }
        }

        listener<AddCollisionBoxEvent> { event ->
            if (mode == Mode.DOLPHIN) return@listener
            if (mc.gameSettings.keyBindSneak.isKeyDown) return@listener
            val entity = event.entity ?: return@listener
            if (entity is EntityBoat) return@listener
            if (event.block !is BlockLiquid) return@listener
            if (player.fallDistance > 3.0f) return@listener
            if (entity != player && entity != player.ridingEntity) return@listener
            if (isInWater(entity) || entity.posY < event.pos.y) return@listener
            if (!isAboveLiquid(entity, event.entityBox, false)) return@listener

            event.collidingBoxes.add(waterWalkBox.offset(event.pos))
        }
    }

    private enum class Mode {
        SOLID, DOLPHIN
    }
}
