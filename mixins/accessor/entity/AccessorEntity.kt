package dev.wizard.meta.mixins.accessor.entity

import net.minecraft.entity.Entity
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor
import org.spongepowered.asm.mixin.gen.Invoker

@Mixin(Entity::class)
interface AccessorEntity {
    @Accessor("isInWeb")
    fun `troll$getIsInWeb`(): Boolean

    @Accessor("inPortal")
    fun `troll$getInPortal`(): Boolean

    @Invoker("getFlag")
    fun `troll$getFlag`(var1: Int): Boolean

    @Invoker("setFlag")
    fun `troll$setFlag`(var1: Int, var2: Boolean)
}
