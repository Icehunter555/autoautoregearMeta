package dev.wizard.meta.mixins.core.render

import dev.wizard.meta.module.modules.render.DyeSpoofer
import dev.wizard.meta.module.modules.render.Glint
import net.minecraft.client.renderer.RenderItem
import net.minecraft.client.renderer.block.model.IBakedModel
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.ModifyArg
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(RenderItem::class)
class MixinRenderItem {
    @Inject(method = ["getItemModelWithOverrides"], at = [At("RETURN")], cancellable = true)
    private fun onGetItemModel(stack: ItemStack, worldIn: World?, entitylivingbaseIn: EntityLivingBase?, cir: CallbackInfoReturnable<IBakedModel>) {
        if (DyeSpoofer.isEnabled) {
            cir.returnValue = DyeSpoofer.handleItemStack(stack)
        }
    }

    @ModifyArg(method = ["renderEffect"], at = At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderItem;renderModel(Lnet/minecraft/client/renderer/block/model/IBakedModel;I)V"), index = 1)
    private fun modifyGlintColor(originalColor: Int): Int {
        if (Glint.isEnabled) {
            return Glint.color
        }
        return originalColor
    }
}
