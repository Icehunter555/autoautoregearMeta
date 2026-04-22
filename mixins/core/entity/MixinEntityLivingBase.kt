package dev.wizard.meta.mixins.core.entity

import dev.wizard.meta.event.events.combat.DeathEvent
import dev.wizard.meta.event.events.player.PlayerJumpEvent
import dev.wizard.meta.module.modules.player.HandSwing
import net.minecraft.client.Minecraft
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.math.BlockPos
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(EntityLivingBase::class, priority = 0x7FFFFFFF)
abstract class MixinEntityLivingBase {
    @Inject(method = ["getArmSwingAnimationEnd"], at = [At("HEAD")], cancellable = true)
    private fun getArmSwingAnimationEnd(cir: CallbackInfoReturnable<Int>) {
        if (HandSwing.modifiedSwingSpeed) {
            cir.returnValue = HandSwing.swingTicks
        }
    }

    @Inject(method = ["jump"], at = [At("HEAD")], cancellable = true)
    private fun onJump(ci: CallbackInfo) {
        if (!this.javaClass.isInstance(Minecraft.getMinecraft().player)) {
            return
        }
        val event = PlayerJumpEvent()
        event.post()
        if (event.isCancelled) {
            ci.cancel()
        }
    }

    @Inject(method = ["setHealth"], at = [At("HEAD")])
    private fun onSetHealth(health: Float, ci: CallbackInfo) {
        val entityLivingBase = this as EntityLivingBase
        val healthFrom = entityLivingBase.health
        if (healthFrom <= 0.0) {
            return
        }
        if (health > 0.0) {
            return
        }
        val event = DeathEvent(entityLivingBase, BlockPos(entityLivingBase.posX, entityLivingBase.posY, entityLivingBase.posZ))
        event.post()
    }
}
