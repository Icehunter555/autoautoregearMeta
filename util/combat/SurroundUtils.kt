package dev.wizard.meta.util.combat

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.world.isAir
import net.minecraft.block.Block
import net.minecraft.entity.Entity
import net.minecraft.init.Blocks
import net.minecraft.util.math.BlockPos

object SurroundUtils {
    val surroundOffset = arrayOf(BlockPos(0, -1, 0), BlockPos(0, 0, -1), BlockPos(1, 0, 0), BlockPos(0, 0, 1), BlockPos(-1, 0, 0))
    val surroundOffsetNoFloor = arrayOf(BlockPos(0, 0, -1), BlockPos(1, 0, 0), BlockPos(0, 0, 1), BlockPos(-1, 0, 0))

    fun SafeClientEvent.checkHole(entity: Entity): HoleType {
        return checkHole(EntityUtils.getFlooredPosition(entity))
    }

    fun SafeClientEvent.checkHole(pos: BlockPos): HoleType {
        if (!(world.isAir(pos) && world.isAir(pos.x, pos.y + 1, pos.z) && world.isAir(pos.x, pos.y + 2, pos.z))) {
            return HoleType.NONE
        }
        var type = HoleType.BEDROCK
        for (offset in surroundOffset) {
            val block = world.getBlockState(pos.add(offset)).block
            if (!checkBlock(block)) {
                return HoleType.NONE
            }
            if (block !== Blocks.BEDROCK) {
                type = HoleType.OBBY
            }
        }
        return type
    }

    private fun checkBlock(block: Block): Boolean {
        return block === Blocks.BEDROCK || block === Blocks.OBSIDIAN || block === Blocks.ENDER_CHEST || block === Blocks.ANVIL
    }

    enum class HoleType {
        NONE, OBBY, BEDROCK
    }
}
