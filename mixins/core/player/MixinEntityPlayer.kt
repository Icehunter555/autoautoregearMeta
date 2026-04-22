package dev.wizard.meta.mixins.core.player

import dev.wizard.meta.event.events.player.PlayerTravelEvent
import dev.wizard.meta.module.modules.misc.PortalTweaks
import dev.wizard.meta.module.modules.movement.KeepSprint
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.MoverType
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Constant
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.ModifyConstant
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(value = [EntityPlayer::class], priority = 0x7FFFFFFF)
abstract class MixinEntityPlayer(worldIn: World) : EntityLivingBase(worldIn) {
    @Inject(method = ["travel"], at = [At("HEAD")], cancellable = true)
    fun travel(strafe: Float, vertical: Float, forward: Float, info: CallbackInfo) {
        if (this is EntityPlayerSP) {
            val event = PlayerTravelEvent()
            event.post()
            if (event.isCancelled) {
                this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ)
                info.cancel()
            }
        }
    }

    @ModifyConstant(method = ["getPortalCooldown"], constant = [Constant(intValue = 10)])
    private fun getPortalCooldown(cooldown: Int): Int {
        var time = cooldown
        if (PortalTweaks.fastPortal()) {
            time = PortalTweaks.getFastPortalCooldown()
        }
        return time
    }

    @Inject(method = ["attackTargetEntityWithCurrentItem"], at = [At("HEAD")])
    fun onAttackPre(targetEntity: Entity, ci: CallbackInfo) {
        KeepSprint.onHitPre()
    }

    @Inject(method = ["attackTargetEntityWithCurrentItem"], at = [At("RETURN")])
    fun onAttackPost(targetEntity: Entity, ci: CallbackInfo) {
        KeepSprint.onHitPost()
    }
}
