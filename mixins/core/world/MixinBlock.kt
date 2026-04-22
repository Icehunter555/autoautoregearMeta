package dev.wizard.meta.mixins.core.world

import dev.wizard.meta.module.modules.render.XRay
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(Block::class)
class MixinBlock {
    @Inject(method = ["getLightValue(Lnet/minecraft/block/state/IBlockState;)I"], at = [At("HEAD")], cancellable = true)
    fun getLightValue(state: IBlockState, cir: CallbackInfoReturnable<Int>) {
        if (XRay.isEnabled) {
            cir.returnValue = 15
        }
    }
}
