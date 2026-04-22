package dev.wizard.meta.mixins.accessor.entity

import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.datasync.DataParameter
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor
import org.spongepowered.asm.mixin.gen.Invoker

@Mixin(EntityLivingBase::class)
interface AccessorEntityLivingBase {
    @Invoker("onItemUseFinish")
    fun trollInvokeOnItemUseFinish()

    @Invoker("updateArmSwingProgress")
    fun trollUpdateArmSwingProgress()

    companion object {
        @Accessor("HEALTH")
        @JvmStatic
        fun trollGetHealthDataKey(): DataParameter<Float> {
            throw AssertionError()
        }
    }
}
