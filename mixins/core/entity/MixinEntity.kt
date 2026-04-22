package dev.wizard.meta.mixins.core.entity

import dev.wizard.meta.module.modules.misc.PortalTweaks
import dev.wizard.meta.module.modules.movement.SafeWalk
import dev.wizard.meta.module.modules.movement.Sprint
import dev.wizard.meta.module.modules.movement.Velocity
import dev.wizard.meta.module.modules.player.Freecam
import dev.wizard.meta.module.modules.player.GhostHand
import dev.wizard.meta.module.modules.player.ViewLock
import dev.wizard.meta.module.modules.render.FreeLook
import dev.wizard.meta.util.Wrapper
import net.minecraft.entity.Entity
import net.minecraft.entity.MoverType
import net.minecraft.util.math.RayTraceResult
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.Redirect
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(Entity::class, priority = 0x7FFFFFFF)
abstract class MixinEntity {
    @Shadow
    private var field_145783_c = 0
    private var modifiedSneaking = false

    @Shadow
    abstract fun func_82145_z(): Int

    @Inject(method = ["applyEntityCollision"], at = [At("HEAD")], cancellable = true)
    fun applyEntityCollisionHead(entityIn: Entity, ci: CallbackInfo) {
        Velocity.handleApplyEntityCollision(this as Entity, entityIn, ci)
    }

    @Inject(method = ["move"], at = [At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isSneaking()Z", ordinal = 0, shift = At.Shift.BEFORE)])
    fun moveInvokeIsSneakingPre(type2: MoverType, x: Double, y: Double, z: Double, ci: CallbackInfo) {
        if (SafeWalk.shouldSafewalk(this.field_145783_c, x, z)) {
            this.modifiedSneaking = true
            SafeWalk.setSneaking(true)
        }
    }

    @Inject(method = ["move"], at = [At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isSneaking()Z", ordinal = 0, shift = At.Shift.AFTER)])
    fun moveInvokeIsSneakingPost(type2: MoverType, x: Double, y: Double, z: Double, ci: CallbackInfo) {
        if (this.modifiedSneaking) {
            this.modifiedSneaking = false
            SafeWalk.setSneaking(false)
        }
    }

    @Inject(method = ["turn"], at = [At("HEAD")], cancellable = true)
    fun turn(yaw: Float, pitch: Float, ci: CallbackInfo) {
        val casted = this as Entity
        if (FreeLook.handleTurn(casted, yaw, pitch, ci)) {
            return
        }
        if (Freecam.handleTurn(casted, yaw, pitch, ci)) {
            return
        }
        ViewLock.handleTurn(casted, yaw, pitch, ci)
    }

    @Inject(method = ["rayTrace"], at = [At("HEAD")], cancellable = true)
    fun `rayTrace$Inject$INVOKE$rayTraceBlocks`(blockReachDistance: Double, partialTicks2: Float, cir: CallbackInfoReturnable<RayTraceResult>) {
        if (this === Wrapper.getPlayer()) {
            GhostHand.handleRayTrace(blockReachDistance, partialTicks2, cir)
        }
    }

    @Inject(method = ["isSprinting"], at = [At("RETURN")], cancellable = true)
    fun `isSprinting$Inject$RETURN`(cir: CallbackInfoReturnable<Boolean>) {
        if (this === Wrapper.getPlayer() && !cir.returnValue) {
            cir.returnValue = Sprint.shouldSprint()
        }
    }

    @Redirect(method = ["onEntityUpdate"], at = At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getMaxInPortalTime()I"))
    private fun getMaxInPortalTime(entity: Entity): Int {
        var time = this.func_82145_z()
        if (PortalTweaks.fastPortal()) {
            time = PortalTweaks.getFastPortalTime()
        }
        return time
    }
}
