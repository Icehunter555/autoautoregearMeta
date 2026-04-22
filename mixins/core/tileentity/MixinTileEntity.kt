package dev.wizard.meta.mixins.core.tileentity

import dev.wizard.meta.util.ClassIDRegistry
import dev.wizard.meta.util.interfaces.ITypeID
import net.minecraft.tileentity.TileEntity
import org.spongepowered.asm.mixin.Mixin

@Mixin(TileEntity::class)
class MixinTileEntity : ITypeID {
    private val typeID = registry.get((this as TileEntity).javaClass)

    override fun getTypeID(): Int {
        return this.typeID
    }

    companion object {
        private val registry = ClassIDRegistry<TileEntity>()
    }
}
