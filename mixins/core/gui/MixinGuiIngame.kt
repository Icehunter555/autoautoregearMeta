package dev.wizard.meta.mixins.core.gui

import dev.wizard.meta.module.modules.render.CrossHair
import dev.wizard.meta.module.modules.render.GuiAnimation
import dev.wizard.meta.module.modules.render.NoRender
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiIngame
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumHandSide
import net.minecraft.util.ResourceLocation
import org.spongepowered.asm.mixin.Final
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.Redirect
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(GuiIngame::class)
abstract class MixinGuiIngame : Gui() {
    companion object {
        @Shadow
        @Final
        @JvmStatic
        protected lateinit var field_110330_c: ResourceLocation
    }

    @Shadow
    @Final
    protected lateinit var field_73839_d: Minecraft

    @Shadow
    protected var field_92017_k = 0

    @Shadow
    protected abstract fun func_184044_a(var1: Int, var2: Int, var3: Float, var4: EntityPlayer, var5: ItemStack)

    @Inject(method = ["renderAttackIndicator"], at = [At("HEAD")], cancellable = true)
    protected fun renderAttackIndicator(partialTicks2: Float, p_184045_2_: ScaledResolution, ci: CallbackInfo) {
        if (CrossHair.isEnabled) {
            ci.cancel()
        }
    }

    @Inject(method = ["renderExpBar"], at = [At("HEAD")], cancellable = true)
    protected fun renderEXPBar(scaledRes: ScaledResolution, x: Int, ci: CallbackInfo) {
        if (NoRender.isEnabled && NoRender.hideExperienceBar) {
            ci.cancel()
        }
    }

    @Inject(method = ["renderSelectedItem"], at = [At("HEAD")], cancellable = true)
    fun renderSelectedItem(scaledRes: ScaledResolution, ci: CallbackInfo) {
        if (NoRender.isEnabled && NoRender.hideSelectedItemName) {
            ci.cancel()
        }
    }

    @Redirect(method = ["renderSelectedItem"], at = At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getDisplayName()Ljava/lang/String;"))
    fun cancelSelectedItemName(instance: ItemStack): String {
        if (NoRender.isEnabled && NoRender.hideSelectedItemName) {
            return ""
        }
        return instance.displayName
    }

    @Inject(method = ["renderHotbar"], at = [At("HEAD")], cancellable = true)
    fun `renderHotbar$INJECT$HEAD`(sr: ScaledResolution, partialTicks2: Float, ci: CallbackInfo) {
        if (GuiAnimation.isEnabled) {
            ci.cancel()
            val renderViewEntity = this.field_73839_d.renderViewEntity
            if (renderViewEntity is EntityPlayer) {
                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
                this.field_73839_d.textureManager.bindTexture(field_110330_c)
                val itemstack = renderViewEntity.heldItemOffhand
                val enumhandside = renderViewEntity.primaryHand.opposite()
                val i = sr.scaledWidth / 2
                val f = this.zLevel
                val x = GuiAnimation.updateHotbar()
                this.zLevel = -90.0f
                this.drawTexturedModalRect(i - 91, sr.scaledHeight - 22, 0, 0, 182, 22)
                this.drawTexturedModalRect((i - 91 - 1).toFloat() + x, (sr.scaledHeight - 22 - 1).toFloat(), 0f, 22f, 24, 22)
                if (!itemstack.isEmpty) {
                    if (enumhandside == EnumHandSide.LEFT) {
                        this.drawTexturedModalRect(i - 91 - 29, sr.scaledHeight - 23, 24, 22, 29, 24)
                    } else {
                        this.drawTexturedModalRect(i + 91, sr.scaledHeight - 23, 53, 22, 29, 24)
                    }
                }
                this.zLevel = f
                GlStateManager.enableRescaleNormal()
                GlStateManager.enableAlpha()
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO)
                RenderHelper.enableGUIStandardItemLighting()
                for (l in 0..8) {
                    val i1 = i - 90 + l * 20 + 2
                    val j1 = sr.scaledHeight - 16 - 3
                    this.func_184044_a(i1, j1, partialTicks2, renderViewEntity, renderViewEntity.inventory.mainInventory[l])
                }
                if (!itemstack.isEmpty) {
                    val l1 = sr.scaledHeight - 16 - 3
                    if (enumhandside == EnumHandSide.LEFT) {
                        this.func_184044_a(i - 91 - 26, l1, partialTicks2, renderViewEntity, itemstack)
                    } else {
                        this.func_184044_a(i + 91 + 10, l1, partialTicks2, renderViewEntity, itemstack)
                    }
                }
                if (this.field_73839_d.gameSettings.attackIndicator == 2) {
                    val f1 = this.field_73839_d.player.getCooledAttackStrength(0.0f)
                    if (f1 < 1.0f) {
                        val i2 = sr.scaledHeight - 20
                        var j2 = i + 91 + 6
                        if (enumhandside == EnumHandSide.RIGHT) {
                            j2 = i - 91 - 22
                        }
                        this.field_73839_d.textureManager.bindTexture(Gui.ICONS)
                        val k1 = (f1 * 19.0f).toInt()
                        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
                        this.drawTexturedModalRect(j2, i2, 0, 94, 18, 18)
                        this.drawTexturedModalRect(j2, i2 + 18 - k1, 18, 112 - k1, 18, k1)
                    }
                }
                RenderHelper.disableStandardItemLighting()
                GlStateManager.disableRescaleNormal()
                GlStateManager.disableBlend()
            }
        }
    }
}
