package dev.wizard.meta.mixins.core.render

import dev.wizard.meta.event.events.render.Render2DEvent
import dev.wizard.meta.graphics.GlStateUtils
import dev.wizard.meta.module.modules.player.Interactions
import dev.wizard.meta.module.modules.render.Ambiance
import dev.wizard.meta.module.modules.render.Fov
import dev.wizard.meta.module.modules.render.NoRender
import dev.wizard.meta.module.modules.render.ThirdPersonCamera
import dev.wizard.meta.util.Wrapper
import dev.wizard.meta.util.math.MathUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.EntityRenderer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemStack
import net.minecraft.util.math.RayTraceResult
import org.lwjgl.opengl.GL20
import org.spongepowered.asm.mixin.Final
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.ModifyVariable
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import javax.vecmath.Vector3f

@Mixin(value = [EntityRenderer::class], priority = 0x7FFFFFFF)
class MixinEntityRenderer {
    @Shadow
    @Final
    private lateinit var field_78531_r: Minecraft

    @Shadow
    @Final
    private lateinit var field_78504_Q: IntArray

    @Inject(method = ["updateCameraAndRender"], at = [At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiIngame;renderGameOverlay(F)V", shift = At.Shift.AFTER)])
    fun updateCameraAndRender(partialTicks2: Float, nanoTime: Long, ci: CallbackInfo) {
        Wrapper.getMinecraft().mcProfiler.startSection("trollRender2D")
        GlStateUtils.alpha(false)
        GlStateUtils.pushMatrixAll()
        Render2DEvent.Mc.post()
        GlStateUtils.rescaleActual()
        Render2DEvent.Absolute.post()
        GlStateUtils.rescaleTroll()
        Render2DEvent.Troll.post()
        GlStateUtils.popMatrixAll()
        GlStateUtils.alpha(true)
        GL20.glUseProgram(0)
        Wrapper.getMinecraft().mcProfiler.endSection()
    }

    @ModifyVariable(method = ["orientCamera"], at = At(value = "STORE", ordinal = 0), ordinal = 0)
    fun `orientCamera$ModifyVariable$0$STORE$0`(value: RayTraceResult?): RayTraceResult? {
        if (ThirdPersonCamera.isEnabled && ThirdPersonCamera.cameraClip) {
            return null
        }
        return value
    }

    @ModifyVariable(method = ["orientCamera"], at = At(value = "STORE", ordinal = 0), ordinal = 3)
    fun `orientCamera$ModifyVariable$3$STORE$0`(value: Double): Double {
        if (ThirdPersonCamera.isEnabled) {
            return ThirdPersonCamera.distance.toDouble()
        }
        return value
    }

    @Inject(method = ["displayItemActivation"], at = [At("HEAD")], cancellable = true)
    fun displayItemActivation(stack: ItemStack, ci: CallbackInfo) {
        if (NoRender.isEnabled && NoRender.totems.value) {
            ci.cancel()
        }
    }

    @Inject(method = ["setupFog"], at = [At("RETURN")])
    fun setupFog(startCoords: Int, partialTicks2: Float, callbackInfo: CallbackInfo) {
        if (NoRender.antiFog) {
            GlStateManager.disableFog()
        }
    }

    @Inject(method = ["hurtCameraEffect"], at = [At("HEAD")], cancellable = true)
    fun hurtCameraEffect(ticks: Float, ci: CallbackInfo) {
        if (NoRender.isEnabled && NoRender.hurtCamera.value) {
            ci.cancel()
        }
    }

    @Inject(method = ["getMouseOver"], at = [At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getPositionEyes(F)Lnet/minecraft/util/math/Vec3d;", shift = At.Shift.BEFORE)], cancellable = true)
    fun getEntitiesInAABBexcluding(partialTicks2: Float, ci: CallbackInfo) {
        if (Interactions.isNoEntityTraceEnabled()) {
            ci.cancel()
            Wrapper.getMinecraft().mcProfiler.endSection()
        }
    }

    @ModifyVariable(method = ["getFOVModifier"], at = At(value = "STORE", ordinal = 1), ordinal = 1)
    fun `getFOVModifier$STORE$float$1$1`(value: Float): Float {
        return Fov.getFOVModifierDynamicFov(value)
    }

    @Inject(method = ["getFOVModifier"], at = [At(value = "RETURN", ordinal = 1)], cancellable = true)
    fun `getFOVModifier$Inject$RETURN`(partialTicks2: Float, useFOVSetting: Boolean, cir: CallbackInfoReturnable<Float>) {
        if (useFOVSetting) {
            Fov.getFOVModifierNoDynamicFov(cir)
        }
    }

    @Inject(method = ["applyBobbing"], at = [At("HEAD")], cancellable = true)
    private fun cancelBobbing(partialTicks2: Float, ci: CallbackInfo) {
        if (NoRender.isEnabled && NoRender.noBobbing) {
            ci.cancel()
        }
    }

    @ModifyVariable(method = ["updateRenderer"], at = At(value = "STORE", ordinal = 0), ordinal = 0)
    fun `updateRenderer$STORE$float$0$0`(value: Float): Float {
        return Fov.getMouseSensitivity(value)
    }

    @ModifyVariable(method = ["updateCameraAndRender"], at = At(value = "STORE", ordinal = 0), ordinal = 0)
    fun `updateCameraAndRender$STORE$float$0$0`(value: Float): Float {
        return Fov.getMouseSensitivity(value)
    }

    @Inject(method = ["updateLightmap"], at = [At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/DynamicTexture;updateDynamicTexture()V", shift = At.Shift.BEFORE)])
    fun tintWorld(partialTicks2: Float, ci: CallbackInfo) {
        if (Ambiance.isEnabled && Ambiance.isTintWorldEnabled()) {
            for (i in field_78504_Q.indices) {
                val tintColor = Ambiance.theWorldTintColor
                val color = field_78504_Q[i]
                val tintAlpha = tintColor shr 24 and 0xFF
                val tintRed = tintColor shr 16 and 0xFF
                val tintGreen = tintColor shr 8 and 0xFF
                val tintBlue = tintColor and 0xFF
                val alpha = color shr 24 and 0xFF
                val red = color shr 16 and 0xFF
                val green = color shr 8 and 0xFF
                val blue = color and 0xFF
                if (Ambiance.shouldUseSaturation()) {
                    val saturation2 = Ambiance.saturationValue
                    var r = red.toFloat() / 255.0f
                    var g = green.toFloat() / 255.0f
                    var b = blue.toFloat() / 255.0f
                    val luminance = 0.299f * r + 0.587f * g + 0.114f * b
                    r = luminance + saturation2 * (r - luminance)
                    g = luminance + saturation2 * (g - luminance)
                    b = luminance + saturation2 * (b - luminance)
                    val newRed = (Math.min(r * (tintRed.toFloat() / 255.0f) * 255.0f, 255.0f)).toInt()
                    val newGreen = (Math.min(g * (tintGreen.toFloat() / 255.0f) * 255.0f, 255.0f)).toInt()
                    val newBlue = (Math.min(b * (tintBlue.toFloat() / 255.0f) * 255.0f, 255.0f)).toInt()
                    field_78504_Q[i] = alpha shl 24 or (newRed shl 16) or (newGreen shl 8) or newBlue
                    continue
                }
                val modifier = tintAlpha.toFloat() / 255.0f
                val values = Vector3f(red.toFloat() / 255.0f, green.toFloat() / 255.0f, blue.toFloat() / 255.0f)
                val newValues = Vector3f(tintRed.toFloat() / 255.0f, tintGreen.toFloat() / 255.0f, tintBlue.toFloat() / 255.0f)
                val finalValues = MathUtils.mix(values, newValues, modifier)
                val newRed = (finalValues.x * 255.0f).toInt()
                val newGreen = (finalValues.y * 255.0f).toInt()
                val newBlue = (finalValues.z * 255.0f).toInt()
                field_78504_Q[i] = alpha shl 24 or (newRed shl 16) or (newGreen shl 8) or newBlue
            }
        }
    }
}
