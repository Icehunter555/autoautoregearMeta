package dev.wizard.meta.mixins.core.world

import dev.wizard.meta.module.modules.render.XRay
import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer.BlockFluidRenderer
import net.minecraft.client.renderer.BufferBuilder
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(BlockFluidRenderer::class)
class MixinBlockFluidRenderer {
    @Inject(method = ["renderFluid"], at = [At("HEAD")], cancellable = true)
    fun renderFluid(blockAccess: IBlockAccess, blockStateIn: IBlockState, blockPosIn: BlockPos, bufferBuilderIn: BufferBuilder, ci: CallbackInfoReturnable<Boolean>) {
        if (XRay.shouldReplace(blockStateIn)) {
            ci.cancel()
        }
    }
}
