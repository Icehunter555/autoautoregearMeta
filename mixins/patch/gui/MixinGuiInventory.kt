package dev.wizard.meta.mixins.patch.gui

import dev.wizard.meta.graphics.RenderUtils3D
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.entity.EntityLivingBase
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.ModifyArg
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(GuiInventory::class)
class MixinGuiInventory {
    companion object {
        @Unique
        private var `trollHack$prevRotationYaw` = 0f
        @Unique
        private var `trollHack$prevRotationPitch` = 0f
        @Unique
        private var `trollHack$prevRenderYawOffset` = 0f

        @Inject(method = ["drawEntityOnScreen"], at = [At("HEAD")])
        @JvmStatic
        private fun `Inject$drawEntityOnScreen$HEAD`(posX: Int, posY: Int, scale: Int, mouseX: Float, mouseY: Float, entity: EntityLivingBase, ci: CallbackInfo) {
            `trollHack$prevRotationYaw` = entity.prevRotationYaw
            `trollHack$prevRotationPitch` = entity.prevRotationPitch
            `trollHack$prevRenderYawOffset` = entity.prevRenderYawOffset
        }

        @Inject(method = ["drawEntityOnScreen"], at = [At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RenderManager;renderEntity(Lnet/minecraft/entity/Entity;DDDFFZ)V", shift = At.Shift.BEFORE)])
        @JvmStatic
        private fun drawEntityOnScreenInvokeRenderEntityPre(posX: Int, posY: Int, scale: Int, mouseX: Float, mouseY: Float, entity: EntityLivingBase, ci: CallbackInfo) {
            entity.prevRotationYaw = entity.rotationYaw
            entity.prevRotationPitch = entity.rotationPitch
            entity.prevRenderYawOffset = entity.renderYawOffset
        }

        @ModifyArg(method = ["drawEntityOnScreen"], at = At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RenderManager;renderEntity(Lnet/minecraft/entity/Entity;DDDFFZ)V"), index = 5)
        @JvmStatic
        private fun drawEntityOnScreenInvokeRenderEntityPartialTicks(partialTicks2: Float): Float {
            return RenderUtils3D.partialTicks
        }

        @Inject(method = ["drawEntityOnScreen"], at = [At("RETURN")])
        @JvmStatic
        private fun `Inject$drawEntityOnScreen$RETURN`(posX: Int, posY: Int, scale: Int, mouseX: Float, mouseY: Float, entity: EntityLivingBase, ci: CallbackInfo) {
            entity.prevRotationYaw = `trollHack$prevRotationYaw`
            entity.prevRotationPitch = `trollHack$prevRotationPitch`
            entity.prevRenderYawOffset = `trollHack$prevRenderYawOffset`
        }
    }
}
