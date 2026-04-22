package dev.wizard.meta.graphics.fastrender.tileentity

import dev.wizard.meta.graphics.fastrender.model.tileentity.ModelChest
import net.minecraft.block.BlockEnderChest
import net.minecraft.tileentity.TileEntityEnderChest
import net.minecraft.util.EnumFacing

object EnderChestRenderBuilder : AbstractTileEntityRenderBuilder<TileEntityEnderChest, ModelChest>() {
    override val model: ModelChest = ModelChest()

    override fun build(tileEntity: TileEntityEnderChest, partialTicks: Float) {
        val block = tileEntity.blockType
        if (block is BlockEnderChest) {
            val i = if (tileEntity.hasWorld()) tileEntity.blockMetadata else 0
            var j = 0
            if (i == 2) j = 180
            if (i == 3) j = 0
            if (i == 4) j = 90
            if (i == 5) j = -90

            var f = tileEntity.prevLidAngle + (tileEntity.lidAngle - tileEntity.prevLidAngle) * partialTicks
            f = 1.0f - (1.0f - f) * (1.0f - f) * (1.0f - f)

            renderModel(
                tileEntity,
                j.toFloat(),
                -(f * (Math.PI.toFloat() / 2f))
            )
        }
    }
}
