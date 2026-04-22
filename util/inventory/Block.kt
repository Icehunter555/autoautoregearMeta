package dev.wizard.meta.util.inventory

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import net.minecraft.block.Block
import net.minecraft.block.BlockColored
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.item.EnumDyeColor
import net.minecraft.item.Item

val shulkerList: Set<Block> = hashSetOf(
    Blocks.field_190977_dl, Blocks.field_190978_dm, Blocks.field_190979_dn, Blocks.field_190980_do,
    Blocks.field_190981_dp, Blocks.field_190982_dq, Blocks.field_190983_dr, Blocks.field_190984_ds,
    Blocks.field_190985_dt, Blocks.field_190986_du, Blocks.field_190987_dv, Blocks.field_190988_dw,
    Blocks.field_190989_dx, Blocks.field_190990_dy, Blocks.field_190991_dz, Blocks.field_190975_dA
)

val blockBlacklist: Set<Block> = hashSetOf(
    Blocks.field_150477_bB, Blocks.field_150486_ae, Blocks.field_150447_bR, Blocks.field_150462_ai,
    Blocks.field_150467_bQ, Blocks.field_150382_bo, Blocks.field_150438_bZ, Blocks.field_150409_cd,
    Blocks.field_150367_z, Blocks.field_150415_aT, Blocks.field_150381_bn
).apply { addAll(shulkerList) }

private val hashMap = Object2IntOpenHashMap<Block>().apply { defaultReturnValue(-1) }

val Block.item: Item
    get() = Item.getItemFromBlock(this)

val Block.id: Int
    get() {
        var result = runCatching { hashMap.getInt(this) }.getOrDefault(-1)
        if (result == -1) {
            result = Block.getIdFromBlock(this)
            synchronized(hashMap) {
                hashMap.put(this, result)
            }
        }
        return result
    }

fun Block.setMeta(meta: Int): IBlockState {
    return this.defaultState.withProperty(BlockColored.COLOR, EnumDyeColor.byMetadata(meta))
}
