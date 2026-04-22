package dev.wizard.meta.mixins.core.render

import dev.wizard.meta.event.events.render.UpdateEquippedItemEvent
import dev.wizard.meta.module.modules.player.Freecam
import dev.wizard.meta.module.modules.render.ViewModel
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.AbstractClientPlayer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.ItemRenderer
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumHand
import net.minecraft.util.EnumHandSide
import net.minecraft.util.math.MathHelper
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(ItemRenderer::class)
class MixinItemRenderer {
    @Inject(method = ["rotateArm"], at = [At("HEAD")], cancellable = true)
    private fun rotateArm(partialTicks2: Float, ci: CallbackInfo) {
        if (Freecam.isEnabled && Freecam.cameraGuy != null) {
            ci.cancel()
        }
    }

    @Inject(method = ["updateEquippedItem"], at = [At("HEAD")], cancellable = true)
    private fun onUpdateEquippedItem(ci: CallbackInfo) {
        val event = UpdateEquippedItemEvent()
        event.post()
        if (event.isCancelled) {
            ci.cancel()
        }
    }

    @Inject(method = ["renderItemInFirstPerson(Lnet/minecraft/client/entity/AbstractClientPlayer;FFLnet/minecraft/util/EnumHand;FLnet/minecraft/item/ItemStack;F)V"], at = [At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;pushMatrix()V", shift = At.Shift.AFTER)])
    private fun transformSideFirstPersonInvokePushMatrix(player: AbstractClientPlayer, partialTicks2: Float, pitch: Float, hand2: EnumHand, swingProgress2: Float, stack: ItemStack, equippedProgress: Float, ci: CallbackInfo) {
        ViewModel.translate(stack, hand2, player)
    }

    @Inject(method = ["renderItemInFirstPerson(Lnet/minecraft/client/entity/AbstractClientPlayer;FFLnet/minecraft/util/EnumHand;FLnet/minecraft/item/ItemStack;F)V"], at = [At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemRenderer;renderItemSide(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/renderer/block/model/ItemCameraTransforms$TransformType;Z)V")])
    private fun transformSideFirstPersonInvokeRenderItemSide(player: AbstractClientPlayer, partialTicks2: Float, pitch: Float, hand2: EnumHand, swingProgress2: Float, stack: ItemStack, equippedProgress: Float, ci: CallbackInfo) {
        ViewModel.rotateAndScale(stack, hand2, player, swingProgress2)
    }

    @Inject(method = ["transformEatFirstPerson"], at = [At("HEAD")], cancellable = true)
    private fun transformEatFirstPerson(p_187454_1_: Float, hand2: EnumHandSide, stack: ItemStack, ci: CallbackInfo) {
        if (ViewModel.isEnabled && ViewModel.eatX != 0.0 && ViewModel.eatY != 0.0) {
            if (!ViewModel.noEatAnimation) {
                val player = Minecraft.getMinecraft().player
                val f = player.itemInUseCount.toFloat() - p_187454_1_ + 1.0f
                val f2 = f / stack.maxItemUseDuration.toFloat()
                if (f2 < 0.8f) {
                    val f3 = Math.abs(MathHelper.cos(f / 4.0f * Math.PI.toFloat()) * 0.1f)
                    GlStateManager.translate(0.0f, f3, 0.0f)
                }
                val f3 = 1.0f - Math.pow(f2.toDouble(), 27.0).toFloat()
                val i = if (hand2 == EnumHandSide.RIGHT) 1 else -1
                GlStateManager.translate((f3 * 0.6f * i).toDouble() * ViewModel.eatX, (f3 * 0.5f).toDouble() * -ViewModel.eatX, 0.0)
                GlStateManager.rotate(i * f3 * 90.0f, 0.0f, 1.0f, 0.0f)
                GlStateManager.rotate(f3 * 10.0f, 1.0f, 0.0f, 0.0f)
                GlStateManager.rotate(i * f3 * 30.0f, 0.0f, 0.0f, 1.0f)
            }
            ci.cancel()
        }
    }
}
