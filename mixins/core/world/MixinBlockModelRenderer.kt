package dev.wizard.meta.mixins.core.world

import dev.wizard.meta.module.modules.render.XRay
import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer.BlockModelRenderer
import net.minecraft.client.renderer.BufferBuilder
import net.minecraft.client.renderer.block.model.IBakedModel
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(BlockModelRenderer::class)
class MixinBlockModelRenderer {
    @Inject(method = ["renderModel(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;ZJ)Z"], at = [At("HEAD")], cancellable = true)
    fun renderModel(worldIn: IBlockAccess, modelIn: IBakedModel, stateIn: IBlockState, posIn: BlockPos, buffer: BufferBuilder, checkSides: Boolean, rand: Long, ci: CallbackInfoReturnable<Boolean>) {
        if (XRay.shouldReplace(stateIn)) {
            ci.cancel()
        }
    }
}
